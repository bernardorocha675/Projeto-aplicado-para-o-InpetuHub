$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
. "$PSScriptRoot\scripts\runtime.ps1"
if (-not (Use-Java)) { throw 'Execute primeiro: .\configurar.bat' }
if (-not (Use-Maven)) { throw 'Execute primeiro: .\configurar.bat' }
& $script:MavenCmd @args
exit $LASTEXITCODE
