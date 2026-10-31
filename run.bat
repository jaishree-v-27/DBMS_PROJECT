@echo off
REM ============================================================================
REM Run script for Blood Donor Network (No Maven required)
REM Launches the application using java with bin, lib, and resources on classpath
REM ============================================================================

echo ===================================================
echo   Starting Blood Donor Network...
echo ===================================================

REM Check if compiled classes exist
if not exist "bin\com\bloodnetwork\Main.class" (
    echo [ERROR] Project is not compiled yet!
    echo Please run build.bat first before running this script.
    pause
    exit /b 1
)

REM Execute the Main console menu
java -cp "bin;lib/*;src/main/resources" com.bloodnetwork.Main

if %ERRORLEVEL% neq 0 (
    echo.
    echo [NOTE] If you saw a database connection error, ensure:
    echo  1. MySQL Server is running.
    echo  2. 'src/main/resources/db.properties' has your correct MySQL password.
)

pause
