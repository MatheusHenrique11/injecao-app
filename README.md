# Calibra (nome provisório)

App desktop para Windows, 100% offline, para oficinas de preparação. Ele dimensiona as peças, faz o orçamento e gera o mapa base conservador de injeção programável para motores 4 tempos de 3 a 12 cilindros. O acerto fino continua com o calibrador.

A especificação completa está em [docs/SPEC.md](docs/SPEC.md).

## Situação

Fase 2 (0.2.0): banco de dados com backup e restauração; projetos com cliente, veículo (com histórico) e motor (com versões); catálogos de peças, com o digitalizador do mapa do compressor; dimensionamento com as peças do catálogo, meta pela pressão do turbo e torque × câmbio; orçamento em PDF e em texto para WhatsApp; instalador MSI. O mapa base chega na fase 3. Pesquisa de mercado em [docs/pesquisa/](docs/pesquisa/).

## Módulos

| Módulo | O que faz | Depende de |
|---|---|---|
| `calibra-core` | Cálculo puro (sem JavaFX, JDBC ou arquivos) | — |
| `calibra-ecu` | Perfis de injeção; exportar e importar tabelas | core |
| `calibra-dados` | SQLite, migrações, anexos, backup | core |
| `calibra-relatorios` | PDFs | core |
| `calibra-app` | Telas JavaFX e casos de uso | todos |

## Requisitos

- Eclipse Temurin 25 LTS (`java -version` → 25.0.x)
- Apache Maven 3.9.x (`mvn -v`)
- Git
- WiX Toolset 3.14.1, só para gerar o instalador. Basta o zip de binários do lançamento oficial (`wix314-binaries.zip`), extraído em `%USERPROFILE%\tools\wix314`.

## Comandos

```
mvn -q verify                          # compila e roda os testes
mvn -q install -DskipTests             # instala os módulos no repositório local
mvn -q -pl calibra-app javafx:run      # abre o app (depois do install)
mvn -q -pl calibra-app verify -Dgroups=ui -Dtestes.excluidos=nenhum   # teste de interface (opcional)
powershell -ExecutionPolicy Bypass -File scripts\gerar-instalador.ps1  # gera dist\Calibra-<versão>.msi
```

O teste de interface precisa da área de trabalho do Windows. Ele salva imagens da tela em `calibra-app/target/`. O PDF de exemplo do caso B fica em `calibra-relatorios/target/exemplo-memoria-caso-B.pdf` depois do `verify`.

O script do instalador roda os testes, monta o Java embutido, confere o pacote (`Inicio --conferir`) e gera o MSI. Com `-VersaoMsi 0.1.99` ele gera um MSI de teste com outra versão (ensaio de instalar por cima); com `-PularTestes`, só repete o empacotamento. O ícone sai de `scripts\gerar-icone.ps1`.

## Documentação

- [docs/SPEC.md](docs/SPEC.md): especificação
- [docs/decisoes.md](docs/decisoes.md): decisões técnicas
- [docs/manual.md](docs/manual.md): manual do usuário
- [docs/fase0/](docs/fase0/): levantamentos da fase 0 (injeções, combustíveis, motores, planilha de conferência)
- [CHANGELOG.md](CHANGELOG.md)
