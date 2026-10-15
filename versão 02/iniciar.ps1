$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
. "$PSScriptRoot\scripts\runtime.ps1"
if (-not (Use-Java)) { throw 'Execute primeiro: .\configurar.bat' }
if (-not (Use-Maven)) { throw 'Execute primeiro: .\configurar.bat' }
if (-not (Test-Path '.env.local') -or -not (Test-Path '.tools\ready')) {
    throw 'A configuracao ainda nao terminou. Execute: .\configurar.bat'
}
& $script:MavenCmd -B -ntp -DskipTests clean package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Iniciando InPETU. Abra http://localhost:8080 (ou a PORT de .env.local). Ctrl+C para parar.'
& "$env:JAVA_HOME\bin\java.exe" -jar target/inpetu-hub-2.0.0.jar --spring.profiles.active=local @args
exit $LASTEXITCODE
