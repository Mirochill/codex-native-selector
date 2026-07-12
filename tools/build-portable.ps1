param(
    [Parameter(Mandatory = $true)]
    [string]$InstallApp,
    [string]$Destination = (Join-Path (Split-Path -Parent $PSScriptRoot) 'outputs\Codex-Native-Selector')
)

$ErrorActionPreference = 'Stop'
$workspace = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$destinationFull = [System.IO.Path]::GetFullPath($Destination)
$installFull = [System.IO.Path]::GetFullPath($InstallApp)
if (-not (Test-Path -LiteralPath $installFull)) { throw "Codex app directory not found: $installFull" }
$sourceArchive = Join-Path $installFull 'resources\app.asar'
if (-not (Test-Path -LiteralPath $sourceArchive)) { throw "Codex app.asar not found: $sourceArchive" }
if (Test-Path -LiteralPath $destinationFull) { Remove-Item -LiteralPath $destinationFull -Recurse -Force }
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
Push-Location $workspace
try { & $node $builder $sourceArchive $asarPath $selector $extracted; if ($LASTEXITCODE -ne 0) { throw 'The modified app.asar could not be created.' } } finally { Pop-Location }
$launcher = @"
@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Launch Codex Native Selector.ps1"
"@
Set-Content -LiteralPath (Join-Path $destinationFull 'Launch Codex Native Selector.cmd') -Value $launcher -Encoding ascii
$launcherPs = @"
`$ErrorActionPreference = 'Stop'
`$root = Split-Path -Parent `$MyInvocation.MyCommand.Path
`$exe = Join-Path `$root 'app\ChatGPT.exe'
`$other = @(Get-CimInstance Win32_Process -Filter "Name = 'ChatGPT.exe'" -ErrorAction SilentlyContinue | Where-Object { `$_.ExecutablePath -ne `$exe })
if (`$other.Count -gt 0) {
    Add-Type -AssemblyName PresentationFramework
    [System.Windows.MessageBox]::Show("Ferme complètement Codex officiel avant de lancer cette copie portable.", 'Codex Native Selector', 'OK', 'Information') | Out-Null
    exit 2
}
`$env:CODEX_SPARKLE_ENABLED = 'false'
Start-Process -FilePath `$exe -WorkingDirectory (Split-Path -Parent `$exe)
"@
Set-Content -LiteralPath (Join-Path $destinationFull 'Launch Codex Native Selector.ps1') -Value $launcherPs -Encoding utf8
$files = Get-ChildItem -LiteralPath $destinationFull -File -Recurse
[pscustomobject]@{ Destination = $destinationFull; FileCount = $files.Count; LogicalBytes = ($files | Measure-Object Length -Sum).Sum; AsarBytes = (Get-Item -LiteralPath $asarPath).Length }
