@echo off
rem Lightweight Maven bootstrap (a simplified stand-in for the official Maven Wrapper).
rem 1) uses Maven from PATH if present, 2) otherwise downloads Apache Maven once into .mvn\ and uses that.
setlocal EnableExtensions
set "MVN_VERSION=3.9.9"
set "BASE=%~dp0"
set "MVN_HOME=%BASE%.mvn\apache-maven-%MVN_VERSION%"

where mvn >nul 2>nul
if not errorlevel 1 goto usepath
if exist "%MVN_HOME%\bin\mvn.cmd" goto runlocal

echo [mvnw] Apache Maven %MVN_VERSION% is not installed locally. Downloading it once ^(internet required^) ...
if not exist "%BASE%.mvn" mkdir "%BASE%.mvn"
powershell -NoProfile -Command "try { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVN_VERSION%/apache-maven-%MVN_VERSION%-bin.zip' -OutFile '%BASE%.mvn\maven.zip'; Expand-Archive -Force -Path '%BASE%.mvn\maven.zip' -DestinationPath '%BASE%.mvn'; exit 0 } catch { Write-Host $_.Exception.Message; exit 1 }"
if errorlevel 1 (
  echo [mvnw] Download failed. Check your internet connection, or install Maven from https://maven.apache.org and add it to PATH.
  exit /b 1
)
del "%BASE%.mvn\maven.zip" >nul 2>nul
if not exist "%MVN_HOME%\bin\mvn.cmd" (
  echo [mvnw] Maven was downloaded but could not be unpacked.
  exit /b 1
)

:runlocal
call "%MVN_HOME%\bin\mvn.cmd" %*
exit /b %errorlevel%

:usepath
call mvn %*
exit /b %errorlevel%
