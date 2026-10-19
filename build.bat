@echo off
rem Cleans, compiles, runs the unit tests and creates target\polling-voting-system.jar
setlocal EnableExtensions
cd /d "%~dp0"
call "%~dp0check-environment.bat" /nodb
if errorlevel 1 exit /b 1
echo.
call "%~dp0mvnw.cmd" -B -ntp clean package
if errorlevel 1 (
  echo.
  echo [FAILED] The build failed. See the output above.
  exit /b 1
)
echo.
echo [SUCCESS] Build finished: target\polling-voting-system.jar
exit /b 0
