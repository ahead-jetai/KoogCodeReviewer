package com.example.codereview.acp

import com.example.codereview.tools.ReadFileTool
import com.example.codereview.tools.SimpleLinterTool
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AcpServer(
    private val reader: BufferedReader,
    private val writer: BufferedWriter
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
    private val sessions = ConcurrentHashMap<String, SessionState>()
    
    data class SessionState(
        val sessionId: String,
        val messages: MutableList<PromptMessage> = mutableListOf()
    )
    
    suspend fun start() = coroutineScope {
        try {
            while (true) {
                val line = withContext(Dispatchers.IO) {
                    reader.readLine()
                } ?: break
                
                if (line.isBlank()) continue

                try {
                    val request = json.decodeFromString<JsonRpcRequest>(line)
                    handleRequest(request)
                } catch (e: Exception) {
                    System.err.println("Error parsing request: ${e.message}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            System.err.println("Server error: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private suspend fun handleRequest(request: JsonRpcRequest) {
        when (request.method) {
            "initialize" -> handleInitialize(request)
            "session/new" -> handleSessionNew(request)
            "session/prompt" -> handleSessionPrompt(request)
            else -> {
                sendError(request.id, -32601, "Method not found: ${request.method}")
            }
        }
    }
    
    private fun handleInitialize(request: JsonRpcRequest) {
        try {
            val params = if (request.params != null) {
                try {
                    json.decodeFromJsonElement<InitializeParams>(request.params)
                } catch (e: Exception) {
                    // If params is empty or missing required fields, use defaults
                    InitializeParams(
                        capabilities = ClientCapabilities(),
                        clientInfo = ClientInfo(name = "unknown", version = "unknown")
                    )
                }
            } else {
                // No params provided, use defaults
                InitializeParams(
                    capabilities = ClientCapabilities(),
                    clientInfo = ClientInfo(name = "unknown", version = "unknown")
                )
            }

            val result = InitializeResult(
                protocolVersion = 1,
                capabilities = ServerCapabilities(
                    tools = listOf(
                        ToolDefinition(
                            name = "readFile",
                            description = "Reads text content from a local file path relative to the project root. Returns file contents or an error message if the file cannot be read.",
                            inputSchema = buildJsonObject {
                                put("type", "object")
                                putJsonObject("properties") {
                                    putJsonObject("path") {
                                        put("type", "string")
                                        put("description", "The file path relative to the project root")
                                    }
                                }
                                putJsonArray("required") {
                                    add("path")
                                }
                            }
                        ),
                        ToolDefinition(
                            name = "scanCode",
                            description = "Scans code for basic style violations like TODOs, print statements, or hardcoded secrets. Returns a formatted list of warnings or 'No issues found.'",
                            inputSchema = buildJsonObject {
                                put("type", "object")
                                putJsonObject("properties") {
                                    putJsonObject("code") {
                                        put("type", "string")
                                        put("description", "The source code content to scan")
                                    }
                                }
                                putJsonArray("required") {
                                    add("code")
                                }
                            }
                        )
                    )
                ),
                serverInfo = ServerInfo(
                    name = "koog-reviewer",
                    version = "1.0.0"
                )
            )
            
            sendResponse(request.id, json.encodeToJsonElement(result))
        } catch (e: Exception) {
            System.err.println("Initialize error: ${e.message}")
            e.printStackTrace()
            sendError(request.id, -32602, "Invalid params: ${e.message}")
        }
    }
    
    private fun handleSessionNew(request: JsonRpcRequest) {
        try {
            val sessionId = UUID.randomUUID().toString()
            sessions[sessionId] = SessionState(sessionId)
            
            val result = SessionNewResult(sessionId = sessionId)
            sendResponse(request.id, json.encodeToJsonElement(result))
        } catch (e: Exception) {
            sendError(request.id, -32603, "Internal error: ${e.message}")
        }
    }
    
    private suspend fun handleSessionPrompt(request: JsonRpcRequest) = coroutineScope {
        try {
            val params = request.params?.let {
                json.decodeFromJsonElement<SessionPromptParams>(it)
            } ?: throw IllegalArgumentException("Missing params")

            val session = sessions[params.sessionId]
                ?: throw IllegalArgumentException("Session not found: ${params.sessionId}")

            // Extract text content from prompt message parts
            val userMessage = params.prompt
                .filter { it.type == "text" }
                .mapNotNull { it.text }
                .joinToString(" ")

            // Build response with explicit description of capabilities
            val responseMessage = """
                I'm a code review agent with the following capabilities:

                Available Tools:
                * readFile - Read source code files from your project
                * scanCode - Analyze code for common issues (TODOs, print statements, hardcoded secrets)

                How to use me:
                Ask me to review a file by providing its path, for example:
                "Review src/main/kotlin/com/example/codereview/Main.kt"

                I'll read the file and check for:
                - TODO comments that need attention
                - Print statements that should use logging
                - Potential hardcoded secrets

                What would you like me to review?
            """.trimIndent()

            // Send message update notification using proper ACP format
            sendSessionUpdate(params.sessionId, buildJsonObject {
                put("sessionUpdate", "message")
                put("role", "assistant")
                putJsonArray("content") {
                    addJsonObject {
                        put("type", "text")
                        put("text", responseMessage)
                    }
                }
            })

            // Send final result
            sendResponse(request.id, buildJsonObject {
                put("stopReason", "end_turn")
                putJsonArray("content") {
                    addJsonObject {
                        put("type", "text")
                        put("text", responseMessage)
                    }
                }
            })
            
        } catch (e: Exception) {
            System.err.println("Session prompt error: ${e.message}")
            e.printStackTrace()
            sendError(request.id, -32603, "Internal error: ${e.message}")
        }
    }
    
    private fun sendSessionUpdate(sessionId: String, update: JsonObject) {
        val params = buildJsonObject {
            put("sessionId", sessionId)
            put("update", update)
        }

        val notification = JsonRpcNotification(
            method = "session/update",
            params = params
        )

        sendMessage(json.encodeToString(notification))
    }
    
    private fun sendResponse(id: JsonElement, result: JsonElement) {
        val response = JsonRpcResponse(
            id = id,
            result = result
        )
        sendMessage(json.encodeToString(response))
    }
    
    private fun sendError(id: JsonElement, code: Int, message: String) {
        val response = JsonRpcResponse(
            id = id,
            error = JsonRpcError(code = code, message = message)
        )
        sendMessage(json.encodeToString(response))
    }
    
    private fun sendMessage(message: String) {
        synchronized(writer) {
            writer.write(message)
            writer.newLine()
            writer.flush()
        }
    }
}
