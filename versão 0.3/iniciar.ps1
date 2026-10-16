$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
. "$PSScriptRoot\scripts\runtime.ps1"
if (-not (Use-Java)) { throw 'Execute primeiro: .\configurar.bat' }
if (-not (Use-Maven)) { throw 'Execute primeiro: .\configurar.bat' }
if (-not (Test-Path '.env.local') -or -not (Test-Path '.tools\ready')) {
    throw 'A configuracao ainda nao terminou. Execute: .\configurar.bat'
}
$updateRunner = Join-Path $PSScriptRoot ('.tools\Atualizador-' + [guid]::NewGuid().ToString() + '.java')
Copy-Item -LiteralPath "$PSScriptRoot\scripts\Atualizador.java" -Destination $updateRunner
try {
    & "$env:JAVA_HOME\bin\java.exe" '-Dfile.encoding=UTF-8' --source 17 $updateRunner
    $updateStatus = $LASTEXITCODE
} finally { Remove-Item -LiteralPath $updateRunner -Force }
if ($updateStatus -eq 10) {
    $previousUpdate = $env:INPETU_AUTO_UPDATE
    try {
        $env:INPETU_AUTO_UPDATE = '0'
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\iniciar.ps1" @args
        exit $LASTEXITCODE
    } finally { $env:INPETU_AUTO_UPDATE = $previousUpdate }
}
if ($updateStatus -ne 0) { throw 'Atualizacao interrompida. Consulte ATUALIZACOES.md.' }
& $script:MavenCmd -B -ntp -DskipTests clean package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Iniciando InPETU. Abra http://localhost:8080 (ou a PORT de .env.local). Ctrl+C para parar.'
& "$env:JAVA_HOME\bin\java.exe" -jar target/inpetu-hub-0.3.3.jar --spring.profiles.active=local @args
exit $LASTEXITCODE
