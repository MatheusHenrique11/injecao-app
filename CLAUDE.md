# Calibra — instruções do projeto

App desktop para Windows, 100% offline, só da minha oficina (um computador, sem login, gratuito). Faz a gestão do dia a dia (ordens de serviço e, na fase 5, agenda e lembretes) e a preparação: dimensiona peças, faz orçamento e gera mapa base de injeção programável para motores 4 tempos de 3 a 12 cilindros. A spec completa fica em `docs/SPEC.md`: leia as seções 0 a 11 e a fase atual da seção 12 antes de trabalhar.

## Comandos
- Testes e build: `mvn -q verify`
- Rodar o app: `mvn -q install -DskipTests` e depois `mvn -q -pl calibra-app javafx:run` (o `-am` não serve aqui: tentaria rodar o JavaFX em todos os módulos). Ele usa a pasta de dados `%LOCALAPPDATA%\Calibra-dev`; os dados de verdade ficam com o app instalado (`%LOCALAPPDATA%\Calibra`)
- Teste de interface (tag `ui`, fora do verify padrão, precisa da área de trabalho): `mvn -q install -DskipTests` e depois `mvn -q -pl calibra-app verify -Dgroups=ui -Dtestes.excluidos=nenhum`
- Avisos de compilação: o `-q` esconde; conferir com `mvn -B clean test-compile` e procurar `[WARNING]`
- Instalador: `powershell -ExecutionPolicy Bypass -File scripts\gerar-instalador.ps1` (roda os testes e gera `dist\GarageTech-<versão>.msi`; WiX 3.14.1 em `%USERPROFILE%\tools\wix314`; `-VersaoMsi 0.1.99` gera um MSI de teste; `-PularTestes` só reempacota)
- Testes de interface no app instalado (depois de instalar o MSI e de `mvn -q install -DskipTests`): `powershell -ExecutionPolicy Bypass -File scripts\testar-instalado.ps1` (`-Testes FluxoCompletoUiTest` para só o ensaio)
- Planilha de conferência das fórmulas: `powershell -ExecutionPolicy Bypass -File docs\fase0\gerar-planilha.ps1 -Saida docs\fase0\cadeia-de-calculo.xlsx`

## Stack (não trocar sem perguntar)
Java 25 LTS (Temurin) · JavaFX 25 LTS + FXML · SQLite (sqlite-jdbc 3.53.x) · Flyway 12 · Jackson 2.x · PDFBox 3.0.8+ · JUnit 6 · Maven 3.9.x · jlink + jpackage + WiX. Sem Spring, sem Lombok.

## Módulos
`calibra-core` (cálculo puro, sem JavaFX, JDBC ou arquivos) ← `calibra-ecu`, `calibra-dados`, `calibra-relatorios` ← `calibra-app`. O core não depende de nenhum módulo.

## Regras
- IMPORTANTE: nenhum código faz chamada de rede; nada grava direto em injeção (USB, CAN, serial); nada de engenharia reversa de formatos fechados.
- Fórmula nova ou alterada = teste novo. Os casos golden da seção 7.6 da spec passam sempre.
- Unidades SI no core; conversão só na interface.
- Motor de 3 a 12 cilindros. Veículo e configuração do motor sempre editáveis; editar o motor cria versão nova e marca resultados antigos como desatualizados.
- Não inventar dado técnico: campo vazio + `aVerificar` e me avisar.
- Perguntar antes de: nova dependência, mudança de versão, mudança de arquitetura, apagar dados ou arquivos, instalar algo no sistema.
- Domínio em português sem acentos (`Motor`, `Projeto`, `Bico`, `Orcamento`); textos de tela em pt-BR.
- Dados do app em `%LOCALAPPDATA%\Calibra\`, nunca na pasta de instalação.

## Como trabalhar comigo
- Uma fase por vez: plano primeiro, passos pequenos, `mvn -q verify` com a saída mostrada, commit `fase N: ...` (sem push sem eu pedir), parar no critério de pronto da fase.
- Explicar em português simples o que mudou, por quê e em quais arquivos. Sou mecânico e programo Java em nível básico a intermediário.
- Registrar decisões técnicas em `docs/decisoes.md`.
- Ao compactar o contexto, preservar: fase atual, arquivos alterados e comandos de teste.
