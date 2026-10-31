@echo off
REM ============================================================================
REM Build script for Blood Donor Network (No Maven required)
REM Compiles all Java files in src/main/java for Java 25 using javac and lib/*.jar into bin/
REM ============================================================================

echo ===================================================
echo   Compiling Blood Donor Network Project...
echo ===================================================

REM Ensure bin directory exists
if not exist "bin" mkdir bin

REM Gather all .java source files across all packages
dir /s /b src\main\java\*.java > sources.txt

REM Compile using javac with the MySQL Connector JAR
javac --release 25 -cp "lib/*" -d bin @sources.txt

if %ERRORLEVEL% equ 0 (
    echo.
    echo [SUCCESS] Compilation successful! Class files are in 'bin/'.
    echo You can now run the project using: run.bat
) else (
    echo.
    echo [ERROR] Compilation failed. Please check the error messages above.
)

REM Clean up temporary sources list
if exist sources.txt del sources.txt

echo.
pause
