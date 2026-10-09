# SPEC — Calibra (nome provisório)

App desktop offline para calibração inicial de injeção programável, feito para oficinas de preparação.

Versão da spec: 1.0 (08/10/2026). Dono do produto: Matheus, mecânico de preparação, que valida todas as regras técnicas.

---

## 0. Como trabalhar comigo

1. Sou mecânico (turbo e injeção programável) e programo Java em nível básico a intermediário; já fiz projetos com Java 21 e Spring Boot. Quero entender e conseguir manter o código.
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

**Usuário:** oficinas de preparação; o primeiro usuário sou eu, na minha oficina.
**Plataforma:** Windows 10 e 11, 64 bits, notebook comum (i5, 8 GB de RAM). **Sem internet.**

**Dentro do escopo:** motores 4 tempos de ignição por centelha (gasolina, etanol, metanol e misturas), **de 3 a 12 cilindros**, aspirados ou sobrealimentados, em carros, motos, utilitários, barcos, lanchas e jet skis.

**Fora do escopo:** gravar mapa direto na injeção (USB, CAN ou serial); engenharia reversa de arquivos ou protocolos fechados; acerto automático; nuvem, sincronização ou uso em rede por vários computadores; motores rotativos, diesel ou 2 tempos.

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
| `tracao` | DIANTEIRA, TRASEIRA, INTEGRAL (só terrestres) |
| `pesoKg` | opcional |
| `cor`, `observacoes` | opcionais |

- Todo salvamento grava no log de auditoria: campo, valor antigo, valor novo, data e hora. A aba Veículo mostra esse histórico.
- Veículo com projetos não pode ser apagado, só arquivado.

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
| `rotacaoPotenciaMaxima` | obrigatória; até a `rotacaoMaxima` do motor. |
| `rotacaoTorqueMaximo` | obrigatória para gerar mapa base. |
| `uso` | RUA, ARRANCADA, CIRCUITO_ENDURANCE, OFF_ROAD, NAUTICO. |
| `injecaoEscolhida` | do catálogo; opcional até a etapa de mapa base. |
| `status` | RASCUNHO → DIMENSIONADO → ORCADO → APROVADO → MAPA_BASE → EM_ACERTO → CONCLUIDO; ARQUIVADO a qualquer momento. |

---

## 7. Núcleo de cálculo

### 7.1 Fórmulas — implementar exatamente assim, cada uma com teste

```
Conversões: 1 cv = 0,98632 hp = 0,73550 kW; 1 lb = 0,453592 kg; 1 psi = 6,894757 kPa; 1 L = 61,0237 pol³

Ar e combustível
  P_hp = P_cv × 0,98632
  Wa [lb/min] = P_hp / k_ar                          k_ar padrão 10,5 (editável de 9,5 a 11)
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
| Ponto no mapa do compressor | dentro das ilhas centrais | perto de surge, choke ou rotação máxima; ou turbo sem mapa cadastrado | fora do mapa |
| BMEP (referência: aspirado 13 bar; sobrealimentado 22 bar) | até a referência | até 15% acima | acima de 15% com pistões ou bielas originais; com os dois forjados, vira amarelo com aviso |
| Velocidade média do pistão | até 20 m/s | 20 a 25 m/s | acima de 25 m/s |
| Saídas da injeção | suficientes | — | insuficientes |
| Faixa do sensor MAP | 1,2 × MAP máximo ou mais | 1,0 a 1,2 × MAP máximo | abaixo do MAP máximo |
| Aspirado: MAP necessário ÷ P_atm local | até 0,90 | 0,90 a 0,98 | acima de 0,98 ("meta acima do possível para aspirado com essa VE") |

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

1. **Início:** lista de projetos com busca por cliente, placa ou nome da embarcação, motor e status; botão "Novo projeto".
2. **Assistente de novo projeto:** Cliente → Veículo → Motor → Meta e uso → Injeção (opcional). O formulário do motor se adapta ao número de cilindros: ao mudar de 4 para 6, por exemplo, a ordem de ignição e os ângulos são sugeridos de novo e pedem confirmação.
3. **Projeto (abas):** Resumo · Veículo (editável, com histórico) · Motor (editável, com versões e comparação entre elas) · Dimensionamento (cartões com semáforo e memória de cálculo) · Peças e orçamento · Mapa base · Sessões de acerto · Relatório.
4. **Editor de tabela:** grade com cores por faixa; edição por célula ou seleção (somar, multiplicar em %, interpolar, suavizar); desfazer; comparação com outra versão em %; vista 3D que gira com o mouse; copiar e colar texto tabulado.
5. **Catálogos (cadastrar, editar, arquivar; sempre com fonte e data):** bicos (vazão, pressão e fluido de teste, impedância, tempo morto por tensão e pressão), turbos (com digitalizador de mapa: carregar imagem, calibrar os eixos com dois pontos cada, clicar surge, choke, ilhas de eficiência e linhas de rotação), injeções (perfil da seção 9), bombas (curvas), combustíveis, intercoolers, sensores, modelos de VE e de ignição.
6. **Biblioteca:** busca por configuração (cilindros, cilindrada ±10%, aspiração, combustível, injeção, bicos, turbo, uso) e comparação entre mapa base e mapa final.
7. **Configurações:** dados da oficina para os PDFs (nome, CNPJ, endereço, logo), unidades, limiares do semáforo, padrões de cálculo, pasta de backup, restaurar backup.

Fonte mínima de 13 px, tudo operável pelo teclado, tema claro.

---

## 9. Injeções: perfil, exportação e importação

**Perfil de injeção (catálogo):** marca, modelo, saídas de bico e de ignição, máximo de cilindros em sequencial, impedância aceita e bicos por saída, modelo de combustível (MS, VE ou MODELO_FISICO), tabelas com eixos e tamanhos, temperatura de referência da correção por ar, se a tabela em ms inclui tempo morto, recursos (controle de pressão do turbo, borboleta eletrônica, sensor de etanol, detonação, wideband interna, câmbio, largada), formato de importação e exportação, proteção contra água, fonte e data.

**Saída do mapa em três níveis:**

- **C — Folha de transcrição (todas as injeções, fase 3):** tela e PDF com a tabela idêntica à do software do fabricante (mesmos eixos, mesma ordem, cores por faixa) e os dados de entrada do assistente de mapa base da injeção.
- **B — Tabela para colar (fase 3 em diante, uma marca por vez):** área de transferência no formato que o software aceita (Link e MoTeC importam tabela com eixos; Haltech cola células). Antes de implementar uma marca, eu copio uma tabela real do software para um editor de texto; esse texto vira o teste do formato.
- **A — Arquivo pronto (fase 5, opcional):** mapa .msq (XML) do TunerStudio para MegaSquirt, Speeduino e rusEFI, gerado sempre a partir do arquivo .ini do firmware do cliente, nunca sem ele.

**Importação:** colar a tabela final (texto tabulado) do software do fabricante; anexar o arquivo nativo do mapa sem decodificar; importar log em CSV e curva de dinamômetro em CSV com mapeamento de colunas salvo por marca.

---

## 10. Requisitos de produção (valem a partir da fase 2)

- **Instalador MSI** via `jlink` + `jpackage`, com o Java embutido; atalho no menu Iniciar; ícone; versão semântica (0.x.y); UpgradeCode fixo para instalar por cima sem perder dados. Desinstalar não apaga `%LOCALAPPDATA%\Calibra`.
- **Script** `scripts/gerar-instalador.ps1`: roda `mvn -q verify`, gera o MSI e o coloca em `dist/` com a versão no nome.
- **Banco:** migração automática ao abrir, com backup antes de migrar; se a migração falhar, restaurar o backup e mostrar a mensagem.
- **Backup:** automático diário e ao fechar o app; manter os 30 mais recentes; pasta configurável (HD externo ou pen drive); restaurar pela tela de Configurações.
- **Logs:** arquivo rotativo em `logs\` (10 arquivos de 5 MB). Erro inesperado mostra mensagem amigável em português e grava o detalhe no log.
- **Interface fluida:** cálculos, PDFs, backup e importações rodam em `javafx.concurrent.Task`, com indicador de progresso.
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

**Fase 3 — Mapa base**
- Perfis de injeção, gerador de tabelas (seção 7.5), editor de tabela com cores e vista 3D, saída nível C para todas as injeções e nível B para uma marca, checklist de primeira partida.
- Pronto quando: um motor pega e segura marcha lenta com o mapa base gerado.

**Fase 4 — Acerto, relatório e biblioteca**
- Sessões de acerto com anexos; colar o mapa final; importar log e curva de dinamômetro; relatório final em PDF; biblioteca com busca e comparação entre base e final.
- Pronto quando: 3 projetos completos estão documentados e são encontrados pela busca.

**Fase 5 — Aprendizado e integrações**
- Métricas de diferença entre base e final; reajuste dos modelos de VE e de ignição por família, como nova versão da base de conhecimento (cálculos antigos não mudam); nível A (.msq) opcional; mais marcas no nível B.
- Pronto quando: a diferença média entre base e final fica abaixo de 10% nas células de plena carga.

**Fase 6 — Produto para outras oficinas**
- Módulo `calibra-licenca` com arquivo de licença assinado (Ed25519, do próprio JDK) e validado sem internet; instalador com assinatura de código; manual revisado; versão de teste para 3 a 5 oficinas.
- Pronto quando: 3 oficinas usam o app por um mês sem precisar de suporte diário.

---

## 13. Glossário rápido

- **Mapa base:** primeiro mapa, conservador, só para o motor pegar e ir ao acerto.
- **Bancada:** conjunto de bicos; "duas bancadas" = primária e secundária por cilindro.
- **Caixa fria / caixa quente:** carcaça do compressor / da turbina do turbo; A/R é a relação de tamanho da carcaça.
- **Watercooler:** intercooler ar-água.
- **DC (duty cycle):** fração do ciclo em que o bico fica aberto.
- **Semáforo:** classificação verde, amarela ou vermelha de cada indicador de segurança.

---

## 14. Primeira ação

Comece pela **Fase 0**. Em modo de plano, me mostre o plano e suas dúvidas antes de executar qualquer coisa.
