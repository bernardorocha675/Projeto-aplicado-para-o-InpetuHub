@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0maven.ps1" %*
exit /b %ERRORLEVEL%
