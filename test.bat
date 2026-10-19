@echo off
rem Compiles and runs the JUnit unit tests. Does not start the server or need MongoDB.
setlocal EnableExtensions
cd /d "%~dp0"
call "%~dp0check-environment.bat" /nodb
if errorlevel 1 exit /b 1
echo.
call "%~dp0mvnw.cmd" -B -ntp test
if errorlevel 1 (
  echo.
  echo [FAILED] The build did not compile or at least one test failed. See the output above.
  exit /b 1
)
echo.
echo [PASSED] Build compiled and all unit tests passed.
exit /b 0
