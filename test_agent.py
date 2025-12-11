#!/usr/bin/env python3
"""
Test script to interact with the ACP agent over stdio
"""
import subprocess
import json
import time
import sys

def send_and_receive(process, request):
    """Send a JSON-RPC request and read the response"""
    request_str = json.dumps(request)
    print(f"\n>>> Sending: {request_str}")
    process.stdin.write(request_str + "\n")
    process.stdin.flush()

    # Give it time to process
    time.sleep(0.5)

# Start the agent
agent_path = "./build/install/koog-reviewer/bin/koog-reviewer"
process = subprocess.Popen(
    [agent_path, "acp"],
    stdin=subprocess.PIPE,
    stdout=subprocess.PIPE,
    stderr=subprocess.PIPE,
    text=True,
    bufsize=1
)

print("Agent started. Sending test requests...")

try:
    # Test 1: Initialize
    send_and_receive(process, {
        "jsonrpc": "2.0",
        "id": 1,
        "method": "initialize",
        "params": {
            "capabilities": {},
            "clientInfo": {"name": "test-client", "version": "1.0"}
        }
    })

    # Test 2: Create session
    send_and_receive(process, {
        "jsonrpc": "2.0",
        "id": 2,
        "method": "session/new",
        "params": {}
    })

    # Test 3: Send prompt (try different formats)
    print("\n\n=== Testing different prompt formats ===\n")

    # Format 1: with messages array
    send_and_receive(process, {
        "jsonrpc": "2.0",
        "id": 3,
        "method": "session/prompt",
        "params": {
            "sessionId": "test-session-1",
            "messages": [{"role": "user", "content": "hello"}]
        }
    })

    # Format 2: with single message
    send_and_receive(process, {
        "jsonrpc": "2.0",
        "id": 4,
        "method": "session/prompt",
        "params": {
            "sessionId": "test-session-2",
            "message": {"role": "user", "content": "hello"}
        }
    })

    # Format 3: with prompt field
    send_and_receive(process, {
        "jsonrpc": "2.0",
        "id": 5,
        "method": "session/prompt",
        "params": {
            "sessionId": "test-session-3",
            "prompt": "hello"
        }
    })

    # Wait for processing
    time.sleep(2)

finally:
    # Terminate the agent
    process.terminate()
    process.wait(timeout=2)

    # Print stderr output (where our logs go)
    print("\n\n=== Agent stderr output (logs) ===")
    stderr_output = process.stderr.read()
    print(stderr_output)

    print("\n\n=== Agent stdout output (responses) ===")
    stdout_output = process.stdout.read()
    print(stdout_output)

print("\n\nTest complete!")
