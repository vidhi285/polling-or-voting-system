@echo off
rem Checks everything run.bat needs. Usage: check-environment.bat [/nodb]
rem   /nodb  skips the MongoDB and port checks (used by test.bat and build.bat)
setlocal EnableExtensions
cd /d "%~dp0"
set "FAIL=0"
set "NODB=0"
if /i "%~1"=="/nodb" set "NODB=1"

echo.
echo ---- Environment check ----

if not exist "pom.xml" (
  echo [FAIL] pom.xml not found. Run this script from the project folder.
  exit /b 1
)

rem ---- Java ----
where java >nul 2>nul
if errorlevel 1 (
  echo [FAIL] Java was not found on PATH.
  echo        Install a JDK 17 or newer, for example Eclipse Temurin from https://adoptium.net
  echo        then close and reopen this terminal.
  exit /b 1
)
set "JVER="
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do if not defined JVER set "JVER=%%~g"
set "JMAJOR=0"
for /f "delims=.-_+ tokens=1" %%a in ("%JVER%") do set "JMAJOR=%%a"
if "%JMAJOR%"=="1" set "JMAJOR=8"
set /a JNUM=%JMAJOR% 2>nul
if %JNUM% LSS 17 (
  echo [FAIL] Java %JVER% detected, but this project needs Java 17 or newer.
  echo        Install a newer JDK and make sure it is first on PATH.
  exit /b 1
)
echo [ OK ] Java %JVER%
where javac >nul 2>nul
if errorlevel 1 (
  echo [FAIL] Java was found but javac was not. A JDK is required, not just a JRE.
  exit /b 1
)

rem ---- Maven ----
where mvn >nul 2>nul
if not errorlevel 1 (
  echo [ OK ] Maven found on PATH
) else (
  if exist "%~dp0mvnw.cmd" (
    echo [ OK ] Using mvnw.cmd. It downloads Apache Maven once into .mvn\ and needs internet on the first run.
  ) else (
    echo [FAIL] Neither Maven nor mvnw.cmd was found.
    exit /b 1
  )
)

if "%NODB%"=="1" goto done

rem ---- MongoDB ----
if defined MONGODB_URI (
  echo [INFO] MONGODB_URI is set. The application will verify that connection when it starts.
) else (
  call :tcp 127.0.0.1 27017
  if errorlevel 1 (
    echo [FAIL] MongoDB is not reachable at 127.0.0.1:27017.
    echo        Start the MongoDB service: press Win+R, run services.msc, find "MongoDB Server",
    echo        right-click it and choose Start. Or in an Administrator terminal: net start MongoDB
    echo        If MongoDB uses another address, set MONGODB_URI first, for example:
    echo        $env:MONGODB_URI="mongodb://127.0.0.1:27018"
    set "FAIL=1"
  ) else (
    echo [ OK ] MongoDB is reachable at 127.0.0.1:27017
  )
)

rem ---- HTTP port ----
set "PORT=8080"
if defined SERVER_PORT set "PORT=%SERVER_PORT%"
call :tcp 127.0.0.1 %PORT%
if not errorlevel 1 (
  echo [FAIL] Port %PORT% is already in use, so a second instance cannot start.
  echo        Find the program using it:   netstat -ano ^| findstr :%PORT%
  echo        Stop it with:                taskkill /PID ^<pid^> /F
  echo        Or use another port:         $env:SERVER_PORT="8081"
  set "FAIL=1"
) else (
  echo [ OK ] Port %PORT% is free
)

:done
if "%FAIL%"=="1" exit /b 1
echo ---- Environment looks good ----
exit /b 0

:tcp
powershell -NoProfile -Command "$c = New-Object System.Net.Sockets.TcpClient; try { $a = $c.BeginConnect('%~1', %~2, $null, $null); if ($a.AsyncWaitHandle.WaitOne(2000) -and $c.Connected) { exit 0 } else { exit 1 } } catch { exit 1 } finally { $c.Close() }"
exit /b %errorlevel%
