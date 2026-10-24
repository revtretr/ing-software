param([string]$Destino = (Join-Path $PSScriptRoot '..\Entregables'), [switch]$Probar)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'compilar.ps1') -Probar:$Probar
$jdk = Find-Jdk
$jpackage = Join-Path $jdk 'bin\jpackage.exe'
if (-not (Test-Path $jpackage)) { throw 'Este JDK no incluye jpackage. Usa un JDK 17 completo.' }
$inputDirectory = Join-Path $PSScriptRoot 'target\package-input'
New-Item -ItemType Directory -Force -Path $inputDirectory | Out-Null
Copy-Item (Join-Path $PSScriptRoot 'target\panaderia.jar') $inputDirectory -Force
New-Item -ItemType Directory -Force -Path $Destino | Out-Null
if (Test-Path (Join-Path $Destino 'Panaderia')) { throw 'Ya existe Panaderia en el destino. Elige otra carpeta con -Destino.' }
& $jpackage --type app-image --name Panaderia --app-version 1.0.0 --vendor 'Panaderia' --description 'Gestion local de pedidos y ventas' --input $inputDirectory --main-jar panaderia.jar --main-class pe.panaderia.Main --add-modules java.base,java.desktop --java-options '-Dfile.encoding=UTF-8' --dest $Destino
if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar la aplicacion portable.' }
Write-Host "Aplicacion Windows: $Destino\Panaderia\Panaderia.exe"
