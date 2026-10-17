# Changelog

Formato: [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versões: semântica (0.x.y).

## [Não lançado] — Fase 3 (vai sair como 0.3.0)

### Adicionado
- **Ordem de serviço (OS)**, do número `OS-ano-sequência` à entrega: status com histórico (aberta, aguardando aprovação, aprovada, em execução, aguardando peças, pronta, entregue ou cancelada com o motivo), entrada do veículo com km, tanque, vistoria e objetos deixados, entrega com km de saída e garantia, e pagamento em partes, com o recebimento errado cancelado (nunca apagado). Serviço só começa com orçamento aprovado e com a entrada registrada.
- **Painel** na abertura do app: as OS por etapa (o cartão abre a lista filtrada), os veículos na oficina, as entregas de hoje e as atrasadas, e o último backup.
- **Oficina:** lista de OS com busca por placa, cliente, embarcação ou número, e filtro por etapa.
- **Nova OS** em três etapas (cliente, veículo, serviço e entrada), com as fotos da entrada; o orçamento pode vir antes do veículo.
- **Tela da OS:** os passos do serviço no cabeçalho, com o próximo passo, e as abas Entrada, Orçamento, Preparação, Entrega e pagamento, Anexos e Histórico.
- **Anexos da OS:** fotos (JPG ou PNG) e PDF até 20 MB, com miniatura, por botão ou arrastando do Explorador; o backup leva junto.
- **Projeto de preparação criado pela OS**, já ligado a ela; o cabeçalho do projeto mostra a OS.
- **Mecânicos:** cadastro rápido pelo nome, para pôr na OS.

### Mudado
- O orçamento passou a pertencer à OS. Ao atualizar, cada projeto que já tinha orçamento ganha uma OS, com o status tirado do orçamento; os números e o conteúdo dos orçamentos não mudam (migração V6). A OS sem projeto tem orçamento com os itens digitados; o PDF e o WhatsApp dele chegam no bloco 3.3.
- Menu: "Projetos" virou "Preparação", e entraram Painel e Oficina.
- A busca de cliente acha também pela placa ou pelo nome da embarcação dos veículos dele.
- Banco de dados na versão 7 (V6: ordens de serviço; V7: anexos). Como sempre, a atualização faz um backup antes.
- O app de desenvolvimento (`javafx:run`) usa uma pasta de dados só dele, `%LOCALAPPDATA%\Calibra-dev`.

## [0.2.0] — 15/10/2026 — Fase 2

### Adicionado
- **Instalador MSI** (`scripts\gerar-instalador.ps1`): Java embutido (jlink) só com os módulos usados e o idioma pt-BR, conferência do pacote antes de gerar, instalação por usuário sem administrador em `%LOCALAPPDATA%\Programs\Calibra`, atalhos no menu Iniciar e na área de trabalho, e instalar por cima sem perder os dados. Ícone próprio. `scripts\testar-instalado.ps1` roda os testes de interface no app instalado.
- **Banco de dados SQLite** em `%LOCALAPPDATA%\Calibra`, com migração automática ao abrir (backup antes; se falhar, volta o backup).
- **Backup** automático (diário e ao fechar, se algo mudou), manual e para pen drive ou HD externo; ficam os 30 mais recentes. **Restauração** conferida, com backup de segurança antes.
- **Logs** em arquivo e mensagem amigável para erro inesperado.
- **Janela principal** com Projetos, Calculadora, Catálogos e Configurações; versão e último backup na barra de baixo.
- **Configurações:** dados da oficina com logo, padrões de cálculo e limiares do semáforo, padrões do orçamento e backup.
- **Projetos:** assistente (cliente, veículo, motor, meta e injeção), lista com busca sem acento, abas Resumo, Veículo (com histórico), Motor (versões e comparação), Dimensionamento (cálculo salvo com as justificativas, "desatualizado — recalcular" com o motivo) e Peças e orçamento. Status do projeto com histórico; arquivar e reativar.
- **LGPD:** consentimento do cliente registrado e "anonimizar cliente".
- **Catálogos** (bicos, bombas, injeções, turbos, combustíveis, intercoolers, sensores, câmbios, e motores e montagens), com fonte, "a verificar", preço de referência, versões, comparação entre versões e arquivamento. Os combustíveis da SPEC 7.2 e 19 motores e 6 montagens do mercado já vêm cadastrados.
- **Peças do catálogo no dimensionamento**, com "Sugerir" para bico, bomba, injeção e turbo. Cálculo desatualizado quando uma peça ganha versão nova ou o câmbio do veículo é trocado.
- **Turbo:** digitalizador do mapa do compressor sobre a imagem do fabricante, ponto de potência e de torque no mapa (com o mapa desenhado no resultado) e semáforo do compressor.
- **Meta pela pressão do turbo**, ponto de torque máximo (com a VE do torque) e indicador **torque × câmbio**.
- **Orçamento:** itens do cálculo com o preço do catálogo, serviços, desconto, prazo e validade; PDF com o logo da oficina e o resumo técnico; texto para WhatsApp; enviado, aprovado (data, hora e meio) ou recusado; revisões com o mesmo número (ORC-AAAA-NNNN).

### Mudado
- **Núcleo de cálculo 1.1.0:** vazão corrigida, conta pela pressão, ponto de torque e torque × câmbio. Os casos golden A a D continuam iguais; cálculos salvos com o núcleo 1.0.0 aparecem como desatualizados.
- O app abre por `br.com.calibra.app.Inicio`, que liga o log antes da tela.

## [0.1.1] — 09/10/2026 — Fase 1.1

### Mudado
- O k_ar agora aceita de 8,0 a 11 (era 9,5 a 11). O padrão continua 10,5. A tela mostra a referência para motor moderno (perto de 10,5) e para motor antigo de 2 válvulas (perto de 8,5–9,5, a validar).

### Adicionado
- Testes com motores do mercado brasileiro de preparação: AP 1.6/1.8/2.0 e montagens, Opala 151 e 250, Omega 4.1 e 3.0, Família II, Fiat 2.0 20V Turbo e 2.4 20V, VW 2.5 20V, Hyundai G4GC, EA211 1.0 TSI e EA888 2.0 TSI.
- Pesquisa do mercado de preparação no Brasil (`docs/pesquisa/mercado-preparacao-brasil.md`).

## [0.1.0] — 09/10/2026 — Fase 1

### Adicionado
- **Núcleo de cálculo** (`calibra-core`):
  - todas as fórmulas da SPEC 7.1, cada uma com teste;
  - cadeia de dimensionamento com iteração MAP ↔ Tm;
  - bico necessário e bico informado (TI, DC com tempo morto, vazão mínima, duas bancadas, marcha lenta);
  - bomba, esforço (BMEP, torque, velocidade do pistão), saídas exigidas, sensor MAP, ponto de operação do compressor e recomendações.
- Combustíveis da SPEC 7.2 (E32 vigente desde 01/08/2026, E25, etanol hidratado, anidro, E85, metanol), com vigência, fonte e versão.
- Configuração e validação do motor de 3 a 12 cilindros (SPEC 6.3), com sugestões de ordem de ignição e ângulos marcadas "confirmar no manual do motor".
- Semáforo da SPEC 7.4, com justificativa obrigatória para vermelho.
- Memória de cálculo com fórmula, entradas, resultado, avisos e versão do núcleo (1.0.0).
- Escolha de bico, bomba e injeção em catálogos (SPEC 7.3), pronta no núcleo para a fase 2.
- PDF da memória de cálculo (`calibra-relatorios`, PDFBox 3.0.8).
- Tela da calculadora (`calibra-app`, FXML): motor, meta e peças, resultado com semáforo, justificativas e geração do PDF.
- **Testes:** golden A–D (1%), coerência, 3 a 12 cilindros e zero rede (regra 2.1), mais um teste de interface opcional (tag `ui`).

### Fica para depois
- Mapa do compressor e escolha de turbo com mapa (fase 2, com o digitalizador).
- Catálogos editáveis, banco de dados, instalador, módulos Java (JPMS) e logs em arquivo (fase 2).

## [0.0.1] — 08/10/2026 — Fase 0

### Adicionado
- Projeto Maven multimódulo: `calibra-core`, `calibra-ecu`, `calibra-dados`, `calibra-relatorios`, `calibra-app`.
- Janela vazia "Calibra 0.0.1" (JavaFX 25.0.4).
- Testes de fumaça (JUnit 6.1.3) que conferem a regra de dependência: só o `calibra-app` enxerga o JavaFX.
- Documentação: `docs/SPEC.md`, `docs/decisoes.md`, `docs/manual.md`, `CLAUDE.md`, `README.md`.
- Planilha de conferência das fórmulas da spec com os casos golden A a D (`docs/fase0/cadeia-de-calculo.xlsx`).
