<#
.SYNOPSIS
Roda os testes de interface (tag ui) no Calibra instalado pelo MSI.

.DESCRIPTION
Usa o Java embutido (pasta runtime) e os jars da pasta de instalação, com as opções do Java do atalho
(Calibra.cfg), no lugar do Java e das classes do Maven. Assim confere que o pacote tem tudo o que as telas usam:
módulos do Java, recursos, fontes e imagens do PDF, área de transferência e backup.

Os testes usam pastas temporárias: os dados em %LOCALAPPDATA%\Calibra não são tocados. As janelas aparecem na
tela durante o teste (precisa da área de trabalho). Nada é baixado: o JUnit vem do repositório local do Maven.

Antes: instalar o MSI e rodar "mvn -q install -DskipTests" (compila as classes de teste).

.EXAMPLE
powershell -ExecutionPolicy Bypass -File scripts\testar-instalado.ps1
powershell -ExecutionPolicy Bypass -File scripts\testar-instalado.ps1 -Testes FluxoCompletoUiTest
#>
param(
    # Nomes das classes de teste (sem o pacote). Vazio = todos os *UiTest.
    [string[]]$Testes = @(),
    [string]$Instalado = (Join-Path $env:LOCALAPPDATA 'Programs\Calibra'),
    [string]$Jdk = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot

$java = Join-Path $Instalado 'runtime\bin\java.exe'
$cfg = Join-Path $Instalado 'app\Calibra.cfg'
if (-not (Test-Path $java) -or -not (Test-Path $cfg)) {
    throw "Calibra instalado não encontrado em $Instalado. Instale o MSI antes."
}
$classesDeTeste = Join-Path $raiz 'calibra-app\target\test-classes'
if (-not (Test-Path $classesDeTeste)) {
    throw 'Faltam as classes de teste. Rode antes: mvn -q install -DskipTests'
}
if (-not $Jdk -or -not (Test-Path (Join-Path $Jdk 'bin\javac.exe'))) {
    $Jdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-25*' -ErrorAction SilentlyContinue |
        Sort-Object Name | Select-Object -Last 1 -ExpandProperty FullName
}
if (-not $Jdk) {
    throw 'JDK 25 não encontrado (o Java embutido não tem o compilador). Use -Jdk <pasta do JDK>.'
}

# JUnit da versão do pom.xml, já no repositório local do Maven. O lançador não vem com o junit-jupiter; o Maven
# baixa junto com o plugin de testes.
$versaoJunit = ([xml](Get-Content (Join-Path $raiz 'pom.xml') -Raw)).project.properties.'junit.version'
$m2 = Join-Path $env:USERPROFILE '.m2\repository'
function Jar([string]$pasta, [string]$nome, [string]$versao) {
    $padrao = if ($versao) { Join-Path $m2 "$pasta\$versao\$nome-$versao.jar" } else { Join-Path $m2 "$pasta\*\$nome-*.jar" }
    $achado = Get-ChildItem $padrao -ErrorAction SilentlyContinue | Sort-Object FullName | Select-Object -Last 1
    if (-not $achado) {
        throw "Não achei $nome no repositório do Maven ($padrao). Rode antes: mvn -q install -DskipTests"
    }
    $achado.FullName
}
$junit = @(
    (Jar 'org\junit\platform\junit-platform-launcher' 'junit-platform-launcher' $versaoJunit),
    (Jar 'org\junit\platform\junit-platform-engine' 'junit-platform-engine' $versaoJunit),
    (Jar 'org\junit\platform\junit-platform-commons' 'junit-platform-commons' $versaoJunit),
    (Jar 'org\junit\jupiter\junit-jupiter-api' 'junit-jupiter-api' $versaoJunit),
    (Jar 'org\junit\jupiter\junit-jupiter-engine' 'junit-jupiter-engine' $versaoJunit),
    (Jar 'org\opentest4j\opentest4j' 'opentest4j' $null),
    (Jar 'org\apiguardian\apiguardian-api' 'apiguardian-api' $null),
    (Jar 'org\jspecify\jspecify' 'jspecify' $null)
)

# Um lançador pequeno do JUnit, compilado com o JDK e rodado com o Java embutido.
$saida = Join-Path $raiz 'calibra-app\target\testar-instalado'
$lancador = Join-Path $saida 'lancador'
New-Item -ItemType Directory -Force $lancador | Out-Null
$fonte = Join-Path $saida 'RodarTestes.java'
$codigo = @'
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/** Roda as classes de teste pedidas e mostra o resultado de cada teste (scripts/testar-instalado.ps1). */
public final class RodarTestes {
    public static void main(String[] classes) {
        List<DiscoverySelector> seletores = new ArrayList<>();
        for (String classe : classes) {
            seletores.add(DiscoverySelectors.selectClass(classe));
        }
        SummaryGeneratingListener resumo = new SummaryGeneratingListener();
        TestExecutionListener cadaTeste = new TestExecutionListener() {
            @Override
            public void executionFinished(TestIdentifier teste, TestExecutionResult resultado) {
                if (teste.isTest()) {
                    String nome = teste.getSource().filter(MethodSource.class::isInstance).map(MethodSource.class::cast)
                            .map(m -> m.getClassName().substring(m.getClassName().lastIndexOf('.') + 1) + "."
                                    + m.getMethodName())
                            .orElse(teste.getDisplayName());
                    System.out.println(resultado.getStatus() + "  " + nome);
                }
            }
        };
        LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request().selectors(seletores).build(),
                resumo, cadaTeste);
        TestExecutionSummary soma = resumo.getSummary();
        soma.printFailuresTo(new PrintWriter(System.out, true), 25);
        System.out.printf("Testes: %d · passaram: %d · falharam: %d%n", soma.getTestsFoundCount(),
                soma.getTestsSucceededCount(), soma.getTotalFailureCount());
        System.exit(soma.getTotalFailureCount() == 0 && soma.getTestsSucceededCount() > 0 ? 0 : 1);
    }
}
'@
[IO.File]::WriteAllText($fonte, $codigo, (New-Object Text.UTF8Encoding $false))
& (Join-Path $Jdk 'bin\javac.exe') -encoding UTF-8 -d $lancador -cp ($junit -join ';') $fonte
if ($LASTEXITCODE -ne 0) { throw 'O lançador do JUnit não compilou.' }

# As classes de teste: as pedidas ou todos os *UiTest.
if ($Testes.Count -eq 0) {
    $classes = Get-ChildItem $classesDeTeste -Recurse -Filter '*UiTest.class' | ForEach-Object {
        $_.FullName.Substring($classesDeTeste.Length + 1).Replace('\', '.') -replace '\.class$', ''
    }
} else {
    $classes = $Testes | ForEach-Object { if ($_.Contains('.')) { $_ } else { "br.com.calibra.app.$_" } }
}

# As opções do Java do atalho, lidas do Calibra.cfg (ex.: --enable-native-access).
$appDir = Join-Path $Instalado 'app'
$opcoes = Get-Content $cfg | Where-Object { $_ -like 'java-options=*' } |
    ForEach-Object { $_.Substring('java-options='.Length).Replace('$APPDIR', $appDir) }

# Log dos testes só com avisos e erros (o Flyway e o backup escrevem muito em INFO).
$configLog = Join-Path $saida 'logging.properties'
[IO.File]::WriteAllText($configLog, "handlers=java.util.logging.ConsoleHandler`n.level=WARNING`n",
    (New-Object Text.UTF8Encoding $false))
$opcoes += "-Djava.util.logging.config.file=$configLog"
# Como no surefire do pom.xml: as DLLs extraídas (SQLite, fontes do JavaFX) e o cache de fontes do PDFBox ficam
# fora da pasta temporária de cada teste; DLL carregada fica presa, e a pasta do teste não seria apagada no fim.
$cache = Join-Path $saida 'cache'
$opcoes += "-Dorg.sqlite.tmpdir=$cache", "-Dpdfbox.fontcache=$cache", "-Djavafx.cachedir=$(Join-Path $cache 'javafx')"

$caminho = (@((Join-Path $appDir '*'), $classesDeTeste, $lancador) + $junit) -join ';'
Write-Host "Java embutido: $java"
Write-Host "Opções do atalho: $($opcoes -join ' ')"
Write-Host "Testes: $($classes -join ', ')"
[Console]::OutputEncoding = [Text.Encoding]::UTF8
& $java @opcoes "-Dbasedir=$saida" '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp $caminho RodarTestes @classes
$codigoSaida = $LASTEXITCODE

$resumo = Join-Path $saida 'target\ensaio-fluxo-completo.txt'
if (Test-Path $resumo) {
    Write-Host ''
    Write-Host 'Resumo do ensaio (FluxoCompletoUiTest):'
    Get-Content $resumo -Encoding UTF8 | ForEach-Object { Write-Host "  $_" }
}
Write-Host ''
Write-Host "Imagens, PDFs e textos dos testes: $(Join-Path $saida 'target')"
exit $codigoSaida
