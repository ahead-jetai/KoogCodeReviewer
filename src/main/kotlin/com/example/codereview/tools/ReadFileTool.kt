package com.example.codereview.tools

import java.nio.file.Files
import java.nio.file.Path

object ReadFileTool {
    /**
     * Reads text content from a local file path relative to the project root.
     * Returns a clean error message string if file not found or unreadable.
     */
    fun readFile(path: String): String {
        return try {
            val p = Path.of(path)
            if (!Files.exists(p)) "Error: File not found at '$path'"
            else if (!Files.isRegularFile(p)) "Error: Path is not a regular file: '$path'"
            else Files.readString(p)
        } catch (e: Exception) {
            "Error: Unable to read file '$path': ${e.message}"
        }
    }
}
