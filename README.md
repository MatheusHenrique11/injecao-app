# Calibra (nome provisório)

App desktop para Windows, 100% offline, só da minha oficina: gratuito, num computador e sem login. Ele faz a gestão do dia a dia (ordens de serviço de mecânica e de auto elétrica e, depois, agenda e lembretes) e a preparação: dimensiona as peças, faz o orçamento e gera o mapa base conservador de injeção programável para motores 4 tempos de 3 a 12 cilindros. O acerto fino continua com o calibrador.

O Calibra não usa internet e nunca grava nada na injeção (USB, CAN ou serial).

A especificação completa está em [docs/SPEC.md](docs/SPEC.md), e o manual do usuário em [docs/manual.md](docs/manual.md).

## O que funciona hoje (versão 0.3.1)

### Painel
- As OS por etapa, os veículos na oficina e as entregas previstas até hoje (as de hoje e as atrasadas).

### Oficina
- **Ordem de serviço** (OS-AAAA-NNNN), da entrada à entrega:
  - a entrada do veículo, com o km (ou as horas de motor, no barco), o tanque, a vistoria item a item, os objetos deixados e as fotos;
  - o orçamento, com revisões e aprovação, que leva a OS de etapa em etapa;
  - a execução e a entrega com garantia, em dias;
  - o pagamento em partes.
- **Auto elétrica, na mesma OS:**
  - os checklists de execução, copiados do catálogo quando o serviço começa;
  - a trava do pronto, que pede justificativa para item obrigatório pendente;
  - os laudos de diagnóstico elétrico;
  - as instalações do veículo;
  - as fichas do chicote de injeção programável, com versões;
  - o serviço de bancada, que começa sem o veículo na oficina.
- **Histórico da OS:** as etapas e as correções da abertura e da entrada, com o antes e o depois.
- **Documentos em PDF ou impressos:**
  - a via do cliente e o comprovante de entrega;
  - o laudo elétrico e a ficha do chicote;
  - os textos para o WhatsApp, só copiados (OS aberta, orçamento e pronto para retirar).
- **Ficha do veículo:** o cliente, as OS, as instalações, os laudos, as fichas do chicote, os projetos e os anexos, com os documentos do próprio veículo.
- **LGPD:** o consentimento do cliente e a anonimização. Ela apaga os dados pessoais e os documentos marcados como pessoais, e troca o nome dos arquivos que ficam.

### Preparação
- Projetos com cliente, veículo (com histórico) e motor (com versões). O projeto fica ligado à OS.
- O orçamento de preparação, com as peças do cálculo.

### Calculadora
- O dimensionamento das peças com as do catálogo: a meta pela pressão do turbo, o turbo pelo mapa do compressor e o torque × câmbio.
- O semáforo de segurança e a memória de cálculo em PDF.

### Catálogos
- **Peças e dados técnicos:** bicos, bombas, injeções, turbos (com o digitalizador do mapa do compressor), combustíveis, intercoolers, sensores, câmbios e motores.
- **Serviços:** com categoria, garantia própria, checklist e "exige o veículo na oficina"; os serviços sugeridos de auto elétrica e injeção programável.
- **Itens, kits e modelos de checklist.**
- Todo item tem fonte e a marca "a verificar"; editar cria uma versão nova, e nada é apagado (só arquivado).

### Configurações
- Os dados da oficina e o logo.
- Os padrões de cálculo e do orçamento, os da OS (a garantia, os itens da vistoria e o texto da via do cliente) e os mecânicos.
- O backup.

## Dados e backup

- **Onde ficam:** todos os dados em `%LOCALAPPDATA%\Calibra\`, nunca na pasta do programa:
  - o banco SQLite;
  - os anexos;
  - os backups;
  - os logs.

  Desinstalar não apaga os dados.
- **Backup automático:** um por dia e outro ao fechar, se algo mudou. Ficam os 30 mais recentes.
- **As fotos no backup:** ficam uma vez só, na pasta `backups\anexos-dos-backups`. Para levar os backups para outro lugar, copie a pasta inteira.
- **Restaurar:** em Configurações › Backup. Antes de trocar os dados, o app confere cada arquivo e guarda os dados atuais num backup de segurança.
- **Ao atualizar:** quando o banco muda de versão, o app faz um backup antes. Se a atualização falhar, ele volta o backup sozinho.

## Instalar e atualizar

- **Instalar:** dois cliques em `Calibra-0.3.1.msi`. Ele instala só para o usuário do Windows, sem senha de administrador, em `%LOCALAPPDATA%\Programs\Calibra`, com atalhos no menu Iniciar e na área de trabalho.
- **Atualizar:** instale a versão nova por cima; os dados ficam. Antes, faça "Fazer backup agora" na versão antiga: é o caminho de volta, se precisar.
- **Detalhes:** no manual, item 2 (inclusive como voltar para a versão anterior).

## Fases

| Fase | Versão | O que trouxe |
|---|---|---|
| 0 | 0.0.1 | Os levantamentos (injeções, combustíveis, motores) e a planilha de conferência das fórmulas |
| 1 e 1.1 | 0.1.0 e 0.1.1 | A calculadora de dimensionamento e o semáforo |
| 2 | 0.2.0 | O banco com backup, os projetos, os catálogos, o orçamento em PDF e o instalador MSI |
| 3 | 0.3.0 | A oficina: a OS, o Painel, os documentos, o catálogo de serviços e a ficha do veículo |
| 3B | 0.3.1 | A auto elétrica: checklists, laudos, instalações, fichas do chicote, itens e kits |

Pesquisa de mercado em [docs/pesquisa/](docs/pesquisa/).

Próximas fases (SPEC 12, com a Fase 3B e a Fase 8 da SPEC 1.2):

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

Stack: Java 25 LTS (Temurin), JavaFX 25 com FXML, SQLite, Flyway, Jackson, PDFBox, JUnit 6 e Maven; jlink, jpackage e WiX para o instalador. Sem Spring e sem Lombok.

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

- **Teste de interface:** precisa da área de trabalho do Windows. Ele salva imagens da tela em `calibra-app/target/`. Depois do `verify`, o PDF de exemplo do caso B fica em `calibra-relatorios/target/exemplo-memoria-caso-B.pdf`.
- **`gerar-instalador.ps1`:** roda os testes, monta o Java embutido, confere o pacote (`Inicio --conferir`) e gera o MSI.
  - `-VersaoMsi 0.1.99` gera um MSI de teste com outra versão (o ensaio de instalar por cima);
  - `-PularTestes` só repete o empacotamento;
  - o ícone sai de `scripts\gerar-icone.ps1`.
- **`testar-instalado.ps1`:** roda os testes de interface com o Java embutido e os jars do Calibra instalado pelo MSI (depois de `mvn -q install -DskipTests`). Os testes usam pastas temporárias e não tocam nos dados do app.
  - `-Testes FluxoCompletoUiTest` roda só o ensaio do cadastro ao orçamento aprovado, com backup e restauração.

## Documentação

- [docs/SPEC.md](docs/SPEC.md): especificação
- [docs/decisoes.md](docs/decisoes.md): decisões técnicas
- [docs/manual.md](docs/manual.md): manual do usuário
- [docs/fase0/](docs/fase0/): levantamentos da fase 0 (injeções, combustíveis, motores, planilha de conferência)
- [docs/fase3/ensaio-instalador.md](docs/fase3/ensaio-instalador.md) e [docs/fase3b/ensaio-instalador.md](docs/fase3b/ensaio-instalador.md): ensaios dos instaladores 0.3.0 e 0.3.1
- [CHANGELOG.md](CHANGELOG.md)
