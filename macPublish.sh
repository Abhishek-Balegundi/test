#!/bin/bash

if ! JAVA_HOME=/Applications/SimplifyQA-CodeEditor.app/Contents/Resources/app/resources/jdk/Contents/Home /Applications/SimplifyQA.app/Contents/dist/agent/libs/apache-maven-3.9.9/bin/mvn compile exec:java -Pgit-release; then
    echo -e "${OS/Darwin/\033[1m\033[31m}=============================== GIT RELEASE FAILED ====================================="
    echo -e "${OS/Darwin/\033[1m\033[31m}=============================== REFER THE LOGS ========================================="
else
    echo -e "${OS/Darwin/\033[1m\033[32m}============================================== GIT RELEASE SUCCESSFULL ==============================================="
fi
