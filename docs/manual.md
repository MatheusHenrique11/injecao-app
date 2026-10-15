# Manual do usuário — Calibra

Versão do app: 0.2.0 (fase 2). Este manual cresce a cada fase.

## 1. O que o Calibra faz

Transforma a meta do cliente (motor, potência, uso, combustível e injeção) em peças dimensionadas, orçamento, mapa base conservador e relatório final. O acerto fino continua sendo feito pelo calibrador, no software da própria injeção. O Calibra nunca grava nada na injeção e funciona sem internet.

Nesta versão (0.2.0) já funcionam: projetos com cliente, veículo e motor; a calculadora de dimensionamento; os catálogos de peças; o orçamento; o backup. O mapa base chega na fase 3.

## 2. Instalação

- **Instalar:** dê dois cliques em `Calibra-0.2.0.msi`. O Calibra instala só para o seu usuário do Windows, sem pedir senha de administrador, em `%LOCALAPPDATA%\Programs\Calibra`. Ele cria atalhos no menu Iniciar (pasta Calibra) e na área de trabalho. As telas do instalador são as do Windows, em inglês.
- **Atualizar:** instale a versão nova por cima, do mesmo jeito. Os dados ficam, e só uma versão aparece em "Aplicativos instalados".
- **Versão mais antiga por cima:** o Windows deixa instalar, mas ela não abre dados que uma versão mais nova já atualizou. O app avisa ("Instale a versão mais nova para abri-los") e não mexe em nada; é só instalar de novo a versão mais nova.
- **Desinstalar:** Configurações do Windows → Aplicativos → Aplicativos instalados → Calibra → Desinstalar. **Os dados não são apagados** (item 3); se reinstalar, tudo volta.
- O Calibra não usa internet: não atualiza sozinho, não manda nada para fora e não precisa de conta.

## 3. Onde ficam os dados

Tudo fica em `%LOCALAPPDATA%\Calibra\` (para abrir, cole esse endereço na barra do Explorador de Arquivos):

| Pasta ou arquivo | O que tem |
|---|---|
| `calibra.db` | o banco de dados: clientes, veículos, projetos, cálculos, catálogos, orçamentos e configurações |
| `anexos\` | o logo da oficina e as imagens dos mapas de turbo |
| `backups\` | os backups automáticos (a pasta pode ser trocada, item 10) |
| `logs\` | o registro do que o app fez, com o detalhe dos erros (para o suporte) |
| `cache\` | arquivos temporários das bibliotecas (fontes do PDF); pode ser apagada |

Essa pasta nunca fica junto do programa, e desinstalar não a apaga.

## 4. Primeiros passos: Configurações

Antes do primeiro orçamento, abra **Configurações** (menu à esquerda). Cada aba tem o próprio "Salvar" e "Descartar alterações".

- **Oficina:** nome, CNPJ, endereço, telefone, e-mail e logo (PNG ou JPG, até 5 MB). Tudo opcional; aparece no cabeçalho dos PDFs.
- **Cálculo e semáforo:** os padrões de cálculo (k_ar, DC máximo, eficiências, perdas...) e os limiares do semáforo (item 12). Valor diferente do padrão da SPEC aparece em azul. "Voltar aos padrões da SPEC" só preenche os campos: grava ao salvar. Os limiares do mapa do compressor e do torque × câmbio estão marcados "a validar".
- **Orçamento:** o que um orçamento novo traz preenchido: desconto, prazo em dias úteis (e a partir de quando conta), validade em dias, condições de pagamento e o valor da hora de mão de obra.
- **Backup:** a pasta dos backups, "Fazer backup agora" e "Restaurar um backup…" (item 9).

## 5. Projetos

### 5.1 Lista de projetos

A tela **Projetos** lista os projetos, do mexido por último para o mais antigo. A busca acha por cliente, placa, nome da embarcação, marca, modelo ou motor, sem precisar de acento ("joao" acha "João"; "abc1234" acha "ABC-1234"). Dá para filtrar pelo status e mostrar os arquivados.

### 5.2 Projeto novo (assistente)

"Novo projeto" abre o assistente, em cinco etapas. Cada etapa é conferida antes de seguir, e nada é gravado até o fim:

1. **Cliente:** um já cadastrado (com busca) ou novo. Nome e telefone são obrigatórios. O **consentimento** (data e finalidade) também: é a regra da LGPD (item 5.5). CPF ou CNPJ é opcional.
2. **Veículo:** um do cliente ou novo. Os campos mudam com o tipo: placa e chassi para carro, moto e utilitário; nome, inscrição e casco para barco, lancha e jet ski. O câmbio pode vir do catálogo (item 7).
3. **Motor:** o formulário do motor (item 6.1). "Preencher pelo catálogo" traz a geometria de um motor ou montagem cadastrada; o que o catálogo não tem fica em branco, e a ordem de ignição precisa ser conferida no manual do motor.
4. **Meta e uso:** potência (ou pressão do turbo, só sobrealimentado), onde é medida, rotações e uso.
5. **Injeção (opcional):** a injeção do catálogo, com o que falta nela para este motor e o botão "Sugerir".

### 5.3 Abas do projeto

- **Resumo:** cliente (editar, histórico e anonimizar), veículo, motor, meta, injeção, último cálculo, histórico de status e observações do projeto.
- **Veículo:** editável a qualquer momento. Cada salvamento grava, campo a campo, o valor antigo, o novo, a data e a hora; o histórico aparece embaixo.
- **Motor:** editar e "Salvar como versão nova" cria a v2, v3... Versões nunca mudam nem são apagadas. Dá para comparar duas versões e usar uma antiga como ponto de partida. Cálculos e orçamentos feitos com uma versão antiga passam a aparecer como **desatualizados**.
- **Dimensionamento:** a calculadora (item 6) com o motor do projeto. "Salvar cálculo no projeto" grava o cálculo com as justificativas dos vermelhos. A aba "Cálculos salvos" lista todos, com o motivo quando um ficou desatualizado (motor novo, núcleo de cálculo novo, peça do catálogo com versão nova ou câmbio do veículo trocado), e gera o PDF de qualquer um deles.
- **Peças e orçamento:** o orçamento (item 8).

### 5.4 Status do projeto

Rascunho → Dimensionado (primeiro cálculo salvo) → Orçado (orçamento enviado) → Aprovado (orçamento aprovado) → Mapa base → Em acerto → Concluído. Anda para a frente, um passo de cada vez. "Arquivar projeto" tira o projeto da lista a qualquer momento; "Reativar" volta ao status de antes.

### 5.5 Dados pessoais (LGPD)

O Calibra guarda só o necessário do cliente: nome e telefone, e CPF ou CNPJ quando precisar de documento, com o consentimento registrado. **Anonimizar cliente** (aba Resumo) apaga os dados pessoais dele e dos veículos dele (o nome vira "Cliente anonimizado #N"; placa, chassi e identificação da embarcação somem) e mantém os projetos, cálculos e orçamentos. **Não tem volta.** PDFs gerados depois disso já saem anonimizados.

## 6. Calculadora e dimensionamento

A **Calculadora** (menu à esquerda) faz a conta sem gravar nada. A aba **Dimensionamento** do projeto é a mesma calculadora, com o motor do projeto e o botão de salvar.

### 6.1 Motor

- **Número de cilindros:** de 3 a 12. Ao trocar, o app sugere de novo a ordem de ignição, os ângulos dos PMS e o arranjo. Se você já tinha editado esses campos, ele pergunta antes. **As sugestões precisam ser conferidas no manual do motor.**
- **Ordem de ignição:** números separados por traço (ex.: `1-3-4-2`). Começa sempre no cilindro 1.
- **Ângulos dos PMS:** um por cilindro, na ordem de ignição, separados por ponto e vírgula (ex.: `0; 180; 360; 540`).
- **Modos de injeção e ignição:** com número ímpar de cilindros, só aparecem os modos possíveis. Injeção ou ignição sequencial exigem sensor de fase.
- **Combustível:** do catálogo de combustíveis, com AFR, densidade e vigência (a gasolina comum está E32 desde 01/08/2026).

### 6.2 Meta e peças

- **Meta pela potência:** potência em cv, medida no virabrequim ou na roda (na roda, informe a perda de transmissão).
- **Meta pela pressão do turbo** (só motor sobrealimentado): você informa a pressão acima da atmosfera (em bar, psi ou kPa), e o cálculo estima a potência. Com "medida na roda" e a perda, ele estima também a potência na roda.
- **Rotação de torque máximo** (opcional): dá o ponto de torque. A **VE no torque máximo** é opcional; sem ela, vale a VE da potência. Com a mesma pressão e a mesma VE, o torque estimado sai igual nas duas rotações; a diferença real vem da VE e da pressão que o turbo entrega naquele giro.
- **VE na potência máxima:** sem padrão, você informa, em fração (0,95 = 95%).
- **k_ar:** o padrão de 10,5 serve para motor moderno de 4 válvulas; motor antigo de 2 válvulas costuma ficar perto de 8,5 a 9,5 (referência a validar). O app aceita de 8,0 a 11.
- **Peças do catálogo (opcional):** bico, bico da bancada secundária, bomba, intercooler, sensor MAP, injeção, turbo e câmbio. Peça escolhida vale no lugar dos campos digitados dela, que ficam travados. A **tensão de trabalho** lê a tabela de tempo morto do bico e a curva da bomba.
- **Sugerir:** escolhe no catálogo o menor bico que mantém o DC, a menor bomba com folga verde, a primeira injeção que atende o motor, ou, em **Sugerir turbos**, o turbo mais eficiente entre os que têm os pontos de potência e de torque dentro do mapa, com o lembrete do A/R da caixa quente.
- **Câmbio:** com o torque máximo cadastrado, entra o indicador torque × câmbio. No projeto, vale o câmbio do veículo (troque na aba Veículo).

**Números:** use vírgula para decimais (`3,5`). O ponto é separador de milhar (`1.050` = mil e cinquenta). O app recusa `3.5` para não virar 35 sem ninguém perceber.

### 6.3 Resultado

Clique em **Calcular** (ou tecle Enter). O resultado mostra:

1. **Semáforo:** verde, amarelo ou vermelho, sempre com a cor escrita e a explicação (item 12).
2. **Resultados** em várias unidades (ex.: 241,0 kPa · 2,41 bar · 34,95 psia), com o ponto de torque e os pontos no mapa do turbo.
3. **Mapa do compressor:** com turbo do catálogo que tenha mapa, o mapa aparece desenhado com o ponto de potência (círculo) e o de torque (quadrado).
4. **Recomendações, avisos e a memória de cálculo**, com cada conta, a fórmula e as entradas.

Se você mudar qualquer campo depois de calcular, o resultado fica **desatualizado** e o PDF é bloqueado até calcular de novo.

### 6.4 PDF da memória de cálculo

**Gerar PDF da memória…**: antes, escreva a justificativa de cada item vermelho, no campo que aparece embaixo dele. Sem isso, o PDF não sai. O PDF leva o motor, a meta, as peças, o semáforo com as justificativas (com data e hora), os resultados, as recomendações, os avisos e a memória completa, com a versão do app e do núcleo de cálculo.

## 7. Catálogos

**Catálogos** (menu à esquerda) guarda as peças e os dados técnicos, em abas: Bicos, Bombas, Injeções, Turbos, Combustíveis, Intercoolers, Sensores, Câmbios, e Motores e montagens.

- **As peças começam vazias:** cadastre a partir do manual ou do site do fabricante. Combustíveis e motores já vêm cadastrados (os motores marcados "a verificar").
- **Todo item tem fonte** (obrigatória), data, a marca **"a verificar"** (para dado ainda não confirmado), preço de referência (vai para o orçamento) e observações.
- **Editar cria uma versão nova**; "Versões…" compara as versões campo a campo, com a mudança em amarelo. Gravar sem mudar nada não cria versão.
- **Arquivar** tira o item das escolhas, mas ele continua nos cálculos antigos. Nada é apagado.
- **Duplicar** faz uma cópia para cadastrar uma peça parecida.
- **Tabelas** (tempo morto do bico, curva da bomba): uma linha por ponto. "Colar tabela" aceita o texto copiado do Excel ou do PDF (colunas separadas por tabulação ou ponto e vírgula; vírgula decimal).

### 7.1 Mapa do compressor (turbos)

Na aba Turbos, escolha o turbo e clique em **Mapa do compressor…**:

1. **Carregar imagem…**: a imagem do mapa do fabricante (PNG, JPG, GIF ou BMP). Ela é copiada para a pasta de anexos.
2. **Eixos:** clique em "Marcar X1" e depois num número da escala de vazão; digite o valor dele. Faça o mesmo com X2, e com Y1 e Y2 na escala de razão de pressão (PR). Escolha se a vazão está em lb/min ou kg/s.
3. **Referência do mapa:** a temperatura e a pressão da legenda do fabricante (ex.: Garrett: 545 °R e 13,95 psia), na unidade que estiver lá, e a rotação máxima.
4. **Desenho:** escolha o que marcar e clique na imagem: a linha de surge, a de choke, cada ilha de eficiência (digite a % antes) e cada linha de rotação (digite o rpm antes). Ctrl+Z desfaz o último ponto.
5. **Salvar mapa:** grava uma versão nova do turbo. Reabrir mostra o desenho por cima da mesma imagem, para continuar.

A escala dos eixos precisa ser linear (é a dos mapas de compressor comuns).

## 8. Orçamento

O orçamento fica na aba **Peças e orçamento** do projeto e sai do último cálculo salvo (que precisa estar em dia).

1. **Itens:** as peças do cálculo vêm com o preço de referência do catálogo (bicos × número de cilindros, bomba, turbo, intercooler, sensor MAP e injeção). Peça sem preço vem com o valor em branco. "+ Serviço (mão de obra)" traz o valor da hora de Configurações; ajuste a quantidade de horas. "+ Peça" acrescenta uma peça digitada. Os totais aparecem enquanto você digita.
2. **Desconto, prazo e condições:** desconto em reais ou em %, prazo em dias úteis (0 = a combinar), validade em dias, condições de pagamento e observações. Os padrões vêm de Configurações.
3. **Salvar rascunho:** grava o orçamento com o número `ORC-ano-sequência` (ex.: ORC-2026-0001). O rascunho pode ser editado à vontade.
4. **Gerar PDF…** e **Copiar texto para WhatsApp:** o PDF tem o cabeçalho da oficina com o logo, as peças, os serviços, os totais, o prazo, a validade, o resumo técnico do cálculo (com o semáforo e as justificativas) e a nota do mapa base conservador. O texto do WhatsApp vai para a área de transferência: cole na conversa (Ctrl+V) e envie você mesmo. O Calibra não envia nada.
5. **Marcar como enviado:** depois de mandar ao cliente. A validade conta a partir daí, e o projeto passa para Orçado.
6. **Aprovado pelo cliente…** ou **Recusado…:** informe a data, a hora e como o cliente respondeu (pessoalmente, WhatsApp ou telefone). Aprovado leva o projeto para Aprovado. **Orçamento aprovado não muda mais.**
7. **Nova revisão:** para mudar um orçamento enviado, aprovado ou recusado. A revisão nova sai com o mesmo número ("ORC-2026-0001 rev. 2"), em rascunho, e a anterior fica como substituída. A tabela "Revisões" mostra todas.

Se o motor, o câmbio do veículo ou o cálculo do projeto mudarem, o orçamento aparece como **desatualizado** e não pode ser enviado. No rascunho, "Usar o cálculo mais novo" resolve; depois confira os itens ("Trazer as peças do cálculo de novo" refaz as peças e mantém os serviços).

## 9. Backup e restauração

- **Automático:** o app confere ao abrir e depois de hora em hora, e faz um backup por dia. Ao fechar, se algo mudou, faz outro ("Fazendo backup antes de fechar…"). Ficam os 30 mais recentes.
- **O backup** é um arquivo `calibra-backup-AAAA-MM-DD-HHMMSS.zip` com o banco, os anexos e um resumo com a versão e a conferência dos arquivos.
- **Pasta:** em Configurações → Backup, dá para escolher outra pasta, como um pen drive ou um HD externo. Se ela não estiver ligada, o backup vai para a pasta padrão, com aviso na barra de baixo.
- **Fazer backup agora:** a qualquer momento.
- **Restaurar um backup…:** escolha o backup na lista e confirme. Antes de trocar os dados, o app guarda os dados atuais num backup de segurança na pasta padrão. Backup corrompido ou de uma versão mais nova do app é recusado sem mexer em nada. O que estiver digitado e não salvo nas telas se perde.
- **Ao atualizar o app** e o banco precisar mudar de versão, o app faz um backup antes. Se a atualização do banco falhar, ele volta o backup e mostra a mensagem.

## 10. Mapa base

*(fase 3)*

## 11. Acerto, relatório e biblioteca

*(fase 4)*

## 12. Semáforo de segurança

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
