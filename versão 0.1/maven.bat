@echo off
setlocal
set "PSModulePath="
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0maven.ps1" %*
exit /b %ERRORLEVEL%
