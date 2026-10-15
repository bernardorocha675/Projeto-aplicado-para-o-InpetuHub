$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
. "$PSScriptRoot\scripts\runtime.ps1"
if (-not (Use-Java)) { throw 'Execute primeiro: .\configurar.bat' }
& "$env:JAVA_HOME\bin\java.exe" '-Dfile.encoding=UTF-8' --source 17 scripts/Backup.java @args
exit $LASTEXITCODE
