@echo off
setlocal

set "BASE_DIR=%~dp0"
set "BASE_DIR=%BASE_DIR:~0,-1%"
set "MVN_VERSION=3.9.9"
set "MVN_DIR=%BASE_DIR%\.mvn\apache-maven-%MVN_VERSION%"
set "MVN_CMD=%MVN_DIR%\bin\mvn.cmd"
set "ZIP_PATH=%BASE_DIR%\.mvn\apache-maven-%MVN_VERSION%-bin.zip"
if "%MVN_DIST_URL%"=="" set "MVN_DIST_URL=https://maven.aliyun.com/repository/public/org/apache/maven/apache-maven/%MVN_VERSION%/apache-maven-%MVN_VERSION%-bin.zip"

if exist "%MVN_CMD%" goto run

if not exist "%BASE_DIR%\.mvn" mkdir "%BASE_DIR%\.mvn"
if not exist "%ZIP_PATH%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%MVN_DIST_URL%' -OutFile '%ZIP_PATH%'"
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%BASE_DIR%\.mvn' -Force"

:run
call "%MVN_CMD%" %*
exit /b %ERRORLEVEL%
