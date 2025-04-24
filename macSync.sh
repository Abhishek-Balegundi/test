
if ! JAVA_HOME=/Applications/SimplifyQA-CodeEditor.app/Contents/Resources/app/resources/jdk/Contents/Home /Applications/SimplifyQA-CodeEditor.app/Contents/Resources/app/resources/maven/bin/mvn compile exec:java -Psync; then
    echo -e "${OS/Darwin/\033[1m\033[31m}============================ COULD NOT SYNC.. REFER ABOVE ERROR/INFO IN LOGS ===================================="
else
    echo -e "${OS/Darwin/\033[1m\033[32m}================================ SYNC ACTION REQUEST SUCCESSFULL ==============================="
fi
