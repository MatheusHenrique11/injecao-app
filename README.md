# Calibra (nome provisório)

App desktop para Windows, 100% offline, só da minha oficina: gratuito, num computador e sem login. Ele faz a gestão do dia a dia (ordens de serviço de mecânica e de auto elétrica e, depois, agenda e lembretes) e a preparação: dimensiona as peças, faz o orçamento e gera o mapa base conservador de injeção programável para motores 4 tempos de 3 a 12 cilindros. O acerto fino continua com o calibrador.

A especificação completa está em [docs/SPEC.md](docs/SPEC.md).

## Situação

Fase 2 (0.2.0): banco de dados com backup e restauração; projetos com cliente, veículo (com histórico) e motor (com versões); catálogos de peças, com o digitalizador do mapa do compressor; dimensionamento com as peças do catálogo, meta pela pressão do turbo e torque × câmbio; orçamento em PDF e em texto para WhatsApp; instalador MSI. Pesquisa de mercado em [docs/pesquisa/](docs/pesquisa/).

Fase 3 (0.3.0): a oficina. Ordem de serviço da entrada do veículo (vistoria e fotos) à entrega com garantia e ao pagamento em partes; Painel e lista da Oficina; o orçamento passa a pertencer à OS; via do cliente e comprovante de entrega em PDF ou impressos; textos para o WhatsApp; catálogo de serviços; ficha do veículo; projeto de preparação ligado à OS.

Em andamento: **Fase 3B — Auto elétrica** (SPEC 1.2), a segunda linha de serviço da oficina, na mesma OS. Ela sai no instalador 0.3.1, no fim da fase. Já prontos no código:
- **3B.0:** o backup que guarda cada foto e documento uma vez só, e a cópia de anexos em segundo plano;
- **3B.1:** o catálogo de serviços com categoria, descrição no orçamento e garantia própria, e os serviços sugeridos de auto elétrica e injeção programável.

Faltam os itens e kits no orçamento, os checklists, o laudo elétrico, as instalações do veículo e a injeção programável (chicote e instalação).

Próximas fases (SPEC 12, roteiro de 15/10/2026, com a Fase 3B e a Fase 8 da SPEC 1.2):
- **4 — Mapa base.**
- **5 — Agenda e lembretes.**
- **6 — Acerto, relatório e biblioteca.**
- **7 — Aprendizado e integrações.**
- **8 — Chicote e caixa de fusíveis** (pode ser antecipada).

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
mvn -q -pl calibra-app javafx:run      # abre o app (depois do install), com os dados em %LOCALAPPDATA%\Calibra-dev
mvn -q -pl calibra-app verify -Dgroups=ui -Dtestes.excluidos=nenhum   # teste de interface (opcional)
powershell -ExecutionPolicy Bypass -File scripts\gerar-instalador.ps1  # gera dist\Calibra-<versão>.msi
powershell -ExecutionPolicy Bypass -File scripts\testar-instalado.ps1  # testes de interface no app instalado
```

O teste de interface precisa da área de trabalho do Windows. Ele salva imagens da tela em `calibra-app/target/`. O PDF de exemplo do caso B fica em `calibra-relatorios/target/exemplo-memoria-caso-B.pdf` depois do `verify`.

O script do instalador roda os testes, monta o Java embutido, confere o pacote (`Inicio --conferir`) e gera o MSI. Com `-VersaoMsi 0.1.99` ele gera um MSI de teste com outra versão (ensaio de instalar por cima); com `-PularTestes`, só repete o empacotamento. O ícone sai de `scripts\gerar-icone.ps1`.

O `testar-instalado.ps1` roda os testes de interface com o Java embutido e os jars do Calibra instalado pelo MSI (depois de `mvn -q install -DskipTests`). Com `-Testes FluxoCompletoUiTest`, roda só o ensaio do cadastro ao orçamento aprovado, com backup e restauração. Os testes usam pastas temporárias e não tocam nos dados do app.

## Documentação

- [docs/SPEC.md](docs/SPEC.md): especificação
- [docs/decisoes.md](docs/decisoes.md): decisões técnicas
- [docs/manual.md](docs/manual.md): manual do usuário
- [docs/fase0/](docs/fase0/): levantamentos da fase 0 (injeções, combustíveis, motores, planilha de conferência)
- [CHANGELOG.md](CHANGELOG.md)
