$script:MavenVersion = '3.9.9'

function Test-Jdk([string] $Candidate) {
    if (-not $Candidate) { return $false }
    if (-not (Test-Path "$Candidate\bin\java.exe") -or -not (Test-Path "$Candidate\bin\javac.exe")) { return $false }
    $version = & "$Candidate\bin\javac.exe" -version 2>&1
    return ($LASTEXITCODE -eq 0 -and "$version" -match '^javac (17|21)(\.|$)')
}

function Use-Java {
    foreach ($candidate in @("$PWD\.tools\jdk", $env:JAVA_HOME)) {
        if (Test-Jdk $candidate) {
            $env:JAVA_HOME = $candidate
            $env:PATH = "$candidate\bin;$env:PATH"
            return $true
        }
    }
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) {
        # Java escreve propriedades em stderr; PS 5.1 nao deve tratar isso como falha.
        $savedPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            $settings = & $javaCommand.Source -XshowSettings:properties -version 2>&1
        } finally { $ErrorActionPreference = $savedPreference }
        $line = $settings | Where-Object { "$_" -match '^\s*java.home = ' } | Select-Object -First 1
        if ($line) {
            $candidate = ("$line" -replace '^\s*java.home = ', '').Trim()
            if (Test-Jdk $candidate) {
                $env:JAVA_HOME = $candidate
                $env:PATH = "$candidate\bin;$env:PATH"
                return $true
            }
        }
    }
    return $false
}

function Use-Maven {
    $script:MavenCmd = "$PWD\.tools\apache-maven-$script:MavenVersion\bin\mvn.cmd"
    return (Test-Path -LiteralPath $script:MavenCmd)
}

function Get-Download([string] $Url, [string] $Destination) {
    $oldProgress = $ProgressPreference
    $ProgressPreference = 'SilentlyContinue'
    try {
        for ($attempt = 1; $attempt -le 3; $attempt++) {
            try {
                Invoke-WebRequest -Uri $Url -OutFile "$Destination.part" -UseBasicParsing -TimeoutSec 900
                Move-Item -LiteralPath "$Destination.part" -Destination $Destination -Force
                return
            } catch {
                if ($attempt -eq 3) { throw "Falha no download. Confira internet/proxy e repita configurar: $Url. $($_.Exception.Message)" }
            }
        }
    } finally { $ProgressPreference = $oldProgress }
}

function Test-ArchiveHash([string] $Archive, [string] $ChecksumFile, [string] $Algorithm) {
    $expected = ((Get-Content -LiteralPath $ChecksumFile -Raw).Trim() -split '\s+')[0]
    $actual = (Get-FileHash -LiteralPath $Archive -Algorithm $Algorithm).Hash
    if (-not $expected -or $actual -ne $expected) { throw "Checksum invalido: $Archive. Repita a configuracao." }
}
