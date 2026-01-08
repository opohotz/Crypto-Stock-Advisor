#!/bin/bash

# CryptoAdvisor JavaFX Application Runner
# This script can be run from anywhere and will find the correct directory

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

# Change to the frontend directory (where this script is located)
cd "$SCRIPT_DIR"

echo "Running CryptoAdvisor JavaFX Application..."
echo "Current directory: $(pwd)"

# Run the JavaFX application
mvn javafx:run
