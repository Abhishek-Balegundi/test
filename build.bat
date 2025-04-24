@echo off
setlocal

REM Set the paths for Maven and Java
set "MAVEN_HOME=C:\Users\abhishek.m\AppData\Local\Programs\SimplifyQA-CodeEditor\resources\app\resources\maven\bin"
set "JAVA_HOME=C:\Users\abhishek.m\AppData\Local\Programs\SimplifyQA-CodeEditor\resources\app\resources\jdk"

REM Prepend Maven and Java to PATH
set "PATH=%JAVA_HOME%;%MAVEN_HOME%;%PATH%"

REM Check for OS type
set "OS=%OS%"

REM Run Maven build
call mvn install

REM Check if Maven build was successful
if %ERRORLEVEL% NEQ 0 (
    if "%OS%"=="Windows_NT" (
        echo [1m[31m--------------- BUILD FAILED DURING MAVEN INSTALL ---------------[0m
    ) else (
        echo -e "\033[1m\033[31m--------------- BUILD FAILED DURING MAVEN INSTALL ---------------\033[0m"
    )
    endlocal
    exit /b 1
)

REM Print the prompt message in yellow
if "%OS%"=="Windows_NT" (
    REM Enable ANSI escape codes in Windows Terminal
    reg add "HKCU\Console" /v VirtualTerminalLevel /t REG_DWORD /d 1 /f >nul 2>&1
    echo [1m[31mPLEASE SELECT: [1m[32mDO YOU WANT TO USE LOCAL JAR PRESENT IN TARGET FOLDER FOR EXECUTION? ENTER [1m[31mY [1m[32m^(for yes^) OR [1m[31mN [1m[32m^(for no^): [0m
) else (
    REM Print prompt message for UNIX-like systems
    echo -e "\033[1m\033[31mPLEASE SELECT: \033[1m\033[32mDO YOU WANT TO USE LOCAL JAR PRESENT IN TARGET FOLDER FOR EXECUTION? ENTER \033[1m\033[31mY \033[1m\033[32m(for yes) OR \033[1m\033[31mN \033[1m\033[32m(for no): \033[0m"
)

REM Prompt the user to use local jar
set "userInput="
set /p "userInput="

REM Validate user input and set the useLocalJar value
if /I "%userInput%"=="Y" (
    set "useLocalJar=true"
) else if /I "%userInput%"=="N" (
    set "useLocalJar=false"
) else (
    if "%OS%"=="Windows_NT" (
        echo [1m[31m--------------- INVALID INPUT ---------------[0m
        echo [1m[31m--------------- BUILD FAILED ---------------[0m
    ) else (
        echo -e "\033[1m\033[31m--------------- INVALID INPUT ---------------\033[0m"
        echo -e "\033[1m\033[31m--------------- BUILD FAILED ---------------\033[0m"
    )
    endlocal
    exit /b 1
)

REM Create a temporary file to store updated configuration
set "tempConfig=temp_configuration.properties"

REM Read the current configuration and update useLocalJar key
for /f "tokens=1,* delims==" %%A in (configuration.properties) do (
    if /I "%%A"=="useLocalJar" (
        echo useLocalJar=%useLocalJar%>> "%tempConfig%"
    ) else (
        echo %%A=%%B>> "%tempConfig%"
    )
)

REM If the useLocalJar key wasn't found, append it
findstr /i "useLocalJar" configuration.properties >nul || echo useLocalJar=%useLocalJar%>> "%tempConfig%"

REM Replace the original configuration file with the updated one
move /Y "%tempConfig%" configuration.properties >nul

REM Inform the user about the recorded input
if "%useLocalJar%"=="true" (
    if "%OS%"=="Windows_NT" (
        echo [1m[32m============== Recorded input as YES, hence we'll use jar from target directory. ==============[0m
    ) else (
        echo -e "\033[1m\033[32m============== Recorded input as Yes ==============\033[0m"
    )
) else (
    if "%OS%"=="Windows_NT" (
        echo [1m[32m============== Recorded input as NO, hence we'll download the jar from cloud and use it. ==============[0m
    ) else (
        echo -e "\033[1m\033[32m============== Recorded input as No ==============\033[0m"
    )
)

REM Store the exit code of the previous command
set "BUILD_SUCCESS=1"

if "%OS%"=="Windows_NT" (
    REM Enable ANSI escape codes in Windows Terminal
    echo [1m[33m============== WAIT!!  UNIQUE ID VALIDATION IS IN-PROGRESS ==============[0m
) else (
    REM Print wait message for UNIX-like systems
    echo -e "\033[1m\033[33m============== WAIT!!  UNIQUE ID VALIDATION IS IN-PROGRESS ==============\033[0m"
)

mvn exec:java -Pduplicate-checker
set "EXIT_CODE=%ERRORLEVEL%"

if %EXIT_CODE% NEQ 0 (
    if "%OS%"=="Windows_NT" (
        echo [1m[31m--------------- BUILD FAILED ---------------[0m
        echo [1m[31m-------- SYNC WILL FAIL UNTIL UNIQUE IDs ARE CORRECTED --------[0m
    ) else (
        echo -e "\033[1m\033[31m--------------- BUILD FAILED ---------------\033[0m"
        echo -e "\033[1m\033[31m-------- Sync will fail until unique Ids are corrected --------\033[0m"
    )
) else (
    if "%OS%"=="Windows_NT" (
        echo [1m[32m--------------- BUILD SUCCESS ---------------[0m
        echo [1m[32m--------- NO DUPLICATE UNIQUE ID IS FOUND. GOOD TO GO. ---------[0m
    ) else (
        echo -e "\033[1m\033[32m--------------- BUILD SUCCESS ---------------\033[0m"
        echo -e "\033[1m\033[32m--------- NO DUPLICATE UNIQUE ID IS FOUND. GOOD TO GO. ---------\033[0m"
    )
)

endlocal
