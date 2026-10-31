@echo off
REM ============================================================================
REM Run script for Blood Donor Network GUI (Swing Desktop Application)
REM Launches com.bloodnetwork.ui.App with bin, lib, and resources on classpath
REM ============================================================================

echo ===================================================
echo   Starting Blood Donor Network (Swing GUI)...
echo ===================================================

REM Check if compiled classes exist
if not exist "bin\com\bloodnetwork\ui\App.class" (
    echo [ERROR] Project is not compiled yet!
    echo Please run build.bat first before launching the GUI.
    pause
    exit /b 1
)

REM Launch the Swing GUI application
start javaw -cp "bin;lib/*;src/main/resources" com.bloodnetwork.ui.App

if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] GUI application launched!
) else (
    REM Fallback to standard java launcher if javaw is unavailable
    java -cp "bin;lib/*;src/main/resources" com.bloodnetwork.ui.App
)
