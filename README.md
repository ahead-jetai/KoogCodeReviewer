# Koog Code Reviewer - ACP Agent

An AI-powered code review agent built using the **Agent Client Protocol (ACP)** for seamless integration with JetBrains IDEs (IntelliJ IDEA, PyCharm, WebStorm, etc.).

## Purpose

This project demonstrates how to build a custom AI agent that integrates directly into JetBrains IDEs using the Agent Client Protocol. The agent acts as an automated code reviewer that can analyze source files for common issues and provide feedback directly in the IDE's AI Assistant chat interface.

## Description

Koog Code Reviewer is a lightweight, server-side AI agent that communicates with IntelliJ IDEA via JSON-RPC 2.0 over stdio. It provides automated code review capabilities by reading files from your project and performing static analysis to identify potential issues such as:

- TODO comments that need attention
- Print statements that should use proper logging
- Potential hardcoded secrets (API keys, tokens)

The agent operates entirely within the IDE, providing a seamless experience where users can simply ask "Review this file" and receive instant feedback.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    JetBrains IDE                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              AI Assistant Chat UI                    │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │ User Messages                       │
│  ┌────────────────────▼─────────────────────────────────┐  │
│  │          ACP Client (IntelliJ Plugin)                │  │
│  │  • Manages agent lifecycle                           │  │
│  │  • Routes messages via JSON-RPC                      │  │
│  │  • Renders responses in UI                           │  │
│  └────────────────────┬─────────────────────────────────┘  │
└─────────────────────┬─┴──────────────────────────────────┘
                      │
                      │ stdio (JSON-RPC 2.0)
                      │
┌─────────────────────▼──────────────────────────────────────┐
│                  Koog Reviewer Agent                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                  Main.kt                             │  │
│  │  • Entry point (acp mode)                            │  │
│  │  • CLI mode for testing                              │  │
│  └────────────────────┬─────────────────────────────────┘  │
│  ┌────────────────────▼─────────────────────────────────┐  │
│  │              AcpServer.kt                            │  │
│  │  • Protocol handler                                  │  │
│  │  • Session management                                │  │
│  │  • Message routing                                   │  │
│  └──────────┬─────────────────────────┬─────────────────┘  │
│             │                         │                     │
│  ┌──────────▼───────────┐  ┌─────────▼──────────────────┐ │
│  │  ReadFileTool.kt     │  │  SimpleLinterTool.kt       │ │
│  │  • Read file content │  │  • Scan for TODOs          │ │
│  │  • Error handling    │  │  • Check for print stmts   │ │
│  │                      │  │  • Detect hardcoded secrets│ │
│  └──────────────────────┘  └────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Communication Flow

1. **Initialization**: IDE sends `initialize` request with client capabilities
2. **Session Setup**: Agent creates a session for the conversation
3. **Prompt Turn**:
   - User sends message via `session/prompt`
   - Agent processes and sends `session/update` notifications
   - Agent responds with final result and `stopReason`
4. **Tool Execution**: Agent uses ReadFileTool and SimpleLinterTool as needed

### Key Components

- **Main.kt**: Entry point that handles both ACP mode and CLI mode for testing
- **AcpServer.kt**: Core protocol implementation handling JSON-RPC messages
- **AcpProtocol.kt**: Data classes for protocol message serialization
- **ReadFileTool**: Utility for reading file contents from disk
- **SimpleLinterTool**: Static analysis engine for detecting code issues

## Technologies Used

### Core Technologies
- **Kotlin 2.1.0**: Primary programming language
- **Gradle**: Build system with Kotlin DSL
- **Agent Client Protocol (ACP)**: Communication protocol for IDE integration

### Libraries & Dependencies
- **kotlinx.serialization**: JSON serialization for protocol messages
- **kotlinx.coroutines**: Asynchronous programming for non-blocking I/O
- **Logback**: Logging framework (configured to log to stderr only)

### Development Tools
- **IntelliJ IDEA**: Primary development environment
- **Python**: Test scripts for protocol validation

## Main Features

### 1. **File Analysis**
Read and analyze source code files from the project directory:
```
"Review src/main/kotlin/com/example/codereview/Main.kt"
```

### 2. **Static Code Analysis**
Detect common code issues:
- **TODO Comments**: Identifies unfinished work that needs attention
- **Print Statements**: Flags `println()` calls that should use proper logging
- **Hardcoded Secrets**: Detects potential API keys, tokens, or passwords

### 3. **ACP Protocol Compliance**
Full implementation of the Agent Client Protocol including:
- ✅ Initialization handshake
- ✅ Session management
- ✅ Proper `session/update` notifications
- ✅ Tool call reporting
- ✅ Cancellation support
- ✅ Stop reasons

### 4. **Dual Operation Modes**

#### ACP Mode (IDE Integration)
```bash
./build/install/koog-reviewer/bin/koog-reviewer acp
```
Runs as an agent connected to the IDE via stdio.

#### CLI Mode (Standalone Testing)
```bash
./build/install/koog-reviewer/bin/koog-reviewer review <file-path>
```
Directly analyzes a file and prints results to console.

### 5. **Real-time Feedback**
Sends progress updates via `session/update` notifications, allowing the IDE to display:
- Agent thinking/processing status
- Tool execution progress
- Results as they become available

## Project Structure

```
koog-reviewer/
├── src/main/
│   ├── kotlin/com/example/codereview/
│   │   ├── Main.kt                      # Entry point
│   │   ├── acp/
│   │   │   ├── AcpServer.kt             # Protocol handler
│   │   │   └── AcpProtocol.kt           # Message data classes
│   │   └── tools/
│   │       ├── ReadFileTool.kt          # File reading utility
│   │       └── SimpleLinterTool.kt      # Code analysis engine
│   └── resources/
│       └── logback.xml                  # Logging configuration
├── build.gradle.kts                     # Build configuration
├── settings.gradle.kts                  # Project settings
├── test_agent_v2.py                     # Protocol test script
├── acp.json                             # Local agent config (project)
├── ~/.jetbrains/acp.json                # Global agent config (IDE)
├── ACP_PROTOCOL.md                      # Protocol documentation
├── PROJECT_SPEC.md                      # Original specification
└── README.md                            # This file
```

## Getting Started

### Prerequisites
- JDK 17 or higher
- Gradle 9.0+ (included via wrapper)
- IntelliJ IDEA 2024.3+ with AI Assistant enabled

### Building the Agent

1. **Clone the repository**
   ```bash
   cd /path/to/koog-reviewer
   ```

2. **Build the distribution**
   ```bash
   ./gradlew installDist
   ```

3. **Locate the executable**
   ```bash
   ls build/install/koog-reviewer/bin/koog-reviewer
   ```

### Configuration

Create or update `~/.jetbrains/acp.json`:

```json
{
  "agent_servers": {
    "Koog Reviewer": {
      "command": "/absolute/path/to/build/install/koog-reviewer/bin/koog-reviewer",
      "args": ["acp"],
      "env": {}
    }
  }
}
```

**Important**: Use an absolute path to the executable.

### Running the Agent

1. **Restart IntelliJ IDEA** to load the agent configuration
2. **Open AI Assistant** (View → Tool Windows → AI Assistant)
3. **Select "Koog Reviewer"** from the agent dropdown
4. **Send a message**:
   - "What can you do?"
   - "Review src/main/kotlin/Main.kt"

### Testing

#### Protocol Test
```bash
python3 test_agent_v2.py
```
Validates the full ACP protocol implementation.

#### CLI Test
```bash
./build/install/koog-reviewer/bin/koog-reviewer review src/main/kotlin/com/example/codereview/Main.kt
```
Tests the code analysis functionality directly.

## How It Works

### Message Flow Example

1. **User asks**: "Review Main.kt"

2. **Agent receives**:
   ```json
   {
     "method": "session/prompt",
     "params": {
       "sessionId": "...",
       "prompt": [{"type": "text", "text": "Review Main.kt"}]
     }
   }
   ```

3. **Agent sends update**:
   ```json
   {
     "method": "session/update",
     "params": {
       "sessionId": "...",
       "update": {
         "sessionUpdate": "message",
         "role": "assistant",
         "content": [{"type": "text", "text": "Analyzing Main.kt..."}]
       }
     }
   }
   ```

4. **Agent executes tools**:
   - ReadFileTool reads the file
   - SimpleLinterTool scans for issues

5. **Agent responds**:
   ```json
   {
     "result": {
       "stopReason": "end_turn",
       "content": [{"type": "text", "text": "Found 2 issues: ..."}]
     }
   }
   ```

## Extending the Agent

### Adding New Tools

1. Create a new tool in `src/main/kotlin/com/example/codereview/tools/`
2. Add tool definition to `handleInitialize()` in `AcpServer.kt`
3. Implement tool execution logic in `handleSessionPrompt()`

### Adding LLM Integration

To make this a true AI agent (not just rule-based), integrate an LLM:

1. Add LLM SDK dependency (e.g., OpenAI, Anthropic, local model)
2. Send user prompt to LLM in `handleSessionPrompt()`
3. Parse LLM response for tool calls
4. Report tool executions back to LLM
5. Stream final response to IDE

## Troubleshooting

### Agent doesn't appear in IDE
- Check `~/.jetbrains/acp.json` uses absolute path
- Restart IntelliJ completely
- Check logs: Help → Show Log in Finder

### Messages not displaying
- Verify logback.xml logs to stderr only
- Ensure `session/update` uses `update` wrapper
- Test with `python3 test_agent_v2.py`

### Build failures
```bash
./gradlew clean build --refresh-dependencies
```

## Resources

- **ACP Protocol Spec**: https://agentclientprotocol.com/
- **Protocol Documentation**: [ACP_PROTOCOL.md](./ACP_PROTOCOL.md)
- **JetBrains AI Assistant**: https://www.jetbrains.com/ai/
- **Project Specification**: [PROJECT_SPEC.md](./PROJECT_SPEC.md)

## License

This project is provided as-is for educational and demonstration purposes.

## Future Enhancements

- [ ] LLM integration for intelligent analysis
- [ ] More sophisticated linting rules
- [ ] Support for multiple programming languages
- [ ] Git integration for reviewing diffs
- [ ] Custom rule configuration
- [ ] Integration with external code analysis tools (SonarQube, etc.)
- [ ] File editing capabilities via ACP
- [ ] Terminal integration for running fixes

---

Built with ❤️ using Kotlin and the Agent Client Protocol
