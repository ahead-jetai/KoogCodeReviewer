# Agent Client Protocol (ACP) Implementation Guide

This document provides essential information about implementing the Agent Client Protocol (ACP) for building agents that integrate with JetBrains IDEs.

## Table of Contents
- [Overview](#overview)
- [Protocol Flow](#protocol-flow)
- [Session Updates](#session-updates)
- [Tool Calls](#tool-calls)
- [Prompt Turn Lifecycle](#prompt-turn-lifecycle)
- [Stop Reasons](#stop-reasons)
- [Cancellation](#cancellation)

## Overview

The Agent Client Protocol (ACP) is a JSON-RPC 2.0-based protocol that enables AI agents to communicate with IDE clients. The protocol operates over stdio (standard input/output).

**Key Requirements:**
- All logs must go to stderr, never stdout
- Stdout is reserved exclusively for JSON-RPC protocol messages
- Each message must be a single line of JSON followed by a newline

## Protocol Flow

### 1. Initialization

The client sends an `initialize` request:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": 1,
    "clientCapabilities": {
      "fs": {
        "readTextFile": true,
        "writeTextFile": true
      },
      "terminal": false
    }
  }
}
```

The agent responds with its capabilities:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": 1,
    "capabilities": {
      "tools": [
        {
          "name": "readFile",
          "description": "Reads a file from disk",
          "inputSchema": {
            "type": "object",
            "properties": {
              "path": {
                "type": "string",
                "description": "File path"
              }
            },
            "required": ["path"]
          }
        }
      ]
    },
    "serverInfo": {
      "name": "my-agent",
      "version": "1.0.0"
    }
  }
}
```

### 2. Session Setup

Create a new session:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "session/new",
  "params": {
    "cwd": "/path/to/project",
    "mcpServers": []
  }
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "sessionId": "sess_abc123def456"
  }
}
```

### 3. Prompt Turn

Client sends a user message:

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "session/prompt",
  "params": {
    "sessionId": "sess_abc123def456",
    "prompt": [
      {
        "type": "text",
        "text": "Review this file for issues"
      }
    ]
  }
}
```

## Session Updates

**CRITICAL:** Session updates MUST use the correct format with an `update` field containing a `sessionUpdate` type.

### Message Updates

Send text responses via `session/update` notifications:

```json
{
  "jsonrpc": "2.0",
  "method": "session/update",
  "params": {
    "sessionId": "sess_abc123def456",
    "update": {
      "sessionUpdate": "message",
      "role": "assistant",
      "content": [
        {
          "type": "text",
          "text": "I'll analyze this file for you..."
        }
      ]
    }
  }
}
```

**Key Points:**
- Use `"sessionUpdate": "message"` for text responses
- Include `"role": "assistant"`
- Content is an array of content blocks
- This is a notification (no `id` field)

### Agent Message Chunks

For streaming responses:

```json
{
  "jsonrpc": "2.0",
  "method": "session/update",
  "params": {
    "sessionId": "sess_abc123def456",
    "update": {
      "sessionUpdate": "agent_message_chunk",
      "content": {
        "type": "text",
        "text": "Analyzing code..."
      }
    }
  }
}
```

### Plan Updates

Show the agent's execution plan:

```json
{
  "jsonrpc": "2.0",
  "method": "session/update",
  "params": {
    "sessionId": "sess_abc123def456",
    "update": {
      "sessionUpdate": "plan",
      "entries": [
        {
          "content": "Read source file",
          "priority": "high",
          "status": "pending"
        },
        {
          "content": "Run static analysis",
          "priority": "medium",
          "status": "pending"
        }
      ]
    }
  }
}
```

## Tool Calls

### Creating Tool Calls

When executing a tool, first report it:

```json
{
  "jsonrpc": "2.0",
  "method": "session/update",
  "params": {
    "sessionId": "sess_abc123def456",
    "update": {
      "sessionUpdate": "tool_call",
      "toolCallId": "call_001",
      "title": "Reading configuration file",
      "kind": "read",
      "status": "pending"
    }
  }
}
```

**Tool Kinds:**
- `read` - Reading files or data
- `edit` - Modifying files or content
- `delete` - Removing files or data
- `move` - Moving or renaming files
- `search` - Searching for information
- `execute` - Running commands or code
- `think` - Internal reasoning or planning
- `fetch` - Retrieving external data
- `other` - Other tool types (default)

### Updating Tool Call Status

```json
{
  "jsonrpc": "2.0",
  "method": "session/update",
  "params": {
    "sessionId": "sess_abc123def456",
    "update": {
      "sessionUpdate": "tool_call_update",
      "toolCallId": "call_001",
      "status": "in_progress"
    }
  }
}
```

### Completing Tool Calls

```json
{
  "jsonrpc": "2.0",
  "method": "session/update",
  "params": {
    "sessionId": "sess_abc123def456",
    "update": {
      "sessionUpdate": "tool_call_update",
      "toolCallId": "call_001",
      "status": "completed",
      "content": [
        {
          "type": "content",
          "content": {
            "type": "text",
            "text": "File read successfully. Found 3 issues."
          }
        }
      ]
    }
  }
}
```

**Tool Call Statuses:**
- `pending` - Not started yet
- `in_progress` - Currently executing
- `completed` - Finished successfully
- `failed` - Failed with an error

## Prompt Turn Lifecycle

A complete prompt turn follows this sequence:

1. **Client sends `session/prompt`** with user message
2. **Agent processes** the message with LLM
3. **Agent sends `session/update`** notifications:
   - Optional: Plan update
   - Message chunks or full messages
   - Tool calls (if needed)
4. **Agent executes tools** (if any):
   - May request permission via `session/request_permission`
   - Updates tool status to `in_progress`
   - Executes the tool
   - Updates tool status to `completed` or `failed`
   - Sends results back to LLM
5. **Loop continues** until no more tool calls
6. **Agent responds** to original `session/prompt` with stop reason

### Final Response

After all updates are sent, respond to the original prompt request:

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "stopReason": "end_turn",
    "content": [
      {
        "type": "text",
        "text": "Analysis complete. Found 3 issues."
      }
    ]
  }
}
```

**Note:** The `content` field in the final response is optional since the actual content was already sent via `session/update` notifications.

## Stop Reasons

The agent must specify why a turn ended:

- `end_turn` - LLM finished responding without requesting more tools
- `max_tokens` - Maximum token limit reached
- `max_turn_requests` - Maximum number of model requests exceeded
- `refusal` - Agent refuses to continue
- `cancelled` - Client cancelled the turn

## Cancellation

Clients can cancel an ongoing turn:

```json
{
  "jsonrpc": "2.0",
  "method": "session/cancel",
  "params": {
    "sessionId": "sess_abc123def456"
  }
}
```

**Agent must:**
1. Stop all LLM requests immediately
2. Abort all tool executions
3. Send any pending updates
4. Respond to `session/prompt` with `"stopReason": "cancelled"`

**Important:** Catch any exceptions from aborted operations and return the `cancelled` stop reason instead of an error. This ensures clients can distinguish cancellations from actual errors.

## Implementation Checklist

- [ ] All logs go to stderr only
- [ ] Stdout contains only JSON-RPC messages
- [ ] Each message is a single line with `\n`
- [ ] `session/update` uses `update` field with `sessionUpdate` type
- [ ] Tool calls include `toolCallId`, `title`, `kind`, and `status`
- [ ] Final response includes `stopReason`
- [ ] Cancellation is handled gracefully
- [ ] Protocol version is 1
- [ ] Server info includes `name` and `version`

## Common Mistakes

1. ❌ **Sending updates without `update` wrapper**
   ```json
   // WRONG
   {"sessionId": "...", "type": "message", "content": {...}}
   ```

   ✅ **Correct format**
   ```json
   {"sessionId": "...", "update": {"sessionUpdate": "message", ...}}
   ```

2. ❌ **Logging to stdout**
   - All logs must go to stderr
   - Use logback.xml to configure logging properly

3. ❌ **Missing sessionUpdate field**
   - Always include `"sessionUpdate": "message"` or appropriate type

4. ❌ **Not sending updates before final response**
   - Send `session/update` notifications first
   - Then send the final JSON-RPC response

## Resources

- Full Protocol Spec: https://agentclientprotocol.com/
- Protocol Reference: https://agentclientprotocol.com/llms.txt
- Example Implementation: See this project's `src/main/kotlin/com/example/codereview/acp/`

## Testing

Test your agent using the included Python test script:

```bash
python3 test_agent_v2.py
```

This validates:
- Initialization handshake
- Session creation
- Prompt handling
- Message format correctness
