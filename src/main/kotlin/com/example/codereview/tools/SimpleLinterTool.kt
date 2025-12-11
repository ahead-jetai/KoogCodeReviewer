package com.example.codereview.tools

object SimpleLinterTool {
    /**
     * Scans code for basic issues and returns a formatted string of warnings or "No issues found.".
     */
    fun scanCode(content: String): String {
        val warnings = mutableListOf<String>()

        if (Regex("(?m)\\bTODO\\b").containsMatchIn(content)) {
            warnings.add("- Found TODO comment: TODOs indicate unfinished work and should be resolved or tracked.")
        }
        if (Regex("(?m)\\bprintln\\s*\\(").containsMatchIn(content)) {
            warnings.add("- Found print statement: Use a proper logger instead of println for production code.")
        }
        // naive API key pattern example
        if (Regex("""(?i)(api_key|secret|token)\s*[:=]\s*['"][A-Za-z0-9_-]{16,}['"]""").containsMatchIn(content)) {
            warnings.add("- Possible hardcoded secret: Avoid committing API keys or tokens. Use environment variables or a secure vault.")
        }

        return if (warnings.isEmpty()) "No issues found." else warnings.joinToString("\n")
    }
}
