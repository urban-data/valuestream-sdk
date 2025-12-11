#!/bin/bash

./gradlew uninstallAll clean installDebug && adb shell am start -n com.example.app/.MainActivity