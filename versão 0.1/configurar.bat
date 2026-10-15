@echo off
setlocal
set "PSModulePath="
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0configurar.ps1"
exit /b %ERRORLEVEL%
