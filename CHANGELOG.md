# Changelog

Formato: [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versões: semântica (0.x.y).

## [0.3.1] — 21/10/2026 — Fase 3B (auto elétrica)

### Adicionado
- **Serviços do catálogo com categoria, descrição, garantia própria e "exige o veículo na oficina":** a categoria (Som, Vidros e travas, Partida e carga, Iluminação, Diagnóstico elétrico, Injeção programável, Preparação ou Outro) organiza a lista; a descrição vira o texto da linha do orçamento; a garantia própria (em dias) vale no lugar da garantia da OS; o serviço de bancada (ex.: confecção de chicote) fica marcado como feito sem o veículo. Os serviços já cadastrados aparecem como Outro, sem perder nada.
- **Serviços sugeridos…:** a lista de partida de auto elétrica e injeção programável (24 serviços), para marcar os que a oficina faz e preencher as horas ou o preço de cada um. Nada entra sem horas ou preço, e o que já está no catálogo não se repete.
- **Garantia própria na entrega:** a janela de entrega avisa quais serviços têm a garantia própria, e a aba Entrega e pagamento e o comprovante de entrega mostram a de cada um, com o dia em que vence. Vale a garantia que o serviço tinha quando entrou no orçamento.
- **Itens e kits nos Catálogos:** a aba Itens (cabo, fusível, relé, a ECU..., com a unidade e o preço de referência, sem estoque) e a aba Kits (itens e serviços do catálogo, cada um com a quantidade; o kit com serviço é um pacote, como "chicote + instalação").
- **Itens e kits no orçamento:** "+ Item do catálogo…" e "+ Kit do catálogo…". O kit vira uma linha para cada item e serviço, com o preço de agora do catálogo, e cada linha se muda ou se apaga sem mexer no kit.
- **Peça do cliente:** a caixa "do cliente" na linha de peça (ex.: a ECU que o cliente já comprou) põe o valor em zero, fora do total; o PDF, o comprovante e o texto de WhatsApp mostram "fornecido pelo cliente".
- **Checklists nos Catálogos:** a aba Checklists, com os 7 modelos da SPEC (som, vidro e trava, partida e alternador, iluminação, confecção de chicote e instalação de injeção programável e diagnóstico elétrico), todos "a verificar" e com todos os itens obrigatórios, para conferir e ajustar. Cada item tem as marcas "obrigatório" e "a verificar". O serviço ganha o campo "Checklist", e os serviços sugeridos já entram com o da categoria.
- **Aba Execução na OS:** ao começar o serviço, cada serviço com checklist ganha uma cópia dele, que não muda se o modelo mudar depois. Cada item se marca como pendente, feito ou não se aplica (com a justificativa), com a observação, quem marcou e quando. Uma revisão aprovada no meio do serviço traz o checklist dos serviços novos.
- **Trava do pronto:** com item obrigatório pendente, "Serviço pronto" mostra o que falta e só deixa a OS pronta com uma justificativa, que fica no histórico com a data e a hora. Depois da entrega, o checklist fica só para ver.
- **Checklist no comprovante de entrega:** a linha "Checklist" com o resumo de cada serviço (ex.: "9 de 9 itens feitos"), sem passar de uma página.
- **Laudo de diagnóstico elétrico:** na aba Execução, um ou mais laudos por OS, feitos em qualquer etapa antes da entrega: a queixa do cliente (já vem com o serviço pedido), as medições (o valor, a unidade, a condição e a referência, como você escreve, com a marca "a verificar"; o app não julga o valor), os códigos de falha com a situação, os testes, a causa, a solução, as recomendações e o resultado ("a definir" até o fim do diagnóstico). Depois da entrega ou do cancelamento, fica só para ver.
- **PDF do laudo:** a folha A4 com as assinaturas do mecânico e do cliente, feita na hora pela aba Execução ou pelo menu Documentos ("Laudo elétrico: gerar PDF…" e "imprimir…"). Com mais de um laudo, o app pergunta qual.
- **Laudos na ficha do veículo:** os laudos de todas as OS do veículo; o duplo clique abre a OS.
- **Aviso de dado pessoal na queixa do laudo:** a dica fica embaixo da queixa, e, ao gravar, o app avisa se ela parece ter o nome do cliente, um telefone ou documento, ou um e-mail. O laudo fica mesmo se o cliente for anonimizado.
- **Instalações do veículo:** na ficha do veículo, o que foi instalado ou mexido no carro (som, alarme ou rastreador, iluminação, vidro ou trava, injeção programável ou outra), pela oficina, com a OS de origem, ou informado pelo cliente. Cada uma tem a descrição, a marca, o modelo, a potência ou especificação, a data e as observações, e um histórico com o antes e o depois de cada mudança. O item que sai do carro se retira e continua na lista, em cinza; apagar é só para o cadastro feito por engano.
- **Instalações na OS:** na aba Execução, "Registrar instalação…" já vem com a OS e a data de hoje. Depois da entrega, a OS só mostra; a instalação continua mudando pela ficha.
- **Serviço de bancada sem o veículo:** com um serviço do catálogo que não exige o veículo (ex.: a confecção de chicote), a OS começa sem a entrada. O checklist dos serviços que precisam do veículo fica bloqueado até a entrada, e o pronto pede a entrada se algum serviço precisar dele.
- **Entrega com "quem retirou":** na OS em que o veículo não passou pela oficina, a entrega pede quem retirou, no lugar do km de saída. A aba Entrega e pagamento e o comprovante mostram "retirado por", e a assinatura do comprovante é de quem retirou.
- **Ficha do chicote:** na aba Execução, uma ficha para cada chicote de injeção programável: a injeção (do catálogo ou escrita), o motor do projeto, a versão, a fonte do pinout (obrigatória), os pontos (a função, o conector, o pino da injeção e a marca "a verificar"), os materiais e as observações. Com o projeto de preparação, a ficha nova já vem com a injeção e o motor dele. O app não confere pinagem nem bitola.
- **Versões do chicote:** "Nova versão…" começa da ficha anterior (v1 vira v2); a anterior fica guardada, em cinza, só para ver.
- **PDF da ficha do chicote:** a folha A4, feita na hora pela aba Execução ou pelo menu Documentos, com os pontos e os testes de bancada tirados do checklist da confecção (cada item, com quem marcou e quando). A ficha comum cabe numa página.
- **Fichas de chicote na ficha do veículo:** as de todas as OS do veículo; o duplo clique abre a OS. Na entrega da OS, a última versão de cada chicote fica "entregue".
- **Correções da OS com o antes e o depois:** na aba Histórico, embaixo das etapas, a lista "Correções da abertura e da entrada" mostra cada campo corrigido depois de gravado (o serviço pedido, o mecânico, a previsão, as observações, a chegada, o km, o tanque, a vistoria e os objetos deixados), com a data e a hora. Antes, a correção não guardava o valor antigo.

### Mudado
- **Backup que guarda cada anexo uma vez:** o arquivo de cada backup leva o banco e a lista dos anexos; as fotos e os documentos ficam uma vez só na pasta `anexos-dos-backups`, ao lado dos backups, conferidos pela impressão digital na restauração. Antes, cada um dos 30 backups levava todas as fotos de novo. Os backups antigos continuam restaurando. A aba Backup mostra o espaço dos anexos guardados.
- **Fotos e documentos copiados em segundo plano:** anexar na OS ou na ficha do veículo e abrir a Nova OS com as fotos não travam mais a tela; a janela de espera só aparece se a cópia passar de meio segundo.
- **"Trazer as peças do cálculo de novo"** (orçamento de preparação) mantém os itens da aba Itens, além dos serviços; as peças digitadas continuam sendo trocadas pelas do cálculo.
- Banco de dados na versão 13 (V8: os checklists da OS; V9: os 7 modelos de checklist; V10: os laudos elétricos; V11: as instalações do veículo; V12: "quem retirou" na entrega; V13: as fichas de chicote). As seis só acrescentam; nada do que existe muda. Como sempre, a atualização faz um backup antes.
- Os modelos de checklist ficam em Catálogos › Checklists, e não em Configurações, como a SPEC dizia: assim ganham busca, versões, duplicar e arquivar.
- **Anonimização com o nome dos arquivos:** os anexos que ficam (fotos, laudos, notas) trocam o nome do arquivo por "arquivo anonimizado", com a extensão; a descrição fica. Antes, uma foto chamada "CNH do João.jpg" e não marcada como documento pessoal continuava com esse nome.
- O PDF do laudo e o da ficha do chicote são feitos na hora, como a via e o comprovante, sem cópia nos anexos (a SPEC dizia "anexo da OS"). O aviso de dado pessoal na queixa só aparece quando ela parece ter um, e não a cada gravação.
- SPEC 1.2: a auto elétrica entra como segunda linha de serviço, com a Fase 3B antes da Fase 4 e a Fase 8 (chicote e caixa de fusíveis).
- **Manual, item 2:** o jeito de voltar para a versão anterior foi corrigido. A versão anterior não abre os dados atualizados nem restaura os backups da nova; o caminho de volta é o backup feito nela antes de atualizar ("Fazer backup agora"), restaurado com o banco atual renomeado.

### Para o dono decidir (revisão final, decisão 275)
- O "quem retirou" da OS entregue sem o veículo fica depois da anonimização (a OS entregue não muda).
- Com a OS pronta, a revisão aprovada traz o checklist dos serviços novos, mas a entrega não confere os itens deles: use "Retomar o serviço" antes de entregar.

## [0.3.0] — 19/10/2026 — Fase 3

### Adicionado
- **Ordem de serviço (OS)**, do número `OS-ano-sequência` à entrega: status com histórico (aberta, aguardando aprovação, aprovada, em execução, aguardando peças, pronta, entregue ou cancelada com o motivo), entrada do veículo com km, tanque, vistoria e objetos deixados, entrega com km de saída e garantia, e pagamento em partes, com o recebimento errado cancelado (nunca apagado). Serviço só começa com orçamento aprovado e com a entrada registrada.
- **Painel** na abertura do app: as OS por etapa (o cartão abre a lista filtrada), os veículos na oficina, as entregas de hoje e as atrasadas, e o último backup.
- **Oficina:** lista de OS com busca por placa, cliente, embarcação ou número, e filtro por etapa.
- **Nova OS** em três etapas (cliente, veículo, serviço e entrada), com as fotos da entrada; o orçamento pode vir antes do veículo.
- **Tela da OS:** os passos do serviço no cabeçalho, com o próximo passo, e as abas Entrada, Orçamento, Preparação, Entrega e pagamento, Anexos e Histórico.
- **Anexos da OS:** fotos (JPG ou PNG) e PDF até 20 MB, com miniatura, por botão ou arrastando do Explorador; o backup leva junto.
- **Projeto de preparação criado pela OS**, já ligado a ela; o cabeçalho do projeto mostra a OS.
- **Mecânicos:** cadastro rápido pelo nome, para pôr na OS; em Configurações › OS, renomear, desativar e reativar.
- **Configurações › OS:** a garantia padrão (90 dias, já preenchida na entrega), os itens da vistoria e o texto da via do cliente.
- **Documentos da OS** (menu Documentos ▾): a via do cliente, para assinar na chegada, com a vistoria item a item, e o comprovante de entrega, com a garantia, o serviço aprovado e os recebimentos; os dois em PDF ou impressos (A4). A via não sai com a aba Entrada mudada e não salva.
- **Textos para o WhatsApp** da OS: "OS aberta" e "pronto para retirar".
- **Catálogo de serviços** (aba Serviços dos Catálogos) e "+ Serviço do catálogo…" no orçamento: com preço próprio, entra 1 serviço pelo preço; sem preço, as horas × o valor da hora.
- **Ficha do veículo:** o cliente e o veículo (editar, histórico e anonimizar), todas as OS e todos os projetos do veículo e os anexos de todas as OS, com os documentos do próprio veículo (ex.: o CRLV, com a validade).

### Mudado
- O orçamento passou a pertencer à OS. Ao atualizar, cada projeto que já tinha orçamento ganha uma OS, com o status tirado do orçamento; os números e o conteúdo dos orçamentos não mudam (migração V6). A OS sem projeto tem orçamento com os itens digitados, e o PDF e o texto de WhatsApp dele saem sem o motor, a meta e o resumo técnico.
- **Anonimizar o cliente** apaga também os anexos marcados como documento pessoal (das OS dele e dos veículos dele); fotos, laudos e notas ficam.
- A janela de escolher um item do catálogo mostra o preço de referência.
- Copiar um texto para o WhatsApp avisa quando o Windows não deixa copiar.
- Menu: "Projetos" virou "Preparação", e entraram Painel e Oficina.
- A busca de cliente acha também pela placa ou pelo nome da embarcação dos veículos dele.
- Banco de dados na versão 7 (V6: ordens de serviço; V7: anexos). Como sempre, a atualização faz um backup antes.
- O app de desenvolvimento (`javafx:run`) usa uma pasta de dados só dele, `%LOCALAPPDATA%\Calibra-dev`.
- A conferência do instalador confere também as páginas para imprimir (5 itens).

### Corrigido (revisão final, antes de o 0.3.0 ser usado com dados reais)
- A aba Peças e orçamento do projeto mostra o orçamento da OS em andamento; depois da entrega, o próximo orçamento abre outra OS (a segunda etapa da preparação), em vez de ficar preso no orçamento antigo. O orçamento da OS entregue ou cancelada fica só para ver.
- Entregar e cancelar a OS pedem para salvar antes o que mudou na aba Entrada; depois da entrega, a via não fica mais presa por uma mudança que não dá para salvar.
- Corrigir só a vistoria ou o tanque da entrada não regrava o km antigo no cadastro do veículo, e o km com duas casas (1.250,25) não é arredondado ao salvar.
- A Nova OS sendo preenchida não se perde ao abrir outra OS ou voltar para a lista da Oficina.
- O recebido a mais aparece na OS, no comprovante e no texto "pronto para retirar".
- A garantia vai até 3650 dias (10 anos), para um 9000 no lugar de 90 não ficar gravado na OS entregue.
- Cliente anonimizado: a placa e o chassi não voltam pela edição do veículo, e ele não recebe OS nova pelo projeto nem documento pessoal novo. O arquivo de documento que estava aberto em outro programa é avisado e sai da pasta na próxima abertura do app.
- O backup não falha mais se um anexo for removido enquanto ele é feito, e não leva cópia de anexo pela metade.

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
