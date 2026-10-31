@echo off
REM ============================================================================
REM Launch script for BloodDonorNetwork-Web (Spring Boot)
REM Reads user and password from db.properties if environment variables are not set
REM ============================================================================
cd /d "%~dp0"

REM Spring Boot is compiled for Java 25; require a Java 25+ runtime.
for /f "tokens=3" %%V in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr "java.specification.version"') do set "JAVA_SPEC_VERSION=%%V"
if not defined JAVA_SPEC_VERSION (
    echo [ERROR] Java 25 or newer is required. Install a JDK and add java.exe to PATH.
    exit /b 1
)
if %JAVA_SPEC_VERSION% LSS 25 (
    echo [ERROR] Java 25 or newer is required. Detected Java %JAVA_SPEC_VERSION%.
    exit /b 1
)

IF "%DB_PASSWORD%"=="" (
    IF EXIST "src\main\resources\db.properties" (
        FOR /F "tokens=1,2 delims==" %%A IN (src\main\resources\db.properties) DO (
            IF "%%A"=="db.user" IF "%DB_USER%"=="" SET DB_USER=%%B
            IF "%%A"=="db.password" IF "%DB_PASSWORD%"=="" SET DB_PASSWORD=%%B
        )
    )
)

IF "%DB_USER%"=="" SET DB_USER=root
IF "%DB_PASSWORD%"=="" SET DB_PASSWORD=root

echo ===================================================
echo   Starting Blood Donor Network Web Application...
echo   Database User: %DB_USER%
echo   URL: http://localhost:8080
echo ===================================================

REM Open default browser after a brief delay
start "" cmd /c "timeout /t 5 >nul && start http://localhost:8080"

REM Launch Spring Boot application with live reload enabled
call mvnw.cmd spring-boot:run -Dspring-boot.run.addResources=true
