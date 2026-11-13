#!/bin/bash
echo "=============================================================="
echo "       Compiling and Launching Smart Bill Splitter            "
echo "=============================================================="
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"
echo "Compiling Java source files..."
javac billsplitter/*.java
if [ $? -ne 0 ]; then
    echo "[ERROR] Compilation failed. Please ensure JDK 11+ is installed."
    exit 1
fi
echo "Launching Smart Bill Splitter..."
java billsplitter.Main "$@"
