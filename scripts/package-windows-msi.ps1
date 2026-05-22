param(
    [ValidateSet("release", "debug", "dev")]
    [string]$BuildKind = "release"
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir

switch ($BuildKind) {
    "release" { $GradleTask = ":app:desktop:packageReleaseMsi" }
    "debug" { $GradleTask = ":app:desktop:packageMsi" }
    "dev" { $GradleTask = ":app:desktop:packageMsi" }
    default {
        throw "Unsupported build kind: $BuildKind"
    }
}

if (-not [System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform([System.Runtime.InteropServices.OSPlatform]::Windows)) {
    throw "Windows MSI packaging must run on a Windows host."
}

Write-Host "Packaging Breeze MSI with task $GradleTask"
Push-Location $RepoRoot
try {
    & "$RepoRoot\gradlew.bat" $GradleTask
}
finally {
    Pop-Location
}

$ArtifactRoot = Join-Path $RepoRoot "app\desktop\build\compose\binaries"
Write-Host ""
Write-Host "Build finished. MSI artifacts:"
Get-ChildItem -Path $ArtifactRoot -Recurse -Filter *.msi |
    Sort-Object FullName |
    ForEach-Object { $_.FullName }
