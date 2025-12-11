#!/bin/bash

# Test script to manually interact with the ACP agent

echo "Starting agent and sending test messages..."
echo ""

# Start the agent in the background
./build/install/koog-reviewer/bin/koog-reviewer acp 2>agent_stderr.log &
AGENT_PID=$!

# Give it a moment to start
sleep 1

echo "Agent started with PID: $AGENT_PID"
echo "Logs will be written to agent_stderr.log"
echo ""

# Send initialize request
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | nc localhost -

# Wait a bit
sleep 1

# Send session/new request
echo '{"jsonrpc":"2.0","id":2,"method":"session/new","params":{}}' | nc localhost -

# Wait a bit
sleep 1

# Send session/prompt request with different possible formats
echo '{"jsonrpc":"2.0","id":3,"method":"session/prompt","params":{"sessionId":"test-session","prompt":"hello"}}' | nc localhost -

# Give it time to process
sleep 2

# Kill the agent
kill $AGENT_PID 2>/dev/null

echo ""
echo "Check agent_stderr.log for the raw requests and responses"
