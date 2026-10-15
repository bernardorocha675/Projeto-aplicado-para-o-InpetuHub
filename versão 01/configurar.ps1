$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
. "$PSScriptRoot\scripts\runtime.ps1"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$null = New-Item -ItemType Directory -Force -Path '.tools', 'data'
if (-not (Use-Java)) {
    $machine = $env:PROCESSOR_ARCHITEW6432
    if (-not $machine) { $machine = $env:PROCESSOR_ARCHITECTURE }
    switch ($machine) {
        'AMD64' { $architecture = 'x64' }
        'ARM64' { $architecture = 'aarch64' }
        default { throw "Arquitetura nao suportada: $machine. Instale JDK 17 ou 21." }
    }
    Write-Host 'Baixando JDK 17 (Temurin) para .tools, sem alterar o sistema...'
    $metadata = '.tools\jdk-metadata.json'
    Get-Download "https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=$architecture&image_type=jdk&os=windows&vendor=eclipse" $metadata
    $releases = Get-Content -LiteralPath $metadata -Raw | ConvertFrom-Json
    $package = @($releases)[0].binary.package
    if (-not $package.link -or -not $package.checksum) { throw 'A API nao retornou um JDK compativel.' }
    Get-Download $package.link '.tools\jdk.zip'
    Set-Content -Path '.tools\jdk.sha256' -Value $package.checksum -Encoding ASCII
    Test-ArchiveHash '.tools\jdk.zip' '.tools\jdk.sha256' 'SHA256'
    if (Test-Path '.tools\jdk-stage') { Remove-Item '.tools\jdk-stage' -Recurse -Force }
    Expand-Archive -LiteralPath '.tools\jdk.zip' -DestinationPath '.tools\jdk-stage'
    $jdkRoot = Get-ChildItem '.tools\jdk-stage' -Directory | Select-Object -First 1
    if (-not $jdkRoot) { throw 'Arquivo JDK sem pasta de instalacao.' }
    if (Test-Path '.tools\jdk') { Remove-Item '.tools\jdk' -Recurse -Force }
    Move-Item -LiteralPath $jdkRoot.FullName -Destination '.tools\jdk'
    Remove-Item '.tools\jdk-stage' -Recurse -Force
    Remove-Item '.tools\jdk.zip', '.tools\jdk.sha256', $metadata
    if (-not (Use-Java)) { throw 'Nao foi possivel executar o JDK baixado.' }
}
Write-Host "Java: $env:JAVA_HOME"
if (-not (Use-Maven)) {
    Write-Host "Baixando Maven $script:MavenVersion para .tools..."
    $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$script:MavenVersion/apache-maven-$script:MavenVersion-bin.zip"
    Get-Download $url '.tools\maven.zip'
    Get-Download "$url.sha512" '.tools\maven.sha512'
    Test-ArchiveHash '.tools\maven.zip' '.tools\maven.sha512' 'SHA512'
    if (Test-Path '.tools\maven-stage') { Remove-Item '.tools\maven-stage' -Recurse -Force }
    Expand-Archive -LiteralPath '.tools\maven.zip' -DestinationPath '.tools\maven-stage'
    $installPath = ".tools\apache-maven-$script:MavenVersion"
    if (Test-Path $installPath) { Remove-Item $installPath -Recurse -Force }
    Move-Item -LiteralPath ".tools\maven-stage\apache-maven-$script:MavenVersion" -Destination '.tools'
    Remove-Item '.tools\maven-stage' -Recurse -Force
    Remove-Item '.tools\maven.zip', '.tools\maven.sha512'
    if (-not (Use-Maven)) { throw 'Falha ao preparar Maven.' }
}
if (-not (Test-Path '.env.local')) { Copy-Item '.env.local.example' '.env.local' }
Write-Host 'Baixando dependencias, compilando e executando os testes...'
Remove-Item '.tools\ready' -ErrorAction SilentlyContinue
& $script:MavenCmd -B -ntp clean verify
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Set-Content -Path '.tools\ready' -Value 'ok' -Encoding ASCII
Write-Host 'Configuracao concluida. Para iniciar: .\iniciar.bat'
Write-Host 'O banco em data/ sera criado automaticamente na primeira inicializacao.'
