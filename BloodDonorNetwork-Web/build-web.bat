@echo off
REM ============================================================================
REM Build script for BloodDonorNetwork-Web (Spring Boot)
REM ============================================================================
cd /d "%~dp0"
echo ===================================================
echo   Building BloodDonorNetwork-Web (Spring Boot)...
echo ===================================================

call mvnw.cmd clean compile

if %ERRORLEVEL% equ 0 (
    echo.
    echo [SUCCESS] Spring Boot Web application compiled successfully!
    echo You can now launch it using: run-web.bat
) else (
    echo.
    echo [ERROR] Build failed. Please inspect the output above.
)

pause
