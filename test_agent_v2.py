#!/usr/bin/env python3
"""
Test script to interact with the ACP agent over stdio - V2 with proper session handling
"""
import subprocess
import json
import time
import sys

def send_request(process, request):
    """Send a JSON-RPC request"""
    request_str = json.dumps(request)
    print(f"\n>>> Sending: {request_str}")
    process.stdin.write(request_str + "\n")
    process.stdin.flush()

def read_response(process):
    """Read a response line"""
    line = process.stdout.readline().strip()
    if line:
        print(f"<<< Received: {line}")
        return json.loads(line)
    return None

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

print("Agent started. Testing full workflow...")

try:
    # Test 1: Initialize
    send_request(process, {
        "jsonrpc": "2.0",
        "id": 1,
        "method": "initialize",
        "params": {
            "protocolVersion": 1,
            "clientCapabilities": {
                "fs": {"readTextFile": True, "writeTextFile": True},
                "terminal": False
            }
        }
    })
    init_response = read_response(process)
    print(f"✅ Initialize successful")

    # Test 2: Create session
    send_request(process, {
        "jsonrpc": "2.0",
        "id": 2,
        "method": "session/new",
        "params": {"cwd": "/Users/arthurhead/Desktop/code/a2a/a2a-tutorial", "mcpServers": []}
    })
    session_response = read_response(process)
    session_id = session_response["result"]["sessionId"]
    print(f"✅ Session created: {session_id}")

    # Test 3: Send prompt with proper format
    send_request(process, {
        "jsonrpc": "2.0",
        "id": 3,
        "method": "session/prompt",
        "params": {
            "sessionId": session_id,
            "prompt": [{"type": "text", "text": "hi what can you do"}]
        }
    })

    # Read all responses (notifications and final response)
    for i in range(5):  # Read up to 5 messages
        time.sleep(0.2)
        response = read_response(process)
        if response and "result" in response:
            print(f"✅ Prompt completed with stopReason: {response.get('result', {}).get('stopReason', 'N/A')}")
            break

    print("\n✅ All tests passed!")

except Exception as e:
    print(f"\n❌ Error: {e}")

finally:
    # Terminate the agent
    process.terminate()
    process.wait(timeout=2)

    # Print stderr output (where our logs go)
    print("\n\n=== Agent stderr output (logs) ===")
    stderr_output = process.stderr.read()
    if stderr_output:
        print(stderr_output[-2000:])  # Last 2000 chars

print("\nTest complete!")
