param(
    [Parameter(Mandatory = $false)]
    [string]$InstallApp,
    [string]$Destination
)

$ErrorActionPreference = 'Stop'
$selectorPatchVersion = '9'
$workspace = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ([string]::IsNullOrWhiteSpace($Destination)) {
    $Destination = Join-Path $workspace 'outputs\Codex-Native-Selector'
}
$destinationFull = [System.IO.Path]::GetFullPath($Destination)

function Resolve-CodexAppPath {
    param([Parameter(Mandatory = $true)][string]$Candidate)

    $candidateFull = [System.IO.Path]::GetFullPath($Candidate)
    foreach ($path in @($candidateFull, (Join-Path $candidateFull 'app'))) {
        if (Test-Path -LiteralPath (Join-Path $path 'resources\app.asar')) { return $path }
    }

    throw "Codex app directory not found: $candidateFull"
}

$sourcePackage = Get-AppxPackage -Name OpenAI.Codex -ErrorAction SilentlyContinue |
    Sort-Object Version -Descending |
    Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($InstallApp)) {
    if ($null -eq $sourcePackage) { throw 'The Microsoft Store Codex package was not found.' }
    $installFull = Resolve-CodexAppPath $sourcePackage.InstallLocation
} else {
    $installFull = Resolve-CodexAppPath $InstallApp
}

$sourceArchive = Join-Path $installFull 'resources\app.asar'
if (-not (Test-Path -LiteralPath $sourceArchive)) { throw "Codex app.asar not found: $sourceArchive" }
if (Test-Path -LiteralPath (Join-Path $destinationFull 'app')) {
    Remove-Item -LiteralPath (Join-Path $destinationFull 'app') -Recurse -Force
}
$portableApp = Join-Path $destinationFull 'app'
$portableResources = Join-Path $portableApp 'resources'
New-Item -ItemType Directory -Path $portableResources -Force | Out-Null
Get-ChildItem -LiteralPath $installFull -File | ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $portableApp $_.Name) }
Get-ChildItem -LiteralPath $installFull -Directory | Where-Object { $_.Name -ne 'resources' } | ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination $portableApp -Recurse }
$installedResources = Join-Path $installFull 'resources'
Get-ChildItem -LiteralPath $installedResources -File | Where-Object { $_.Name -ne 'app.asar' } | ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $portableResources $_.Name) }
Get-ChildItem -LiteralPath $installedResources -Directory | ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination $portableResources -Recurse }
$asarPath = Join-Path $portableResources 'app.asar'
$builder = Join-Path $PSScriptRoot 'build-inplace-asar.mjs'
$selector = Join-Path $PSScriptRoot 'selector-v2.js.txt'
$extracted = Join-Path $workspace 'work\asar-extracted'
$node = (Get-Command node -ErrorAction SilentlyContinue).Source
if (-not $node) { throw 'Node.js 20 or newer is required.' }
$asar = Join-Path $workspace 'node_modules\.bin\asar.cmd'
if (-not (Test-Path -LiteralPath $asar)) { throw "Electron ASAR tooling not found: $asar" }
if (Test-Path -LiteralPath $extracted) { Remove-Item -LiteralPath $extracted -Recurse -Force }
& $asar extract $sourceArchive $extracted
if ($LASTEXITCODE -ne 0) { throw 'The current Codex app.asar could not be extracted.' }
Push-Location $workspace
try { & $node $builder $sourceArchive $asarPath $selector $extracted; if ($LASTEXITCODE -ne 0) { throw 'The modified app.asar could not be created.' } } finally { Pop-Location }
$sourceVersion = if ($null -ne $sourcePackage) {
    $sourcePackage.Version.ToString()
} else {
    (Get-Item -LiteralPath $sourceArchive).LastWriteTimeUtc.ToString('O')
}
$metadata = [ordered]@{
    SourceVersion = $sourceVersion
    PatchVersion = $selectorPatchVersion
    SourceInstall = $installFull
    BuiltAtUtc = [DateTime]::UtcNow.ToString('O')
}
$metadata | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $destinationFull 'Codex-Native-Selector.source.json') -Encoding utf8
$launcher = @"
@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Launch Codex Native Selector.ps1"
"@
Set-Content -LiteralPath (Join-Path $destinationFull 'Launch Codex Native Selector.cmd') -Value $launcher -Encoding ascii
$launcherPs = @"
`$ErrorActionPreference = 'Stop'
`$root = Split-Path -Parent `$MyInvocation.MyCommand.Path
`$exe = Join-Path `$root 'app\ChatGPT.exe'
`$running = @(Get-CimInstance Win32_Process -Filter "Name = 'ChatGPT.exe'" -ErrorAction SilentlyContinue)
if (`$running.Count -gt 0) {
    Add-Type -AssemblyName PresentationFramework
    [System.Windows.MessageBox]::Show("Ferme complètement Codex officiel avant de lancer cette copie portable.", 'Codex Native Selector', 'OK', 'Information') | Out-Null
    exit 2
}

function Normalize-Version([string]`$value) {
    return `$value -replace '\.0`$',''
}

`$package = Get-AppxPackage -Name OpenAI.Codex -ErrorAction SilentlyContinue |
    Sort-Object Version -Descending |
    Select-Object -First 1
if (`$null -eq `$package) { throw 'The Microsoft Store Codex package was not found.' }
`$metadataPath = Join-Path `$root 'Codex-Native-Selector.source.json'
`$metadata = if (Test-Path -LiteralPath `$metadataPath) { Get-Content -LiteralPath `$metadataPath -Raw | ConvertFrom-Json } else { `$null }
`$latestVersion = Normalize-Version `$package.Version.ToString()
if (`$null -eq `$metadata -or `$metadata.PatchVersion -ne '$selectorPatchVersion' -or (Normalize-Version `$metadata.SourceVersion) -ne `$latestVersion) {
    `$repo = Split-Path -Parent (Split-Path -Parent `$root)
    `$builder = Join-Path `$repo 'tools\build-portable.ps1'
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File `$builder -InstallApp `$package.InstallLocation -Destination `$root
    if (`$LASTEXITCODE -ne 0) { throw 'Codex Native Selector could not be refreshed.' }
}
`$profile = Join-Path `$env:APPDATA 'Codex\web\Codex\Default'
foreach (`$cacheName in @('Cache', 'Code Cache')) {
    `$cache = Join-Path `$profile `$cacheName
    if (Test-Path -LiteralPath `$cache) { Remove-Item -LiteralPath `$cache -Recurse -Force -ErrorAction SilentlyContinue }
}
`$env:CODEX_SPARKLE_ENABLED = 'false'
Start-Process -FilePath `$exe -WorkingDirectory (Split-Path -Parent `$exe)
"@
Set-Content -LiteralPath (Join-Path $destinationFull 'Launch Codex Native Selector.ps1') -Value $launcherPs -Encoding utf8
$files = Get-ChildItem -LiteralPath $destinationFull -File -Recurse
[pscustomobject]@{ Destination = $destinationFull; FileCount = $files.Count; LogicalBytes = ($files | Measure-Object Length -Sum).Sum; AsarBytes = (Get-Item -LiteralPath $asarPath).Length }
