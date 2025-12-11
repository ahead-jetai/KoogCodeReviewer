package com.example.codereview

import com.example.codereview.acp.AcpServer
import com.example.codereview.tools.ReadFileTool
import com.example.codereview.tools.SimpleLinterTool
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private const val SYSTEM_PROMPT: String =
    "You are a senior code reviewer using the Agent Client Protocol (ACP). " +
            "You must use the readFile tool to inspect files before answering. " +
            "When you find issues using the scanCode tool, explain why they are bad practice."

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "acp" -> runBlocking {
            startAcpServer()
        }
        "review" -> {
            val path = args.getOrNull(1)
            if (path.isNullOrBlank()) {
                printUsageAndExit()
                return
            }
            val content = ReadFileTool.readFile(path)
            if (content.startsWith("Error:")) {
                println(content)
                return
            }
            val result = SimpleLinterTool.scanCode(content)
            println("System Prompt: \n$SYSTEM_PROMPT\n")
            println("Analysis of $path:\n$result")
        }
        else -> {
            printUsageAndExit()
        }
    }
}

private suspend fun startAcpServer() {
    // Create buffered readers/writers for stdio
    val reader = BufferedReader(InputStreamReader(System.`in`))
    val writer = BufferedWriter(OutputStreamWriter(System.out))

    // Create and start the ACP server
    val server = AcpServer(reader, writer)
    server.start()
}

private fun printUsageAndExit() {
    println(
        """
        Usage:
          koog-reviewer acp
              Start the agent in ACP (Agent Client Protocol) mode.

          koog-reviewer review <path>
              Read the file at <path> and report simple linter findings.
        """.trimIndent()
    )
}
