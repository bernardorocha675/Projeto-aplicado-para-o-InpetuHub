$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
. "$PSScriptRoot\scripts\runtime.ps1"
if ($args.Count -ne 1 -or $args[0] -notin @('verificar', 'recuperar')) { throw 'Use: .\acessos.bat verificar OU .\acessos.bat recuperar' }
$accessMode = if ($args[0] -eq 'verificar') { '--verificar-acessos' } else { '--recuperar-acessos' }
if (-not (Use-Java)) { throw 'Execute primeiro: .\configurar.bat' }
if (-not (Use-Maven)) { throw 'Execute primeiro: .\configurar.bat' }
& $script:MavenCmd -B -ntp -DskipTests clean package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$env:JAVA_HOME\bin\java.exe" -jar target/inpetu-hub-0.4.2.jar $accessMode
exit $LASTEXITCODE
