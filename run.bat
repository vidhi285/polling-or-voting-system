@echo off
rem One command: checks the environment, compiles, runs the unit tests, then starts the server.
setlocal EnableExtensions
cd /d "%~dp0"
title Online Polling and Voting System

echo ==================================================
echo  Online Polling and Voting System
echo ==================================================

call "%~dp0check-environment.bat"
if errorlevel 1 (
  echo.
  echo [STOPPED] Fix the problem above, then run .\run.bat again.
  exit /b 1
)

echo.
echo [1/2] Compiling and running the unit tests.
echo       The first run downloads dependencies, so internet access is needed once.
call "%~dp0mvnw.cmd" -B -ntp package
if errorlevel 1 (
  echo.
  echo [FAILED] The build or a unit test failed. The server was NOT started.
  echo          Read the messages above; failing tests are listed under "Tests run".
  exit /b 1
)
if not exist "target\polling-voting-system.jar" (
  echo [FAILED] target\polling-voting-system.jar was not produced.
  exit /b 1
)

echo.
echo [2/2] Starting the server. Press Ctrl+C to stop it.
echo.
java -jar "target\polling-voting-system.jar"
set "EXITCODE=%errorlevel%"
if not "%EXITCODE%"=="0" (
  echo.
  echo [STOPPED] The application exited with code %EXITCODE%. See the message above.
)
exit /b %EXITCODE%
