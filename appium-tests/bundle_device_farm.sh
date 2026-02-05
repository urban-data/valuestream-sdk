#!/bin/bash
set -e

cd "$(dirname "$0")"

if [ -z "$1" ]; then
    echo "Usage: ./bundle_device_farm.sh <test_file.py>"
    echo "Available tests:"
    ls test_*.py
    exit 1
fi

TEST_NAME="$1"
ZIP_NAME="${TEST_NAME%.py}.zip"

if [ ! -f "$TEST_NAME" ]; then
    echo "Error: $TEST_NAME not found"
    exit 1
fi

# Clean previous
rm -rf tests "$ZIP_NAME"

# Create structure
mkdir -p tests

# Copy test files
cp "$TEST_NAME" tests/
cp conftest.py tests/

# Create the zip (deps installed from PyPI by testspec)
zip -r "$ZIP_NAME" tests/ testspec.yml requirements.txt

# Cleanup intermediate files
rm -rf tests

echo "Created $ZIP_NAME"
