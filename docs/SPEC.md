# SPEC — GarageTech (antes Calibra)

**Nome comercial desde a versão 0.3.3 (21/10/2026):** GarageTech — Gestão e Tecnologia Automotiva. "Calibra" era o nome provisório. Ele fica no código (pacotes, módulos), na pasta de dados, no histórico deste documento e nas decisões. A marca está em docs/marca/identidade-visual.md, e as decisões nos itens 282 a 287.

App desktop offline da minha oficina: gestão do dia a dia (ordens de serviço, agenda e lembretes), auto elétrica e preparação (dimensionamento, orçamento, mapa base e acerto de injeção programável).

Versão da spec: 1.2 (18/10/2026; a 1.1 é de 15/10/2026 e a 1.0 de 08/10/2026). Dono do produto: Matheus, mecânico de preparação e auto elétrica, que valida todas as regras técnicas.

---

## 0. Como trabalhar comigo

1. Sou mecânico (turbo e injeção programável) e trabalho também com auto elétrica (som, vidros e travas elétricas, partida e alternador, iluminação e diagnóstico elétrico). Programo Java em nível básico a intermediário; já fiz projetos com Java 21 e Spring Boot. Quero entender e conseguir manter o código.
2. Trabalhe **uma fase por vez** (seção 12). Em cada fase:
   1. leia só o que precisa e me mostre o plano em modo de plano;
   2. implemente em passos pequenos;
   3. rode `mvn -q verify` e mostre a saída;
   4. faça commit com mensagem `fase N: <o que mudou>` (não faça push sem eu pedir);
   5. pare no critério de pronto da fase e me mostre a evidência (saída dos testes, captura de tela ou PDF gerado). Só avance quando eu aprovar.
3. Depois de cada passo, explique em português simples, em até 10 linhas, o que mudou, por quê e em quais arquivos.
4. **Pergunte antes de:** adicionar dependência, mudar versão, mudar arquitetura, apagar dados ou arquivos, instalar qualquer coisa no meu computador.
5. **Não invente dado técnico** (vazão de bico, tempo morto, mapa de compressor, ordem de ignição, recursos de uma injeção). Se faltar, crie o campo editável, deixe vazio ou marque `aVerificar = true` e me avise.
6. Se algo desta spec parecer errado ou perigoso para um motor, pare e me explique antes de seguir.
7. Use subagentes para varreduras longas no código e para revisar o diff contra esta spec no fim de cada fase; relate só falhas que afetem as regras ou o critério de pronto.

---

## 1. Produto e escopo

O app transforma a meta do cliente (motor, potência, uso, combustível e injeção) em:

1. peças dimensionadas, cada uma com justificativa e memória de cálculo;
2. orçamento e prazo;
3. mapa base conservador no formato de cada injeção;
4. registro do acerto, mapa final, relatório ao cliente e biblioteca pesquisável de projetos.

Também faz a **gestão da oficina**:
- ordens de serviço: entrada do veículo com vistoria e fotos, orçamento, execução, entrega com garantia e registro do pagamento;
- painel do dia e ficha do veículo com todo o histórico;
- agenda e lembretes (fase 5).

O projeto de preparação passa a ser uma parte opcional da ordem de serviço.

A oficina atende duas linhas de serviço, na mesma ordem de serviço: **preparação** (projeto, dimensionamento e mapa base) e **auto elétrica** (instalações e reparos elétricos, diagnóstico), com a **injeção programável como serviço**: confecção de chicote e instalação da injeção, contratados só um deles ou os dois. Na auto elétrica não há projeto de preparação: a OS usa o catálogo de serviços, os checklists de execução e o laudo de diagnóstico elétrico (seção 6.6). Reconstrução de chicote e modelagem de caixa de fusíveis entram em fase posterior (Fase 8).

**Usuário:** só a minha oficina, num computador, sem login; os mecânicos entram só como nomes. O app é gratuito: sem assinatura, sem cobrança pelo software e sem venda ou licença para outras oficinas.
**Plataforma:** Windows 10 e 11, 64 bits, notebook comum (i5, 8 GB de RAM). **Sem internet.**

**Dentro do escopo:** motores 4 tempos de ignição por centelha (gasolina, etanol, metanol e misturas), **de 3 a 12 cilindros**, aspirados ou sobrealimentados, em carros, motos, utilitários, barcos, lanchas e jet skis.

**Fora do escopo:**
- gravar mapa direto na injeção (USB, CAN ou serial);
- engenharia reversa de arquivos ou protocolos fechados;
- acerto automático;
- nuvem, sincronização ou uso em rede por vários computadores;
- motores rotativos, diesel ou 2 tempos;
- venda ou licença para outras oficinas;
- login e perfis de usuário;
- controle de estoque e financeiro (caixa, contas a pagar e a receber), por enquanto. Itens e kits de material existem só para compor o orçamento: não há saldo, entrada nem saída de estoque;
- emissão de nota fiscal: o app só guarda o PDF da nota emitida no site da prefeitura;
- avisos automáticos ou acompanhamento pela internet: o app gera o texto para o WhatsApp e o PDF.

Alterado em 15/10/2026 (gestão da oficina) — ver docs/decisoes.md, itens 162 a 167. Alterado em 18/10/2026 (auto elétrica) — ver docs/decisoes.md, itens 213 a 226.

---

## 2. Regras inegociáveis

1. **Zero rede.** Nenhum código abre conexão de rede: sem telemetria, sem atualização automática, sem fonte ou ícone baixado em tempo de execução. Crie um teste que varre `src/main` de todos os módulos e falha se encontrar `java.net.http`, `HttpURLConnection`, `java.net.Socket` ou `openConnection`.
2. **Nunca grava na injeção.** A saída é sempre arquivo, área de transferência ou PDF.
3. **Conservador por padrão** (seção 7.5).
4. **Semáforo** (seção 7.4): resultado vermelho só avança com justificativa escrita, gravada no projeto com data e hora.
5. **Memória de cálculo:** todo resultado salvo guarda entradas, constantes, fórmula usada, avisos e a versão do núcleo. Nunca é recalculado em silêncio.
6. **Unidades SI dentro do núcleo;** conversão só na interface (bar, psi, kPa; °C, °F; cv, hp, kW; cc/min, lb/h, g/min).
7. **Dados de fabricantes não vêm com o app.** Vazões, tempos mortos, mapas de compressor e fichas de injeção são cadastrados ou importados pelo usuário; cada item guarda fonte e data.
8. **LGPD:** dados pessoais mínimos (nome e telefone; CPF ou CNPJ só para documento), finalidade informada e função "anonimizar cliente", que apaga os dados pessoais e mantém os técnicos.
9. **Interface em português do Brasil;** vírgula decimal na tela; ponto decimal em arquivos e JSON.

---

## 3. Ambiente de desenvolvimento

- Windows 11, VS Code com Claude Code e o Extension Pack for Java.
- Eclipse Temurin 25 LTS: `java -version` deve mostrar 25.0.x.
- Apache Maven 3.9.x: `mvn -v`. Não usar Maven 4, que ainda não saiu como versão estável.
- Git, com repositório privado no GitHub.
- WiX Toolset para o instalador MSI, a partir da fase 2 (o jpackage do JDK 24 em diante aceita WiX 3, 4 ou 5).

Se faltar algo, me diga o comando de instalação e espere eu confirmar.

---

## 4. Stack (versões fixas — não trocar sem me perguntar)

| Peça | Escolha | Versão |
| --- | --- | --- |
| Linguagem e runtime | Eclipse Temurin (OpenJDK) | 25 LTS (25.0.x) |
| Interface | OpenJFX (JavaFX) com FXML e CSS | 25.0.x (LTS; exige JDK 23 ou mais novo) |
| Desenho de telas (opcional) | Gluon Scene Builder | 25 ou 26 |
| Banco de dados | SQLite via `org.xerial:sqlite-jdbc` | 3.53.x |
| Migrações | Flyway Community | 12.x — confirme se o SQLite exige módulo extra nessa linha; se exigir licença paga, troque por migração própria com `PRAGMA user_version` e me avise |
| JSON | Jackson Databind | 2.x, última estável |
| PDF | Apache PDFBox | 3.0.8 ou mais nova da linha 3.0 (nunca abaixo de 3.0.7, que trouxe correção de segurança) |
| Testes | JUnit Jupiter | 6.x |
| Build | Apache Maven | 3.9.x |
| Rodar em desenvolvimento | `org.openjfx:javafx-maven-plugin` | última estável |
| Instalador | `jlink` + `jpackage` do JDK, com WiX | — |
| Logs | `java.util.logging` / `System.Logger` | do JDK, sem dependência extra |

Antes de adicionar qualquer dependência ou plugin, confirme na Maven Central a versão estável mais recente compatível e me mostre. Nada de alpha, beta, RC ou milestone. Sem Spring, sem Lombok.

---

## 5. Arquitetura

Projeto Maven multimódulo. Pacote base provisório: `br.com.calibra`.

```
calibra/
├─ pom.xml                  (pai: Java 25, versões e plugins)
├─ calibra-core/            (cálculo puro: sem JavaFX, sem JDBC, sem arquivos)
├─ calibra-ecu/             (perfis de injeção; exportar e importar tabelas)
├─ calibra-dados/           (SQLite, migrações, repositórios, anexos, backup)
├─ calibra-relatorios/      (PDFs)
├─ calibra-app/             (JavaFX: telas e casos de uso)
├─ scripts/gerar-instalador.ps1
├─ docs/SPEC.md · docs/decisoes.md · docs/manual.md
└─ CLAUDE.md · CHANGELOG.md · README.md
```

- **Regra de dependência:** `calibra-core` não depende de nenhum outro módulo; `calibra-ecu`, `calibra-dados` e `calibra-relatorios` dependem só do core; `calibra-app` depende de todos. Nenhum módulo depende do app.
- **Core:** records imutáveis, sealed interfaces e funções puras. Todo cálculo devolve valor + memória de cálculo + avisos.
- **Persistência:** JDBC com repositórios simples (sem ORM). Tabelas de mapa e dados técnicos flexíveis em JSON numa coluna TEXT; campos de busca em colunas próprias. SQLite com `journal_mode=WAL` e `foreign_keys=ON`.
- **Pasta de dados:** `%LOCALAPPDATA%\Calibra\` com `calibra.db`, `anexos\`, `backups\` e `logs\`. Nunca na pasta de instalação.
- **Nomes:** domínio em português sem acentos (`Motor`, `ConfiguracaoMotor`, `Bico`, `Turbo`, `Injecao`, `Projeto`, `Orcamento`, `Mapa`, `Tabela`, `Eixo`); textos de tela em português com acentos.
- Registre cada decisão técnica relevante em `docs/decisoes.md` (data, decisão, motivo).

---

## 6. Domínio

### 6.1 Cliente

Nome, telefone, e-mail (opcional), CPF ou CNPJ (opcional), consentimento registrado (data e finalidade), observações. Um cliente tem vários veículos. Anonimizar substitui os dados pessoais por "Cliente anonimizado #id" e mantém projetos e mapas.

### 6.2 Veículo — editável a qualquer momento, com histórico

| Campo | Regra |
| --- | --- |
| `tipo` | CARRO, PICAPE_UTILITARIO, MOTO, QUADRICICLO_UTV, BARCO, LANCHA, JET_SKI, OUTRO |
| `marca`, `modelo`, `versao` | texto; marca e modelo obrigatórios |
| `anoFabricacao`, `anoModelo` | 1900 até o ano atual + 1 |
| Identificação terrestre | `placa` e `chassi` opcionais (aparecem só para tipos terrestres) |
| Identificação náutica | `nomeEmbarcacao`, `numeroInscricao` e `numeroCasco` opcionais (aparecem só para tipos náuticos) |
| `uso acumulado` | km para terrestres; horas de motor para náuticos |
| `cambio` | MANUAL, AUTOMATICO, AUTOMATIZADO, DUPLA_EMBREAGEM, CVT, NAUTICO_RABETA_OU_REVERSOR, JATO |
| `cambioControladoPelaInjecao` | sim ou não; só para AUTOMATICO, AUTOMATIZADO e DUPLA_EMBREAGEM |
| `cambioItemId` | opcional: câmbio do catálogo (8.5), do mesmo tipo do `cambio`; o torque máximo dele entra no indicador torque × câmbio (7.4). Alterado em 14/10/2026 — ver docs/decisoes.md, itens 93 e 111 |
| `tracao` | DIANTEIRA, TRASEIRA, INTEGRAL (só terrestres) |
| `pesoKg` | opcional |
| `cor`, `observacoes` | opcionais |

- Todo salvamento grava no log de auditoria: campo, valor antigo, valor novo, data e hora. A aba Veículo mostra esse histórico.
- Veículo com projetos não pode ser apagado, só arquivado.

**Instalações do veículo (Fase 3B).** Lista de itens instalados ou modificados pela oficina ou informados pelo cliente, visível na ficha do veículo e editável com histórico (mesmo log de auditoria do veículo).

| Campo | Regra |
| --- | --- |
| `tipo` | SOM, ALARME_RASTREADOR, ILUMINACAO, VIDRO_TRAVA, INJECAO_PROGRAMAVEL, OUTRA; obrigatório |
| `descricao` | texto obrigatório (ex.: o que foi instalado e onde) |
| `marca`, `modelo` | texto opcional |
| `potenciaOuEspecificacao` | texto opcional; não é validado |
| `dataInstalacao` | opcional; não pode estar no futuro nem depois da retirada. Na instalação registrada pela OS, vem a data do dia |
| `osId` | opcional: a OS de origem, do mesmo veículo; quando existe, o item aparece na OS (aba Execução). Sem OS: o item informado pelo cliente ou feito antes do app |
| `observacoes` | texto opcional |
| `retiradaEm` | opcional: a data em que o item saiu do veículo, nem no futuro nem antes da instalação. O item continua na lista, marcado; a retirada se desfaz (acrescentado em 20/10/2026, escolha do dono) |

Serve de histórico para garantia e diagnóstico (o que já foi mexido no carro) e prepara o terreno do chicote (Fase 8). Veículo arquivado mantém as instalações. Alterado em 18/10/2026 — ver docs/decisoes.md, item 213. Detalhes fixados no bloco 3B.5 (20/10/2026 — ver docs/decisoes.md, itens 254 a 260):

- **Onde se muda:** pela ficha do veículo, sempre, também com a OS de origem entregue. Na OS, as instalações dela se registram e se mudam até a entrega ou o cancelamento; depois, a OS só as mostra (escolha do dono em 20/10/2026), e a esquecida se registra pela ficha, com aquela OS de origem.
- **Histórico:** cada instalação tem o seu, no mesmo log de auditoria do veículo (a criação, cada campo mudado com o antes e o depois, a retirada e o desfazer). O histórico do veículo continua só com o cadastro dele (escolha do dono em 20/10/2026).
- **Apagar:** só a instalação cadastrada por engano, com confirmação; o histórico dela vai junto. O item que saiu do carro se retira (escolha do dono em 20/10/2026).
- **LGPD:** a instalação não é dado pessoal e fica igual quando o cliente é anonimizado.

### 6.3 Configuração do motor — editável, versionada, de 3 a 12 cilindros

| Campo | Regra |
| --- | --- |
| `numeroCilindros` | inteiro de **3 a 12**. Fora disso: erro "O app aceita motores de 3 a 12 cilindros". |
| `arranjo` | EM_LINHA, V, BOXER, VR_W. Combinações raras (V ou boxer com número ímpar, em linha com mais de 6) geram aviso amarelo, nunca bloqueio. |
| `anguloEntreBancadas` | graus; obrigatório para V e VR_W (ex.: 60, 90; 15 no VR). |
| `diametroMm`, `cursoMm` | maiores que zero. Cilindrada calculada = π/4 × D² × S × n. |
| `cilindradaInformadaCc` | opcional; diferença acima de 2% da calculada gera aviso. |
| `taxaCompressao` | 6,0 a 16,0; acima de 11,0 com sobrealimentação gera aviso amarelo. |
| `valvulasPorCilindro` | 2 a 5. |
| `comando` | ORIGINAL, LEVE, MEDIO, AGRESSIVO; duração e cruzamento opcionais. |
| `aspiracao` | ASPIRADO, TURBO, BITURBO, COMPRESSOR, COMPRESSOR_E_TURBO. |
| `pistoes`, `bielas` | ORIGINAL ou FORJADO, cada um. |
| `rotacaoMaxima` | 1.000 a 12.000 rpm; acima de 9.000 gera aviso. |
| `rotacaoMarchaLenta` | 500 a 1.500 rpm. |
| `ordemIgnicao` | exatamente n números, permutação de 1..n, começando em 1. O app sugere modelos comuns marcados "confirmar no manual do motor"; sempre editável. |
| `angulosPms` | n ângulos do virabrequim (0 a 719°), um por cilindro, na ordem de ignição. Padrão: espaçamento igual de 720/n. Editável para motores de ignição irregular. |
| `bicosPorCilindro` | 1 ou 2 (bancadas primária e secundária). Bicos extras no corpo de borboleta: opcionais, contados à parte. |
| `modoInjecao` | SEQUENCIAL, SEMISSEQUENCIAL, SIMULTANEO. |
| `modoIgnicao` | SEQUENCIAL (bobina individual), CENTELHA_PERDIDA, DISTRIBUIDOR. |
| `rodaFonica` | dentes totais e faltantes (ex.: 60-2, 36-1) e sensor de fase sim ou não. |
| `combustivel` | do catálogo (seção 7.2) e sensor de etanol sim ou não. |
| `altitudeLocalM` | 0 a 3.000 m. |

**Regras de combinação (bloqueiam e explicam o motivo):**

- CENTELHA_PERDIDA e SEMISSEQUENCIAL exigem que todo cilindro tenha um par defasado 360° no virabrequim (calcule pelos `angulosPms`). Com número ímpar de cilindros isso nunca acontece: em motores de 3, 5, 7, 9 e 11 cilindros, o app oferece só SEQUENCIAL ou DISTRIBUIDOR na ignição e SEQUENCIAL ou SIMULTANEO na injeção.
- Injeção ou ignição SEQUENCIAL exigem sensor de fase.
- Mudar `numeroCilindros` recria `ordemIgnicao` e `angulosPms` com sugestões e pede confirmação antes de salvar.

**Saídas exigidas** (calculadas na tela e conferidas contra a injeção escolhida):

- Bicos: SEQUENCIAL = n × `bicosPorCilindro`; SEMISSEQUENCIAL = (n/2) × `bicosPorCilindro`; SIMULTANEO = 1 ou 2 grupos, conforme o perfil da injeção.
- Ignição: SEQUENCIAL = n; CENTELHA_PERDIDA = n/2; DISTRIBUIDOR = 1.
- Também confira impedância do bico contra o driver da injeção e o máximo de bicos por saída, quando o perfil informar.
- Saídas insuficientes = vermelho, com a mensagem "A injeção X tem Y saídas de bico; este motor pede Z" e a sugestão de drivers externos ou de outra injeção do catálogo que atenda.

**Edição e versões:**

- Todo salvamento cria uma nova versão imutável da configuração (v1, v2...). A aba Motor mostra o histórico com campo, antes, depois e data.
- Cálculos, orçamentos e mapas apontam para a versão que usaram. Se a versão atual for mais nova, eles aparecem como "desatualizado — recalcular". Recalcular cria um resultado novo; o antigo fica no histórico.
- Versão usada por orçamento ou mapa não pode ser apagada; pode ser duplicada como ponto de partida.

**Testes obrigatórios (fase 1, exceto o último):**

- Parametrizado para n = 3 a 12: configuração padrão válida, cilindrada correta, saídas exigidas corretas, ordem de ignição sugerida é permutação válida, ângulos padrão com espaçamento 720/n.
- n = 2 e n = 13 rejeitados com a mensagem certa.
- n ímpar: CENTELHA_PERDIDA e SEMISSEQUENCIAL rejeitados; n par com ângulos iguais: aceitos.
- Ordem de ignição com número repetido, faltando ou de tamanho errado: rejeitada.
- SEQUENCIAL sem sensor de fase: rejeitado.
- Fase 2: editar o motor cria versão nova e marca os resultados antigos como desatualizados.

### 6.4 Projeto e meta

| Campo | Regra |
| --- | --- |
| `potenciaMetaCv` e `pontoMedicao` | RODA ou VIRABREQUIM. Se RODA, `perdaTransmissaoPct` é obrigatório (o usuário informa) e o cálculo converte para virabrequim. |
| meta pela pressão | alternativa à potência, só para sobrealimentado: a pressão do turbo acima da atmosférica (bar, psi ou kPa); a potência sai do cálculo (7.1). Com RODA e a perda, o cálculo estima também a potência na roda. Alterado em 14/10/2026 — ver docs/decisoes.md, itens 117 e 127 |
| `rotacaoPotenciaMaxima` | obrigatória; até a `rotacaoMaxima` do motor. |
| `rotacaoTorqueMaximo` | obrigatória para gerar mapa base; com ela, o cálculo estima o ponto de torque (7.1). |
| `veTorque` | opcional: VE na rotação de torque máximo; sem ela, vale a VE da potência. Alterado em 14/10/2026 — ver docs/decisoes.md, item 116 |
| `uso` | RUA, ARRANCADA, CIRCUITO_ENDURANCE, OFF_ROAD, NAUTICO. |
| `injecaoEscolhida` | do catálogo; opcional até a etapa de mapa base. |
| `status` | RASCUNHO → DIMENSIONADO → ORCADO → APROVADO → MAPA_BASE → EM_ACERTO → CONCLUIDO; ARQUIVADO a qualquer momento. A partir da fase 3, ORCADO e APROVADO acompanham o orçamento da ordem de serviço ligada ao projeto (6.5). Alterado em 15/10/2026 — ver docs/decisoes.md, item 166 |

### 6.5 Ordem de serviço (OS) — fase 3

O registro de um veículo na oficina, da entrada à entrega. É o centro da gestão; o projeto de preparação (6.4) é opcional e fica ligado a uma OS. Alterado em 15/10/2026 — ver docs/decisoes.md, itens 162 a 167.

| Campo | Regra |
| --- | --- |
| `numero` | OS-AAAA-NNNN, sequencial por ano. |
| cliente e veículo | Obrigatórios; os cadastros de 6.1 e 6.2. |
| `projeto` | Opcional: o projeto de preparação ligado. Um projeto pode ter várias OS ao longo do tempo (ex.: segundo estágio). |
| entrada | Data e hora; km (ou horas de motor, nos náuticos), que atualiza o uso acumulado do veículo, com histórico; nível do tanque (reserva, 1/4, 1/2, 3/4, cheio); vistoria (itens com observação, como riscos, vidros e estepe); objetos deixados no veículo; fotos; serviço pedido, nas palavras do cliente; mecânico (cadastro simples: nome e ativo); previsão de entrega. |
| `status` | ABERTA → AGUARDANDO_APROVACAO → APROVADA → EM_EXECUCAO ⇄ AGUARDANDO_PECAS → PRONTA → ENTREGUE. CANCELADA a qualquer momento antes de ENTREGUE, com o motivo. Cada mudança fica no histórico com data e hora. |
| orçamento | Pertence à OS, com os itens, revisões e aprovação da fase 2. Na OS de preparação, aponta para o cálculo do projeto, como hoje; na OS comum, não aponta para cálculo. Enviar o orçamento leva a OS a AGUARDANDO_APROVACAO; aprovar, a APROVADA; a recusa deixa a OS aberta para uma revisão nova ou para cancelar. |
| entrega | Data e hora; km de saída; garantia em dias, de 0 a 3650 (padrão em Configurações › OS: 90 dias, decidido em 17/10/2026; o limite, em 18/10/2026, item 208) e a data em que ela vence; pagamento: forma (dinheiro, PIX, cartão de débito, cartão de crédito, transferência ou outro), valor e situação (pago, parcial ou pendente), com o recebido a mais à vista. Não há caixa nem contas. |
| anexos | Fotos, documentos, nota fiscal (PDF emitido no site da prefeitura) e laudos, na OS e no veículo; validade opcional, que gera aviso na fase 5. |

- **OS não é apagada:** a cancelada fica no histórico. Depois de ENTREGUE, só o pagamento e os anexos mudam.
- **Correções da abertura e da entrada** (bloco 3B.7, item 272): cada campo corrigido depois de gravado vai para o log de auditoria (o mesmo do veículo), com o antes, o depois, a data e a hora. A aba Histórico da OS mostra a lista embaixo das etapas, com a correção mais nova em cima. O primeiro registro da entrada não é correção.
- **Regras da OS, fixadas no bloco 3.1** (alterado em 15/10/2026 — ver docs/decisoes.md, item 169):
  1. **O orçamento pode vir antes do veículo:** a OS abre sem a entrada (orçamento por WhatsApp ou telefone), e a entrada é registrada quando o veículo chega. EM_EXECUCAO exige a entrada.
  2. **Só se executa com orçamento aprovado** (Código de Defesa do Consumidor, art. 39, VI): da ABERTA não se pula para EM_EXECUCAO.
  3. **O orçamento move a OS só antes da aprovação:** enviar leva a AGUARDANDO_APROVACAO, aprovar leva a APROVADA, e recusar ou abrir uma revisão volta para ABERTA. Depois de aprovada, uma revisão nova não muda o status da OS. O valor da OS é o total da última revisão aprovada.
  4. **Pagamento em partes:** a OS aceita vários recebimentos (ex.: sinal na aprovação e o resto na entrega). A situação (pendente, parcial, pago) é calculada contra o valor da OS. Recebimento errado não se apaga: é cancelado, com o motivo, e deixa de contar.
  - **Projeto e OS:** o orçamento feito pela aba do projeto vai para a OS atual dele, a mais nova que não foi entregue nem cancelada. Sem nenhuma, uma OS nova é aberta para o projeto, sem a entrada. A aba do projeto mostra só o orçamento da OS atual; o das OS anteriores fica nelas, só para ver (alterado em 18/10/2026 — ver docs/decisoes.md, item 209). Cliente anonimizado não ganha OS nova (item 207).
- **Execução e laudos (Fase 3B, ver seção 6.6 e docs/decisoes.md, itens 217 a 225):**
  - Ao passar para `EM_EXECUCAO`, a OS cria uma **cópia (instantâneo)** do checklist modelo de cada serviço do orçamento aprovado. Mudar o modelo depois não altera as cópias já criadas. Detalhes fixados no bloco 3B.3 (19/10/2026 — ver docs/decisoes.md, itens 240 e 241):
    - a cópia é do checklist que o serviço tem no catálogo naquela hora, na versão de agora do modelo; uma por serviço do catálogo; a linha digitada não tem checklist; voltar a `EM_EXECUCAO` só cria as que faltam;
    - revisão aprovada com a OS em `EM_EXECUCAO`, `AGUARDANDO_PECAS` ou `PRONTA`: os serviços novos ganham a cópia na hora. A cópia do serviço que saiu do orçamento fica guardada, mas não trava `PRONTA` e não sai no comprovante.
  - Cada item da cópia tem situação `PENDENTE`, `FEITO` ou `NAO_SE_APLICA`, com observação, mecânico e data e hora de marcação. `NAO_SE_APLICA` exige justificativa escrita.
  - Para a OS ir a `PRONTA`, todos os itens marcados **obrigatórios** precisam estar `FEITO` ou `NAO_SE_APLICA` com justificativa. Se não estiverem, o app mostra quais faltam e só avança com justificativa escrita, gravada na OS com data e hora (mesmo padrão do semáforo vermelho). A justificativa fica no histórico de status da OS, junto com a lista do que faltou (item 243).
  - **Trava da entrega** (versão 0.3.2, item 278): com a OS em `PRONTA`, a revisão aprovada dá o checklist aos serviços novos, e a etapa não muda (regra 3). A entrega fica travada enquanto houver item obrigatório pendente num checklist criado depois do pronto ou, sem a entrada, um serviço que exige o veículo. Para destravar, marca-se o item, ou a OS volta a `EM_EXECUCAO` e passa de novo pela trava do pronto.
  - A OS pode ter vários **laudos de diagnóstico elétrico** (6.6.3). Depois de `ENTREGUE`, checklists e laudos ficam só para leitura (continua valendo: depois de entregue, só pagamento e anexos mudam).
  - **Itens e kits** entram no orçamento como linhas (6.6.4), sem estoque.
  - **Serviço sem o veículo na oficina:** cada serviço do catálogo diz se exige a entrada do veículo (`exigeEntradaDoVeiculo`). A regra "EM_EXECUCAO exige a entrada" (regra 1 acima) passa a valer assim: a OS pode ir a `EM_EXECUCAO` sem entrada se tiver ao menos um serviço que não a exige (ex.: confecção de chicote em bancada); nesse caso, os checklists dos serviços que exigem o veículo ficam bloqueados até a entrada ser registrada, e a OS só vai a `PRONTA` com a entrada registrada se algum serviço a exigir. Cliente e veículo continuam obrigatórios no cadastro da OS, mesmo que o veículo não chegue. Detalhes fixados no bloco 3B.6 (20/10/2026 — ver docs/decisoes.md, itens 261 a 264): contam os serviços do catálogo da última revisão aprovada, como estão na versão do orçamento; a linha digitada não conta (ela não diz se exige o veículo).
  - **Entrega sem veículo:** quando todos os serviços da OS dispensam a entrada, a entrega registra **quem retirou** (nome) no lugar do km de saída. O resto da entrega (garantia, pagamento) não muda. Na prática, é a OS entregue sem a entrada registrada: "quem retirou" é obrigatório e não há km de saída; o comprovante mostra "Retirado por" e a assinatura de quem retirou (bloco 3B.6, item 263).
  - Documentos novos no menu Documentos da OS: **laudo de diagnóstico elétrico (A4)** e **ficha do chicote (A4)**. O comprovante de entrega passa a trazer a garantia dos serviços que têm a própria (6.6.1) e, por serviço, o resumo do checklist (itens feitos ÷ total). O comprovante continua cabendo numa página na OS comum, com a assinatura (itens 231 e 232).
- **Documentos e mensagens** (bloco 3.3 — ver docs/decisoes.md, itens 195 a 198):
  - via do cliente (A4): a OS, o cliente e o veículo, o serviço pedido, a entrada com a vistoria item a item, o texto de Configurações › OS e as linhas de assinatura do cliente e do responsável pela oficina. Não sai com a entrada mudada na tela e não salva;
  - comprovante de entrega (A4, depois da entrega): a entrega, a garantia com a data em que vence, os itens e o total da revisão aprovada, os recebimentos e a assinatura de quem recebeu o veículo;
  - imprimir: a janela de impressão do Windows; as páginas são desenhadas a 200 DPI em segundo plano, com o PDFBox;
  - textos para o WhatsApp, que só copiam: OS aberta, orçamento (também o da OS comum, sem o resumo técnico) e pronto para retirar.
- **Padrões da OS** em Configurações (bloco 3.3, itens 192 e 193): a garantia padrão, os itens da vistoria, o texto da via do cliente e o cadastro dos mecânicos.
- **Catálogo de serviços** (bloco 3.3, item 194): nome, horas e preço de referência opcional; sem preço, as horas são obrigatórias. No orçamento, o serviço com preço próprio entra uma vez pelo preço; sem preço, entram as horas × o valor da hora.
- **Ficha do veículo** (bloco 3.3, item 199): o cliente, o veículo, as OS, os projetos e os anexos de todas as OS, com os documentos do próprio veículo (anexo sem OS).
- **LGPD:** anonimizar o cliente (regra 2.8) apaga também os anexos marcados como documento pessoal, os das OS dele e os dos veículos dele, e mantém fotos, laudos e notas. O arquivo só sai da pasta se nenhum outro anexo o usar (bloco 3.3, item 201); o que estiver aberto em outro programa sai na próxima abertura do app. Depois, a identificação dos veículos dele não volta, e ele não recebe OS nova nem documento pessoal novo (itens 206 e 207). Os anexos que ficam trocam o nome original do arquivo por "arquivo anonimizado", com a extensão; a descrição fica (bloco 3B.7, item 273). O "quem retirou" das OS dele vira "(apagado na anonimização)", também na OS entregue (versão 0.3.2, migração V14, item 279).

### 6.6 Auto elétrica — Fase 3B

Alterado em 18/10/2026 (SPEC 1.2) — ver docs/decisoes.md, itens 213 a 226.

#### 6.6.1 Categorias e catálogo de serviços

O serviço do catálogo (nome, horas e preço de referência opcional; sem preço, as horas são obrigatórias) ganha:

| Campo | Regra |
| --- | --- |
| `categoria` | PREPARACAO, SOM, VIDROS_TRAVAS, PARTIDA_CARGA, ILUMINACAO, DIAGNOSTICO_ELETRICO, INJECAO_PROGRAMAVEL, OUTRO; obrigatória no serviço novo. Serviços já cadastrados viram OUTRO, e a categoria se troca na edição (nada é apagado). |
| `descricaoPadrao` | texto que entra na linha do orçamento (e daí no PDF do orçamento e no comprovante de entrega); editável na OS. |
| `checklistModeloId` | opcional: o checklist modelo do serviço (entra com os modelos de checklist, no bloco 3B.3). |
| `exigeEntradaDoVeiculo` | sim ou não (padrão: sim); "não" para serviço de bancada, como a confecção de chicote (regra na 6.5). |
| `garantiaDias` | opcional, de 0 a 3650 (0 = sem garantia para aquele serviço); sem valor, vale a garantia da OS. Vale a da versão do serviço que está no orçamento aprovado. A janela de entrega, a aba Entrega e pagamento e o comprovante de entrega mostram, embaixo da garantia da OS, a de cada serviço que tem a própria, com o dia em que vence (alterado em 19/10/2026 — ver docs/decisoes.md, itens 231 e 232). |

**Serviços sugeridos** (lista de partida, sem preço e sem horas). O botão "Serviços sugeridos…", na aba Serviços dos Catálogos, abre a lista; **eu preencho as horas ou o preço de cada um antes de confirmar**, porque a regra do catálogo exige um dos dois e o app não inventa tempo de serviço. Todos entram numa gravação só; o que já existe no catálogo com o mesmo nome não entra de novo (item 230):

- **Som:** instalação de som completa; troca de alto-falantes; instalação de módulo de potência; instalação de subwoofer e caixa; instalação de multimídia ou central.
- **Vidros e travas:** troca de motor de vidro elétrico; troca de regulador (máquina de vidro); instalação de kit de vidro elétrico; instalação de kit de trava elétrica; reparo de chave ou botoeira.
- **Partida e carga:** reparo ou troca de motor de partida; reparo ou troca de alternador; teste de bateria e do sistema de carga.
- **Iluminação:** instalação de LED ou xenon em farol; instalação de farol auxiliar (milha); instalação de DRL ou setas sequenciais; troca ou adaptação de farol e projetor.
- **Diagnóstico elétrico:** diagnóstico de falha elétrica; diagnóstico de injeção eletrônica com scanner; medição de consumo em repouso (fuga de corrente).
- **Injeção programável** (ver 6.6.5): confecção de chicote para injeção programável (bancada, `exigeEntradaDoVeiculo = não`); instalação de injeção programável (`exigeEntradaDoVeiculo = sim`); revisão ou correção de chicote de injeção programável; diagnóstico de injeção programável (falhas, leitura de sensores e log). O **pacote** "chicote + instalação" é um kit de serviços (6.6.4), não um serviço novo.

#### 6.6.2 Checklists

- **Modelo:** cadastro em Catálogos › Checklists, com lista, busca, versões, duplicar e arquivar, e escolhido no serviço (campo `checklistModeloId`, 6.6.1). Alterado em 19/10/2026: era em Configurações › Checklists — ver docs/decisoes.md, item 238. Cada modelo tem nome e itens; cada item tem `texto`, `obrigatorio` (sim ou não), `aVerificar` (sim ou não) e ordem.
- **Execução:** a cópia na OS (6.5). Marca quem fez e quando.
- **Textos iniciais** (todos marcados `aVerificar = sim` até eu validar e, como escolhi em 19/10/2026, com todos os itens `obrigatorio = sim`; sem números técnicos, só o que conferir; vêm no app pela migração V9 — ver docs/decisoes.md, item 239):
  - *Som:* medir a tensão da bateria antes de começar; conferir a carga do alternador; cabo de alimentação com fusível próximo à bateria; proteção do cabo na passagem de chapa; aterramento em ponto de metal limpo e firme; bitola do cabo compatível com a potência (campo para anotar a tabela usada); teste de ruído com o motor ligado; teste de volume e balanço com o cliente; fotos de antes e depois.
  - *Vidro e trava elétrica:* testar motor e botoeira antes de desmontar; conferir fusível e relé do circuito; conferir chicote da porta (fios quebrados na dobra); testar curso completo e one-touch (quando houver); montagem de forro e vedações; teste final com o cliente.
  - *Partida e alternador:* teste da bateria; conferir cabos e terminais (queda de tensão, campo para anotar a medição); medir a tensão de carga com o motor ligado (campo para anotar); conferir correia e polias; teste de partida a quente e a frio; registrar peça removida e peça instalada.
  - *Iluminação:* conferir erro de canbus ou aviso no painel; relé dedicado quando a carga pedir; vedação do conjunto; regulagem de altura do farol; teste de todas as funções (baixo, alto, DRL, seta).
  - *Confecção de chicote para injeção programável:* conferir o pinout da injeção no manual do fabricante (anotar a fonte); levantar sensores e atuadores do motor; definir comprimentos e rota no veículo; conferir conectores, terminais e vedação adequados ao ambiente; identificar cada fio conforme a ficha do chicote; proteção do chicote (conduíte, fita, abraçadeiras); fusíveis e relés dimensionados (campo para anotar); teste de continuidade de cada fio; teste de isolamento e de curto; conferir aterramentos de sinal e de potência separados (a verificar); fotos; ficha do chicote preenchida.
  - *Instalação de injeção programável:* conferir o estado do chicote e dos sensores antes de começar; fixar a ECU em local protegido de calor e água; alimentação com fusível e relé principal; aterramentos em pontos limpos e firmes; conferir sensor de rotação e sensor de fase (sinal e folga); sensores instalados (MAP, temperaturas, TPS, banda larga, quando houver); bicos e bobinas ligados na ordem de ignição do motor; pressão de combustível conferida; vazamentos; primeira partida conforme o checklist da seção 7.5; registrar a injeção instalada (modelo e versão do firmware) nas instalações do veículo.
  - *Diagnóstico elétrico:* anotar a queixa do cliente; ler códigos de falha; medir bateria em repouso e carga; conferir consumo em repouso quando a queixa for descarga; registrar testes feitos e causa encontrada (no laudo).

#### 6.6.3 Laudo de diagnóstico elétrico

Pertence à OS (vários por OS). Detalhes fixados no bloco 3B.4 (20/10/2026 — ver docs/decisoes.md, itens 246 a 253). Campos:

| Campo | Regra |
| --- | --- |
| `queixa` | texto obrigatório, nas palavras do cliente; o laudo novo já vem com o serviço pedido da OS |
| `dataHora`, `mecanico` | automáticos (agora; o mecânico da OS, se estiver ativo), editáveis até a entrega. A data não pode ser no futuro nem antes da abertura da OS; o mecânico pode ficar vazio |
| `medicoes` | lista editável: `nome`, `valor`, `unidade`, `condicao` (REPOUSO, MOTOR_LIGADO, CARGA_ACIONADA, OUTRA) e `referencia`, com `referenciaAVerificar`. Na linha, o nome, o valor e a condição são obrigatórios. **O valor, a unidade e a referência são texto livre** (escolha do dono em 20/10/2026): o app não lê o número nem julga o valor. **O app não traz faixa "boa" ou "ruim" pré-definida**: a faixa é a `referencia`, campo opcional por medição, preenchido por mim (`aVerificar`). |
| `codigosFalha` | lista: `codigo` (obrigatório), `descricao` (opcional), `situacao` (ATIVO, HISTORICO, APAGADO; obrigatória) |
| `testesRealizados` | texto opcional |
| `causa` | texto opcional |
| `solucao` | texto opcional |
| `recomendacoes` | texto opcional |
| `resultado` | RESOLVIDO, PARCIAL, NAO_RESOLVIDO; vazio = "a definir", enquanto o diagnóstico não termina |

- **Quando:** o laudo se cria e se muda em qualquer etapa antes da entrega, também antes de o orçamento ser aprovado. Com a OS `ENTREGUE` ou `CANCELADA`, ele fica só para leitura, e o banco também recusa criar, mudar e apagar. Apagar (o laudo feito por engano) só com a OS em andamento.
- **Onde aparece:** na área Laudos da aba Execução (seção 8, item 9); como **PDF A4 gerado na hora**, pela própria área e pelo menu Documentos da OS, sem cópia nos anexos (escolha do dono em 20/10/2026; a proposta dizia "via do cliente e anexo da OS"); e como seção na ficha do veículo, com os laudos de todas as OS dele.
- **LGPD:** laudos são dados técnicos e permanecem quando o cliente é anonimizado; o PDF tirado depois sai com o nome anonimizado. Como `queixa` é texto livre, a janela mostra embaixo dela a dica fixa "sem dados pessoais", e o app avisa ao salvar **só quando a queixa parece ter dado pessoal**: o nome do cliente, um telefone ou documento (8 dígitos ou mais) ou um e-mail (escolha do dono em 20/10/2026; a proposta avisava sempre). O aviso não impede de gravar.

#### 6.6.4 Itens e kits de material (só para orçamento)

- **Item:** `nome`, `unidade` (un, m, kit...; obrigatória, vem "un"), `precoReferencia` opcional, `fonte` (vem "Tabela da oficina": todo item do catálogo tem fonte, regra 2.7). Sem saldo. No orçamento, entra como peça: 1 × a unidade, pelo preço de referência.
- **Kit:** `nome` e lista de linhas, cada uma **item** ou **serviço** do catálogo, com `quantidade` (maior que zero; o mesmo item não se repete). Cada linha precisa apontar para um item ou serviço do catálogo. O kit não tem preço próprio. Um kit que contém serviços é um **pacote** (ex.: "chicote + instalação"). Ao incluir um kit no orçamento, ele **se expande em linhas editáveis**, com a versão de agora de cada item e serviço e a quantidade do kit (o serviço pela regra dele, vezes a quantidade); dá para trocar a quantidade ou o preço de cada linha na OS, ou apagar uma linha, por exemplo, quando o cliente leva só o chicote. O item ou serviço arquivado entra com um aviso.
- **Fornecido pelo cliente:** qualquer linha de peça (item) pode ser marcada `fornecidoPeloCliente`. Ela aparece no orçamento com valor zero e a indicação "fornecido pelo cliente" (ex.: a ECU que o cliente já comprou), também no comprovante de entrega e no texto de WhatsApp. Os orçamentos gravados antes da Fase 3B são lidos como "não fornecido".
- No orçamento, item e kit entram ao lado dos serviços; a mão de obra continua pelas regras do catálogo de serviços (horas × valor da hora, ou preço próprio). "Trazer as peças do cálculo de novo" mantém as linhas do catálogo de itens. Alterado em 19/10/2026 — ver docs/decisoes.md, itens 233 a 237.

#### 6.6.5 Injeção programável: chicote e instalação

A oficina vende dois serviços **independentes**; o cliente pode contratar só um ou os dois:

- **Confecção de chicote para injeção programável:** o chicote novo, feito na bancada para a injeção e o motor do cliente. Pode ser entregue ao cliente ou a outra oficina, sem o veículo ter passado pela oficina.
- **Instalação de injeção programável:** ECU, sensores e atuadores no veículo, com chicote da oficina ou trazido pelo cliente.

Regras:

1. São dois serviços do catálogo, da categoria INJECAO_PROGRAMAVEL (6.6.1), com `exigeEntradaDoVeiculo` diferente (regra na 6.5). O pacote "chicote + instalação" é um kit (6.6.4).
2. Se a OS tem os dois serviços, a bancada pode começar antes de o veículo chegar; a instalação só começa com a entrada registrada.
3. **Injeção e peças do cliente:** a ECU e outros itens podem ser marcados `fornecidoPeloCliente` (6.6.4).
4. **Vínculo com a preparação:** se a OS tem projeto de preparação com `injecaoEscolhida`, a ficha do chicote já vem com a injeção preenchida; sem projeto, a injeção vem do catálogo ou é texto livre. O mapa base e o acerto continuam nas Fases 4 e 6; estes serviços não os substituem, e o app continua não gravando mapa na injeção (seção 2).
5. **Ficha do chicote** (uma por chicote, pertence à OS; PDF A4 gerado na hora, sem cópia nos anexos, como o laudo — escolha do dono em 20/10/2026; a proposta dizia "PDF A4 e anexo da OS"):

| Campo | Regra |
| --- | --- |
| `injecao` | do catálogo (opcional) ou texto livre; com o projeto de preparação ligado à OS, a ficha nova já vem com a injeção dele |
| `motorVersaoId` | opcional: a versão da configuração do motor (6.3), do projeto de preparação da OS; a ficha nova vem com a de agora |
| `versaoChicote` | texto (ex.: v1); nova versão não apaga a anterior, que fica só para leitura (escolha do dono em 20/10/2026) |
| `fontePinout` | texto obrigatório: de onde veio o pinout (manual, modelo do fabricante) |
| `pontos` | lista editável: `funcao` (ex.: bico do cilindro 1, bobina, TPS, MAP; obrigatória na linha começada), `conector`, `pinoDaInjecao`, `observacao`, `aVerificar` |
| `materiais` | texto livre (cabo, bitola, conectores, proteção); **o app não valida pinagem nem bitola** |
| `testesBancada` | vem do checklist (continuidade, isolamento, curto), com resultado e quem fez: os itens do checklist dos serviços de bancada da OS, como estão marcados na aba Execução, sem anotar de novo (escolha do dono em 20/10/2026) |
| `fotos`, `observacoes` | anexos da OS e texto |
| `situacao` | EM_CONFECCAO, TESTADO (escolhidas na ficha), ENTREGUE (vem sozinha na entrega da OS, na última versão de cada chicote) |

A ficha se cria e se muda até a entrega ou o cancelamento da OS; depois, fica só para leitura (o banco também recusa). Ela aparece na aba Execução (área Chicotes), no menu Documentos da OS e na ficha do veículo. Detalhes fixados no bloco 3B.6 (20/10/2026 — ver docs/decisoes.md, itens 265 a 271). A ficha é o ponto de partida da tabela de circuitos da Fase 8.

---

## 7. Núcleo de cálculo

### 7.1 Fórmulas — implementar exatamente assim, cada uma com teste

```
Conversões: 1 cv = 0,98632 hp = 0,73550 kW; 1 lb = 0,453592 kg; 1 psi = 6,894757 kPa; 1 L = 61,0237 pol³

Ar e combustível
  P_hp = P_cv × 0,98632
  Wa [lb/min] = P_hp / k_ar                          k_ar padrão 10,5 (editável de 8,0 a 11; era 9,5 a 11,
                                                     alterado em 09/10/2026 — ver docs/decisoes.md, item 40)
  mf [lb/h]   = 60 × Wa / (AFR_est × lambda)
  BSFC        = mf / P_hp                             só conferência (turbo a gasolina: 0,50 a 0,65)

Bicos e bomba
  Q_bico [massa]  = mf / (N_bicos × DC_max)           DC_max padrão 0,80
  Q2 = Q1 × raiz(dP2 / dP1)                           dP = pressão do combustível menos a do coletor
  Q_comb [volume] = Q_teste × raiz(rho_teste / rho_comb)    rho_teste padrão 0,70 kg/L (editável por bico)
  Bomba [L/h]     = mf [kg/h] / rho_comb, exigida na pressão de base + pressão do turbo, com folga (7.4)

Turbo (método Garrett)
  MAP [psia] = Wa × 639,6 × (460 + Tm[°F]) / (VE × (RPM/2) × Vd[pol³])
  PR         = (MAP + dP_perdas) / (P_atm − dP_admissao)     padrões: 2 psi e 1 psi
  P_atm [kPa] = 101,325 × (1 − 2,25577e−5 × h[m])^5,25588
  T2 [K]     = T1 × (1 + (PR^0,2857 − 1) / eta_c)             eta_c padrão 0,72
  Tm         = T2 − eps × (T2 − T_amb)                        eps padrão 0,80 (ar-ar)
  Iterar MAP ↔ Tm a partir de Tm = 50 °C até |ΔTm| < 0,01 °C (máximo 100 iterações; se não convergir, erro)
  Aspirado: sem compressor; Tm = T_amb + 10 °C (padrão editável)

  Alterado em 14/10/2026 (núcleo 1.1.0) — ver docs/decisoes.md, itens 114, 116 e 117:
  Vazão corrigida = Wa × raiz(T1 / T_ref) / (P1 / P_ref)     T1 = T_amb; P1 = P_atm − dP_admissao;
                                                     T_ref e P_ref são os do próprio mapa do compressor
  Meta pela pressão: MAP = P_atm + pressão do turbo; PR, T2 e Tm sem iteração (o MAP é conhecido);
                     Wa pela fórmula do MAP ao contrário; P_hp = Wa × k_ar (ou pelo BSFC)
  Ponto de torque:   o mesmo MAP e a mesma Tm da potência, na rotação de torque, com a VE do torque:
                     Wa = MAP × VE × (RPM/2) × Vd / (639,6 × (460 + Tm[°F])); daí a potência e o torque

Tempo de injeção
  m_ar_cil [kg] = VE × MAP[Pa] × V_cil[m³] / (287,05 × Tm[K])
  m_f_inj       = m_ar_cil / (AFR_est × lambda × injecoesPorCiclo)
                  injecoesPorCiclo: SEQUENCIAL = 1; SEMISSEQUENCIAL e SIMULTANEO = 2
  TI [ms]       = m_f_inj / mdot_bico + t_morto(V_bat, dP)    tempo morto somado em cada injeção
  DC            = TI × injecoesPorCiclo / (120000 / RPM)
  Com 2 bicos por cilindro, na plena carga a vazão se divide na proporção da vazão de cada bico;
  o ponto de entrada da bancada secundária (carga ou pressão) é parâmetro do projeto.

Esforço
  BMEP [bar]  = 1200 × P[kW] / (Vd[L] × RPM)
  c_m [m/s]   = 2 × curso[m] × RPM / 60
  T [N·m]     = 9549 × P[kW] / RPM
  Aspirado: MAP necessário comparado com P_atm local (7.4)
```

### 7.2 Combustíveis — catálogo editável e versionado

A gasolina C é calculada pela fração em massa de etanol: `x = (v × 0,789) / (v × 0,789 + (1 − v) × 0,74)` e `AFR = (1 − x) × 14,7 + x × 9,0`, onde v é a fração em volume de etanol anidro. Etanol hidratado: `AFR = 9,0 × fração em massa de etanol`.

| Combustível | AFR estequiométrico | Densidade (kg/L) | Lambda sob pressão | Lambda aspirado em plena carga |
| --- | --- | --- | --- | --- |
| Gasolina C comum E32 (vigente desde 01/08/2026) | 12,80 (calculado) | 0,756 | 0,80 | 0,87 |
| Gasolina premium E25 | 13,21 (calculado) | 0,752 | 0,80 | 0,87 |
| Etanol hidratado (93% em massa) | 8,37 (calculado) | 0,809 | 0,80 | 0,87 |
| Etanol anidro | 9,0 | 0,789 | 0,80 | 0,87 |
| E85 | 9,75 | 0,78 | 0,80 | 0,87 |
| Metanol | 6,42 | 0,792 | 0,65, com aviso amarelo | 0,70, com aviso amarelo |

O teor de etanol da gasolina muda por decisão do governo (E30 em 2025, E32 em 2026, E35 em teste). Por isso o combustível é registro de catálogo com data de vigência, e cada projeto guarda a versão usada. Lambdas são padrões editáveis.

### 7.3 Dimensionamento das peças

- **Bicos:** escolher no catálogo o menor bico que mantém o DC, já com tempo morto, até 80% na rotação de potência máxima. Se o tempo efetivo de injeção na marcha lenta ficar abaixo de 1,0 ms (padrão editável), avisar e propor duas bancadas por cilindro.
- **Bomba:** menor bomba do catálogo cuja vazão, na pressão de base mais a do turbo e na tensão informada, cobre a necessidade com a folga do semáforo. Curvas de vazão × pressão × tensão interpoladas a partir dos pontos cadastrados.
- **Turbo:** calcular os pontos de operação no torque máximo e na potência máxima e listar os turbos cujo mapa de compressor contém os dois pontos, ordenados pela eficiência no ponto de potência. O fluxo corrigido usa a temperatura e a pressão de referência gravadas no próprio mapa. Para a caixa quente, mostrar o compromisso: A/R menor faz a pressão chegar mais cedo e limita o pico; maior faz o contrário.
- **Intercooler:** ar-ar por padrão; ar-água sugerido para ARRANCADA.
- **Radiador de óleo:** recomendado para CIRCUITO_ENDURANCE, NAUTICO e OFF_ROAD com meta alta.
- **Sensor MAP:** faixa mínima pelo semáforo (7.4).
- **Injeção:** filtrar o catálogo por saídas de bico e ignição, cilindros em sequencial e recursos exigidos (controle de pressão do turbo, borboleta eletrônica, sensor de etanol, detonação, wideband, câmbio).

### 7.4 Semáforo — limiares padrão, editáveis em Configurações

| Indicador | Verde | Amarelo | Vermelho |
| --- | --- | --- | --- |
| DC do bico, com tempo morto, na rotação de potência máxima | até 80% | 80% a 85% | acima de 85% |
| Folga da bomba sobre a vazão necessária | 30% ou mais | 15% a 30% | menos de 15% |
| Ponto no mapa do compressor (potência e torque; vale a pior cor) | eficiência até 5 pontos abaixo do pico do mapa | menos de 10% de vazão acima do surge ou de 5% até o choke (na mesma PR); acima de 95% da rotação máxima ou das linhas de rotação desenhadas; fora das ilhas ou mais de 5 pontos abaixo do pico; ou turbo sem mapa cadastrado | fora do mapa |
| BMEP (referência: aspirado 13 bar; sobrealimentado 22 bar) | até a referência | até 15% acima | acima de 15% com pistões ou bielas originais; com os dois forjados, vira amarelo com aviso |
| Velocidade média do pistão | até 20 m/s | 20 a 25 m/s | acima de 25 m/s |
| Saídas da injeção | suficientes | — | insuficientes |
| Faixa do sensor MAP | 1,2 × MAP máximo ou mais | 1,0 a 1,2 × MAP máximo | abaixo do MAP máximo |
| Aspirado: MAP necessário ÷ P_atm local | até 0,90 | 0,90 a 0,98 | acima de 0,98 ("meta acima do possível para aspirado com essa VE") |
| Torque estimado (o maior entre potência e torque máximo) ÷ torque máximo do câmbio | até 90% | 90% a 100% | acima de 100%; sem câmbio ou sem limite cadastrado, só aviso |

Os números do mapa do compressor e a linha do torque × câmbio foram alterados em 14/10/2026 — ver docs/decisoes.md, itens 115 e 119. Os limiares desses dois indicadores ficam marcados "a validar" em Configurações.

### 7.5 Mapa base — conservador por padrão

- **Eixos:** vêm do perfil da injeção (pontos de rotação e de carga em MAP kPa ou TPS %). Sem injeção escolhida: rotação de 500 em 500 rpm até a máxima; carga de 20 kPa até 1,1 × MAP máximo previsto, de 10 em 10 kPa (aspirado: até 105 kPa).
- **VE:** modelo por família de motor guardado como tabela de fatores editável no catálogo, nunca como constante no código. Valores iniciais, marcados "a validar": marcha lenta entre 40% e 60%; pico perto do torque máximo de 95% a 99% (4 válvulas) ou 80% a 95% (2 válvulas); comando agressivo desloca o pico para rotação mais alta e reduz a VE em baixa.
- **Lambda alvo:** abaixo de 70 kPa, 1,00 em RUA; de 70 a 100 kPa, interpolar até o alvo aspirado de plena carga; acima de 100 kPa, alvo sob pressão do combustível (7.2). No mapa base, 0,03 mais rico que o alvo nas células acima de 90 kPa; em NAUTICO e CIRCUITO_ENDURANCE, mais 0,02. Tudo editável.
- **Combustível:** para injeções em ms, calcular o TI de cada célula na temperatura de referência da correção por ar do perfil (padrão 25 °C) e respeitar o campo do perfil que diz se a tabela inclui o tempo morto. Para injeções por VE, exportar VE + lambda alvo + dados do bico.
- **Ignição: o app não inventa avanço.** Fontes em ordem: (1) mapa final de projeto parecido da biblioteca; (2) modelo cadastrado para a família do motor; (3) tabela digitada pelo usuário. Sem fonte: tabela vazia e semáforo vermelho "ignição sem modelo". Sobre a fonte, aplicar reduções conservadoras configuráveis (por pressão acima de 100 kPa, por temperatura do ar e por troca de combustível) e mostrar a diferença célula a célula.
- **Travas:** pressão do turbo alvo limitada à da mola da wastegate informada ou a 50% da prevista, o que for menor; limitador de rotação 500 rpm abaixo da máxima; cortes de proteção (mistura pobre, pressão de óleo, temperatura) listados no checklist.
- **Demais tabelas** (partida, aquecimento, aceleração rápida, correções por temperatura e tensão): checklist apontando para o assistente da própria injeção, com os dados do motor prontos para digitar.
- **Checklist de primeira partida:** calibração de TPS, MAP, temperaturas e wideband; ponto conferido com lâmpada; pressão de combustível conferida; vazamentos; extintor à mão.

### 7.6 Casos de referência (testes golden) — tolerância de 1%

**A. Garrett (gasolina):** 650 hp, A/F 11,5, BSFC 0,46 → Wa = 57,31 lb/min. Com VE 0,80, Tm 130 °F, 6.000 rpm e 348 pol³ → MAP = 25,89 psia (a Garrett publica 26,025; a diferença é arredondamento do exemplo e cabe na tolerância). PR com 2 psi de perda e 13,7 psia na entrada → 2,04.

**B. 4 cilindros 2.0 turbo, etanol hidratado:** curso 92,8 mm; 400 cv no virabrequim a 7.000 rpm; VE 0,95; lambda 0,80; 4 bicos em injeção sequencial; DC máximo 0,80; eta_c 0,72; eps 0,80; ar a 25 °C; 600 m; perdas 2 e 1 psi; tempo morto 1,0 ms; bico de 1.050 cc/min a 3 bar; fluido de teste 0,70 kg/L; etanol 0,809 kg/L.

| Saída | Esperado |
| --- | --- |
| Wa | 37,57 lb/min |
| Combustível | 152,8 kg/h |
| Vazão por bico | 796 g/min = 984 cc/min de etanol = 1.058 cc/min nominais |
| P_atm / MAP / PR | 94,3 kPa / 241 kPa / 2,91 |
| T2 / Tm | 173 °C / 55 °C |
| TI e DC com o bico de 1.050 cc | 13,81 + 1,0 = 14,81 ms num ciclo de 17,14 ms → 86% (vermelho) |
| Alternativas | 1.300 cc a 3 bar → 71%; 1.050 cc a 4 bar → 76% |
| Esforço | BMEP 25,2 bar (amarelo); 401 N·m; pistão a 21,7 m/s (amarelo) |

**C. 3 cilindros 1.0 turbo, gasolina E32:** diâmetro 74,5 × curso 76,4 mm; 150 cv no virabrequim a 5.500 rpm; VE 0,95; lambda 0,80; 3 bicos em injeção sequencial; DC máximo 0,80; eta_c 0,72; eps 0,80; ar a 25 °C; 600 m; perdas 2 e 1 psi; tempo morto 1,0 ms; bico de 440 cc/min a 3 bar.

| Saída | Esperado |
| --- | --- |
| Cilindrada / Wa | 999 cc / 14,09 lb/min |
| Combustível | 37,5 kg/h (BSFC 0,558) |
| Vazão por bico | 260 g/min = 344 cc/min de gasolina = 358 cc/min nominais |
| P_atm / MAP / PR | 94,3 kPa / 229 kPa / 2,78 |
| T2 / Tm | 165 °C / 53 °C |
| TI e DC | 14,19 + 1,0 = 15,19 ms num ciclo de 21,82 ms → 70% (verde) |
| Esforço | BMEP 24,1 bar (amarelo); 192 N·m; pistão a 14,0 m/s (verde) |
| Regras | ímpar: CENTELHA_PERDIDA e SEMISSEQUENCIAL bloqueados; 3 saídas de bico e 3 de ignição em sequencial |

**D. V12 6.0 aspirado, gasolina E32:** diâmetro 89,0 × curso 80,4 mm; 450 cv no virabrequim a 6.500 rpm; VE 0,90; lambda 0,87; 12 bicos em injeção sequencial; DC máximo 0,80; Tm = 35 °C (25 + 10); tempo morto 1,0 ms; bico de 260 cc/min a 3 bar.

| Saída | Esperado |
| --- | --- |
| Cilindrada / Wa | 6.002 cc / 42,27 lb/min |
| Combustível | 103,3 kg/h (BSFC 0,513) |
| Vazão por bico | 179 g/min = 237 cc/min de gasolina = 247 cc/min nominais |
| MAP necessário | 96,6 kPa → ÷ P_atm: 0,953 ao nível do mar (amarelo) e 1,024 a 600 m (vermelho) |
| TI e DC | 14,01 + 1,0 = 15,01 ms num ciclo de 18,46 ms → 81% (amarelo) |
| Esforço | BMEP 10,2 bar (verde); 486 N·m; pistão a 17,4 m/s (verde) |
| Saídas | 12 bicos em sequencial; 12 saídas de ignição em sequencial ou 6 em centelha perdida |

**Testes de coerência (sempre verdadeiros):** mais potência nunca pede bico menor; mais altitude nunca pede razão de pressão menor; trocar o bico não muda a tabela de VE; converter unidade e voltar devolve o valor original; para a mesma potência total, a vazão por bico cai na proporção de 1/n.

---

## 8. Telas (fluxo do app)

1. **Preparação** (era "Início" e, no menu, "Projetos"; a tela inicial passa a ser o Painel, item 8): lista de projetos com busca por cliente, placa ou nome da embarcação, motor e status; botão "Novo projeto".
2. **Assistente de novo projeto:** Cliente → Veículo → Motor → Meta e uso → Injeção (opcional). O formulário do motor se adapta ao número de cilindros: ao mudar de 4 para 6, por exemplo, a ordem de ignição e os ângulos são sugeridos de novo e pedem confirmação. A partir de uma OS, o assistente já vem com o cliente e o veículo dela, começa no motor, e o projeto nasce ligado à OS.
3. **Projeto (abas):** Resumo · Veículo (editável, com histórico) · Motor (editável, com versões e comparação entre elas) · Dimensionamento (cartões com semáforo e memória de cálculo) · Peças e orçamento · Mapa base · Sessões de acerto · Relatório. O cabeçalho mostra a OS do projeto, com o botão para abri-la, e o botão da ficha do veículo (fase 3).
4. **Editor de tabela:** grade com cores por faixa; edição por célula ou seleção (somar, multiplicar em %, interpolar, suavizar); desfazer; comparação com outra versão em %; vista 3D que gira com o mouse; copiar e colar texto tabulado.
5. **Catálogos (cadastrar, editar, arquivar; sempre com fonte e data):** bicos (vazão, pressão e fluido de teste, impedância, tempo morto por tensão e pressão), turbos (com digitalizador de mapa: carregar imagem, calibrar os eixos com dois pontos cada, clicar surge, choke, ilhas de eficiência e linhas de rotação), injeções (perfil da seção 9), bombas (curvas), combustíveis, intercoolers, sensores, câmbios (tipo e torque máximo), motores e montagens (geometria, biela e taxa, com fonte, para preencher o motor), modelos de VE e de ignição, serviços (nome, horas e preço de referência opcional; sem preço, as horas são obrigatórias; fase 3; na Fase 3B, com categoria, descrição padrão, checklist modelo, "exige a entrada do veículo" e garantia, e o botão "Serviços sugeridos…", seção 6.6.1), itens e kits de material (Fase 3B, seção 6.6.4, nas abas Itens e Kits; kits podem incluir serviços, formando pacotes), checklists (Fase 3B, seção 6.6.2, na aba Checklists; os 7 modelos da SPEC já vêm cadastrados). Câmbios e motores e montagens: alterado em 14/10/2026 — ver docs/decisoes.md, itens 86 e 91. Serviços: alterado em 15/10/2026, em 17/10/2026 e em 18/10/2026 — ver docs/decisoes.md, itens 166, 194 e 215. Checklists: alterado em 19/10/2026 — ver docs/decisoes.md, item 238.
6. **Biblioteca:** busca por configuração (cilindros, cilindrada ±10%, aspiração, combustível, injeção, bicos, turbo, uso) e comparação entre mapa base e mapa final.
7. **Configurações:** dados da oficina para os PDFs (nome, CNPJ, endereço, logo), limiares do semáforo, padrões de cálculo, padrões do orçamento, a aba OS (fase 3: garantia padrão, itens da vistoria, texto da via do cliente e mecânicos com nome e ativo; ver docs/decisoes.md, itens 192 e 193), pasta de backup, restaurar backup. Os modelos de checklist da Fase 3B (seção 6.6.2) ficam nos Catálogos, não aqui (alterado em 19/10/2026 — ver docs/decisoes.md, item 238). As unidades preferidas na tela passam para a fase 4, junto do mapa base; até lá, os resultados aparecem em várias unidades ao mesmo tempo (alterado em 14/10/2026 — ver docs/decisoes.md, itens 137 e 152; a fase do mapa base passou de 3 para 4 em 15/10/2026, item 165).
8. **Painel (fase 3, tela inicial):** as OS em andamento por status (abertas, aguardando aprovação, aprovadas na fila, em execução, aguardando peças, prontas para retirar; o cartão abre a Oficina com o filtro), os veículos na oficina, as entregas previstas até hoje (com as atrasadas) e o último backup. Na fase 5, ganha os avisos dos lembretes. Alterado em 16/10/2026 — ver docs/decisoes.md, item 177.
9. **Oficina (fase 3):**
   - lista de OS com busca por placa, cliente, embarcação ou número da OS e filtro por status;
   - botão "Nova OS": cliente e veículo existentes ou novos (a busca do cliente acha pela placa), serviço pedido e, se o veículo já chegou, a entrada com a vistoria e as fotos;
   - **OS:** os passos do serviço ficam no cabeçalho (começar, aguardando peças, retomar, pronto, entregar, cancelar), com o próximo passo, a ficha do veículo e o menu Documentos (via do cliente, comprovante de entrega, imprimir e os textos para o WhatsApp; na Fase 3B, também o laudo elétrico e a ficha do chicote); as abas são Entrada · Orçamento · Preparação (o projeto ligado, ou ligar ou criar um) · Execução (Fase 3B) · Entrega e pagamento · Anexos · Histórico. Alterado em 16/10/2026 e em 17/10/2026 — ver docs/decisoes.md, itens 184 e 196.
   - **Aba Execução (Fase 3B):** as áreas *Checklists* (um bloco por serviço), *Laudos* (lista, com "Novo laudo"), *Instalações* (as da OS, com "Registrar instalação"; bloco 3B.5, item 258) e *Chicotes* (as fichas do chicote, com "Nova ficha" e "Nova versão"; bloco 3B.6, item 270). Em OS sem projeto de preparação, a aba Preparação continua mostrando só "ligar ou criar um projeto".
10. **Ficha do veículo (fase 3):** o cliente e o veículo (editar, histórico e anonimizar), todas as OS e todos os projetos do veículo (também os arquivados) e os anexos de todas as OS, com os documentos do próprio veículo (ex.: o CRLV, com a validade). Abre pela OS e pelo cabeçalho do projeto. Ver docs/decisoes.md, itens 199 a 201. Na Fase 3B, ganha a seção **Instalações** (6.2) e os laudos e as fichas de chicote de todas as OS do veículo.
11. **Agenda (fase 5):** agendamentos do dia e da semana; o agendamento vira OS quando o veículo chega.

Fonte mínima de 13 px, tudo operável pelo teclado, tema claro.

---

## 9. Injeções: perfil, exportação e importação

**Perfil de injeção (catálogo):** marca, modelo, saídas de bico e de ignição, máximo de cilindros em sequencial, impedância aceita e bicos por saída, modelo de combustível (MS, VE ou MODELO_FISICO), tabelas com eixos e tamanhos, temperatura de referência da correção por ar, se a tabela em ms inclui tempo morto, recursos (controle de pressão do turbo, borboleta eletrônica, sensor de etanol, detonação, wideband interna, câmbio, largada), formato de importação e exportação, proteção contra água, fonte e data.

**Saída do mapa em três níveis:**

- **C — Folha de transcrição (todas as injeções, fase 4):** tela e PDF com a tabela idêntica à do software do fabricante (mesmos eixos, mesma ordem, cores por faixa) e os dados de entrada do assistente de mapa base da injeção.
- **B — Tabela para colar (fase 4 em diante, uma marca por vez):** área de transferência no formato que o software aceita (Link e MoTeC importam tabela com eixos; Haltech cola células). Antes de implementar uma marca, eu copio uma tabela real do software para um editor de texto; esse texto vira o teste do formato.
- **A — Arquivo pronto (fase 7, opcional):** mapa .msq (XML) do TunerStudio para MegaSquirt, Speeduino e rusEFI, gerado sempre a partir do arquivo .ini do firmware do cliente, nunca sem ele.

Fases dos níveis C, B e A alteradas em 15/10/2026 (eram 3, 3 e 5), com o roteiro novo da seção 12 — ver docs/decisoes.md, item 165.

**Importação:** colar a tabela final (texto tabulado) do software do fabricante; anexar o arquivo nativo do mapa sem decodificar; importar log em CSV e curva de dinamômetro em CSV com mapeamento de colunas salvo por marca.

---

## 10. Requisitos de produção (valem a partir da fase 2)

- **Instalador MSI** via `jlink` + `jpackage`, com o Java embutido; atalho no menu Iniciar; ícone; versão semântica (0.x.y); UpgradeCode fixo para instalar por cima sem perder dados. Desinstalar não apaga `%LOCALAPPDATA%\Calibra`.
- **Script** `scripts/gerar-instalador.ps1`: roda `mvn -q verify`, gera o MSI e o coloca em `dist/` com a versão no nome.
- **Banco:** migração automática ao abrir, com backup antes de migrar; se a migração falhar, restaurar o backup e mostrar a mensagem.
- **Backup:** automático diário e ao fechar o app; manter os 30 mais recentes; pasta configurável (HD externo ou pen drive); restaurar pela tela de Configurações. Desde a Fase 3B, cada backup é o banco e a lista dos anexos, e os anexos ficam guardados uma vez só na pasta de backup (`anexos-dos-backups`), conferidos pela impressão digital na restauração; os backups antigos, com os anexos no zip, continuam restaurando (alterado em 19/10/2026 — ver docs/decisoes.md, item 227).
- **Logs:** arquivo rotativo em `logs\` (10 arquivos de 5 MB). Erro inesperado mostra mensagem amigável em português e grava o detalhe no log.
- **Interface fluida:** cálculos, PDFs, backup, importações e a cópia de fotos e documentos (Fase 3B, item 228) rodam em `javafx.concurrent.Task`, com indicador de progresso.
- **Desempenho:** abrir em menos de 5 s e recalcular um projeto em menos de 1 s num i5 com 8 GB.
- **Versão visível** no rodapé; `CHANGELOG.md` atualizado a cada entrega; `docs/manual.md` (manual do usuário em português) atualizado a cada fase.
- **Opcional:** GitHub Actions com runner Windows gerando o MSI quando eu criar uma tag.

---

## 11. Definição de pronto de qualquer tarefa

Compila sem avisos novos; `mvn -q verify` verde; testes novos cobrindo a regra criada ou alterada; casos golden da seção 7.6 passando; explicação curta em português; commit `fase N: ...`; nada fora do escopo pedido.

---

## 12. Fases

**Fase 0 — Preparação (sem código de produto)**
- Verificar `java -version`, `mvn -v`, `git --version` e WiX; me dizer o que falta.
- Criar o repositório Git com `.gitignore`, o projeto Maven multimódulo vazio que compila, `CLAUDE.md` (já existe; só complete os comandos reais), `README.md`, `CHANGELOG.md`, `docs/decisoes.md` e `docs/manual.md`.
- Pronto quando: `mvn -q verify` passa no projeto vazio e eu aprovo a estrutura.

**Fase 1 — Núcleo de cálculo e calculadora**
- `calibra-core` completo das seções 6.3 (validação do motor) e 7 (fórmulas, combustíveis, dimensionamento, semáforo), com os testes golden A a D, os testes de coerência e os testes de 3 a 12 cilindros.
- Uma tela de calculadora (sem banco) e o PDF da memória de cálculo.
- Pronto quando: todos os testes passam e eu uso a calculadora em 2 orçamentos reais.

**Fase 2 — Cadastros editáveis, catálogos, orçamento e primeiro instalador (MVP em produção)**
- Banco, migrações e backup; cliente, veículo (editável com histórico), motor (editável e versionado, de 3 a 12 cilindros) e projeto; catálogos com cadastro, edição e arquivamento; orçamento e prazo em PDF e em texto para WhatsApp; requisitos de produção da seção 10; MSI 0.2.0.
- Pronto quando: um projeto real vai do cadastro ao orçamento aprovado no app instalado pelo MSI, e um backup é restaurado com sucesso.

Roteiro das fases 3 em diante alterado em 15/10/2026: a gestão da oficina entra, intercalada com a preparação, e a antiga fase 6 ("Produto para outras oficinas", com licença) sai, porque o app é só da minha oficina. Ver docs/decisoes.md, itens 162 a 167.

**Fase 3 — Oficina (ordens de serviço)**
- A OS da seção 6.5:
  - entrada com vistoria e fotos;
  - orçamento, que passa a pertencer à OS (o de preparação continua apontando para o cálculo);
  - execução, entrega com garantia e registro do pagamento.
- Projeto de preparação ligado à OS.
- Telas novas: Painel, Oficina e ficha do veículo.
- Catálogo de serviços; mecânicos e garantia padrão em Configurações.
- PDFs (via do cliente e comprovante de entrega), impressão e textos para WhatsApp.
- MSI 0.3.0.
- Pronto quando: 3 carros reais passam pela OS no app instalado (entrada, orçamento aprovado, execução e entrega), um deles de preparação, ligado ao projeto.

Em 18/10/2026 entraram a Fase 3B (auto elétrica), antes da Fase 4, e a Fase 8 (chicote e caixa de fusíveis), depois da 7 — ver docs/decisoes.md, itens 213, 214 e 226.

**Fase 3B — Auto elétrica (entra depois da Fase 3 e antes da Fase 4)**

Blocos, um por vez, com plano antes e evidência no fim de cada um:

- **3B.0 — Base:** backup que guarda cada anexo uma vez só, na pasta de backup (os backups antigos continuam restaurando), e cópia de fotos e documentos em segundo plano. São os itens 1 e 2 da decisão 211, escolhidos para antes da elétrica em 19/10/2026 (item 226).
- **3B.1 — Categorias e catálogo:** `categoria`, `descricaoPadrao`, `exigeEntradaDoVeiculo` e `garantiaDias` nos serviços (o `checklistModeloId` entra no 3B.3); a garantia por serviço no comprovante; botão de serviços sugeridos (incluindo injeção programável).
- **3B.2 — Itens, kits e pacotes no orçamento:** catálogos de itens e kits (kits com itens e serviços); `fornecidoPeloCliente`; linhas no orçamento; PDF do orçamento e texto de WhatsApp com os itens.
- **3B.3 — Checklists e aba Execução:** modelos, cópia ao entrar em `EM_EXECUCAO`, trava de `PRONTA`, resumo no comprovante de entrega.
- **3B.4 — Laudo elétrico:** cadastro, PDF A4, seção na ficha do veículo, aviso de LGPD.
- **3B.5 — Instalações na ficha do veículo:** lista com histórico; ligação opcional com a OS.
- **3B.6 — Injeção programável:** serviços de chicote e de instalação, regra de OS sem entrada do veículo (6.5), "quem retirou" na entrega, ficha do chicote com PDF, checklists dos dois serviços.
- **3B.7 — Fechamento:** histórico das correções da OS e nome dos arquivos na anonimização (itens 3 e 4 da decisão 211); `docs/manual.md`, `CHANGELOG.md`, `docs/decisoes.md` e MSI 0.3.1.

Testes obrigatórios da fase:
1. Banco com dados da Fase 3 mantém todos os serviços, OS e orçamentos (serviços antigos viram OUTRO).
2. Entrar em `EM_EXECUCAO` cria uma cópia do checklist por serviço; mudar o modelo depois **não** altera a cópia.
3. `PRONTA` bloqueada com item obrigatório pendente; liberada com justificativa gravada com data e hora.
4. `NAO_SE_APLICA` sem justificativa é rejeitado.
5. Kit no orçamento expande em linhas e o total confere; editar uma linha não altera o kit.
6. Depois de `ENTREGUE`, checklist e laudo não editam; pagamento e anexos continuam editando.
7. Anonimizar cliente mantém laudos e instalações e apaga só os dados pessoais.
8. Teste de rede zero continua passando.
9. OS só com chicote (serviço que não exige entrada) vai a `EM_EXECUCAO` sem entrada e é entregue com "quem retirou", sem km de saída.
10. OS com chicote e instalação: a bancada começa sem entrada, o checklist da instalação fica bloqueado até a entrada, e `PRONTA` exige a entrada registrada.
11. Pacote "chicote + instalação" expande em duas linhas; apagar a linha da instalação deixa um orçamento só de chicote com o total certo.
12. Item `fornecidoPeloCliente` entra com valor zero e não soma no total.
13. Nova versão do chicote não apaga a versão anterior.
14. O backup novo guarda cada anexo uma vez; um backup antigo (com os anexos dentro do zip) e um novo restauram os mesmos dados.

Pronto quando: 4 OS reais passam pelo app instalado (uma de som, uma de vidro ou trava, uma de partida ou alternador e uma de injeção programável, com chicote, instalação ou os dois), cada uma com checklist cumprido; ao menos uma com laudo em PDF e a de injeção com ficha do chicote em PDF.

Situação em 21/10/2026: os blocos 3B.0 a 3B.7 estão prontos no código, com os 14 testes acima, e o MSI 0.3.1 foi gerado e ensaiado neste PC (docs/fase3b/ensaio-instalador.md). As duas escolhas da revisão final (docs/decisoes.md, item 275) foram corrigidas na versão 0.3.2 (itens 278 e 279), a pedido do dono, antes do uso com dados reais. Na 0.3.3, o app ganhou a marca GarageTech (itens 282 a 287), sem mudar regra nem dado. Falta o critério de pronto, com as 4 OS reais.

**Fase 4 — Mapa base** (era a fase 3)
- Perfis de injeção, gerador de tabelas (seção 7.5), editor de tabela com cores e vista 3D, saída nível C para todas as injeções e nível B para uma marca, checklist de primeira partida.
- Também: as unidades preferidas na tela (decisão 152) e o cálculo em segundo plano, com aviso de progresso na abertura (decisão 160).
- Pronto quando: um motor pega e segura marcha lenta com o mapa base gerado.

**Fase 5 — Agenda e lembretes**
- Agenda: agendamentos do dia e da semana; o agendamento vira OS quando o veículo chega; texto de confirmação para WhatsApp.
- Lembretes, com intervalos que eu defino:
  - revisão por km ou data;
  - garantia vencendo;
  - orçamento sem resposta;
  - OS parada aguardando peças;
  - pagamento pendente;
  - documento vencendo.
- Os avisos aparecem no Painel com o texto para WhatsApp. Eles são conferidos ao abrir o app e de hora em hora, nunca com o app fechado.
- Pronto quando: uma semana real de agenda é usada e os avisos aparecem certos.

**Fase 6 — Acerto, relatório e biblioteca** (era a fase 4)
- Sessões de acerto com anexos; colar o mapa final; importar log e curva de dinamômetro; relatório final em PDF, que também vai para os anexos da OS; biblioteca com busca e comparação entre base e final.
- Pronto quando: 3 projetos completos estão documentados e são encontrados pela busca.

**Fase 7 — Aprendizado e integrações** (era a fase 5)
- Métricas de diferença entre base e final; reajuste dos modelos de VE e de ignição por família, como nova versão da base de conhecimento (cálculos antigos não mudam); nível A (.msq) opcional; mais marcas no nível B.
- Pronto quando: a diferença média entre base e final fica abaixo de 10% nas células de plena carga.

**Fase 8 — Chicote e caixa de fusíveis (depois da Fase 7; pode ser antecipada)**
- Levantamento prévio em `docs/fase8/` (eu defino o que quero registrar: circuitos, cores, bitolas, fusíveis, relés), antes de qualquer código.
- Tabela de **circuitos por veículo** (nome, origem, destino, bitola, cor, fusível, relé, `aVerificar`).
- Ficha e modelagem de **caixa de fusíveis** (posições, amperagem, o que alimenta).
- Reaproveita as instalações do veículo (6.2), a ficha do veículo e a ficha do chicote da Fase 3B (6.6.5), que vira ponto de partida da tabela de circuitos. A Fase 8 trata de **reconstruir ou modelar** chicote e caixa de fusíveis existentes; a confecção de chicote para injeção programável já fica pronta na Fase 3B.
- Pronto quando: uma caixa de fusíveis e um chicote real estão documentados e consultáveis na ficha do veículo.

---

## 13. Glossário rápido

- **OS (ordem de serviço):** o registro de um veículo na oficina, da entrada à entrega (seção 6.5).
- **Mapa base:** primeiro mapa, conservador, só para o motor pegar e ir ao acerto.
- **Bancada:** conjunto de bicos; "duas bancadas" = primária e secundária por cilindro.
- **Caixa fria / caixa quente:** carcaça do compressor / da turbina do turbo; A/R é a relação de tamanho da carcaça.
- **Watercooler:** intercooler ar-água.
- **DC (duty cycle):** fração do ciclo em que o bico fica aberto.
- **Semáforo:** classificação verde, amarela ou vermelha de cada indicador de segurança.

---

## 14. Primeira ação

Comece pela **Fase 0**. Em modo de plano, me mostre o plano e suas dúvidas antes de executar qualquer coisa.
