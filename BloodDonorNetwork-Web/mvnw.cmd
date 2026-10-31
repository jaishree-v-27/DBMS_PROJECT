@echo off
REM ============================================================================
REM Maven Wrapper for BloodDonorNetwork-Web
REM Uses local portable Maven distribution
REM ============================================================================
set DIR=%~dp0
"%DIR%.tools\apache-maven-3.9.6\bin\mvn.cmd" %*
