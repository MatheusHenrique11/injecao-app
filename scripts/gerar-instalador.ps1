# Gera o instalador MSI do Calibra (SPEC 10) e para no primeiro erro:
#   1. testes (mvn verify);
#   2. o app e as bibliotecas (perfil "instalador");
#   3. o Java embutido (jlink): só os módulos que o app usa (jdeps), mais o JavaFX e o idioma pt-BR;
#   4. a conferência do pacote no Java embutido (Inicio --conferir);
#   5. o MSI (jpackage + WiX 3.14), em dist\Calibra-<versão>.msi.
#
# Uso (na raiz do projeto):
#   powershell -ExecutionPolicy Bypass -File scripts\gerar-instalador.ps1
#   ... -VersaoMsi 0.1.99   MSI de teste com outra versão (ensaio de instalar por cima)
#   ... -PularTestes        não roda o mvn verify (só para repetir o empacotamento)
#
# O app instala por usuário, sem precisar de administrador, em %LOCALAPPDATA%\Programs\Calibra. Os dados ficam em
# %LOCALAPPDATA%\Calibra e não são apagados ao desinstalar.
param(
    [string]$VersaoMsi = '',
    [switch]$PularTestes,
    [string]$Jdk = $env:JAVA_HOME,
    [string]$Maven = '',
    [string]$Wix = ''
)

$ErrorActionPreference = 'Stop'

# UpgradeCode fixo do Calibra: é ele que faz o Windows trocar a versão antiga pela nova ao instalar por cima, sem
# duplicar o app em "Aplicativos instalados". Nunca mudar (docs\decisoes.md).
$UpgradeCode = '45107A4F-BD64-4EDB-815D-3611CD926209'

$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

function Passo([string]$texto) {
    Write-Host ''
    Write-Host "==> $texto" -ForegroundColor Cyan
}

function Executar([string]$programa, [string[]]$argumentos) {
    & $programa @argumentos
    if ($LASTEXITCODE -ne 0) {
        throw "Falhou (código $LASTEXITCODE): $programa $($argumentos -join ' ')"
    }
}

# ---------- Ferramentas: JDK 25, Maven e WiX 3.14 ----------
if (-not $Jdk -or -not (Test-Path (Join-Path $Jdk 'bin\jlink.exe'))) {
    $achado = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-25*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if (-not $achado) {
        throw 'JDK 25 não encontrado: informe -Jdk ou defina JAVA_HOME.'
    }
    $Jdk = $achado.FullName
}
$bin = Join-Path $Jdk 'bin'
$env:JAVA_HOME = $Jdk
$env:Path = "$bin;$env:Path"

if (-not $Maven) {
    $comando = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($comando) {
        $Maven = $comando.Source
    } else {
        $pasta = Get-ChildItem (Join-Path $env:USERPROFILE 'tools') -Directory -Filter 'apache-maven-3.9*' `
            -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
        if (-not $pasta) {
            throw 'Maven 3.9 não encontrado: informe -Maven ou ponha o mvn no PATH.'
        }
        $Maven = Join-Path $pasta.FullName 'bin\mvn.cmd'
    }
}

if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue)) {
    if (-not $Wix) {
        $Wix = Join-Path $env:USERPROFILE 'tools\wix314'
    }
    if (-not (Test-Path (Join-Path $Wix 'candle.exe'))) {
        throw "WiX 3.14 não encontrado em $Wix (informe -Wix com a pasta do candle.exe e do light.exe)."
    }
    $env:Path = "$Wix;$env:Path"
}

[xml]$pom = Get-Content (Join-Path $raiz 'pom.xml') -Encoding UTF8
$versaoPom = $pom.project.version
$versao = if ($VersaoMsi) { $VersaoMsi } else { $versaoPom -replace '-SNAPSHOT$', '' }
if ($versaoPom -like '*-SNAPSHOT') {
    Write-Warning "Versão de desenvolvimento ($versaoPom): o MSI sai como $versao."
}
Write-Host "Calibra $versaoPom · MSI $versao · JDK $Jdk"

# ---------- 1. Testes ----------
if ($PularTestes) {
    Passo '1/5 Testes: pulados (-PularTestes)'
} else {
    Passo '1/5 Testes (mvn verify)'
    Executar $Maven @('-q', '-B', 'verify')
}

# ---------- 2. App e bibliotecas ----------
Passo '2/5 App e bibliotecas'
$alvo = Join-Path $raiz 'calibra-app\target\instalador'
if (Test-Path $alvo) {
    Remove-Item $alvo -Recurse -Force
}
Executar $Maven @('-q', '-B', 'package', '-DskipTests', '-Pinstalador', '-pl', 'calibra-app', '-am')
$app = Join-Path $alvo 'app'
$javafx = Join-Path $alvo 'javafx'
$jarApp = "calibra-app-$versaoPom.jar"
Copy-Item (Join-Path $raiz "calibra-app\target\$jarApp") $app
$jars = @(Get-ChildItem $app -Filter *.jar | ForEach-Object { $_.FullName })
Write-Host "$($jars.Count) jars no classpath do app; JavaFX: $((Get-ChildItem $javafx -Filter *.jar).Count) módulos"

# ---------- 3. Java embutido ----------
Passo '3/5 Java embutido (jdeps + jlink)'
$saida = & (Join-Path $bin 'jdeps.exe') --multi-release 25 --ignore-missing-deps --print-module-deps `
    --class-path ($jars -join ';') @jars
if ($LASTEXITCODE -ne 0) {
    throw "O jdeps falhou: $saida"
}
$usados = ($saida | Select-Object -Last 1).Trim() -split ','
$modulos = @($usados + @('javafx.controls', 'javafx.fxml', 'jdk.localedata') | Sort-Object -Unique)
if ($modulos -contains 'java.net.http') {
    throw 'O app passou a usar java.net.http: a regra 2.1 (zero rede) não permite.'
}
Write-Host "Módulos: $($modulos -join ', ')"
$runtime = Join-Path $alvo 'runtime'
Executar (Join-Path $bin 'jlink.exe') @('--module-path', $javafx, '--add-modules', ($modulos -join ','),
    '--include-locales', 'en,pt,pt-BR', '--strip-debug', '--no-header-files', '--no-man-pages',
    '--compress', 'zip-6', '--output', $runtime)
# Regra 2.1 (zero rede): o Java embutido não pode ter o cliente HTTP do Java.
$noPacote = & (Join-Path $runtime 'bin\java.exe') --list-modules
if ($noPacote -match '^java\.net\.http@') {
    throw 'O Java embutido ficou com o módulo java.net.http: a regra 2.1 (zero rede) não permite.'
}
Write-Host "Java embutido: $(@($noPacote).Count) módulos, sem o cliente HTTP do Java."

# ---------- 4. Conferência ----------
Passo '4/5 Conferência do pacote no Java embutido'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Executar (Join-Path $runtime 'bin\java.exe') @('--enable-native-access=javafx.graphics,ALL-UNNAMED',
    '-Dstdout.encoding=UTF-8', '-cp', (Join-Path $app '*'), 'br.com.calibra.app.Inicio', '--conferir')

# ---------- 5. MSI ----------
Passo '5/5 MSI (jpackage + WiX)'
$dist = Join-Path $raiz 'dist'
New-Item -ItemType Directory -Force $dist | Out-Null
$msi = Join-Path $dist "Calibra-$versao.msi"
if (Test-Path $msi) {
    Remove-Item $msi -Force
}
Executar (Join-Path $bin 'jpackage.exe') @('--type', 'msi', '--dest', $dist, '--name', 'Calibra',
    '--app-version', $versao, '--vendor', 'Calibra',
    '--description', 'Dimensiona peças, faz orçamento e prepara o mapa base de injeção programável (offline).',
    '--icon', (Join-Path $raiz 'calibra-app\src\main\instalador\calibra.ico'),
    '--input', $app, '--main-jar', $jarApp, '--main-class', 'br.com.calibra.app.Inicio',
    '--runtime-image', $runtime, '--java-options', '--enable-native-access=javafx.graphics,ALL-UNNAMED',
    '--win-per-user-install', '--install-dir', 'Programs\Calibra',
    '--win-menu', '--win-menu-group', 'Calibra', '--win-shortcut',
    '--win-upgrade-uuid', $UpgradeCode)

$arquivo = Get-Item $msi
Write-Host ''
Write-Host "Pronto: $($arquivo.FullName)" -ForegroundColor Green
Write-Host ("Tamanho: {0:N1} MB · SHA-256: {1}" -f ($arquivo.Length / 1MB), (Get-FileHash $msi -Algorithm SHA256).Hash)
