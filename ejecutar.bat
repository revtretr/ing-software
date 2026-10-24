@echo off
setlocal
cd /d "%~dp0"
set "JAVA_CMD="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_CMD for /f "delims=" %%J in ('where java 2^>nul') do if not defined JAVA_CMD set "JAVA_CMD=%%J"
if not defined JAVA_CMD for /d %%J in ("%USERPROFILE%\.jdks\*" "%ProgramFiles%\Java\*" "%ProgramFiles%\Eclipse Adoptium\*") do if exist "%%~J\bin\java.exe" set "JAVA_CMD=%%~J\bin\java.exe"
if not defined JAVA_CMD (
    echo Instala Java 17 o superior, o configura JAVA_HOME. Consulta README.md.
    pause
    exit /b 1
)
if not exist "target\panaderia.jar" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0compilar.ps1"
    if errorlevel 1 (
        pause
        exit /b 1
    )
)
"%JAVA_CMD%" -jar "target\panaderia.jar"
if errorlevel 1 pause
