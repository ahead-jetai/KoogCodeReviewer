package com.example.codereview.acp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// JSON-RPC 2.0 Base Types
@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: JsonElement,
    val method: String,
    val params: JsonObject? = null
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: JsonElement,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcNotification(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: JsonObject
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

// ACP Initialize
@Serializable
data class InitializeParams(
    val capabilities: ClientCapabilities,
    val clientInfo: ClientInfo
)

@Serializable
data class ClientCapabilities(
    val tools: ToolCapabilities? = null
)

@Serializable
data class ToolCapabilities(
    val supported: List<String> = emptyList()
)

@Serializable
data class ClientInfo(
    val name: String,
    val version: String
)

@Serializable
data class InitializeResult(
    val protocolVersion: Int,
    val capabilities: ServerCapabilities,
    val serverInfo: ServerInfo
)

@Serializable
data class ServerCapabilities(
    val tools: List<ToolDefinition> = emptyList()
)

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)

@Serializable
data class ServerInfo(
    val name: String,
    val version: String
)

// ACP Session
@Serializable
data class SessionNewParams(
    val sessionId: String? = null
)

@Serializable
data class SessionNewResult(
    val sessionId: String
)

@Serializable
data class SessionPromptParams(
    val sessionId: String,
    val prompt: List<MessagePart> = emptyList()
)

@Serializable
data class MessagePart(
    val type: String, // "text", "image", etc.
    val text: String? = null,
    val data: String? = null,
    val mimeType: String? = null
)

@Serializable
data class PromptMessage(
    val role: String,
    val content: String
)

@Serializable
data class SessionUpdateParams(
    val sessionId: String,
    val update: JsonObject
)

// Message update for text responses
@Serializable
data class MessageUpdate(
    val sessionUpdate: String = "message",
    val role: String = "assistant",
    val content: List<ContentBlock>
)

@Serializable
data class ContentBlock(
    val type: String = "text",
    val text: String
)
