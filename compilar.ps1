param([switch]$Probar)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

function Find-Jdk {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) { return $env:JAVA_HOME }
    $compiler = Get-Command javac -ErrorAction SilentlyContinue
    if ($compiler) { return (Split-Path (Split-Path $compiler.Source -Parent) -Parent) }
    $candidates = @()
    foreach ($base in @("$env:USERPROFILE\.jdks", "$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium")) {
        if (Test-Path $base) { $candidates += Get-ChildItem $base -Directory }
    }
    foreach ($candidate in ($candidates | Sort-Object Name -Descending)) {
        if (Test-Path (Join-Path $candidate.FullName 'bin\javac.exe')) { return $candidate.FullName }
    }
    throw 'No se encontro un JDK. Instala JDK 17 o superior y configura JAVA_HOME. Consulta README.md.'
}

$jdk = Find-Jdk
$classes = Join-Path $root 'target\classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = @(Get-ChildItem (Join-Path $root 'src\main\java') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& (Join-Path $jdk 'bin\javac.exe') --release 17 -encoding UTF-8 -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'La compilacion fallo.' }
$jar = Join-Path $root 'target\panaderia.jar'
& (Join-Path $jdk 'bin\jar.exe') --create --file $jar --main-class pe.panaderia.Main -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el JAR.' }
Write-Host "JAR generado: $jar"

if ($Probar) {
    $testClasses = Join-Path $root 'target\test-classes'
    New-Item -ItemType Directory -Force -Path $testClasses | Out-Null
    $tests = @(Get-ChildItem (Join-Path $root 'src\test\java') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    & (Join-Path $jdk 'bin\javac.exe') --release 17 -encoding UTF-8 -cp $classes -d $testClasses $tests
    if ($LASTEXITCODE -ne 0) { throw 'No se pudieron compilar las pruebas.' }
    & (Join-Path $jdk 'bin\java.exe') -cp "$classes;$testClasses" pe.panaderia.BakeryServiceTest
    if ($LASTEXITCODE -ne 0) { throw 'Las pruebas fallaron.' }
}
