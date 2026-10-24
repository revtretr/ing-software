@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0compilar.ps1" %*
if errorlevel 1 (
    echo No se pudo compilar. Revisa el mensaje anterior.
    pause
    exit /b 1
)
pause
