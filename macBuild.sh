#!/bin/bash

# Check for OS type
OS=$(uname)

# Run Maven install
if ! JAVA_HOME=/Applications/SimplifyQA-CodeEditor.app/Contents/Resources/app/resources/jdk/Contents/Home /Applications/SimplifyQA.app/Contents/dist/agent/libs/apache-maven-3.9.9/bin/mvn install; then
    echo -e "${OS/Darwin/\033[1m\033[31m}--------------- BUILD FAILED DURING MAVEN INSTALL ---------------\033[0m"
    exit 1
fi

# Prompt message for local JAR usage
echo -e "${OS/Darwin/\033[1m\033[31m}PLEASE SELECT: DO YOU WANT TO USE LOCAL JAR PRESENT IN TARGET FOLDER FOR EXECUTION? ENTER \033[1m\033[31mY \033[1m\033[32m(for yes) OR \033[1m\033[31mN \033[1m\033[32m(for no): \033[0m"
read -r userInput

# Convert userInput to lowercase
userInput=$(echo "$userInput" | tr '[:upper:]' '[:lower:]')

# Validate user input and set useLocalJar variable
case "$userInput" in
    y) 
        useLocalJar='true' 
        ;;
    n) 
        useLocalJar='false' 
        ;;
    *) 
        echo -e "${OS/Darwin/\033[1m\033[31m}--------------- INVALID INPUT ---------------\033[0m\n--------------- BUILD FAILED ---------------"
        exit 1 
        ;;
esac

# Debugging: Output the value of useLocalJar
echo "Debug: useLocalJar is set to: $useLocalJar"

# Directly write the useLocalJar value to the configuration file
if grep -q "useLocalJar" configuration.properties; then
    # Update existing entry
    sed -i.bak "s/^useLocalJar=.*/useLocalJar=$useLocalJar/" configuration.properties
else
    # Append if it doesn't exist
    echo "useLocalJar=$useLocalJar" >> configuration.properties
fi

# Inform the user about the recorded input
if [[ "$useLocalJar" == 'true' ]]; then
    echo -e "${OS/Darwin/\033[1m\033[32m}============== Recorded input as YES, hence we'll use jar from target directory. ============== \033[0m"
else
    echo -e "${OS/Darwin/\033[1m\033[32m}============== Recorded input as NO, hence we'll download the jar from cloud and use it. ============== \033[0m"
fi

# Print waiting message for unique ID validation
echo -e "${OS/Darwin/\033[1m\033[33m}============== WAIT!!  UNIQUE ID VALIDATION IS IN-PROGRESS ============== \033[0m"

# Run Maven compile and exec:java command
if ! JAVA_HOME=/Applications/SimplifyQA-CodeEditor.app/Contents/Resources/app/resources/jdk/Contents/Home /Applications/SimplifyQA.app/Contents/dist/agent/libs/apache-maven-3.9.9/bin/mvn compile exec:java -Pduplicate-checker; then
    echo -e "${OS/Darwin/\033[1m\033[31m}--------------- BUILD FAILED ---------------\033[0m\n-------- Sync will fail until unique Ids are corrected --------"
else
    echo -e "${OS/Darwin/\033[1m\033[32m}--------------- BUILD SUCCESS ---------------\033[0m\n--------- NO DUPLICATE UNIQUE ID IS FOUND. GOOD TO GO. ---------"
fi
