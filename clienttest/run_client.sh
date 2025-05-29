#!/bin/bash

# Simple script to compile and run the chat client
echo "=== Chat Client Test Runner ==="
echo "Compiling client test files..."

# Compile the Java files
javac -d . *.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo "Starting chat client..."
    echo "Press Ctrl+C to exit"
    echo "=========================="
    java ChatClient
else
    echo "❌ Compilation failed!"
    exit 1
fi