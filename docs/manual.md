# Manual do usuário — Calibra

Versão do app: 0.3.0 (fase 3). Este manual cresce a cada fase.

## 1. O que o Calibra faz

Cuida da oficina e da preparação num app só. Na oficina: a ordem de serviço (OS), da entrada do veículo, com a vistoria e as fotos, ao orçamento, ao serviço, à entrega com garantia e ao pagamento. Na preparação: transforma a meta do cliente (motor, potência, uso, combustível e injeção) em peças dimensionadas, orçamento, mapa base conservador e relatório final. O acerto fino continua sendo feito pelo calibrador, no software da própria injeção. O Calibra nunca grava nada na injeção e funciona sem internet.

Nesta versão (0.3.0) já funcionam: a oficina (item 5), com as ordens de serviço, o Painel, os documentos da OS e a ficha do veículo; os projetos de preparação, com cliente, veículo e motor; a calculadora de dimensionamento; os catálogos; o orçamento; o backup. O mapa base chega na fase 4.

## 2. Instalação

- **Instalar:** dê dois cliques em `Calibra-0.3.0.msi`. O Calibra instala só para o seu usuário do Windows, sem pedir senha de administrador, em `%LOCALAPPDATA%\Programs\Calibra`. Ele cria atalhos no menu Iniciar (pasta Calibra) e na área de trabalho. As telas do instalador são as do Windows, em inglês.
- **Atualizar:** instale a versão nova por cima, do mesmo jeito. Os dados ficam, e só uma versão aparece em "Aplicativos instalados".
- **Atualizar do 0.2.0 para o 0.3.0:**
  - na primeira vez que o 0.3.0 abrir, ele faz um backup ("antes de atualizar o banco") na pasta `backups` dos dados e atualiza o banco. Se algo der errado, o backup volta sozinho, e o app avisa;
  - cada projeto que já tinha orçamento ganha uma OS, com a etapa tirada do orçamento (ex.: orçamento aprovado → OS aprovada). Os números e o conteúdo dos orçamentos não mudam;
  - depois disso, o 0.2.0 não abre mais esses dados. Para voltar ao 0.2.0 (só se precisar): instale o `Calibra-0.2.0.msi` por cima e restaure o backup "antes de atualizar o banco" pela aba Backup de Configurações (item 10). O que foi feito no 0.3.0 depois desse backup se perde.
- **Versão mais antiga por cima:** o Windows deixa instalar, mas ela não abre dados que uma versão mais nova já atualizou. O app avisa ("Instale a versão mais nova para abri-los") e não mexe em nada; é só instalar de novo a versão mais nova.
- **Desinstalar:** Configurações do Windows → Aplicativos → Aplicativos instalados → Calibra → Desinstalar. **Os dados não são apagados** (item 3); se reinstalar, tudo volta.
- O Calibra não usa internet: não atualiza sozinho, não manda nada para fora e não precisa de conta.

## 3. Onde ficam os dados

Tudo fica em `%LOCALAPPDATA%\Calibra\` (para abrir, cole esse endereço na barra do Explorador de Arquivos):

| Pasta ou arquivo | O que tem |
|---|---|
| `calibra.db` | o banco de dados: clientes, veículos, ordens de serviço, projetos, cálculos, catálogos, orçamentos e configurações |
| `anexos\` | o logo da oficina, as imagens dos mapas de turbo e as fotos e documentos das OS (em `anexos\veiculos\`). Ao abrir, o app apaga de `anexos\veiculos\` as cópias que nenhum anexo usa mais (ex.: um arquivo que estava aberto em outro programa quando o anexo foi removido) |
| `backups\` | os backups automáticos e, em `backups\anexos-dos-backups\`, as fotos e os documentos deles, guardados uma vez só (a pasta pode ser trocada, item 10) |
| `logs\` | o registro do que o app fez, com o detalhe dos erros (para o suporte) |
| `cache\` | arquivos temporários das bibliotecas (fontes do PDF); pode ser apagada |

Essa pasta nunca fica junto do programa, e desinstalar não a apaga.

## 4. Primeiros passos: Configurações

Antes do primeiro orçamento, abra **Configurações** (menu à esquerda). Cada aba tem o próprio "Salvar" e "Descartar alterações".

- **Oficina:** nome, CNPJ, endereço, telefone, e-mail e logo (PNG ou JPG, até 5 MB). Tudo opcional; aparece no cabeçalho dos PDFs.
- **Cálculo e semáforo:** os padrões de cálculo (k_ar, DC máximo, eficiências, perdas...) e os limiares do semáforo (item 13). Valor diferente do padrão da SPEC aparece em azul. "Voltar aos padrões da SPEC" só preenche os campos: grava ao salvar. Os limiares do mapa do compressor e do torque × câmbio estão marcados "a validar".
- **OS:** a garantia padrão em dias (vem 90 e já aparece preenchida na entrega), os itens da vistoria (adicionar, renomear, remover e mudar a ordem; valem para as entradas seguintes) e o texto da via do cliente, que vai acima das assinaturas (em branco, ficam só as assinaturas). O texto que vem é um modelo, não um texto jurídico revisado: confira antes de usar com cliente. Embaixo, os **mecânicos**: novo, renomear, desativar e reativar, valendo na hora.
- **Orçamento:** o que um orçamento novo traz preenchido: desconto, prazo em dias úteis (e a partir de quando conta), validade em dias, condições de pagamento e o valor da hora de mão de obra.
- **Backup:** a pasta dos backups, "Fazer backup agora" e "Restaurar um backup…" (item 10).

## 5. Oficina

O app abre no **Painel**. O menu à esquerda tem Painel, Oficina, Preparação, Calculadora, Catálogos e Configurações.

### 5.1 Painel

- **Cartões** com quantas OS há em cada etapa: abertas, aguardando aprovação, aprovadas (na fila), em execução, aguardando peças e prontas para retirar. Clicar no cartão abre a Oficina só com essas OS.
- **Na oficina agora:** as OS com o veículo na oficina, de quem chegou primeiro para quem chegou por último.
- **Entregas previstas até hoje:** as de hoje e as atrasadas. "Atrasada" aparece quando a previsão passou e o serviço não ficou pronto (a OS pronta espera o cliente buscar).
- O último backup. Duplo clique numa linha abre a OS.

### 5.2 Lista da Oficina

Busca por placa, cliente, embarcação ou número da OS, sem precisar de acento nem de traço ("abc1d23", "joao", "2026-0003"). O filtro de etapa já vem em "Em andamento"; "Todas" mostra também as entregues e as canceladas. Duplo clique ou Enter abre a OS.

### 5.3 Nova OS

"Nova OS…" (no Painel ou na Oficina) tem três etapas. Se você sair no meio para outra tela (também para a lista da Oficina ou para outra OS), o que foi digitado fica lá: "Nova OS…" volta para ela. Ela só some quando a OS é aberta ou quando você cancela.

1. **Cliente:** um já cadastrado ou novo (como no projeto, item 6.2). A busca acha também pela placa ou pelo nome da embarcação dos veículos do cliente.
2. **Veículo:** um do cliente ou novo. Achando o cliente pela placa, o veículo daquela placa já vem escolhido.
3. **Serviço e entrada:**
   - o **serviço pedido**, nas palavras do cliente (obrigatório), o mecânico ("Novo mecânico…" cadastra pelo nome), a previsão de entrega e observações;
   - com **"O veículo já está aqui"** marcado (o padrão): a chegada (dia e hora), o km no painel (ou as horas de motor, na embarcação), o tanque, a **vistoria** e os objetos deixados no veículo. A vistoria traz os itens de Configurações › OS (de fábrica: lataria e pintura; vidros, faróis e retrovisores; pneus, rodas e estepe; interior e painel). Anote o que já estava assim na chegada; em branco quer dizer nada a anotar;
   - as **fotos da entrada**: JPG ou PNG, até 20 MB cada, pelo botão ou arrastando do Explorador de Arquivos;
   - desmarcando "O veículo já está aqui", a OS abre sem a entrada (orçamento por WhatsApp ou telefone). A entrada fica para quando o veículo chegar, na aba Entrada da OS.

**Abrir a OS** grava tudo de uma vez e abre a OS. As fotos são copiadas em segundo plano: a tela não trava, o botão espera, e uma janela de espera aparece se a cópia passar de meio segundo. Se uma foto não servir, nada é gravado: tire a foto da lista e tente de novo. Foto do iPhone em HEIC não abre: mande pelo WhatsApp, que ele converte para JPG.

O número sai como `OS-ano-sequência` (ex.: OS-2026-0001), e a sequência recomeça a cada ano. O km da entrada atualiza o do cadastro do veículo; se for menor, o app avisa e grava assim mesmo (troca de painel acontece).

### 5.4 Tela da OS

O cabeçalho mostra o número, o cliente, o veículo, a etapa, o valor e o que já foi recebido, e o **próximo passo**. No canto, **Ficha do veículo** (item 5.6) e **Documentos ▾** (item 5.5). Os botões dos passos mudam com a etapa:

- **Começar o serviço:** só com o orçamento aprovado e a entrada registrada. Serviço só com orçamento aprovado é a regra do Código de Defesa do Consumidor (art. 39, VI). Ao começar, cada serviço do catálogo que tem checklist (item 8) ganha uma cópia dele, na aba Execução.
- **Aguardando peças…** (diga quais), **Retomar o serviço** e **Serviço pronto**. Com item **obrigatório** do checklist ainda pendente, "Serviço pronto" abre uma janela com o que falta: a OS só fica pronta com uma justificativa escrita, que vai para o Histórico, com a data e a hora, junto com a lista do que faltou.
- **Entregar…:** dia e hora, km na saída e garantia em dias (0 = sem garantia, até 3650 = 10 anos; já vem com a garantia padrão de Configurações › OS). A data em que a garantia vence aparece na OS. Se algum serviço do orçamento aprovado tiver **garantia própria** no catálogo (item 8), a janela avisa qual; a garantia digitada vale para os outros. Vale a garantia que o serviço tinha quando entrou no orçamento: mudar o catálogo depois não muda a OS.
- **Cancelar OS…:** a qualquer momento antes da entrega, com o motivo. A OS cancelada fica no histórico; OS não se apaga.

Com algo mudado e não salvo na aba Entrada, Entregar e Cancelar pedem para salvar antes: depois, a OS não muda mais, e a mudança se perderia. Com marcações do checklist não salvas (aba Execução), o mesmo vale para Serviço pronto, Entregar e Cancelar.

As abas:

- **Entrada:** os dados da abertura e a entrada do veículo (registrar quando ele chegar, ou corrigir o que foi anotado errado). Corrigir só a vistoria ou o tanque não mexe no km do cadastro do veículo; corrigir o km, sim.
- **Orçamento:** o mesmo do projeto (item 9). Na OS sem projeto de preparação, os itens são digitados. A OS acompanha o orçamento até a aprovação: enviado leva a Aguardando aprovação; aprovado, a Aprovada; recusado ou revisão nova volta a Aberta. Depois de aprovada, uma revisão nova (o cliente pediu mais coisa) não muda a etapa, e o valor da OS passa a ser o da última revisão aprovada. Com a OS entregue ou cancelada, o orçamento fica só para ver (o PDF e o texto continuam saindo).
- **Preparação:** com projeto ligado, o resumo e "Abrir o projeto". Sem projeto: "Criar projeto de preparação…" (o assistente começa no motor, com o cliente e o veículo da OS, e o projeto já nasce ligado a ela) ou ligar um projeto do mesmo veículo.
- **Execução:** os checklists dos serviços, um quadro por serviço, copiados do catálogo quando o serviço começou. Mudar o modelo no catálogo depois não muda a cópia da OS. Em cada item:
  - marque **Pendente**, **Feito** ou **Não se aplica**; o "não se aplica" pede por que (a justificativa), e sem ela nada é gravado;
  - escreva na observação o que for preciso anotar (ex.: a tensão medida);
  - as marcas "obrigatório" e "a verificar" vêm do modelo; embaixo aparece quem marcou e quando.

  "Quem está marcando" já vem com o mecânico da OS. **Salvar marcações** grava o que mudou, com quem e a hora. O resumo de cada quadro mostra quantos itens estão feitos e quantos obrigatórios ainda travam o pronto. Se o cliente pedir mais coisa no meio do serviço, a revisão aprovada traz o checklist dos serviços novos; o serviço que saiu do orçamento deixa o quadro guardado, marcado, mas ele não trava o pronto. Depois da entrega ou do cancelamento, a aba fica só para ver.
- **Entrega e pagamento:** depois da entrega, quando, com quantos km e a garantia, com o dia em que vence (e, para cada serviço com garantia própria, a dele, ex.: "Garantia própria: Reparo ou troca de alternador, 365 dias, até 19/10/2027"). Também o valor da OS, o recebido e o que falta (pendente, parcial ou pago). Dá para receber em partes (ex.: sinal na aprovação e o resto na entrega). Recebimento lançado errado se cancela com o motivo: ele fica na lista, em cinza, e deixa de contar. Se o recebido passar do valor, aparece "recebido a mais": confira se nenhum recebimento foi lançado duas vezes e cancele o errado.
- **Anexos:** fotos e PDF (nota fiscal, laudos, documentos), com miniatura. "Adicionar arquivos…" ou arrastar do Explorador; a cópia roda em segundo plano, como na Nova OS. Foto vira "foto da entrada" antes de o serviço começar e "foto do serviço" depois; PDF vira "documento". "Editar…" muda o tipo, a descrição, a validade e a marca de documento pessoal. "Abrir" usa o programa do Windows. "Remover…" só com a OS em andamento.
- **Histórico:** cada mudança de etapa, com data, hora e observação.

Depois da entrega, a OS não muda mais; os recebimentos e os anexos continuam valendo. Avisos que não impedem (ex.: km de saída menor que o da entrada) aparecem em amarelo, embaixo.

### 5.5 Documentos da OS

O botão **Documentos ▾**, no cabeçalho da OS:

- **Via do cliente** (gerar PDF ou imprimir): a folha A4 que o cliente assina na chegada. Tem a OS, o cliente e o veículo, o serviço pedido, a previsão e o mecânico, a entrada (chegada, km, tanque, a vistoria item a item e os objetos deixados), o texto de Configurações › OS e as linhas de assinatura do cliente e do responsável pela oficina. Com algo mudado e não salvo na aba Entrada, a via não sai: a mensagem diz o que salvar. Assim nenhuma observação da vistoria fica de fora da folha assinada.
- **Comprovante de entrega** (gerar PDF ou imprimir, depois da entrega): a entrega, o km de saída, a garantia com a data em que vence (e, embaixo, a linha "Garantia própria" com os serviços que têm a própria, quando houver), os itens e o total do orçamento aprovado (a peça do cliente sai marcada, com R$ 0,00), o resumo do checklist de cada serviço (a linha "Checklist", ex.: "Instalação de som completa: 9 de 9 itens feitos"; o que ficou pendente aparece como pendente), os recebimentos (o cancelado aparece marcado e não conta), o que falta (ou o recebido a mais), a situação e a assinatura de quem recebeu o veículo.
- **Copiar texto para WhatsApp:** "OS aberta" (o número, o veículo, quando chegou ou "orçamento a fazer", o serviço pedido e a previsão) e "pronto para retirar" (com a OS pronta: o valor, o recebido e o que falta, ou o recebido a mais). O app só copia: cole na conversa (Ctrl+V), confira e envie você mesmo.
- **Imprimir…** abre a janela de impressão do Windows. As páginas são preparadas em segundo plano; a primeira impressão demora alguns segundos (o app procura as fontes do Windows). Sem impressora, gere o PDF ou use a "Microsoft Print to PDF".

### 5.6 Ficha do veículo

**Ficha do veículo** (no cabeçalho da OS e no do projeto) mostra tudo do veículo, numa tela da Oficina:

- **Cliente:** o contato e o consentimento, com "Editar cliente…", "Histórico…" e "Anonimizar…" (item 6.6).
- **Veículo:** o resumo, com "Editar veículo…" e "Histórico do veículo…". É aqui que se edita o veículo de uma OS sem projeto.
- **OS do veículo** e **projetos de preparação:** todas as OS e todos os projetos, também os arquivados. Duplo clique abre.
- **Anexos e documentos:** os anexos de todas as OS do veículo e os documentos do próprio veículo. "Adicionar documento do veículo…" anexa sem OS, marcado como documento pessoal (ex.: o CRLV; ponha a validade em "Editar…"). O anexo de uma OS se remove na própria OS.

O botão de voltar leva para onde a ficha foi aberta (a OS ou o projeto).

## 6. Preparação (projetos)

### 6.1 Lista de projetos

A tela **Preparação** (no menu, era "Projetos") lista os projetos de preparação, do mexido por último para o mais antigo. A busca acha por cliente, placa, nome da embarcação, marca, modelo ou motor, sem precisar de acento ("joao" acha "João"; "abc1234" acha "ABC-1234"). Dá para filtrar pelo status e mostrar os arquivados.

### 6.2 Projeto novo (assistente)

"Novo projeto" abre o assistente, em cinco etapas. Cada etapa é conferida antes de seguir, e nada é gravado até o fim:

1. **Cliente:** um já cadastrado (com busca, que acha também pela placa) ou novo. Nome e telefone são obrigatórios. O **consentimento** (data e finalidade) também: é a regra da LGPD (item 6.6). CPF ou CNPJ é opcional.
2. **Veículo:** um do cliente ou novo. Os campos mudam com o tipo: placa e chassi para carro, moto e utilitário; nome, inscrição e casco para barco, lancha e jet ski. O câmbio pode vir do catálogo (item 8).
3. **Motor:** o formulário do motor (item 7.1). "Preencher pelo catálogo" traz a geometria de um motor ou montagem cadastrada; o que o catálogo não tem fica em branco, e a ordem de ignição precisa ser conferida no manual do motor.
4. **Meta e uso:** potência (ou pressão do turbo, só sobrealimentado), onde é medida, rotações e uso.
5. **Injeção (opcional):** a injeção do catálogo, com o que falta nela para este motor e o botão "Sugerir".

Aberto pela OS ("Criar projeto de preparação…", item 5.4), o assistente já começa no motor.

### 6.3 Abas do projeto

- **Resumo:** cliente (editar, histórico e anonimizar), veículo, motor, meta, injeção, último cálculo, histórico de status e observações do projeto.
- **Veículo:** editável a qualquer momento. Cada salvamento grava, campo a campo, o valor antigo, o novo, a data e a hora; o histórico aparece embaixo.
- **Motor:** editar e "Salvar como versão nova" cria a v2, v3... Versões nunca mudam nem são apagadas. Dá para comparar duas versões e usar uma antiga como ponto de partida. Cálculos e orçamentos feitos com uma versão antiga passam a aparecer como **desatualizados**.
- **Dimensionamento:** a calculadora (item 7) com o motor do projeto. "Salvar cálculo no projeto" grava o cálculo com as justificativas dos vermelhos. A aba "Cálculos salvos" lista todos, com o motivo quando um ficou desatualizado (motor novo, núcleo de cálculo novo, peça do catálogo com versão nova ou câmbio do veículo trocado), e gera o PDF de qualquer um deles.
- **Peças e orçamento:** o orçamento (item 9).

### 6.4 A OS do projeto

O cabeçalho do projeto mostra a OS dele, num botão que a abre (ex.: "Abrir a OS-2026-0003 · Aprovada"), e o botão **Ficha do veículo** (item 5.6). O orçamento feito na aba Peças e orçamento vai para essa OS; se o projeto não tem OS em andamento, o app abre uma para ele, sem a entrada do veículo.

Depois que a OS do projeto é entregue (ou cancelada), a aba Peças e orçamento fica sem orçamento: o próximo abre outra OS para o projeto (ex.: a segunda etapa da preparação). O orçamento antigo continua na OS dele, na ficha do veículo.

### 6.5 Status do projeto

Rascunho → Dimensionado (primeiro cálculo salvo) → Orçado (orçamento enviado) → Aprovado (orçamento aprovado) → Mapa base → Em acerto → Concluído. Anda para a frente, um passo de cada vez. "Arquivar projeto" tira o projeto da lista a qualquer momento; "Reativar" volta ao status de antes.

### 6.6 Dados pessoais (LGPD)

O Calibra guarda só o necessário do cliente: nome e telefone, e CPF ou CNPJ quando precisar de documento, com o consentimento registrado. **Anonimizar cliente** (na aba Resumo do projeto ou na ficha do veículo) apaga:

- os dados pessoais dele e dos veículos dele: o nome vira "Cliente anonimizado #N"; placa, chassi e identificação da embarcação somem;
- os anexos marcados como documento pessoal (como CNH e CRLV), os das OS dele e os dos veículos dele. O arquivo só sai da pasta de anexos se nenhum outro anexo usar o mesmo.

Fotos, laudos, notas fiscais, OS, projetos, cálculos e orçamentos ficam. **Não tem volta.** PDFs gerados depois disso já saem anonimizados.

- Se um desses documentos estiver aberto em outro programa (como um PDF no leitor), o arquivo não sai da pasta na hora: o app avisa, e ele sai na próxima vez que o Calibra abrir. Feche o programa antes, se puder.
- Depois de anonimizar, a placa, o chassi e a identificação da embarcação não voltam pela edição do veículo, e o cliente não recebe OS nova pelo orçamento do projeto nem documento pessoal novo. Se a pessoa voltar à oficina, cadastre-a de novo, com um consentimento novo.

O que a anonimização não alcança:

- os **backups** feitos antes dela continuam com os dados e os documentos do cliente. Os da pasta de backups saem sozinhos quando deixam de estar entre os 30 mais recentes; os copiados para pen drive ou HD ficam até você apagar;
- os **textos livres da OS** (serviço pedido, observações, vistoria e observações dos recebimentos) não mudam. Evite escrever neles dados pessoais do cliente, como CPF, endereço ou telefone.

## 7. Calculadora e dimensionamento

A **Calculadora** (menu à esquerda) faz a conta sem gravar nada. A aba **Dimensionamento** do projeto é a mesma calculadora, com o motor do projeto e o botão de salvar.

### 7.1 Motor

- **Número de cilindros:** de 3 a 12. Ao trocar, o app sugere de novo a ordem de ignição, os ângulos dos PMS e o arranjo. Se você já tinha editado esses campos, ele pergunta antes. **As sugestões precisam ser conferidas no manual do motor.**
- **Ordem de ignição:** números separados por traço (ex.: `1-3-4-2`). Começa sempre no cilindro 1.
- **Ângulos dos PMS:** um por cilindro, na ordem de ignição, separados por ponto e vírgula (ex.: `0; 180; 360; 540`).
- **Modos de injeção e ignição:** com número ímpar de cilindros, só aparecem os modos possíveis. Injeção ou ignição sequencial exigem sensor de fase.
- **Combustível:** do catálogo de combustíveis, com AFR, densidade e vigência (a gasolina comum está E32 desde 01/08/2026).

### 7.2 Meta e peças

- **Meta pela potência:** potência em cv, medida no virabrequim ou na roda (na roda, informe a perda de transmissão).
- **Meta pela pressão do turbo** (só motor sobrealimentado): você informa a pressão acima da atmosfera (em bar, psi ou kPa), e o cálculo estima a potência. Com "medida na roda" e a perda, ele estima também a potência na roda.
- **Rotação de torque máximo** (opcional): dá o ponto de torque. A **VE no torque máximo** é opcional; sem ela, vale a VE da potência. Com a mesma pressão e a mesma VE, o torque estimado sai igual nas duas rotações; a diferença real vem da VE e da pressão que o turbo entrega naquele giro.
- **VE na potência máxima:** sem padrão, você informa, em fração (0,95 = 95%).
- **k_ar:** o padrão de 10,5 serve para motor moderno de 4 válvulas; motor antigo de 2 válvulas costuma ficar perto de 8,5 a 9,5 (referência a validar). O app aceita de 8,0 a 11.
- **Peças do catálogo (opcional):** bico, bico da bancada secundária, bomba, intercooler, sensor MAP, injeção, turbo e câmbio. Peça escolhida vale no lugar dos campos digitados dela, que ficam travados. A **tensão de trabalho** lê a tabela de tempo morto do bico e a curva da bomba.
- **Sugerir:** escolhe no catálogo o menor bico que mantém o DC, a menor bomba com folga verde, a primeira injeção que atende o motor, ou, em **Sugerir turbos**, o turbo mais eficiente entre os que têm os pontos de potência e de torque dentro do mapa, com o lembrete do A/R da caixa quente.
- **Câmbio:** com o torque máximo cadastrado, entra o indicador torque × câmbio. No projeto, vale o câmbio do veículo (troque na aba Veículo).

**Números:** use vírgula para decimais (`3,5`). O ponto é separador de milhar (`1.050` = mil e cinquenta). O app recusa `3.5` para não virar 35 sem ninguém perceber.

### 7.3 Resultado

Clique em **Calcular** (ou tecle Enter). O resultado mostra:

1. **Semáforo:** verde, amarelo ou vermelho, sempre com a cor escrita e a explicação (item 13).
2. **Resultados** em várias unidades (ex.: 241,0 kPa · 2,41 bar · 34,95 psia), com o ponto de torque e os pontos no mapa do turbo.
3. **Mapa do compressor:** com turbo do catálogo que tenha mapa, o mapa aparece desenhado com o ponto de potência (círculo) e o de torque (quadrado).
4. **Recomendações, avisos e a memória de cálculo**, com cada conta, a fórmula e as entradas.

Se você mudar qualquer campo depois de calcular, o resultado fica **desatualizado** e o PDF é bloqueado até calcular de novo.

### 7.4 PDF da memória de cálculo

**Gerar PDF da memória…**: antes, escreva a justificativa de cada item vermelho, no campo que aparece embaixo dele. Sem isso, o PDF não sai. O PDF leva o motor, a meta, as peças, o semáforo com as justificativas (com data e hora), os resultados, as recomendações, os avisos e a memória completa, com a versão do app e do núcleo de cálculo.

## 8. Catálogos

**Catálogos** (menu à esquerda) guarda as peças, os dados técnicos, os serviços, os itens, os kits e os checklists da oficina, em abas: Bicos, Bombas, Injeções, Turbos, Combustíveis, Intercoolers, Sensores, Câmbios, Motores e montagens, Serviços, Itens, Kits e Checklists.

- **As peças, os serviços, os itens e os kits começam vazios:** cadastre as peças a partir do manual ou do site do fabricante, e o resto pela tabela da oficina. Combustíveis, motores e checklists já vêm cadastrados (os motores e os checklists marcados "a verificar").
- **Serviços:** o nome, a categoria, as horas (o tempo padrão) e, se o serviço tiver preço próprio, o preço de referência. Sem preço, as horas são obrigatórias. A fonte já vem "Tabela da oficina". No orçamento, entram pelo "+ Serviço do catálogo…" (item 9). Cada serviço tem também:
  - **Categoria** (obrigatória): Preparação, Som, Vidros e travas, Partida e carga, Iluminação, Diagnóstico elétrico, Injeção programável ou Outro. Os serviços cadastrados antes do 0.3.1 aparecem como Outro; troque na edição. Clique no título da coluna "Categoria" para ordenar a lista por ela;
  - **Descrição no orçamento** (opcional, uma linha): o texto que entra na linha do orçamento e daí no PDF, no WhatsApp e no comprovante. Em branco, entra o nome;
  - **Garantia própria (dias)** (opcional, de 0 a 3650): em branco, o serviço fica com a garantia da OS; 0 quer dizer sem garantia para aquele serviço (ex.: um diagnóstico). Ela aparece na entrega e no comprovante (itens 5.4 e 5.5);
  - **Exige o veículo na oficina** (vem marcado): desmarque no serviço de bancada, feito sem o veículo, como a confecção de chicote;
  - **Checklist** (opcional): o modelo da aba Checklists que a OS copia quando o serviço começa (item 5.4). Com "(nenhum)", o serviço não tem checklist. A lista de serviços mostra a coluna "Checklist" (sim ou —). Ligar um checklist a um serviço vale para as OS que começarem depois.
- **Serviços sugeridos…** (na aba Serviços, ao lado de "Cadastrar…"): uma lista de partida de auto elétrica e injeção programável (som, vidros e travas, partida e carga, iluminação, diagnóstico elétrico e injeção programável). Marque os que a oficina faz e preencha as **horas ou o preço** de cada um (digitar já marca a linha): a lista vem sem os dois, porque o Calibra não inventa tempo de serviço. "Adicionar ao catálogo" grava todos de uma vez; se faltar algo, a janela mostra o que e não grava nenhum. O que já existe no catálogo com o mesmo nome aparece como "já cadastrado". Cada um entra com o checklist da categoria dele (na injeção programável, só a confecção de chicote e a instalação). Depois, edite cada um como os outros (descrição, garantia própria, checklist).
- **Checklists:** o que conferir em cada tipo de serviço. Cada modelo tem o nome e os itens, na ordem em que se confere; cada item tem o texto e as marcas **obrigatório** (pendente, trava o pronto da OS até uma justificativa) e **a verificar** (o texto ainda não foi conferido). Na janela do modelo, "+ Item" acrescenta um item (já obrigatório); ↑ e ↓ mudam a ordem; ✕ tira. O item sem texto fica de fora. Vêm cadastrados os 7 modelos da SPEC (Som, Vidro e trava elétrica, Partida e alternador, Iluminação, Confecção de chicote para injeção programável, Instalação de injeção programável e Diagnóstico elétrico), com todos os itens obrigatórios e "a verificar": confira cada um, desmarque "obrigatório" no que for opcional e "a verificar" no que já conferiu. Onde o item pede para anotar algo (ex.: a medição), use a observação do item na OS. Mudar um modelo grava uma versão nova; as OS que já começaram ficam com a cópia que fizeram.
- **Itens:** o material que entra no orçamento, como cabo, fusível, relé, conector ou a ECU: o nome, a **unidade** (vem "un"; mude para m, par, kit...) e o preço de referência. A fonte já vem "Tabela da oficina". Não há saldo nem estoque. No orçamento, entram pelo "+ Item do catálogo…" (item 9).
- **Kits:** um conjunto de itens e serviços do catálogo, cada um com a quantidade (ex.: "Alimentação do som": 5 m de cabo, 1 fusível e a instalação). O kit com serviço é um **pacote** (ex.: "chicote + instalação"). Na janela do kit, "+ Item do catálogo…" e "+ Serviço do catálogo…" põem as linhas; mude a quantidade de cada uma ou tire com o ✕. O kit não tem preço próprio: no orçamento, ele vira as linhas, com o preço de agora de cada uma (item 9). A lista mostra as linhas ("2 itens, 1 serviço") e se é pacote.
- **Todo item tem fonte** (obrigatória), data, a marca **"a verificar"** (para dado ainda não confirmado), preço de referência (vai para o orçamento; o kit e o checklist não têm) e observações.
- **Editar cria uma versão nova**; "Versões…" compara as versões campo a campo, com a mudança em amarelo. Gravar sem mudar nada não cria versão.
- **Arquivar** tira o item das escolhas, mas ele continua nos cálculos antigos. Nada é apagado.
- **Duplicar** faz uma cópia para cadastrar uma peça parecida.
- **Tabelas** (tempo morto do bico, curva da bomba): uma linha por ponto. "Colar tabela" aceita o texto copiado do Excel ou do PDF (colunas separadas por tabulação ou ponto e vírgula; vírgula decimal).

### 8.1 Mapa do compressor (turbos)

Na aba Turbos, escolha o turbo e clique em **Mapa do compressor…**:

1. **Carregar imagem…**: a imagem do mapa do fabricante (PNG, JPG, GIF ou BMP). Ela é copiada para a pasta de anexos.
2. **Eixos:** clique em "Marcar X1" e depois num número da escala de vazão; digite o valor dele. Faça o mesmo com X2, e com Y1 e Y2 na escala de razão de pressão (PR). Escolha se a vazão está em lb/min ou kg/s.
3. **Referência do mapa:** a temperatura e a pressão da legenda do fabricante (ex.: Garrett: 545 °R e 13,95 psia), na unidade que estiver lá, e a rotação máxima.
4. **Desenho:** escolha o que marcar e clique na imagem: a linha de surge, a de choke, cada ilha de eficiência (digite a % antes) e cada linha de rotação (digite o rpm antes). Ctrl+Z desfaz o último ponto.
5. **Salvar mapa:** grava uma versão nova do turbo. Reabrir mostra o desenho por cima da mesma imagem, para continuar.

A escala dos eixos precisa ser linear (é a dos mapas de compressor comuns).

## 9. Orçamento

O orçamento pertence à OS. Ele aparece na aba **Orçamento** da OS e, quando a OS tem projeto de preparação, também na aba **Peças e orçamento** do projeto: é o mesmo orçamento nas duas (no projeto, o da OS em andamento; item 6.4). Com a OS entregue ou cancelada, ele fica só para ver.

- **De preparação** (a OS tem projeto): sai do último cálculo salvo do projeto, que precisa estar em dia.
- **Comum** (sem projeto, ex.: revisão, freios, suspensão, auto elétrica): os itens são digitados ou vêm do catálogo (serviços, itens e kits). O PDF e o texto de WhatsApp dele saem sem o motor, a meta, o resumo técnico e a nota do mapa base.

1. **Itens:**
   - no de preparação, as peças do cálculo vêm com o preço de referência do catálogo (bicos × número de cilindros, bomba, turbo, intercooler, sensor MAP e injeção); peça sem preço vem com o valor em branco;
   - "+ Serviço (mão de obra)" traz o valor da hora de Configurações; ajuste a quantidade de horas;
   - "+ Serviço do catálogo…" escolhe um serviço da aba Serviços dos Catálogos: com preço próprio, entra 1 serviço pelo preço; sem preço, entram as horas × o valor da hora de Configurações (sem o valor da hora configurado, o valor fica em branco para preencher). O texto da linha é a descrição do serviço no catálogo (sem descrição, o nome), e dá para mudar aqui;
   - "+ Item do catálogo…" escolhe um item da aba Itens: entra como peça, 1 × a unidade dele, pelo preço de referência (sem preço, o valor fica em branco para preencher);
   - "+ Kit do catálogo…" escolhe um kit da aba Kits, que vira uma linha para cada item e serviço dele, com a quantidade do kit e o preço de agora do catálogo (o serviço pela regra dele: preço próprio, ou horas × o valor da hora). Depois, mude ou apague cada linha à vontade (ex.: o cliente leva só o chicote, sem a instalação): o kit no catálogo não muda. Se um item ou serviço do kit estiver arquivado, ele entra mesmo assim, com um aviso em amarelo para conferir;
   - "+ Peça" acrescenta uma peça digitada.

   Os valores continuam editáveis, e os totais aparecem enquanto você digita. Na peça que o cliente traz (ex.: a ECU que ele já comprou), marque **"do cliente"**: ela entra com valor zero e não soma no total; desmarcar devolve o valor de antes.
2. **Desconto, prazo e condições:** desconto em reais ou em %, prazo em dias úteis (0 = a combinar), validade em dias, condições de pagamento e observações. Os padrões vêm de Configurações.
3. **Salvar rascunho:** grava o orçamento com o número `ORC-ano-sequência` (ex.: ORC-2026-0001). O rascunho pode ser editado à vontade.
4. **Gerar PDF…** e **Copiar texto para WhatsApp:** o PDF tem o cabeçalho da oficina com o logo, as peças, os serviços, os totais, o prazo e a validade; no de preparação, também o resumo técnico do cálculo (com o semáforo e as justificativas) e a nota do mapa base conservador. A peça do cliente sai com "(fornecido pelo cliente)" e R$ 0,00; no WhatsApp, "1 un, fornecido pelo cliente". O texto do WhatsApp vai para a área de transferência: cole na conversa (Ctrl+V) e envie você mesmo. O Calibra não envia nada. Se o Windows não deixar copiar naquele instante (outro programa usando a área de transferência), a tela avisa: clique de novo.
5. **Marcar como enviado:** depois de mandar ao cliente. A validade conta a partir daí; a OS passa para Aguardando aprovação e o projeto, se houver, para Orçado.
6. **Aprovado pelo cliente…** ou **Recusado…:** informe a data, a hora e como o cliente respondeu (pessoalmente, WhatsApp ou telefone). Aprovado leva a OS para Aprovada e o projeto para Aprovado. **Orçamento aprovado não muda mais.**
7. **Nova revisão:** para mudar um orçamento enviado, aprovado ou recusado. A revisão nova sai com o mesmo número ("ORC-2026-0001 rev. 2"), em rascunho, e a anterior fica como substituída. A tabela "Revisões" mostra todas.

Se o motor, o câmbio do veículo ou o cálculo do projeto mudarem, o orçamento aparece como **desatualizado** e não pode ser enviado. No rascunho, "Usar o cálculo mais novo" resolve; depois confira os itens ("Trazer as peças do cálculo de novo" refaz as peças e mantém os serviços e os itens da aba Itens, como cabos e relés; as peças digitadas são trocadas pelas do cálculo).

## 10. Backup e restauração

- **Automático:** o app confere ao abrir e depois de hora em hora, e faz um backup por dia. Ao fechar, se algo mudou, faz outro ("Fazendo backup antes de fechar…"). Ficam os 30 mais recentes.
- **O backup** é um arquivo `calibra-backup-AAAA-MM-DD-HHMMSS.zip` com o banco e um resumo com a versão, a conferência dos arquivos e a lista dos anexos. As fotos e os documentos ficam na pasta **`anexos-dos-backups`**, ao lado dos arquivos dos backups, **uma vez só**: uma foto que não mudou não é copiada de novo a cada backup. Por isso cada backup é pequeno, e a pasta cresce só com as fotos novas. A aba Backup mostra quanto ela ocupa. Um anexo removido enquanto o backup é feito fica de fora, sem atrapalhar o resto.
- **Para levar os backups para outro lugar** (outro computador, outro HD), copie a pasta de backup inteira, com a `anexos-dos-backups`. O arquivo `.zip` sozinho não tem as fotos.
- **Limpeza:** quando um backup antigo sai (ficam os 30 mais recentes), sai também da `anexos-dos-backups` o que nenhum backup restante usa.
- **Pasta:** em Configurações → Backup, dá para escolher outra pasta, como um pen drive ou um HD externo. Se ela não estiver ligada, o backup vai para a pasta padrão, com aviso na barra de baixo. Cada pasta tem a sua `anexos-dos-backups`.
- **Fazer backup agora:** a qualquer momento.
- **Restaurar um backup…:** escolha o backup na lista e confirme. Antes de trocar os dados, o app confere cada foto e documento pela impressão digital e guarda os dados atuais num backup de segurança na pasta padrão. Backup corrompido, de uma versão mais nova do app ou sem as fotos na `anexos-dos-backups` é recusado sem mexer em nada, com a explicação. O que estiver digitado e não salvo nas telas se perde.
- **Backups antigos** (feitos até a versão 0.3.0, com as fotos dentro do `.zip`) continuam restaurando normalmente. Eles saem da pasta conforme os novos entram.
- **Ao atualizar o app** e o banco precisar mudar de versão, o app faz um backup antes. Se a atualização do banco falhar, ele volta o backup e mostra a mensagem.

## 11. Mapa base

*(fase 4)*

## 12. Acerto, relatório e biblioteca

*(fase 6)*

## 13. Semáforo de segurança

Cada indicador importante sai em verde, amarelo ou vermelho. **Vermelho só segue com justificativa escrita**, gravada com data e hora. Os limites são editáveis em Configurações (item 4); a tabela mostra os padrões.

| Indicador | Verde | Amarelo | Vermelho |
|---|---|---|---|
| DC do bico (com tempo morto) na potência máxima | até 80% | 80% a 85% | acima de 85% |
| Folga da bomba | 30% ou mais | 15% a 30% | menos de 15% |
| Ponto no mapa do compressor (turbo com mapa) | perto do pico de eficiência | menos de 10% de vazão acima do surge, menos de 5% até o choke, acima de 95% da rotação máxima, fora das ilhas desenhadas ou mais de 5 pontos abaixo do pico | fora do mapa |
| Mapa do compressor (sem turbo com mapa) | — | sempre amarelo: confira o ponto no mapa do fabricante | — |
| BMEP (aspirado 13 bar; sobrealimentado 22 bar) | até a referência | até 15% acima | mais de 15% acima com pistões ou bielas originais (com os dois forjados, amarelo) |
| Velocidade média do pistão | até 20 m/s | 20 a 25 m/s | acima de 25 m/s |
| Saídas da injeção | suficientes | — | insuficientes |
| Faixa do sensor MAP | 1,2 × o MAP máximo ou mais | 1,0 a 1,2 × | abaixo do MAP máximo |
| Aspirado: MAP necessário ÷ pressão atmosférica local | até 0,90 | 0,90 a 0,98 | acima de 0,98 |
| Torque estimado × torque máximo do câmbio | até 90% | 90% a 100% | acima de 100% |

O mapa do compressor avalia os pontos de potência e de torque e fica com a pior cor dos dois. Sem câmbio do catálogo, ou sem o torque máximo cadastrado nele, o torque × câmbio vira só um aviso.

Os resultados são **estimativas** para dimensionar peças e preparar o mapa base. O acerto final é do calibrador, com wideband, no dinamômetro, na rua ou na água.
