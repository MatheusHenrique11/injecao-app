# Decisões técnicas

Formato: data — decisão — motivo.

## 08/10/2026 — Fase 0

1. **Estrutura em 5 módulos `calibra-*`, pacote `br.com.calibra`** — segue a seção 5 da spec. O rascunho anterior (6 módulos `dominio`, `calculo`, etc., feito antes da spec) foi apagado com o seu aval. O histórico do Git foi mantido.
2. **Versões fixadas no POM pai** (conferidas na Maven Central em 08/10/2026, só estáveis):
   - JUnit BOM 6.1.3;
   - OpenJFX 25.0.4 (última da linha 25 LTS);
   - javafx-maven-plugin 0.0.8;
   - maven-compiler-plugin 3.16.0;
   - maven-surefire-plugin 3.6.0.

   SQLite, Flyway, Jackson e PDFBox entram só nas fases em que forem usados, com nova confirmação.
3. **`-Xlint:all` no compilador** — para a regra "compila sem avisos novos" (seção 11) ser visível.
4. **`.mvn/jvm.config` com `--sun-misc-unsafe-memory-access=allow`** — o Maven 3.9.11 (biblioteca Guice interna) usa `sun.misc.Unsafe`, e o Java 25 avisa a cada build. O aviso não vem do nosso código. A opção vale só para o processo do Maven, não para o app.
5. **Rodar o app com `install` + `-pl calibra-app javafx:run`** — com `-am` o plugin do JavaFX tenta rodar no POM pai e nos outros módulos e falha por falta de `mainClass`.
6. **Testes de fumaça conferem a regra de dependência** — os módulos core, ecu, dados e relatórios falham se o JavaFX aparecer no classpath deles; o app falha se não aparecer.
7. **Golden B (seção 7.6) usa cilindrada de 2.000 cc** — a spec dá só o curso (92,8 mm), sem o diâmetro. Com 2.000 cc os valores da tabela batem (MAP 241 kPa, PR 2,91). Com 82,5 mm (1.984 cc) daria MAP 243 kPa e PR 2,94, no limite da tolerância. **Pendente:** incluir o diâmetro (≈ 82,83 mm) ou a cilindrada na spec.
8. **Dois caminhos para a vazão de ar no core (fase 1)** — padrão `Wa = P_hp / k_ar`; opcional `Wa = P_hp × AFR × BSFC / 60`. O golden A (Garrett) parte de BSFC e A/F, então testa o segundo caminho; B, C e D testam o primeiro.
9. **WiX ainda não instalado** — só é preciso na fase 2. Instalar com `winget install --id WiXToolset.WiXCLI -e` quando você autorizar.
10. **Conferência prévia dos golden** — um cálculo independente (PowerShell) com as fórmulas da seção 7.1 reproduz A, B, C e D dentro de 1%. A mesma conta está na planilha `docs/fase0/cadeia-de-calculo.xlsx`.
11. **Pasta do projeto** — a spec chama a raiz de `calibra/`. A pasta ainda se chama `projetos\injecao-app`, porque o Windows bloqueou o renome enquanto o VS Code estava com ela aberta. Renomear depois de fechar a pasta no VS Code.

## 08 e 09/10/2026 — Fase 1

### Respostas do dono do produto

12. **Mapa do compressor fica para a fase 2.**
    - Na fase 1, o app calcula o ponto de operação (vazão e PR na potência máxima) e o semáforo do compressor sai AMARELO ("turbo sem mapa cadastrado").
    - Na fase 2, junto com o digitalizador, ficam: a geometria do mapa, os limiares de "ilhas centrais" e de "perto de surge/choke/rotação máxima", e o ponto de torque máximo.
13. **Checagem de marcha lenta do bico.**
    - MAP e VE de marcha lenta são campos opcionais, sem padrão. Sem eles, sai o aviso "marcha lenta não verificada".
    - A conta usa lambda 1,00 (SPEC 7.5) e T = T_amb + acréscimo do aspirado (10 °C).
14. **Radiador de óleo.**
    - Circuito/endurance e náutico: recomendado sempre.
    - Off-road: só quando a BMEP da meta passa da referência do semáforo (13 bar aspirado, 22 bar sobrealimentado).
15. **Meta na roda.** P_vira = P_roda ÷ (1 − perda), com a perda em fração da potência do motor.

### Decisões técnicas

16. **Unidades no núcleo.**
    - A API pública do core usa unidades métricas com a unidade no nome (`potenciaKw`, `mapKpa`, `tmC`, `combustivelKgH`, `vazaoCm3Min`).
    - As fórmulas Garrett da 7.1 rodam por dentro em lb/min, psia, °F e pol³, como a spec escreve, e são convertidas na entrada e na saída.
    - A memória mostra cada fórmula nas unidades da spec.
17. **Versão do núcleo = 1.0.0.** Vai em todo resultado e no PDF. Mudou fórmula, sobe a versão.
18. **Sugestões de ordem de ignição** (sempre "confirmar no manual do motor"):
    - 3: 1-2-3
    - 4: 1-3-4-2
    - 5: 1-2-4-5-3
    - 6: 1-5-3-6-2-4
    - 7: 1-3-5-7-2-4-6 (genérica)
    - 8: 1-8-4-3-6-5-7-2
    - 9: 1-3-5-7-9-2-4-6-8 (genérica)
    - 10: 1-10-9-4-3-6-5-8-7-2
    - 11: 1-3-5-7-9-11-2-4-6-8-10 (genérica)
    - 12: 1-7-5-11-3-9-6-12-2-8-4-10

    Até 6 cilindros, em linha; acima disso, V (90°; 60° no V12). Os ângulos sugeridos têm espaçamento de 720/n. Os modos padrão são sequenciais.
19. **Ângulos dos PMS** precisam crescer na ordem de ignição, entre 0 e 719°. O par a 360° (centelha perdida, semissequencial) aceita 0,5° de folga, porque a tela arredonda ângulos como 102,9°.
20. **Ângulo entre bancadas**: entre 0 e 180°, obrigatório em V e VR/W.
21. **Modo simultâneo**: conta 1 grupo de saídas por bancada até existir o perfil da injeção (fase 3).
22. **PR ≤ 1** (meta sem pressão positiva): o compressor não aquece o ar (T2 = T1), com aviso de informação.
23. **Sensor MAP**: o MAP máximo previsto é o **maior entre o MAP necessário e a P_atm local**. No turbo, normalmente é o MAP necessário. No aspirado, ou num turbo com meta baixa (PR ≤ 1), é a P_atm: o sensor precisa ler pelo menos a pressão atmosférica. Ajustado depois da revisão da fase 1.
24. **Vazão nominal mínima com tempo morto**:
    - É o inverso das fórmulas de TI e DC: janela = DC_max × ciclo / injeções − t_morto. É mostrada junto com a vazão sem tempo morto da 7.1.
    - Com duas bancadas, a vazão se divide na proporção de cada bico e o semáforo usa o maior DC.
25. **VE fora de 0–1,5** é recusada, com a dica "use fração: 0,95 = 95%", para pegar quem digita 95.
26. **Avisos de informação** (não mudam o semáforo):
    - BSFC de turbo a gasolina fora de 0,50–0,65 (faixa de conferência da 7.1);
    - biturbo (cada turbo recebe Wa/2 se for paralelo);
    - nenhum bico informado.
27. **Seleção em catálogos (7.3)** já existe no core e é testada com dados fictícios só de teste; entra na tela na fase 2:
    - `core.dimensionamento.SelecaoBico`: menor bico com DC até 80%, já com o tempo morto, avaliado pela mesma cadeia;
    - `SelecaoBomba`: menor bomba com folga verde; curva pressão × tensão, sem extrapolar;
    - `SelecaoInjecao`: saídas (contando os bicos extras), cilindros em sequencial e recursos.

    As recomendações ficaram em `core.calculo.Recomendacoes`, porque a cadeia as usa.
28. **Testes de coerência**: "trocar o bico não muda a tabela de VE" virou "trocar o bico não muda o lado do ar" (Wa, MAP, PR, Tm, massas). A versão com a tabela de VE vem na fase 3. "Converter e voltar" está no `ConversoesTest`.
29. **PDF (PDFBox 3.0.8)**:
    - Fonte Helvetica padrão. Caracteres que ela não tem (λ, ≤, Δ, ↔, −) viram texto ("lambda", "<=", "d", "<->", "-").
    - Números com vírgula, porque o PDF é documento para pessoas.
    - Grava num temporário e troca, para o arquivo nunca ficar pela metade.
    - Recusa gerar com vermelho sem justificativa.
30. **Cache de fontes do PDFBox**: ele varre as fontes do Windows e grava um cache.
    - No app, o cache fica em `%LOCALAPPDATA%\Calibra\cache`.
    - Nos testes, fica em `target/`.
    - O arquivo `.pdfbox.cache` que o primeiro teste criou na pasta do usuário foi apagado.
31. **Tela**:
    - Números com vírgula; "3.5" é recusado ("use vírgula para decimais") e "1.050" vale 1050.
    - Mudou um campo depois de calcular, o resultado fica "desatualizado" e o PDF é bloqueado (regra 2.5).
    - O PDF exige justificativa escrita em cada vermelho (regra 2.4), com data e hora.
    - Campos técnicos do motor e a pressão de base começam em branco; os parâmetros vêm com os padrões da 7.1.
    - A temperatura do ar começa em 25 °C (a referência dos casos golden), editável.
32. **Teste de interface (tag `ui`)** — fica fora do `verify` padrão, porque precisa da área de trabalho do Windows. Ele abre a calculadora sem mostrar janela, preenche o caso B, calcula, confere o vermelho, a justificativa e o bloqueio do PDF, e salva imagens só da cena em `calibra-app/target/`.
33. **JavaFX sem módulos Java (classpath)** na fase 1:
    - O `javafx:run` usa `--enable-native-access=javafx.graphics` para não avisar a cada abertura.
    - `module-info` (JPMS) e logs em arquivo entram na fase 2, com o jlink/jpackage do instalador.
34. **Versão do app 0.1.0** (POM e rodapé).

### Revisão da fase 1 (subagente, SPEC 0.7)

35. **Nenhuma falha bloqueante.** A revisão do diff contra a spec passou com 272 testes. Corrigi as duas falhas menores apontadas:
    - **Erro de não convergência sem teste:** a iteração MAP ↔ Tm virou um método próprio (`CalculoDimensionamento.iterarMapTm`), com teste que força o erro (limite de 1 passo) e teste do caso normal.
    - **Valores que decidem avisos fora da memória:** o TI mínimo de marcha lenta e a cilindrada informada agora entram na memória (passos "TI efetivo na marcha lenta" e "Cilindrada informada (conferência)") e no PDF.
36. **Perda antes do compressor ≥ P_atm local é recusada** (ex.: 15 psi digitado no lugar de 1,5). Antes, isso virava uma PR negativa tratada como "PR ≤ 1".
37. **O semáforo "Saídas da injeção" também confere o máximo de cilindros em sequencial**, quando a injeção informar. Na calculadora, a injeção digitada não tem esse campo; ele vale para o catálogo da fase 2.
38. **O PDF mostra os limiares do semáforo** ("padrão da SPEC 7.4" ou "editados"), o fluido de teste padrão e, no modo simultâneo, os grupos de saídas usados.
39. **Impedância do bico × driver da injeção e máximo de bicos por saída (6.3) ficam para a fase 3.** A spec condiciona a conferência ao perfil da injeção ("quando o perfil informar"), e o perfil completo só entra na fase 3. O `Bico` já tem o campo de impedância.

## 09/10/2026 — Fase 1.1 (motores do mercado)

Origem: o pedido de cobrir os motores do mercado, não só o 2.0, e a pesquisa em `docs/pesquisa/mercado-preparacao-brasil.md`. Das propostas da pesquisa, o dono do produto escolheu para agora só os testes com motores do mercado. Meta pela pressão do turbo, catálogo de motores e montagens, torque × câmbio e geometria de montagem ficam para a fase 2.

40. **k_ar aceito de 8,0 a 11** (a spec dizia 9,5 a 11). Decisão do dono do produto.
    - **O que se viu:** com k_ar 10,5 (referência da Garrett para motores modernos), os dados de fábrica do Omega 4.1 (168 cv a 4.500 rpm) e do Omega 3.0 (165 cv a 5.800 rpm) só fecham com VE implícita de 0,68 e 0,71.
    - **O que isso quer dizer:** motor antigo de 2 válvulas tira menos cv do mesmo ar. Com 10,5, o app subestimaria ar e combustível em 15–20%: bico pequeno e risco de mistura pobre.
    - **O que muda:** o padrão continua 10,5. A tela mostra a referência a validar: perto de 10,5 em motor moderno de 4 válvulas e perto de 8,5–9,5 em motor antigo de 2 válvulas.
    - **Na spec:** a linha da 7.1 foi atualizada, com nota.
    - **Fase 5:** a ideia é aprender o k_ar de cada família a partir das curvas de dinamômetro dos projetos.
41. **Testes com motores do mercado** (`MotoresDoMercadoTest`, 53 casos). A geometria vem da pesquisa, com a fonte anotada em cada motor:
    - AP 1.6/1.8/2.0, Opala 151 e 250, Omega 4.1 e 3.0;
    - Família II C20NE, C22NE e 2.4;
    - Fiat 2.0 20V Turbo e 2.4 20V, VW 2.5 20V;
    - G4GC, EA211 1.0 TSI, EA888 2.0 TSI.

    Os testes conferem:
    - a cilindrada contra a ficha (1%) e as montagens do AP;
    - a cadeia inteira em 3, 4, 5 e 6 cilindros;
    - que motor de série dá BMEP e velocidade do pistão verdes;
    - a VE implícita dos aspirados de série.
42. **Faixas de plausibilidade nos testes** (VE implícita entre 0,60 e 1,05; motores antigos com k_ar 9,0 entre 0,75 e 1,05): são escolha minha, para pegar erro grosseiro e registrar o achado da decisão 40. Não são regras da spec e não aparecem no app.

## 11/10/2026 — Fase 2 (bloco 2.1: banco, migrações e backup)

43. **Flyway 12.11.0 com SQLite só com o `flyway-core`.** Não precisou de módulo extra nem de licença paga (pergunta da SPEC 4). O Flyway traz o Jackson 3 (`tools.jackson`) para uso próprio; o nosso código usa o Jackson 2.22.3, como diz a spec. Os dois convivem porque ficam em pacotes diferentes. Não veio Lombok nem outra dependência.
44. **Banco aberto por migração automática.** Banco novo: cria do zero. Banco com dados e migração pendente: backup antes, migra; se falhar, volta o backup e mostra a mensagem. Banco gravado por versão mais nova do app: recusado sem mexer em nada. Tabelas `STRICT` (o SQLite confere os tipos) e datas em texto ISO.
45. **Backup em zip** `calibra-backup-AAAA-MM-DD-HHMMSS.zip` (com segundos, para dois backups no mesmo minuto não brigarem), com o banco copiado por `VACUUM INTO`, a pasta `anexos\` e um `manifesto.json` (versão do app e do banco, data e SHA-256). O zip é montado com outro nome e renomeado no fim. Ficam os 30 mais recentes.
46. **Restauração** confere o SHA-256 e o `PRAGMA integrity_check`, recusa backup de banco mais novo, faz um backup de segurança antes de trocar os arquivos e migra se o backup for mais antigo.
47. **JSON sem anotação no core.** O que o Jackson precisa saber (subtipos do combustível, datas ISO, NaN) fica em `calibra-dados/json/Json.java`. Campo desconhecido é ignorado. O teste `JsonDeOuroTest` lê um cálculo do caso B gravado na 0.2.0: se alguém renomear um campo do core, ele falha.
48. **Configurações salvas como JSON** na tabela `configuracao`, mescladas com o padrão na leitura: um campo novo entra com o valor padrão sem quebrar o que já foi salvo.
49. **`.gitignore`: `backup/` virou `/backup/` e `/backups/`.** A regra antiga, sem a barra no começo, ignorava também o pacote de código `br/com/calibra/dados/backup/`, e o `Backup.java` ficaria fora do Git.
50. **Partida do app em `Inicio`** (novo `main`), separada do `CalibraApp`: liga o log e o tratamento de erros antes do JavaFX e deixa o app rodar pelo classpath, como no instalador. A parte sem tela (pastas, caches, banco migrado) fica em `Partida`, com teste. A propriedade `-Dcalibra.pasta=...` troca a pasta de dados (testes e ensaios).
51. **Versão vem do POM** por um `versao.properties` filtrado pelo Maven; só esse arquivo é filtrado, porque o FXML usa `${...}` para outra coisa. A constante `CalibraApp.VERSAO` saiu.
52. **Log em `logs\calibra-0.log` a `calibra-9.log`, 5 MB cada**, com `java.util.logging`. Erro não tratado vai para o log com o detalhe e aparece na tela com mensagem em português; erro do banco ou do backup mostra a mensagem dele.

## 12/10/2026 — Fase 2 (bloco 2.1: janela principal e Configurações)

53. **Janela principal com navegação à esquerda.** Por enquanto tem Calculadora e Configurações; Projetos e Catálogos entram nos blocos 2.2 e 2.3, quando existirem, para não ter botão que não faz nada. A versão saiu do rodapé da calculadora e foi para a barra de status da janela. Em tela pequena (notebook 1366 × 768), a janela abre maximizada e as telas rolam.
54. **Configurações em abas: Oficina, Cálculo e semáforo, e Backup.** Cada aba tem o próprio "Salvar" e "Descartar alterações".
    - **Padrões e limiares:** valem as faixas do núcleo (`CalculoDimensionamento.validarParametros`), e os dois limites de cada indicador precisam ficar em ordem. Valor diferente do padrão da SPEC aparece em azul. "Voltar aos padrões da SPEC" só preenche os campos; grava ao salvar.
    - **Caminho do ar:** o padrão salvo é sempre o k_ar. O BSFC continua sendo escolhido em cada cálculo.
    - **Calculadora:** recebe os padrões salvos. Campo que ainda mostra o padrão anterior recebe o novo; campo que o usuário já mudou fica como está. Limiares novos deixam o resultado da tela "desatualizado", porque as cores foram calculadas com os antigos.
55. **Logo da oficina:** PNG ou JPG até 5 MB, copiado para `anexos\oficina\logo-<impressão digital>.png` (entra no backup). O nome vem do conteúdo da imagem, então trocar o logo nunca sobrescreve o arquivo em uso.
56. **`NumeroBr.formatar(valor, mínimo, máximo de casas)`:** mostra o valor salvo sem cortar o que foi digitado (k_ar 9,75 não vira 9,8 ao reabrir a tela).
57. **Backup automático conferido de hora em hora, feito uma vez por dia.** O app confere ao abrir e depois a cada hora; só faz o backup quando o mais recente da pasta atual tem mais de 24 h (SPEC 10: diário). Assim fica coberta a oficina que deixa o app aberto por dias. O plano dizia "a cada hora com o app aberto"; um backup por hora, com 30 guardados, apagaria em um dia e pouco o histórico de um mês. Backup com data no futuro (relógio do computador mudou) conta como vencido. Se o automático falhar, a barra de status mostra o erro em vermelho e ele tenta de novo na hora seguinte.
58. **Backup ao fechar só quando algo mudou** desde o último backup feito com o app aberto. A janela mostra "Fazendo backup antes de fechar…" e também espera um backup que já esteja rodando. Se falhar, pergunta: "Sair sem backup" ou "Continuar no Calibra".
59. **Uma fila só para backup e restauração** (`TarefasFundo`: `javafx.concurrent.Task` numa única linha de execução): os dois nunca rodam juntos. A restauração bloqueia a tela com uma janela de espera; depois, as telas são montadas de novo com os dados restaurados.
60. **Pasta de backup escolhida:** o app testa se dá para gravar nela na hora de escolher. Com o pen drive ou HD desligado, o backup vai para a pasta padrão, com aviso na aba Backup e na barra de status. A lista para restaurar junta a pasta atual e a padrão, onde ficam os backups de antes de atualizar o banco e de antes de restaurar.

## 12/10/2026 — Fase 2 (bloco 2.2: cadastros e projeto)

61. **Veículo tipo OUTRO conta como terrestre** (placa, chassi, tração e uso em km). A SPEC não diz; os casos comuns de "outro" (kart, trator, gerador) são terrestres. Se aparecer embarcação fora da lista, cria-se um tipo novo.
62. **Cliente:** nome e telefone obrigatórios, e o telefone precisa de pelo menos 8 números. O consentimento é obrigatório para salvar, com data e finalidade (regra 2.8). A tela já traz a data de hoje e uma finalidade sugerida, que pode ser trocada. CPF ou CNPJ válido fica formatado. Anonimizar mantém o consentimento, que não identifica ninguém.
63. **Status do projeto:** para a frente, um passo de cada vez (SPEC 6.4); arquivar a qualquer momento; reativar volta ao status de antes de arquivar. Voltar na sequência (ex.: orçamento recusado) fica para o bloco 2.5, quando o orçamento existir.
64. **Trocar o tipo do veículo tira os campos que o novo tipo não usa:** de carro para lancha saem placa, chassi, tração e câmbio de carro; de lancha para carro saem nome, inscrição e casco. A tela mostra só os campos do tipo, e o histórico guarda os valores antigos.
65. **Diferença entre versões do motor** pelo valor como aparece na tela (até 3 casas): diferença menor que isso não conta e não cria versão nova. A cilindrada calculada entra como linha da comparação.
66. **Busca sem acento, sem pontuação e em minúsculas** (`TextoBusca`): "joao" acha "João" e "abc1234" acha "ABC-1234". O banco guarda esse texto numa coluna `busca` de cada cadastro.
67. **O motor pertence ao projeto, não ao veículo.** Cada projeto tem as suas versões (v1, v2...). Um projeto novo para o mesmo veículo não mexe nos cálculos dos projetos antigos. A descrição do motor ("AP 2.0 8V"), para reconhecer na lista, fica na versão, fora da `ConfiguracaoMotor`.
68. **Como cada coisa é gravada (migração V2):**
    - **Cliente e veículo:** em colunas, porque o histórico e a anonimização trabalham campo a campo.
    - **Meta do projeto:** em JSON.
    - **Versão do motor e cálculo:** JSON com colunas de busca (cilindros, cilindrada, aspiração, combustível).
    - **Cálculo:** guarda a contagem de vermelhos e amarelos para a lista.
69. **Histórico (auditoria):**
    - Grava o nome do campo e o valor como aparece na tela (ex.: "250.000 km").
    - A criação é uma linha só, "Cadastro: criado". Os campos de um mesmo salvamento aparecem na ordem da tela.
    - **Anonimizar** troca os valores pessoais por "(apagado na anonimização)". Isso inclui placa, chassi e identificação da embarcação dos veículos do cliente.
    - Cliente anonimizado não é editado e sai da busca de clientes.
70. **Apagar só cadastro sem vínculo:** cliente sem veículo; veículo sem projeto (SPEC 6.2). Projeto não é apagado, só arquivado. Apagar leva o histórico junto.
71. **O banco recusa alterar ou apagar** versões do motor, cálculos e justificativas (gatilhos na V2). Mesmo um erro de programação não muda um resultado salvo (regra 2.5).
72. **Migrações só de teste viraram V900 e V901** ("versões futuras"), para não colidir com as do app. Os testes usam a constante `BaseDeTeste.VERSAO_BANCO`, que sobe a cada migração nova.
73. **O assistente de novo projeto confere tudo antes de gravar** e mostra todos os erros de uma vez: cliente, veículo, motor (SPEC 6.3) e meta contra o motor. Cliente, veículo, projeto e a v1 do motor são gravados juntos, tudo ou nada. Regras:
    - cliente novo precisa de veículo novo;
    - veículo já cadastrado precisa ser do cliente escolhido;
    - cliente anonimizado não recebe projeto novo.
74. **Salvar cálculo:**
    - O resultado gravado é sempre o da entrada gravada, porque o app recalcula na hora de salvar.
    - O salvamento é recusado se o motor do projeto mudou depois do cálculo.
    - Grava uma justificativa por vermelho; as de outros indicadores ficam de fora.
    - O primeiro cálculo passa o projeto de RASCUNHO para DIMENSIONADO, e a meta do cálculo vira a meta do projeto.
75. **"Desatualizado — recalcular" mostra o motivo:** o motor está numa versão mais nova (ex.: "usou a v1 e o motor está na v2") ou o núcleo de cálculo é outro (o bloco 2.4 leva o núcleo à 1.1.0). Peça alterada no catálogo entra como motivo no bloco 2.3.
76. **Salvar o motor sem mudança não cria versão nova:** a descrição é a mesma e nenhum campo mudou na comparação da tela (decisão 65).
77. **Formulário do motor separado da calculadora** (`motor.fxml` e `MotorFormController`). A calculadora o inclui na aba 1 sem mudar nada na tela; o assistente e a aba Motor do projeto usam o mesmo formulário.
    - **Ajudante comum:** `CamposTela` lê e escreve os campos como texto, para a calculadora e o formulário do motor.
    - **`EscritorFormulario`:** é o caminho de volta do `LeitorFormulario`, que mostra na tela um motor ou um cálculo salvo. Os números vão com até 4 casas; escrever e ler de volta dá a mesma entrada (teste).
78. **A tela inicial passa a ser Projetos** (a lista; SPEC 8.1). O menu fica Projetos · Calculadora · Configurações, e Catálogos entra no bloco 2.3. A área de projetos lembra o que estava aberto (lista, assistente ou projeto) quando se volta pelo menu.
79. **Assistente em quatro etapas**, cada uma conferida antes de seguir:
    - **Cliente:** já cadastrado (com busca) ou novo.
    - **Veículo:** um do cliente ou novo.
    - **Motor:** inclui a descrição opcional (ex.: "AP 2.0 8V").
    - **Meta e uso.**
    - A etapa Injeção entra no bloco 2.3.
80. **Projeto em abas: Resumo · Veículo · Motor · Dimensionamento.** Peças e orçamento entram no bloco 2.5; Mapa base, Sessões de acerto e Relatório, nas fases 3 e 4.
    - **Resumo:** cliente (editar, histórico e anonimizar, este com confirmação e aviso de que não tem volta), veículo, motor, meta, último cálculo, status com histórico e observações do projeto.
    - **Cabeçalho:** arquivar e reativar o projeto.
81. **A aba Dimensionamento é a calculadora sem a aba do motor:**
    - **Motor:** é a versão atual do projeto, usada direto, sem passar pelo texto da tela. O cálculo aponta exatamente para a versão usada.
    - **Botões e abas:** ganha "Salvar cálculo no projeto" e a aba "Cálculos salvos".
    - **Ao abrir:** vem com as entradas do último cálculo salvo, ou com a meta do projeto se ainda não houver cálculo.
    - **Motor mudou:** o resultado da tela fica desatualizado, e o último cálculo salvo ganha a faixa "desatualizado — recalcular", que também aparece no nome da aba.
82. **PDF da memória do projeto:** o cabeçalho leva o número do projeto, o cliente, o veículo e a versão do motor. Os "Cálculos salvos" geram o PDF de um cálculo antigo, com as justificativas gravadas.
83. **Mensagens de "salvo" aparecem na própria tela, sem janela.** Só confirmações e erros abrem janela: incomoda menos e não trava o teste automático.
84. **Arquivar veículo:** existe no banco e foi testado (o veículo sai da lista do assistente), mas ainda não tem botão. Por enquanto todo veículo nasce com um projeto. O botão entra quando houver uma tela de clientes e veículos.
85. **Testes de interface:** o cache do JavaFX dos testes fica em `target/javafx-cache` (configuração do surefire no POM). Na pasta temporária do teste, a DLL de fonte ficava presa e a pasta não era apagada no fim.

## 12/10/2026 — Fase 2 (bloco 2.3: catálogos e seleção de peças)

86. **Peças novas de catálogo no núcleo.** Campo que não tem fonte fica vazio (regra: não inventar dado técnico).
    - **Intercooler:** tipo ar-ar ou ar-água; a eficiência é opcional e, se informada, vira o eps do cálculo.
    - **Sensor:** tipo (MAP, pressão, temperatura ou outro) e faixa. A faixa máxima do sensor MAP é a do semáforo.
    - **Câmbio:** tipo e torque máximo em N·m, para o indicador "Torque × câmbio" do bloco 2.4.
    - **Motor ou montagem (`ModeloMotor`):** geometria, biela e taxa, com cilindrada, relação biela/curso e diferença da ficha calculadas.
87. **Bomba do catálogo no cálculo:**
    - A entrada aceita a bomba, com a curva, no lugar da vazão digitada (nunca as duas).
    - O núcleo lê a vazão na pressão exigida (base + turbo) e na tensão, e grava esse passo na memória de cálculo.
    - Sem tensão, ou com a curva sem cobrir a condição: aviso amarelo, e a folga não é avaliada.
    - Nenhuma fórmula mudou, então o núcleo continua 1.0.0.
88. **Dinheiro em centavos inteiros** (`Dinheiro`, no núcleo): lê "1.234,56" e "R$ 1.234,56" e recusa "12.5", que é ambíguo. Serve ao preço de referência dos catálogos e ao orçamento do bloco 2.5.
89. **Catálogo em duas tabelas (migração V3):**
    - **`catalogo_item`:** a versão atual, com nome e busca em colunas. Os dados técnicos ficam em JSON, no record do núcleo de cada tipo.
    - **`catalogo_versao`:** todas as versões; o banco recusa alterar ou apagar.
    - Editar grava uma versão nova. Item nunca é apagado, só arquivado: sai das escolhas e continua nos cálculos antigos.
90. **Todo item de catálogo tem:** nome, fonte (obrigatória), data, "a verificar", preço de referência (em centavos) e observações. O combustível guarda no próprio registro o número da versão do catálogo, que o motor e o cálculo levam junto (SPEC 7.2).
91. **Sementes num JSON legível** (`sementes/catalogo.json`), gravadas uma vez pela migração V4, que é em Java porque lê o JSON.
    - **Proteção:** o checksum da V4 é o do arquivo. Se alguém editar o arquivo depois de publicado, o Flyway acusa; semente nova entra numa migração nova.
    - **Combustíveis:** os 6 da SPEC 7.2, iguais aos do código (o teste confere), sem "a verificar".
    - **Motores:** 19 da pesquisa (o plano falava em 17; entraram também C20XE, Opala 250-S e Fiat 2.0 20V aspirado, que têm geometria e fonte). Mais 6 montagens do AP. Todos com fonte e "a verificar".
    - **Sem fonte, sem valor:** campo que a pesquisa não trouxe fica vazio (válvulas, biela, taxa, aspiração). A ordem de ignição não entra; o formulário sugere uma, com "confirmar no manual".
92. **O cálculo salvo grava as peças do catálogo que usou** (tabela `calculo_peca`: papel, item e versão; o banco recusa alterar ou apagar). É a base do "desatualizado" por peça.
93. **Câmbio do catálogo no veículo e injeção escolhida no projeto:** colunas `cambio_item_id` e `injecao_item_id`, opcionais. A troca do câmbio entra no histórico do veículo, com o nome do item.
94. **Formulário de catálogo montado por lista de campos** (`br.com.calibra.app.catalogo`, sem JavaFX, testado sozinho):
    - Cada tipo descreve os próprios campos, as colunas da lista e a conversão texto ↔ record do núcleo (`FormaBico`, `FormaBomba`...). Uma única janela (`JanelaItemCatalogo`) serve aos 8 tipos.
    - Os campos comuns (nome, fonte, data, "a verificar", preço e observações) são os mesmos para todos. Na injeção, o nome sai da marca e do modelo.
    - Primeiro aparecem os erros de digitação; depois, as regras da peça. Os casos de uso conferem as mesmas regras antes de gravar.
95. **Unidades da tela iguais às da calculadora:** pressão do bico e da bomba em bar (o núcleo guarda kPa), eficiência do intercooler em fração (0,80, como o eps), teor de etanol em % (o núcleo guarda fração).
96. **Tabelas (tempo morto e curva da bomba) num editor próprio:**
    - Uma linha de campos por ponto, com "+ Linha", "✕" e "Colar tabela" (texto do Excel ou do PDF, colunas separadas por tabulação ou ponto e vírgula; a vírgula é o decimal).
    - Não usa a célula editável do TableView, que perde o que foi digitado quando se clica fora.
    - Tempo morto com uma linha só e sem tensão e pressão = valor único, que vale para qualquer condição.
    - Ponto repetido (mesma tensão e pressão) é recusado.
97. **Gravar sem mudar nada não cria versão.** Abrir um item das sementes e salvar sem mexer também não (testado em todos).
98. **Código do combustível:**
    - Único, em maiúsculas, sem espaço, até 15 caracteres.
    - Não muda depois de criado, porque os motores guardam o código. Para outro código, use Duplicar (a cópia vem com o código vazio).
    - O campo de composição mostra só o valor que se aplica: % de etanol em volume, % em massa ou AFR fixo.
99. **Item novo começa sem "a verificar":** quem cadastra copia da ficha do fabricante e marca "a verificar" quando o dado é incerto. Os motores das sementes já vêm marcados.
    - **Bico novo:** vem com o fluido de teste de 0,70 kg/L, o padrão da SPEC, com a dica "confira na ficha".
100. **Histórico de versões:** tabela campo × versão, com a mudança em amarelo em relação à versão anterior. Campo vazio em todas as versões ou que não se aplica fica de fora.
101. **Data digitada é conferida de verdade:** 31/02/2026 é recusada, em vez de virar 28/02 sem aviso.
102. **Testes de interface:** o JavaFX não se desliga quando a última janela fecha (`Platform.setImplicitExit(false)`). Uma janela aberta só para a foto desligava o JavaFX, e os passos seguintes nunca rodavam.
103. **Peças do catálogo no dimensionamento** (calculadora e aba Dimensionamento):
    - Peça escolhida vale no lugar dos campos digitados dela, que ficam travados mostrando os dados do catálogo.
    - **Campo novo "Tensão de trabalho (V)":** lê a tabela de tempo morto e a curva da bomba.
    - **Intercooler:** com eficiência cadastrada, ela troca o eps; sem ela, vale o eps digitado.
    - **Sensor MAP:** a lista só traz sensores do tipo MAP, e a faixa máxima vira a faixa do semáforo.
104. **"Sugerir"** usa as regras do núcleo, só com itens ativos:
    - **Bico:** o menor que mantém o DC (só com uma bancada).
    - **Bomba:** a menor com folga verde na pressão exigida (precisa da tensão).
    - **Injeção:** a primeira, em ordem de nome, que atende às saídas, ao sequencial e aos recursos pedidos (sensor de etanol do motor e controle do câmbio do veículo).
105. **O cálculo salvo grava as peças escolhidas e o combustível do motor, com a versão de cada um.** O caso de uso confere se as peças são as mesmas da entrada calculada.
106. **Desatualizado por peça:**
    - A mensagem diz o que mudou (ex.: "o bico mudou no catálogo: X v1 → v2").
    - **Combustível:** vem com o motor, então a versão nova só entra gravando o motor de novo, e a mensagem diz isso.
    - Arquivar uma peça não deixa o cálculo desatualizado.
107. **Ao abrir o Dimensionamento**, as peças do último cálculo vêm escolhidas na versão atual do catálogo, para recalcular. Peça arquivada continua escolhida, marcada "arquivado".
108. **Combustíveis do formulário do motor vêm do catálogo**, na versão atual.
    - Motor com combustível arquivado continua mostrando esse combustível.
    - Gravar o motor depois que o combustível mudou cria uma versão nova do motor (a comparação mostra "versão 1 → versão 2").
109. **"Preencher pelo catálogo" no motor:**
    - Preenche cilindros, diâmetro, curso, cilindrada da ficha, taxa, válvulas e aspiração.
    - O que o catálogo não tem fica em branco, apagando o que estava, e aparece um aviso.
    - A ordem de ignição é sugerida de novo, com "confirmar no manual".
    - A descrição do motor, se estiver vazia, recebe o nome do item.
110. **Assistente com a etapa 5, Injeção (opcional):**
    - Mostra o que falta na injeção escolhida para este motor e tem o botão Sugerir.
    - A injeção do projeto aparece no Resumo (escolher ou tirar).
    - Enquanto não há cálculo salvo, ela entra escolhida no Dimensionamento.
111. **Câmbio do catálogo no veículo:**
    - Define o tipo de câmbio, que fica travado enquanto o item está escolhido.
    - A lista traz só câmbios terrestres ou só náuticos, conforme o veículo.
    - O caso de uso recusa câmbio do catálogo de tipo diferente do veículo.
112. **Fotos dos testes de interface:** o cache de imagem dos nós é desligado antes da foto. Fora de janela, a área com rolagem mostrava o painel antigo (ex.: a etapa anterior do assistente).

## 13/10/2026 — Fase 2 (bloco 2.4: turbo, meta pela pressão e torque × câmbio)

113. **Núcleo 1.1.0.** Entram o mapa do compressor, o ponto de torque máximo, a meta pela pressão e o indicador torque × câmbio, cada fórmula com teste. Os casos golden A a D continuam iguais. Cálculo salvo com o núcleo 1.0.0 passa a aparecer como "desatualizado — o núcleo de cálculo mudou" (regra 2.5); o que estava gravado continua legível.
114. **Mapa do compressor no núcleo (`MapaCompressor`):**
    - Pontos em kg/s × PR (SI). O eixo original (lb/min ou kg/s) fica guardado só para mostrar o mapa como o fabricante desenhou.
    - A temperatura e a pressão de referência ficam no próprio mapa; a vazão corrigida de cada turbo usa as dele: Wc = Wa × raiz(T1/T_ref) ÷ (P1/P_ref), com T1 = temperatura ambiente e P1 = P_atm − perda antes do compressor.
    - **Fora do mapa:** acima do topo das linhas de surge e de choke, à esquerda do surge, à direita do choke ou acima da rotação máxima.
    - Abaixo do primeiro ponto marcado, o surge e o choke valem o primeiro ponto (as linhas descem quase retas até PR 1).
    - **Rotação no ponto:** cada linha de rotação dá a sua PR na vazão do ponto, e a rotação é interpolada entre as duas linhas vizinhas. Acima da maior linha desenhada: se ela é a rotação máxima, fora do mapa; se não, amarelo ("conferir no mapa do fabricante").
    - **Eficiência no ponto:** a da ilha mais interna que contém o ponto; o pico é a maior ilha marcada.
115. **Semáforo do compressor** (resposta do dono do produto no plano; limiares em Configurações, "a validar"):
    - Vermelho fora do mapa; amarelo com menos de 10% de vazão sobre o surge, menos de 5% até o choke, acima de 95% da rotação máxima, acima das linhas desenhadas, fora das ilhas ou mais de 5 pontos abaixo do pico; verde no resto.
    - Avalia os pontos de potência e de torque, e vale a pior cor. Sem turbo com mapa: amarelo, como antes.
    - **Limiares novos** (compressor e câmbio): um JSON gravado antes deles chega com zero nesses campos, e o zero vira o padrão.
116. **Ponto de torque máximo:** o mesmo MAP e a mesma Tm da potência, na rotação de torque, com a VE do torque (campo novo; sem ela, vale a VE da potência). A vazão sai da fórmula do MAP ao contrário; a potência, do k_ar ou do BSFC ao contrário; e daí o torque estimado.
117. **Meta pela pressão** (só sobrealimentado):
    - MAP = P_atm + pressão relativa; PR, T2 e Tm sem iteração (o MAP é conhecido); a vazão de ar pela fórmula do MAP ao contrário; a potência pelo k_ar ou pelo BSFC; e o resto da cadeia normal.
    - Na meta pela pressão, a potência da meta fica vazia (NaN) e sai do cálculo. Com a perda de transmissão informada, o cálculo também estima a potência na roda.
    - Ida e volta testada: potência → MAP → potência, com diferença menor que 0,1%.
118. **Biturbo:** os pontos do compressor usam metade da vazão por turbo, considerando os dois em paralelo (a memória diz isso; o aviso de "confirmar a montagem" continua).
119. **Torque × câmbio:** compara o maior torque estimado (na potência ou no torque máximo) com o torque máximo do câmbio do catálogo: verde até 90%, amarelo até 100%, vermelho acima. Sem câmbio ou sem limite cadastrado, só um aviso de informação.
120. **Seleção de turbo (`SelecaoTurbo`):** só os turbos cujo mapa contém os pontos de potência e de torque, do mais eficiente no ponto de potência para o menos (fora das ilhas por último). Turbo sem mapa fica de fora, com aviso. Junto da escolha aparece o compromisso do A/R da caixa quente (SPEC 7.3).
121. **Turbo no catálogo** (tipo `TURBO`, sem migração: o tipo é texto livre na tabela):
    - O mapa, o nome da imagem e a calibração ficam no JSON da versão, como as outras peças.
    - O formulário mostra só o A/R da caixa quente. O mapa vai junto, oculto, para não se perder ao editar o resto.
    - Na comparação de versões, o mapa aparece resumido ("5 pontos de surge, 5 de choke, 3 ilhas…").
    - Turbo novo começa sem mapa ("sem mapa" na lista); no cálculo, continua amarelo "turbo sem mapa".
122. **Digitalizador do mapa** ("Mapa do compressor…" na lista de turbos):
    - A imagem é copiada para `anexos\turbos\turbo-<id>-<data>.<ext>` e vai no backup. Aceita PNG, JPG, GIF e BMP; o turbo guarda só o nome do arquivo.
    - Cada eixo se calibra com dois pontos da escala, em escala **linear** (a dos mapas de compressor que conhecemos). Escala logarítmica não é aceita por enquanto.
    - O surge e o choke são uma linha só cada: voltar a eles continua a mesma linha. Cada ilha (com a eficiência) e cada linha de rotação (com o rpm) é um elemento novo.
    - Desfazer (Ctrl+Z) tira o último ponto clicado. Salvar confere o mapa no núcleo e grava uma versão nova do turbo.
    - Reabrir mostra a mesma imagem, a calibração e o desenho, para continuar.
123. **Referência do mapa com unidade:** a temperatura (°C, K, °F ou °R) e a pressão absoluta (kPa, psia ou bar) se digitam como estão na legenda do fabricante. Ficam gravadas em K e kPa e reabrem em °C e kPa.
124. **Nenhum mapa de fabricante no repositório:** o teste de interface desenha um mapa fictício (com a legenda "MAPA FICTÍCIO PARA TESTE") e digitaliza esse.
125. **Limiares novos em Configurações** (em %, "a validar"): folga do surge (10), folga do choke (5), rotação do turbo (95% da máxima), queda de eficiência no verde (5 pontos) e torque × câmbio (verde até 90, amarelo até 100). O verde do câmbio não pode passar do amarelo; a rotação vai até 100%.
126. **Ações extras na lista de um catálogo:** cada tipo pode ganhar botões próprios, desligados enquanto nenhum item está escolhido. O turbo usa isso para o digitalizador.
127. **Meta no formulário:** o campo novo "tipo de meta" escolhe entre potência e pressão do turbo.
    - Sem o tipo (cálculos e projetos de antes do bloco 2.4), a meta é pela potência.
    - A pressão se digita em bar, psi ou kPa (sem unidade, vale bar) e volta para a tela sempre em bar.
    - Na meta pela pressão, a potência fica em branco. "Medida na roda" com a perda faz o cálculo estimar também a potência na roda.
    - A VE do torque é opcional: sem ela, vale a VE da potência.
128. **Turbo e câmbio gravados com o cálculo,** como as outras peças (papéis `TURBO` e `CAMBIO`, sem migração: o papel é texto). Versão nova de qualquer um deixa o cálculo desatualizado ("o turbo mudou no catálogo: … v1 → v2"). No projeto, o câmbio é o do veículo.
129. **PDF da memória:**
    - Mostra a meta pela pressão, a VE do torque, o turbo (A/R e resumo do mapa) e o câmbio (torque máximo).
    - Mostra o ponto de torque estimado e os pontos de potência e de torque no mapa (vazão corrigida, PR, eficiência, rotação e folgas).
    - Mostra os limiares novos do compressor e do câmbio.
130. **Turbo e câmbio no Dimensionamento:**
    - "Sugerir turbos" escolhe, entre os turbos com mapa que têm os pontos de potência e de torque dentro, o mais eficiente no ponto de potência.
    - A mensagem lista os outros que atendem, com a eficiência, e traz o compromisso do A/R. Os avisos dizem por que cada um dos outros ficou de fora.
    - O câmbio se escolhe na calculadora avulsa. No projeto vale o câmbio do veículo, e a lista fica travada (troca-se na aba Veículo).
131. **Mapa desenhado no resultado** a partir dos dados gravados, sem a imagem do fabricante:
    - Usa o eixo de vazão em que o mapa foi digitalizado, com escala automática.
    - Os pontos de potência (círculo) e de torque (quadrado) aparecem com a vazão corrigida pela referência do mapa.
    - As linhas "No mapa" do resultado e do PDF usam o mesmo texto.
132. **Meta pela pressão também no assistente** (etapa Meta e uso). A regra "só sobrealimentado" é conferida com o motor da etapa anterior.
133. **Evidência do G4GC pela meta de pressão** (teste `EvidenciaG4gcTest`, tabela em `calibra-core/target/evidencia-g4gc-meta-pela-pressao.md`):
    - Usa as premissas da pesquisa (seção 4), com a VE 0,90 como chute a confirmar.
    - Resultado: 0,5 e 0,6 bar dão 211 e 224 cv ao nível do mar, e 201 e 214 cv a 600 m. A pesquisa, pela meta de potência, tinha chegado a cerca de 210, 225, 200 e 210–215. O teste confere essa diferença em até 5%.
    - Com a mesma pressão e a mesma VE, o torque estimado é igual nas duas rotações. A diferença real vem da VE no torque e da pressão que o turbo entrega nesse giro.
    - Sem limite de câmbio cadastrado para o A4CF1, o torque × câmbio não entra (não inventar dado técnico).

## 13/10/2026 — Fase 2 (bloco 2.5: orçamento e prazo)

134. **Orçamento no núcleo** (`core/orcamento`):
    - Itens de peça ou serviço, com quantidade exata (ex.: 2,5 h) e valor em centavos. Cada total é arredondado no centavo, e meio centavo sobe.
    - Desconto em valor ou em percentual (nunca os dois), sobre peças + serviços. O desconto não pode passar desse subtotal.
    - Prazo em dias úteis mais uma observação. Validade em dias corridos, que vira uma data a partir do envio.
    - Status: RASCUNHO → ENVIADO → APROVADO ou RECUSADO.
      - Aprovar ou recusar só depois de enviar: quem aprova pessoalmente marca "enviado" antes (gerou o PDF ou o texto).
      - Uma revisão nova (de enviado, aprovado ou recusado) marca a anterior como SUBSTITUIDO.
      - Só o rascunho se edita.
135. **Número ORC-AAAA-NNNN**, com a sequência recomeçando a cada ano. As revisões guardam o mesmo número ("ORC-2026-0007 rev. 2") para o cliente reconhecer o orçamento; a revisão não gasta número.
136. **Banco V5** (o plano dizia V4, mas a V4 virou as sementes do catálogo):
    - Tabela `orcamento` (uma linha por revisão, conteúdo em JSON, total em coluna) e `orcamento_status` (histórico).
    - Cada revisão aponta para o cálculo e para a versão do motor.
    - O próprio banco recusa mudar o conteúdo de quem não é rascunho e recusa apagar orçamento.
137. **Padrões do orçamento** (Configurações, chave `orcamento`): sem desconto, prazo de 10 dias úteis "a partir da aprovação e da chegada das peças", validade de 15 dias, sem condições e sem valor da hora. É política comercial da oficina, editável; o valor da hora começa vazio.
138. **Um número de orçamento por projeto:** o primeiro orçamento se cria; os seguintes são revisões dele. Criar outro com o rascunho aberto ou depois de enviado é recusado, com a mensagem de editar o rascunho ou de abrir revisão.
139. **Itens sugeridos a partir do último cálculo salvo** (que precisa estar em dia com o motor, o núcleo e o catálogo):
    - Entram, nesta ordem: bicos (um por cilindro, e a bancada secundária à parte), bomba, turbo (2 no biturbo), intercooler, sensor MAP e injeção.
    - Cada peça do catálogo leva o nome e o preço de referência da versão usada no cálculo. Peça sem preço vem com o valor em branco, para preencher.
    - Bico e injeção digitados no cálculo entram sem preço.
    - O câmbio (do veículo) e o combustível (do motor) não entram.
    - Os serviços (ex.: mão de obra em horas) a pessoa acrescenta.
140. **Status do projeto com o orçamento:**
    - Marcar como enviado leva o projeto a ORCADO; a aprovação, a APROVADO. Recusa não mexe no projeto, e o projeto nunca volta.
    - Orçamento desatualizado não pode ser enviado: há cálculo mais novo, ou o motor ou uma peça mudou. O rascunho pode passar a usar o cálculo mais novo, e a pessoa confere os itens.
    - Data e hora da resposta do cliente não podem estar no futuro. Com o projeto arquivado, nada muda no orçamento.
141. **PDF e WhatsApp do orçamento** (`calibra-relatorios`):
    - O PDF tem o cabeçalho da oficina com o logo (logo que não abre fica de fora), as tabelas de peças e serviços, os totais, o prazo e a validade, o resumo técnico do cálculo (com o semáforo e as justificativas) e a nota do mapa base conservador.
    - O cliente sai como está no cadastro no momento da geração: anonimizado, se foi anonimizado.
    - A validade conta da data do envio (no rascunho, da data da geração, e o PDF diz "RASCUNHO").
    - O texto do WhatsApp usa o *negrito* e o _itálico_ do WhatsApp e só vai para a área de transferência.
142. **Aba "Peças e orçamento" no projeto**, depois do Dimensionamento:
    - O editor mostra a revisão mais nova. Sem orçamento gravado, mostra a sugestão do último cálculo, que vira o orçamento ao clicar em "Salvar rascunho".
    - Cada item é uma linha (tipo, descrição, quantidade, unidade, valor e total), e os totais se atualizam enquanto se digita.
    - "+ Serviço" já traz a mão de obra com o valor da hora de Configurações. "Trazer as peças do cálculo de novo" refaz as peças e mantém os serviços.
    - Só o rascunho se edita. O resto fica travado, com a explicação do porquê.
    - O que está sendo digitado não se perde ao trocar de aba.
143. **PDF, WhatsApp e envio gravam o rascunho antes:** o que sai é sempre o que está gravado. O texto do WhatsApp vai para a área de transferência, e o app não envia nada.
144. **A resposta do cliente é registrada numa janela:** data com o calendário em português, hora, meio (pessoalmente, WhatsApp ou telefone) e observação. Marcar enviado e aprovar atualizam o status do projeto na hora.
145. **Configurações ganha a aba Orçamento** com os padrões (decisão 137). O PDF da memória e o do orçamento passam a usar o mesmo gerador (`ArquivoPdf`): perguntar onde salvar, gerar em segundo plano e oferecer abrir.

## 14/10/2026 — Fase 2 (bloco 2.6: instalador MSI 0.2.0 e fechamento)

146. **WiX 3.14.1 pelo zip de binários oficial**, extraído em `%USERPROFILE%\tools\wix314`, como o Maven:
    - Não precisa de administrador e não aparece em "Aplicativos instalados".
    - O zip veio do lançamento oficial (GitHub `wixtoolset/wix3`, tag `wix3141rtm`), com o tamanho conferido. SHA-256: `6AC824E1642D6F7277D0ED7EA09411A508F6116BA6FAE0AA5F2C7DAA2FF43D31`.
    - Os `.exe` do zip não têm assinatura digital; só o instalador `.exe` do WiX tem.
147. **`maven-dependency-plugin` 3.11.0** (o mais novo estável da Maven Central, de maio de 2026), só no perfil `instalador` do `calibra-app`. O `mvn verify` do dia a dia não copia biblioteca nenhuma.
148. **Java embutido (jlink), com o nosso código fora dos módulos Java** (substitui a decisão 33):
    - O app e as bibliotecas rodam no classpath. O JavaFX entra no Java embutido como módulos, a partir dos jars `win` da Maven Central.
    - O jdeps acha `java.base`, `java.desktop` e `java.sql`. Somam-se `javafx.controls`, `javafx.fxml` e o `jdk.localedata` só com en, pt e pt-BR (sem ele a vírgula decimal viraria ponto). Com as dependências, são 15 módulos.
    - O JDK 25 da Temurin não traz a pasta `jmods`: o jlink usa o próprio Java instalado como fonte (JEP 493).
149. **Conferência do pacote** (`Inicio --conferir`), no Java embutido, antes de gerar o MSI:
    - Confere a vírgula decimal, o SQLite com as 5 migrações e as sementes, o PDF com fontes e imagem PNG, e o JavaFX.
    - Usa uma pasta temporária e não mexe nos dados.
    - O próprio script confere que o Java embutido não tem `java.net.http` (regra 2.1). Essa conferência fica no script para o código de produção nem citar o módulo de rede.
150. **Instalação por usuário**, sem administrador, em `%LOCALAPPDATA%\Programs\Calibra`:
    - Os dados já são por usuário e ficam separados, em `%LOCALAPPDATA%\Calibra`. Desinstalar não toca neles.
    - Atalhos no menu Iniciar (grupo Calibra) e na área de trabalho.
    - UpgradeCode fixo `45107A4F-BD64-4EDB-815D-3611CD926209`, para instalar por cima sem duplicar o app. Nunca mudar.
    - O jpackage roda com a pasta temporária dele: o lançador que ele gera é somente leitura e travaria o `mvn clean` se ficasse no `target`.
151. **Ícone próprio, gerado por `scripts\gerar-icone.ps1`**: um manômetro sobre o azul do app. Os PNG de 16 a 256 px vão para a janela, e o `.ico` para o instalador e o atalho. Os scripts PowerShell ficam em UTF-8 com BOM, porque o PowerShell 5.1 precisa disso para ler os acentos.
152. **Unidades preferidas na tela ficam para a fase 3**, junto do mapa base (SPEC 8.7, alterada). Até lá, a tela e os PDFs mostram os resultados em várias unidades ao mesmo tempo (ex.: 241,0 kPa · 2,41 bar · 34,95 psia).
153. **Versão 0.2.0** (sai o SNAPSHOT) e a documentação da fase:
    - manual com todas as telas da fase 2, backup, restauração e LGPD;
    - CHANGELOG 0.2.0, README e o comando do instalador no CLAUDE.md;
    - as mudanças da SPEC 6.2, 6.4, 7.1, 7.4, 8.5 e 8.7, cada uma com a nota "alterado em 14/10/2026" e a decisão.

## 15/10/2026 — Fase 2 (bloco 2.6: ensaio do instalador e revisão final)

154. **Os testes manuais 2 e 3 do bloco 2.6 viraram um teste de interface, `FluxoCompletoUiTest`** (tag `ui`), que roda também no app instalado:
    - **O que faz:** pelas telas, numa pasta vazia, configura a oficina, cadastra um bico com preço no catálogo, cria o projeto pelo assistente, calcula (cronometrando cada recálculo), monta o orçamento, envia e aprova pela janela de resposta. Depois faz um backup pela aba Backup, muda os dados (revisão 2 e cor do veículo) e restaura respondendo às três janelas da restauração; confere que os dados voltaram e que há o backup de segurança.
    - **Por quê:** essas janelas só respondem a cliques, e o "teste manual" precisava de evidência repetível. A janela de escolher arquivo do Windows (PDF e logo) não dá para automatizar; o PDF é gravado pelo mesmo código, sem a janela.
    - **No app instalado:** `scripts\testar-instalado.ps1` roda os testes `ui` com o Java embutido e os jars da pasta de instalação, com as opções do atalho (`Calibra.cfg`). O JUnit vem do repositório local do Maven (nada é baixado, nada novo no projeto); o lançador do JUnit é compilado pelo script. Os testes usam pastas temporárias, nunca `%LOCALAPPDATA%\Calibra`.
155. **Janelas de aviso com o texto inteiro (`Alertas`).** O ensaio mostrou que o JavaFX cortava com "..." as mensagens longas: a confirmação da restauração perdia o aviso do que não foi salvo, e o aviso do fim escondia o caminho do backup de segurança. Todas as janelas de aviso do app passam por `Alertas.criar`: largura de 600 px (cabe um caminho de arquivo) e altura do texto. O ensaio confere que nenhum texto das janelas da restauração sai cortado (`Labeled.isTextTruncated`, do JavaFX 23 em diante).
156. **Trocar o câmbio do veículo deixa o cálculo desatualizado** (achado da revisão final). No projeto vale o câmbio do veículo (decisão 130); trocado, tirado ou posto depois do cálculo, o torque × câmbio salvo não vale mais, e um vermelho poderia passar sem justificativa (SPEC 2.4). Motivo: "o câmbio do veículo mudou: A → B" (ou "nenhum"). O orçamento usa os motivos do cálculo, então também não pode ser enviado.
157. **O checksum da V4 ignora os CR (`\r`) do JSON das sementes** (achado da revisão final). Ele era o CRC32 dos bytes do arquivo; com o Git em `autocrlf=true`, uma cópia nova do repositório grava o JSON com CRLF, o número mudaria e o Flyway recusaria abrir os bancos que já têm a V4 (a atualização falharia). Sem os CR, o número é o mesmo do arquivo com LF, que é o que os bancos guardam: -451915068, conferido no banco criado pelo MSI. Um teste fixa esse número; se ele falhar, o arquivo das sementes mudou, e item novo vai numa migração nova.
158. **A limpeza dos backups nunca apaga o backup que acabou de ser feito** (achado da revisão final). A ordem vem da data no nome; com o relógio do PC atrasado (PC sem internet, bateria do relógio fraca), o backup novo parecia o mais antigo dos 31 e era apagado na hora. Todo backup falhava, e com ele a restauração e a atualização do banco. Agora ficam o novo e os 29 mais recentes dos outros.
159. **Falha ao gerar PDF vai para o log** com o detalhe (SPEC 10); a janela mostra a mensagem do erro ou "Erro inesperado. O detalhe foi gravado no log.".
160. **Cálculo na thread da tela, por enquanto** (achado da revisão final, menor). A SPEC 10 pede cálculos em `Task` com indicador de progresso. O cálculo roda direto no clique e leva 46 a 74 ms no app instalado (meta do bloco: menos de 1 s), então a tela não trava. Passar para `Task` mexe em todas as telas de cálculo; fica para a fase 3, junto do mapa base, que deixa o cálculo mais pesado. Também na fase 3: um aviso de progresso na abertura, para o backup e a migração antes da janela numa atualização com muitos anexos (hoje a janela abre em 2 a 3,5 s).
161. **O MSI deixa instalar uma versão mais antiga por cima** (visto no ensaio: o MSI de teste 0.1.99 substituiu o 0.2.0). É o comportamento do jpackage. Os dados ficam protegidos: o app mais antigo não abre um banco atualizado por um mais novo (`BancoMaisNovoException`, decisão 44) e não mexe em nada. O manual explica.

## 15/10/2026 — Novo escopo: gestão da oficina + preparação (SPEC 1.1)

Origem: a comparação com o PitStop Manager, outro projeto do dono (SaaS de gestão de oficinas). O dono quer que o Calibra absorva parte dessa gestão, offline e só para a loja dele. O plano aprovado fica em `C:\Users\mathe\.claude\plans\proud-twirling-blanket.md`.

162. **O Calibra é só da oficina do dono:** gestão do dia a dia e preparação num app só, gratuito, sem assinatura, sem cobrança pelo software e sem venda ou licença para outras oficinas. A antiga fase 6 ("Produto para outras oficinas", com o módulo `calibra-licenca`) sai do roteiro. A loja continua cobrando os clientes dela; o Calibra só registra o pagamento na entrega da OS.
163. **Um computador, sem rede e sem login** (escolha do dono). A arquitetura não muda: SQLite local, backup no pen drive, uso em rede continua fora do escopo. Os mecânicos são um cadastro simples (nome e ativo), para pôr na OS; não há senha nem perfis.
164. **Da gestão, entram a OS com a entrada do veículo (fase 3) e a agenda com os lembretes (fase 5).** Estoque e financeiro (caixa, contas a pagar e a receber) ficam fora por agora; se o dia a dia pedir, voltam como fases novas, com plano próprio.
165. **Roteiro intercalado:** fase 3 Oficina (OS), 4 Mapa base, 5 Agenda e lembretes, 6 Acerto, relatório e biblioteca, 7 Aprendizado e integrações. O que as decisões 152 (unidades preferidas) e 160 (cálculo em segundo plano) deixaram para a "fase 3, junto do mapa base" passa para a fase 4. Os níveis de saída do mapa mudam de fase junto (C e B na 4, A na 7).
166. **A OS é o centro da loja, e o projeto de preparação passa a ser opcional e ligado a ela** (SPEC 6.5):
    - o orçamento passa a pertencer à OS: na de preparação, continua apontando para o cálculo; na comum, não aponta para cálculo nenhum;
    - o status comercial (aguardando aprovação, aprovada, entregue) fica na OS; o técnico (dimensionado, mapa base, em acerto) fica no projeto; ORCADO e APROVADO do projeto acompanham o orçamento da OS;
    - como hoje o orçamento exige projeto, cálculo e versão do motor, a migração V6 refaz a tabela e cria uma OS para cada projeto que já tem orçamento; o detalhe fica no plano do bloco 3.1;
    - um catálogo de serviços (mão de obra padrão: nome, horas e preço) alimenta o orçamento da OS comum.
167. **Do PitStop, adaptado ao offline:**
    - OS com status, histórico, mecânico e km de entrada e saída; histórico por veículo; painel; documentos do veículo (como anexos, com validade opcional);
    - o acompanhamento por link e os avisos automáticos viram texto para o WhatsApp e PDF para imprimir;
    - a nota fiscal (NFS-e) vira um anexo: o PDF da nota emitida no site da prefeitura;
    - ficam de fora: assinatura e planos, Stripe, várias oficinas, login e perfis, metas por mecânico, app de celular, consulta de CNPJ e Detran.

## 15/10/2026 — Fase 3, bloco 3.1: a OS no núcleo e no banco

168. **O app de desenvolvimento usa uma pasta de dados só dele, `%LOCALAPPDATA%\Calibra-dev`** (opção `-Dcalibra.pasta` do `javafx:run` no `calibra-app/pom.xml`). Antes, ele usava a mesma pasta do app instalado: com a migração V6, rodar a versão de desenvolvimento atualizaria o banco de verdade, e o 0.2.0 instalado deixaria de abri-lo ("Instale a versão mais nova", decisão 44). Os dados de verdade ficam com o app instalado até a 0.3.0.
169. **Regras da OS fixadas no bloco 3.1** (SPEC 6.5, alterada):
    - o orçamento pode vir antes do veículo: a entrada é opcional na abertura e obrigatória para começar o serviço;
    - só se executa com orçamento aprovado (Código de Defesa do Consumidor, art. 39, VI);
    - o orçamento move a OS só antes da aprovação; depois, uma revisão nova (o cliente pediu mais coisa) não mexe no status, e o valor da OS é o total da última revisão aprovada;
    - pagamento em partes, com a situação calculada; recebimento errado é cancelado com o motivo, nunca apagado;
    - o km da entrada (e o da saída) vira o uso acumulado do veículo, com histórico; menor que o atual vira aviso e é gravado assim mesmo (troca de painel acontece);
    - o projeto tem uma "OS atual" (a mais nova não entregue nem cancelada); o orçamento da aba do projeto vai para ela, e sem nenhuma uma OS é aberta para o projeto;
    - mecânico na OS é opcional; mecânico inativo não entra em OS nova.
170. **Migração V6 em Java, refazendo a tabela `orcamento`:**
    - **Por quê:** a V5 exige projeto, cálculo e versão do motor no orçamento, e o SQLite não muda coluna.
    - **Como:** o procedimento da documentação do SQLite (tabela nova, cópia, troca de nome, índices e gatilhos de novo). As chaves estrangeiras só desligam fora de transação, então a migração roda fora da transação do Flyway (`canExecuteInTransaction() = false`): desliga as chaves só nessa conexão, abre a própria transação e roda o `PRAGMA foreign_key_check` antes de gravar. Se algo der errado, nada é gravado, e o `Migracoes` ainda volta o backup feito antes.
    - **Tabelas e gatilhos:** `mecanico`, `ordem_servico`, `os_status` e `os_pagamento`. OS e recebimento não se apagam, OS entregue ou cancelada não muda, recebimento só se cancela, uma vez. No orçamento, um CHECK exige projeto, cálculo e versão do motor juntos ou nenhum.
    - **Dados:** cada projeto que já tinha orçamento ganha uma OS, numerada pelo ano e pela ordem do primeiro orçamento, sem entrada, com o serviço "Preparação" e a observação "criada na atualização para a 0.3.0". O status vem da última revisão: rascunho e recusado viram ABERTA; enviado, AGUARDANDO_APROVACAO; aprovado, APROVADA. Ids, números, conteúdo e histórico dos orçamentos não mudam (conferido no `MigracaoV6Test`).
    - **Checksum:** um CRC32 do SQL da migração, sem os CR, para o Flyway acusar uma edição depois de aplicada.
171. **A tabela de anexos fica para a V7, no bloco 3.2**, junto com a tela que a usa: migração aplicada não se edita, e cada bloco cria só o que usa e testa.
172. **PDF e WhatsApp do orçamento da OS comum ficam para o bloco 3.3** (documentos). Até lá, o documento do orçamento continua só para o de preparação, que tem o resumo técnico do cálculo.

## 16/10/2026 — Fase 3, bloco 3.2: as telas da oficina

Plano aprovado em 15/10/2026. Vistoria padrão escolhida pelo dono: lataria e pintura; vidros, faróis e retrovisores; pneus, rodas e estepe; interior e painel.

173. **Anexos da OS e do veículo (migração V7, só a tabela `anexo`):**
    - o veículo é obrigatório e a OS é opcional: o documento do veículo sem OS serve para a ficha do bloco 3.3;
    - aceita foto em JPG ou PNG e documento em PDF, até 20 MB. Foto do iPhone em HEIC é recusada com a dica de mandar pelo WhatsApp, que converte para JPG (o JavaFX não abre HEIC);
    - a cópia vai para `anexos\veiculos\<id do veículo>\<16 primeiros caracteres do SHA-256>.<extensão>`, gravada como arquivo parcial e depois renomeada. O mesmo arquivo anexado duas vezes é uma cópia só, apagada quando nenhum anexo a usa mais. O backup já leva a pasta de anexos;
    - não há gatilho que impeça apagar, porque a anonimização (LGPD, bloco 3.3) precisa apagar documento pessoal mesmo de OS entregue. A regra "remover só com a OS em andamento" fica no caso de uso.
174. **A busca de cliente acha também pela placa e pelo nome da embarcação dos veículos dele**, na Nova OS e no assistente de projeto: no balcão, quem chega é o carro.
175. **Ligar um projeto de preparação à OS:** só projeto do mesmo veículo, não arquivado e que não esteja em outra OS em andamento.
176. **O app abre no Painel, e o menu fica Painel · Oficina · Preparação · Calculadora · Catálogos · Configurações.** "Projetos" virou "Preparação" (SPEC 8.1); a lista de projetos continua igual.
177. **Painel (SPEC 8.8):**
    - um cartão para cada status de OS em andamento. Entra também "Aprovadas, na fila", que a SPEC não listava: a OS aprovada que ainda não começou é serviço esperando a oficina;
    - o cartão abre a Oficina com o filtro daquele status;
    - "Entregas previstas até hoje" traz as de hoje e as atrasadas. "Atrasada" quer dizer que a previsão passou e o serviço não ficou pronto; a OS pronta não conta, porque espera o cliente buscar, não a oficina.
178. **Listas de OS (Painel e Oficina):** o veículo e a placa (ou o nome da embarcação) ficam numa coluna só, e as colunas se ajustam à largura da tabela, sem rolagem para o lado na janela padrão.
179. **Nova OS em 3 etapas: Cliente → Veículo → Serviço e entrada.** As etapas de cliente e veículo são as mesmas do assistente de projeto, separadas em `escolha-cliente.fxml` e `escolha-veiculo.fxml` para as duas telas usarem. O cliente achado pela placa já traz o veículo daquela placa escolhido.
180. **As fotos da entrada são gravadas junto com a OS, tudo ou nada.** Se uma foto não servir, nada é gravado (nem a OS, nem o cliente novo) e as cópias feitas são apagadas: dá para tirar a foto e tentar de novo sem criar OS repetida. Na Nova OS só entram fotos (JPG ou PNG); documento em PDF vai depois, na OS.
181. **Entrada na Nova OS:**
    - vem marcado "o veículo já está aqui", com o dia e a hora de agora; desmarcado, a OS abre sem a entrada (orçamento antes do veículo);
    - a vistoria tem os 4 itens padrão, cada um com a observação; em branco quer dizer nada a anotar, e o item é gravado assim mesmo, para a via do cliente (bloco 3.3) mostrar o que foi conferido;
    - "Abrir a OS" não é o botão do Enter: um Enter no meio dos campos não abre a OS pela metade.
182. **Cadastro rápido do mecânico na Nova OS** ("Novo mecânico…", só o nome), para o campo não ficar sem uso até o bloco 3.3, quando Configurações ganha a lista completa (renomear, ativar e desativar).
183. **A Nova OS sendo preenchida fica guardada:** sair para outra tela e voltar, ou clicar de novo em "Nova OS…", mostra o que já foi digitado. Ela some quando a OS é aberta ou cancelada.
184. **Tela da OS: os passos do serviço ficam no cabeçalho**, não numa aba "Execução" (SPEC 8.9 previa a aba):
    - os botões aparecem conforme o status: Começar o serviço (só com o orçamento aprovado), Aguardando peças… (com quais peças), Retomar o serviço, Serviço pronto, Entregar… e Cancelar OS… (com o motivo);
    - uma linha diz o próximo passo e o que falta (o orçamento aprovado, a entrada do veículo, as peças que a OS espera);
    - as abas são Entrada, Orçamento, Preparação, Entrega e pagamento, Anexos e Histórico.
185. **O editor de orçamento é o mesmo no projeto e na OS** (a "fonte do orçamento" diz de onde ele vem):
    - na OS comum, os itens são digitados e não há "Trazer as peças do cálculo";
    - o PDF e o texto de WhatsApp do orçamento sem cálculo ficam desligados, com o aviso, até o bloco 3.3 (decisão 172);
    - um orçamento gravado sem cálculo continua assim mesmo se um projeto for ligado à OS depois.
186. **Anexos na tela da OS:**
    - foto anexada antes de o serviço começar vira "foto da entrada", e depois, "foto do serviço"; PDF vira "documento", marcado como pessoal (o padrão do tipo). O tipo, a descrição, a validade e a marca de pessoal mudam em "Editar…";
    - as miniaturas carregam em segundo plano pelo próprio JavaFX, já no tamanho da miniatura, em vez da fila de tarefas do app (o plano dizia `TarefasFundo`): a fila faz uma tarefa por vez e é a do backup.
187. **Avisos que não impedem** (km menor que o do cadastro, km de saída menor que o da entrada) aparecem em amarelo embaixo da tela da OS, junto com a confirmação da ação. Na Nova OS continuam numa janela, porque a tela da Nova OS fecha ao abrir a OS.
188. **A garantia da entrega ainda não tem valor padrão:** a janela de entrega pede os dias (0 = sem garantia), com o exemplo "90". O padrão entra em Configurações no bloco 3.3.
189. **O mecânico da OS aparece mesmo que tenha ficado inativo**, marcado "(inativo)": salvar a abertura não o tira da OS.
190. **Projeto de preparação criado pela OS** ("Criar projeto de preparação…", na aba Preparação):
    - o assistente começa no motor, com o cliente e o veículo da OS, e não volta para as etapas de cliente e veículo; cancelar volta para a OS;
    - o projeto nasce ligado à OS numa gravação só: se a OS já tiver projeto (ou não estiver em andamento), nada é gravado, e não sobra projeto solto;
    - criado, o projeto abre na Preparação, para o dimensionamento; o orçamento da OS passa a ser o de preparação, com as peças do cálculo.
191. **O cabeçalho do projeto mostra a OS dele** (a mais nova), num botão que a abre: "Abrir a OS-2026-0003 · Aprovada".

## 17/10/2026 — Fase 3, bloco 3.3: documentos da OS e cadastros da oficina

Plano aprovado em 17/10/2026. Respostas do dono:
- garantia padrão de 90 dias;
- serviço do catálogo com preço próprio entra pelo preço, e sem preço entra horas × valor da hora;
- a via do cliente leva um texto padrão editável acima das assinaturas;
- a oficina imprime em A4 comum.

192. **Padrões das OS em Configurações (aba OS):**
    - a garantia padrão (90 dias), que vem preenchida na entrega;
    - os itens da vistoria, editáveis (vêm os 4 escolhidos em 15/10/2026);
    - o texto acima das assinaturas na via do cliente: um modelo aprovado pelo dono, editável; em branco, ficam só as assinaturas.
    
    Os três são gravados juntos ("Salvar padrões da OS") numa linha nova da tabela de configurações, sem migração. A entrada já registrada guarda os itens de quando foi feita; a lista nova vale para as entradas seguintes.
193. **Mecânicos em Configurações:**
    - novo, renomear, desativar e reativar, valendo na hora, sem "Salvar";
    - renomear corrige o nome também nas OS antigas, porque elas guardam o mecânico, não o nome.
194. **Catálogo de serviços (Catálogos › Serviços):**
    - cada serviço tem nome, horas (o tempo padrão), preço de referência (opcional), fonte (vem "Tabela da oficina") e observações, com versões como as outras abas; sem preço, as horas são obrigatórias. O tipo é texto no banco, então a aba nova não precisou de migração;
    - no orçamento (do projeto e da OS), "+ Serviço do catálogo…" acrescenta a linha: com preço, "1 serviço" pelo preço; sem preço, as horas × o valor da hora de Configurações (sem valor da hora configurado, o valor fica em branco para preencher). A linha guarda o item e a versão do catálogo, e os valores continuam editáveis;
    - a janela de escolha do catálogo passou a mostrar o preço de referência; vale também na escolha do motor e da injeção.
195. **PDF e texto de WhatsApp do orçamento da OS sem projeto:**
    - saem com o cliente e o veículo da OS, os itens, os totais, o prazo e a validade;
    - não levam o motor, a meta, o resumo técnico nem a nota do mapa base, que só valem para a preparação;
    - o aviso "chegam no bloco 3.3" saiu da tela do orçamento.
196. **Documentos da OS (menu Documentos ▾ no cabeçalho da OS):**
    - **via do cliente** (A4): a OS, o cliente e o veículo, o serviço pedido, a previsão e o mecânico, a entrada com a vistoria item a item ("nada anotado" no item sem observação) e os objetos deixados, o texto de Configurações › OS e as linhas de assinatura do cliente e do responsável pela oficina. Não sai da OS cancelada;
    - **comprovante de entrega** (A4, só depois da entrega): a entrega, o km de saída, a garantia com a data, os itens e os totais da última revisão aprovada numa tabela só (as peças antes dos serviços), os recebimentos (o cancelado aparece marcado e não conta), o valor, o recebido, o que falta, a situação e a assinatura de quem recebeu o veículo;
    - **textos para o WhatsApp**, que só copiam: "OS aberta" (com a OS em andamento) e "pronto para retirar" (com a OS pronta), com o contato da oficina no fim. Se o Windows não deixar copiar (outro programa usando a área de transferência naquele instante), a tela avisa em vez de dizer "Texto copiado"; vale também para o texto do orçamento;
    - o cabeçalho da oficina passou a ser o mesmo do orçamento (`TextoOficina`), e a tabela de itens e os totais do orçamento são reaproveitados no comprovante.
197. **Os documentos usam o que está gravado:** com a abertura ou a entrada mudadas na tela e não salvas, a via e o texto "OS aberta" não saem, e a mensagem diz o que salvar (ex.: "a entrada (Salvar a correção da entrada)"). Assim a via que o cliente assina nunca sai sem uma observação da vistoria.
198. **Imprimir** (via e comprovante):
    - primeiro a janela de impressão do Windows, presa à do app; sem impressora instalada, um aviso (com a dica da "Microsoft Print to PDF");
    - depois, em segundo plano, o PDF vai para um arquivo temporário em `cache\tmp` dos dados (apagado no fim) e as páginas são desenhadas a 200 DPI com o desenhista do PDFBox que já estava no projeto, sem dependência nova;
    - cada página vai para a impressora do tamanho da área de impressão da folha escolhida, sem distorcer;
    - na primeira vez o PDFBox procura as fontes do Windows (alguns segundos); a mensagem avisa, e o cache dele já fica na pasta `cache` dos dados desde a fase 2.
199. **Ficha do veículo** (área Oficina; abre pela OS e pelo cabeçalho do projeto):
    - o cliente, com o contato e o consentimento, e Editar, Histórico e Anonimizar;
    - o veículo, com Editar e Histórico. Antes, o veículo de uma OS sem projeto não tinha onde ser editado;
    - as OS do veículo (todas) e os projetos de preparação (também os arquivados), que abrem com duplo clique;
    - os anexos de todas as OS do veículo e os do próprio veículo. "Adicionar documento do veículo…" anexa sem OS, como documento pessoal (ex.: o CRLV, com a validade em Editar). O anexo de uma OS se remove na OS;
    - o botão de voltar leva para onde a ficha foi aberta (a OS ou o projeto).
200. **Componentes reaproveitados:** a lista de anexos virou um componente (`anexos.fxml` + `AnexosController`), usado na aba Anexos da OS e na ficha. As janelas de editar e de ver o histórico do cliente e do veículo, e a confirmação da anonimização, ficam em `JanelasCadastro`, usadas pelo projeto e pela ficha.
201. **LGPD dos anexos:** anonimizar o cliente (pela ficha ou pelo projeto) apaga, na mesma gravação, os anexos marcados como documento pessoal: os das OS dele e os dos veículos dele que não são de OS. O arquivo sai da pasta só se nenhum outro anexo usar o mesmo. Fotos, laudos, notas, OS, projetos e cálculos ficam, e a confirmação diz que não tem volta. A anonimização passou de `CasosProjeto` para `CasosOficina`.

## 18/10/2026 — Fase 3, bloco 3.4: revisão final, versão 0.3.0 e ensaio do instalador

Plano aprovado em 18/10/2026. Resposta do dono: antes de ele abrir o 0.3.0 com os dados de verdade, a atualização do banco (V5 → V7) é ensaiada numa cópia deles, só com números (sem nome de cliente), e a cópia é apagada no fim.

202. **Versão 0.3.0 e a conferência do pacote:**
    - os 6 `pom.xml` passam para 0.3.0; o CHANGELOG fecha a 0.3.0, e o README e o manual dizem o que a versão faz;
    - a conferência do instalador (`Inicio --conferir`) ganha o 5º item, "Páginas para imprimir": desenha a página A4 do PDF de exemplo no Java embutido, com o cache de fontes do PDFBox na pasta `cache`. Confere que o jlink não deixou de fora o que a impressão usa (a janela de impressão do Windows não dá para automatizar);
    - o manual ganha "Atualizar do 0.2.0 para o 0.3.0": o backup "antes de atualizar o banco", os orçamentos que viram OS, o 0.2.0 que não abre mais os dados e como voltar se precisar.
203. **Revisão final (bloco 3.4):** o agente de revisão parou logo no começo, por limite de uso da conta. Para o bloco não parar, eu revisei os pontos de maior risco:
    - as migrações V6 e V7: chaves estrangeiras desligadas fora da transação, conferência antes de gravar e o status das OS criadas;
    - backup e restauração com os anexos, que não mudaram na fase 3;
    - a anonimização e os arquivos, as regras de pagamento e de status;
    - a regra 2.1: nenhum uso de rede, serial ou USB no código;
    - o código novo do bloco 3.3.

    Os achados:
    - **corrigido:** o km (ou as horas) gravado com duas casas aparece com uma na aba Entrada (1.250,25 vira 1.250,3), e a trava dos documentos (decisão 197) via mudança onde não havia: a via ficava presa até "salvar" de novo, o que ainda trocaria o valor gravado. A comparação passou a usar o mesmo texto que a tela mostra. Teste de interface novo;
    - **teste que faltava:** a anonimização apaga o documento pessoal também da OS entregue (o banco não tem gatilho contra, de propósito, e a tela não deixa remover). Teste novo;
    - **fica documentado** (manual 6.6): os backups feitos antes da anonimização ainda têm os dados e os documentos do cliente, até saírem da lista dos 30 mais recentes (e para sempre nos copiados para pen drive ou HD); os textos livres da OS não mudam na anonimização. Apagar backup ou mexer nesses textos é decisão do dono, para uma fase seguinte.
204. **Ensaio do instalador 0.3.0** (resultado em `docs/fase3/ensaio-instalador.md`):
    - o MSI instalou por cima do 0.2.0 sem pedir clique (`/passive`), com uma entrada só em "Aplicativos instalados";
    - o app abre em 2,9 a 3,0 s; a primeira abertura depois de instalar, com banco novo e disco "frio", levou 5,9 s, como na fase 2;
    - os 18 testes de tela passaram no app instalado;
    - a atualização do banco foi ensaiada numa cópia dos dados reais, com o OK do dono: o backup "antes de atualizar o banco", as migrações V6 e V7 e a conferência do SQLite sem erro, com só números anotados e a cópia apagada no fim;
    - os dados reais ficaram com o mesmo SHA-256 e os mesmos 68 arquivos.

### Revisão pelo agente (18/10/2026, pedida pelo dono depois do limite de uso)

205. **O que a revisão achou e o que foi feito:** o agente (só leitura) revisou o diff da fase 3 e apontou 15 achados. Conferi cada um no código: 10 foram corrigidos agora, cada um com teste (decisões 206 a 210); 4 dependem de escolha do dono e ficaram para ele (decisão 211); 1 fica só documentado (decisão 212). A versão continua 0.3.0: o MSI foi refeito antes de o dono abrir o app com os dados reais.
206. **Arquivos dos anexos e backup:**
    - o backup que encontra um anexo removido no meio da cópia (a tela segue aberta durante o backup automático) deixa esse anexo de fora, em vez de falhar inteiro; o manifesto conta só o que foi junto;
    - a cópia pela metade de um anexo (`.parcial`, de uma gravação em andamento ou de uma queda de energia) não vai no backup;
    - o arquivo que não sai da pasta na hora (ex.: um PDF aberto no leitor ao anonimizar o cliente) antes só ia para o log e ficava para sempre. Agora a anonimização conta quantos ficaram e avisa na tela, e o app, ao abrir, antes da tela, apaga as cópias sem uso. Para não apagar o que não deve, a limpeza só mexe em `anexos\veiculos\`, só nos arquivos com o nome que o próprio Calibra dá (os 16 primeiros caracteres do SHA-256 + jpg, png ou pdf, ou a cópia `.parcial`), que nenhum anexo do banco usa e que foram criados (e mexidos) há mais de uma hora. Um erro na limpeza só vai para o log: o app abre do mesmo jeito.
207. **LGPD depois da anonimização:**
    - a placa, o chassi e a identificação da embarcação não voltam pela edição do veículo: o banco recusa e a mensagem pede os campos em branco. Os outros campos do veículo continuam editáveis;
    - o orçamento pelo projeto não abre OS nova para cliente anonimizado (a abertura pela Nova OS já recusava); o projeto e os cálculos ficam para consulta;
    - documento pessoal novo não se anexa (na OS ou no veículo) de cliente anonimizado; anexo sem a marca de pessoal, sim. Se a pessoa voltar à oficina, ela é cadastrada de novo, com consentimento novo.
208. **Regras da OS e dinheiro:**
    - a garantia vai até 3650 dias (10 anos), na entrega e no padrão de Configurações: depois da entrega a OS não muda, e um 9000 no lugar de 90 ficaria gravado para sempre;
    - o recebido a mais (ex.: um recebimento lançado duas vezes) aparece na aba Entrega e pagamento, no comprovante e no texto "pronto para retirar", em vez de sumir atrás do "Pago";
    - corrigir a entrada com o mesmo km (ex.: só a vistoria ou o tanque) não regrava o km no veículo, que pode ter mudado depois por outra OS ou pelo cadastro. A aba Entrada também devolve o km gravado exato (1.250,25) quando o campo não foi mexido, em vez do 1.250,3 que a tela mostra.
209. **O orçamento do projeto e a segunda etapa da preparação:** a aba Peças e orçamento juntava as revisões de todas as OS do projeto. Depois da entrega da primeira OS, ela ficava presa no orçamento antigo, sem como começar o da etapa seguinte. Agora:
    - ela mostra só o orçamento da OS em andamento do projeto; sem OS em andamento, ela fica vazia, e o orçamento novo abre outra OS (regra 7 do bloco 3.1);
    - o orçamento antigo continua na OS dele (aba Orçamento da OS e ficha do veículo);
    - na OS entregue ou cancelada, o editor do orçamento fica só para ver, com o motivo na tela, em vez de deixar clicar e dar erro. O PDF e o texto continuam;
    - a conta do "desatualizado" ficou num lugar só (`comMotivos`), usada pelo projeto, pela OS e pelo envio.
210. **Telas:**
    - Entregar e Cancelar OS pedem para salvar antes o que mudou na aba Entrada (a mudança se perderia: depois, a OS não muda mais). Com a OS entregue ou cancelada, a trava da decisão 197 não vale mais: não há o que salvar, e a via não fica presa;
    - a Nova OS sendo preenchida fica guardada também ao voltar para a lista da Oficina, ao abrir outra OS ou a ficha de um veículo. Ela só some quando a OS é aberta ou cancelada, como a decisão 183 já dizia.
211. **Ficam para o dono decidir** (não mexi):
    - **o tamanho dos backups:** cada backup leva todos os anexos, e ficam 30. Com fotos de 3 a 6 MB, em alguns meses cada backup pode ter centenas de MB, e a pasta, dezenas de GB (pior num pen drive). Proposta: os anexos irem para o backup só uma vez (cópia por conteúdo, como já são guardados), com a restauração montando tudo de novo;
    - **anexar fotos grandes trava a tela por um instante:** a cópia e o SHA-256 rodam na thread da tela. Com 20 MB num disco lento, a janela "congela" por alguns segundos. Proposta: passar para segundo plano, junto com a fase 4;
    - **o histórico das correções da OS:** a abertura e a entrada corrigidas não guardam o valor antigo (o veículo e o cliente guardam). Proposta: um histórico como o do veículo;
    - **o nome do arquivo e a descrição dos anexos que ficam** (fotos, laudos) não mudam na anonimização: uma foto chamada "CNH do João.jpg" e não marcada como documento pessoal continua com esse nome no banco. Proposta: trocar o nome original por "arquivo anonimizado" na anonimização.
212. **Só documentado:** na atualização para a 0.3.0 (migração V6), a OS criada para um projeto que já tinha orçamento pega o status da última revisão. Se o cliente tinha aprovado a revisão 1 e havia uma revisão 2 em rascunho, a OS nasce "Aberta", e não "Aprovada": é preciso enviar e aprovar a revisão 2 antes de começar o serviço. Não afeta os dados deste PC (o banco real não tem orçamento), e mexer numa migração já publicada no MSI (o Flyway confere a impressão digital dela) não vale o risco.

## 19/10/2026 — Fase 3B: auto elétrica (SPEC 1.2)

Proposta do dono de 18/10/2026 (SPEC 1.2), com o plano aprovado em 19/10/2026. As decisões da proposta eram numeradas de 202 a 214; viraram 213 a 225 porque até a 212 já estavam usadas.

213. **A auto elétrica entra como segunda linha de serviço da oficina, na mesma OS.** É o serviço que mais aparece hoje; a OS já resolve entrada, orçamento, execução e entrega.
214. **A Fase 3B (auto elétrica) entra antes da Fase 4 (mapa base).** Há demanda imediata de elétrica; o mapa base não é afetado. A Fase 8 (chicote e caixa de fusíveis) fica depois da 7 e pode ser antecipada.
215. **Categoria nos serviços do catálogo; os já cadastrados viram OUTRO.** Organiza o catálogo sem apagar dados. A proposta dizia "PREPARACAO ou OUTRO", sem regra para escolher entre os dois: ficou OUTRO, e a categoria se troca na edição (o banco real não tinha serviço cadastrado).
216. **Serviços sugeridos vêm sem horas e sem preço; o dono preenche antes de confirmar.** A regra do catálogo exige horas ou preço, e o app não inventa tempo de serviço.
217. **O checklist de execução é uma cópia (instantâneo) do modelo, criada ao entrar em EM_EXECUCAO.** Mudar o modelo não pode reescrever OS em andamento ou entregues.
218. **Item obrigatório pendente trava PRONTA; o avanço só com justificativa escrita, com data e hora.** Mesmo padrão do semáforo vermelho (regra 2.4).
219. **Laudo elétrico sem faixas "boas" ou "ruins" prontas; a faixa é campo opcional preenchido pelo dono.** Regra "não invente dado técnico". O laudo é dado técnico e fica na anonimização; a queixa, que é texto livre, tem aviso para não levar dado pessoal.
220. **Itens e kits só compõem o orçamento; sem estoque.** Estoque e financeiro continuam fora do escopo.
221. **Injeção programável vira categoria própria; confecção de chicote e instalação são dois serviços independentes.** O cliente pode contratar só um ou os dois.
222. **Cada serviço diz se exige a entrada do veículo; a OS com serviço de bancada pode ir à execução sem a entrada.** O chicote é feito na bancada e pode ser entregue sem o carro ter passado pela oficina; nesse caso, a entrega registra quem retirou, no lugar do km de saída.
223. **Kit pode conter serviços (pacote); no orçamento, ele se expande em linhas editáveis.** Vender "chicote + instalação" de uma vez sem prender o cliente ao combo.
224. **Ficha do chicote com a fonte do pinout obrigatória; o app não valida pinagem nem bitola.** Regra "não invente dado técnico" (seção 0).
225. **Linha de item pode ser "fornecido pelo cliente" (valor zero); garantia opcional por serviço.** O cliente que já tem a ECU; chicote e instalação podem ter garantias diferentes.
226. **A ordem da Fase 3B e o que mudou da proposta** (plano aprovado em 19/10/2026):
    - **bloco 3B.0, antes da elétrica,** com os itens 1 e 2 da decisão 211: o backup que guarda cada anexo uma vez e a cópia de fotos em segundo plano. Escolha do dono em 19/10/2026: com o uso real, cada backup levaria todas as fotos de novo, e a pasta de backups passaria de 10 GB em pouco tempo. Os itens 3 e 4 da decisão 211 entram no 3B.7, antes do MSI 0.3.1;
    - **as 6 perguntas da proposta,** respondidas no plano olhando o código:
      - itens e kits viram tipos do catálogo que já existe, sem tabela nova;
      - os checklists têm tabelas próprias, porque a vistoria é só uma lista de nomes; o editor dela serve de modelo para a tela;
      - o orçamento já aceita linhas digitadas e guarda de que item do catálogo cada linha veio;
      - a regra "EM_EXECUCAO exige a entrada" mora num lugar só (`CasosOficina.mudarStatus`);
      - o km de saída já é opcional, e "quem retirou" é um campo novo da entrega (3B.6);
      - a garantia por serviço cabe no comprovante, numa tabela "Por serviço" (3B.1);
    - **a "descrição padrão" do serviço é o texto da linha do orçamento.** A via do cliente não lista o orçamento, porque é assinada na chegada;
    - **o vínculo do serviço com o checklist modelo entra no 3B.3,** junto com os modelos;
    - **o texto da SPEC usa "oficina" onde a proposta dizia "escritório",** como no resto da SPEC e do app.

### Bloco 3B.0: backup e cópia de anexos (19/10/2026)

227. **Backup formato 2: cada anexo guardado uma vez** (item 1 da decisão 211):
    - o zip leva o banco e o manifesto; o manifesto lista cada anexo (caminho, SHA-256 e tamanho), e o conteúdo fica na pasta `anexos-dos-backups`, ao lado dos zips, com o SHA-256 como nome. Uma foto que não muda é guardada uma vez, não 30;
    - os anexos são guardados **antes** da cópia do banco: um anexo removido no meio do backup fica de fora, e o banco, copiado depois, também não o tem. Um removido depois de guardado só sobra no backup, sem uso. A cópia de um anexo novo calcula o SHA-256 enquanto copia e grava com `.parcial`, renomeando no fim;
    - a restauração do formato 2 confere primeiro se todos os arquivos estão lá (sem copiar nada); depois copia cada um e confere o SHA-256. Arquivo faltando, arquivo com outro conteúdo, caminho que sai da pasta de anexos ou nome que não é um SHA-256 recusam a restauração sem mexer em nada. A conferência sem restaurar confere os arquivos na própria pasta;
    - o formato 1 (zip com os anexos dentro, até a 0.3.0) continua restaurando como antes; o 0.3.0 recusa o formato 2 com a mensagem de "versão mais nova", que já existia;
    - a limpeza: depois de ficar com os 30 mais recentes, sai da `anexos-dos-backups` o que nenhum backup restante usa (e as cópias `.parcial`). Se algum backup da pasta não puder ser lido, a limpeza espera o próximo backup, porque ele pode usar um desses arquivos. Erro na limpeza só vai para o log: o backup já está feito;
    - cada backup ainda lê todos os anexos para conferir o SHA-256 (o formato 1 lia e gravava tudo de novo). Se com o tempo isso pesar no backup ao fechar, dá para guardar o SHA-256 de cada arquivo entre um backup e outro;
    - a aba Backup mostra quanto a `anexos-dos-backups` ocupa. Para levar os backups para outro lugar, copia-se a pasta inteira (manual, item 10).
228. **Cópia de fotos e documentos em segundo plano** (item 2 da decisão 211):
    - anexar na aba Anexos da OS ou na ficha do veículo, e abrir a Nova OS com as fotos, rodam na fila de segundo plano do app (`TarefasFundo`), a mesma do backup e da restauração. Assim a cópia nunca roda junto com uma restauração, que troca a pasta de anexos inteira;
    - o formulário e o tipo de cada anexo são lidos na thread da tela; só o trabalho de copiar e gravar vai para a fila;
    - durante a cópia, o botão fica desligado; a janela de espera só aparece se passar de meio segundo, para não piscar na tela numa cópia rápida;
    - a cópia de um anexo lê a foto uma vez (o SHA-256 sai durante a cópia, com um nome provisório `.parcial` até o fim), e não duas;
    - nos testes de tela, os passos que copiam esperam a fila terminar; um teste novo segura a fila (como um backup comprido faria) e confere que a tela responde e que a foto de 5 MB entra quando a fila chega nela.

### Bloco 3B.1: categorias e catálogo de serviços (19/10/2026)

Plano aprovado em 19/10/2026, em 4 passos.

229. **Campos novos do serviço do catálogo** (SPEC 6.6.1):
    - **categoria:** obrigatória no serviço novo (a escolha começa vazia);
    - **descrição no orçamento:** numa linha só, porque a linha do orçamento é de uma linha; em branco, entra o nome;
    - **garantia própria:** de 0 a 3650 dias; em branco, vale a da OS; 0 quer dizer sem garantia para aquele serviço;
    - **"exige o veículo na oficina":** vem marcado no serviço novo. Neste bloco ele só é gravado e mostrado; a regra na OS vem no 3B.6;
    - **sem migração:** os campos ficam no JSON do serviço. O serviço gravado pelo 0.3.0 é lido como Outro, exigindo o veículo, sem descrição e com a garantia da OS, e nada é regravado. O teste nº 1 da fase lê o JSON gravado pelo código do 0.3.0 (arquivo de teste `servico-0.3.0.json`) e confere que abrir e salvar sem mudar não cria versão nova;
    - **colunas:** a lista da aba Serviços e a janela de escolha do orçamento mostram Categoria, Horas e Garantia. Para ordenar por categoria, clica-se no título da coluna.
230. **Serviços sugeridos…** (botão na aba Serviços, ao lado de "Cadastrar…"):
    - **a lista:** os 24 da SPEC, agrupados por categoria; cada linha tem a caixa de marcar, as horas e o preço, e digitar já marca a linha;
    - **conferência:** cada linha marcada precisa de horas ou preço (decisão 216), com horas maiores que zero e preço que não seja negativo. Os erros aparecem todos juntos, e a janela não fecha;
    - **gravação:** tudo numa gravação só. Se um nome já existir no catálogo (sem diferença de acento, maiúscula ou pontuação, também entre os arquivados), nada é gravado; a janela já mostra esses como "já cadastrado";
    - **o que cada um leva:** a fonte "Tabela da oficina", a categoria da lista, sem descrição, sem data e com a garantia da OS. Só a confecção de chicote dispensa o veículo. O pacote "chicote + instalação" é um kit e fica para o 3B.2.
231. **A garantia de cada serviço:**
    - **de onde vem:** da versão do serviço que está na revisão aprovada (a linha do orçamento guarda o item e a versão). Sem garantia própria, e na linha digitada, vale a da OS. Mudar o serviço no catálogo depois não muda a OS. Como a revisão aprovada e as versões do catálogo não mudam, não é preciso guardar uma cópia: o comprovante tirado de novo depois sai igual;
    - **onde aparece:** a janela de entrega avisa quais serviços têm a garantia própria. A aba Entrega e pagamento e o comprovante mostram, embaixo da garantia da OS, a linha "Garantia própria", com cada serviço e o dia em que a garantia dele vence;
    - **o que não muda:** a data da garantia guardada na OS (`garantia_ate`) continua sendo a da OS. Os lembretes da fase 5 decidem se precisam olhar serviço por serviço.
232. **Mudança do plano no comprovante:**
    - o plano previa uma seção "Por serviço", com uma tabela de todos os serviços (serviço, garantia e vencimento). No PDF de exemplo, numa OS de três serviços, a tabela empurrou a assinatura de quem recebe o veículo para uma segunda página;
    - ficou a linha "Garantia própria", só com os serviços que têm a própria. O comprovante de quem não usa garantia própria fica igual ao do 0.3.0, e o de três serviços cabe numa página, com a assinatura (teste novo);
    - o resumo do checklist por serviço (3B.3) terá o próprio desenho, no plano daquele bloco.

### Bloco 3B.2: itens, kits e pacotes no orçamento (19/10/2026)

Plano aprovado em 19/10/2026, em 5 passos.

233. **Itens** (aba Itens dos Catálogos, SPEC 6.6.4):
    - **campos:** o nome, a unidade (obrigatória; o item novo vem com "un") e o preço de referência, comum a todo item do catálogo. Sem saldo nem estoque (decisão 220);
    - **a fonte vem preenchida** com "Tabela da oficina", como nos serviços. A SPEC dizia que ela é opcional no item, mas todo item do catálogo tem fonte (regra 2.7); vindo preenchida, não dá trabalho;
    - **sem migração:** os itens são um tipo novo do catálogo que já existe (`ITEM`, com os dados em JSON), com versões, arquivar, busca e a janela de escolha.
234. **Kits** (aba Kits):
    - **linhas:** cada linha é um item ou um serviço do catálogo, com a quantidade. O kit com serviço é um pacote (ex.: "chicote + instalação"). As linhas se escolhem dentro da janela do kit ("+ Item do catálogo…" e "+ Serviço do catálogo…"), e o formulário do catálogo ganhou esse tipo de campo (`EditorLinhasKit`);
    - **regras ao gravar:** pelo menos uma linha, quantidade maior que zero e o mesmo item uma vez só (some as quantidades). Cada linha precisa apontar para um item ou serviço que exista no catálogo; o serviço não vale como item, nem o item como serviço;
    - **sem preço próprio:** o valor sai das linhas, no orçamento. A janela do kit não tem o campo do preço, e a lista não tem a coluna. A lista mostra as linhas ("2 itens, 1 serviço") e se é pacote;
    - **o nome de cada linha** é guardado só para mostrar (nas versões, "2 × Relé 12 V 40 A (item)"). Ao abrir e ao gravar o kit, ele vem do catálogo como está agora, e o item arquivado aparece marcado.
235. **O kit no orçamento** ("+ Kit do catálogo…"):
    - **expande:** vira uma linha para cada item e serviço do kit, com a versão de agora de cada um e a quantidade do kit. O item entra como peça (quantidade × preço de referência; sem preço, o valor fica em branco). O serviço entra pela regra dele (preço próprio, ou horas × o valor da hora), vezes a quantidade (2 × um serviço de 0,5 h = 1 h);
    - **linhas soltas:** depois, cada linha se muda ou se apaga, e o kit no catálogo não muda (testes nº 5 e nº 11 da Fase 3B). A linha guarda o item e a versão do catálogo, então o serviço que entra por um kit tem a garantia própria do 3B.1;
    - **arquivado:** o item ou serviço arquivado no catálogo entra mesmo assim, com um aviso em amarelo para conferir;
    - **"+ Item do catálogo…"** põe uma linha de peça: 1 × a unidade do item, pelo preço de referência.
236. **Peça fornecida pelo cliente:**
    - **no núcleo:** só linha de peça; o valor fica sempre zero e não soma no total (teste nº 12 da Fase 3B). Os orçamentos gravados antes da Fase 3B são lidos como "não fornecido", sem migração (teste com o JSON de um orçamento do 0.3.0);
    - **na tela:** a caixa "do cliente" em cada linha de peça. Marcada, o valor vai a 0,00 e trava; desmarcada, volta o valor de antes;
    - **nos documentos:** no PDF do orçamento e no comprovante de entrega, a linha sai com "(fornecido pelo cliente)" na descrição e R$ 0,00; no texto de WhatsApp, sem conta: "• ECU … — 1 un, fornecido pelo cliente".
237. **"Trazer as peças do cálculo de novo"** (orçamento de preparação) passa a manter as linhas do catálogo de Itens (cabos, relés...), que não saem do cálculo, além dos serviços. As peças digitadas continuam sendo trocadas pelas do cálculo, como antes.

### Bloco 3B.3: checklists, aba Execução e a trava do pronto (19/10/2026)

Plano aprovado em 19/10/2026, em 6 passos. Duas respostas do dono no plano: os modelos ficam nos Catálogos, e os itens dos 7 modelos iniciais começam todos obrigatórios.

238. **Os modelos de checklist ficam em Catálogos › Checklists** (resposta do dono), e não em Configurações › Checklists, como a SPEC 6.6.2 dizia:
    - **por quê:** nos Catálogos, o modelo ganha de graça o que todo item tem: lista, busca, versões (mudar o modelo grava a v2, e as cópias já feitas nas OS não mudam), duplicar e arquivar;
    - **o modelo:** o nome e os itens, na ordem; cada item tem o texto e as marcas "obrigatório" e "a verificar". Pelo menos um item, sem texto repetido (sem diferença de maiúscula); o item sem texto fica de fora. Sem preço. O editor dos itens tem "+ Item" (o item novo entra obrigatório e sem o "a verificar": quem escreve é o dono), subir, descer e remover;
    - **no serviço:** o campo "Checklist" é a escolha de um modelo ativo, ou "(nenhum)". O já escolhido aparece mesmo arquivado, marcado. O formulário do catálogo ganhou esse tipo de campo (a referência a um item de outro catálogo, gravada pelo id); nas versões do serviço aparece o nome do modelo como está no catálogo. Gravar um serviço com um checklist que não existe é recusado; o arquivado continua valendo para quem já apontava para ele.
239. **Os 7 modelos iniciais** (migração V9, arquivo `sementes/checklists.json`):
    - **os textos são os da SPEC 6.6.2,** na ordem dela, todos "a verificar" (o item e o modelo) e, como o dono escolheu, todos obrigatórios. Ao conferir, ele desmarca o que for opcional;
    - **duas adaptações:** onde a SPEC diz "campo para anotar" (e "anotar a fonte"), o texto pede "anote na observação", porque todo item da OS já tem a observação; o "(a verificar)" de um item virou a marca "a verificar" dele. O resto ficou igual, inclusive "Primeira partida conforme o checklist da seção 7.5", que aponta para a SPEC: o checklist de primeira partida entra na fase 4;
    - **a chave de cada modelo** (ex.: `checklist:som`) é a que os serviços sugeridos usam: o sugerido entra com o modelo da categoria dele (na injeção programável, só a confecção de chicote e a instalação; a revisão do chicote e o diagnóstico de injeção ficam sem, para o dono escolher). O modelo arquivado não é usado pelos sugeridos.
240. **A cópia (instantâneo) na OS:**
    - **quando:** ao entrar em execução. Voltar a "em execução" (depois de aguardar peças ou do pronto) só cria as cópias que faltam;
    - **de quê:** de cada serviço do catálogo da última revisão aprovada, uma cópia por serviço. A linha digitada não tem checklist;
    - **qual modelo:** o que o serviço tem no catálogo na hora em que a OS entra em execução, na versão de agora do modelo (o modelo arquivado também é copiado: o serviço ainda aponta para ele). É diferente da garantia (decisão 231), que vale como estava no orçamento: a garantia é um compromisso com o cliente; o checklist é o procedimento da oficina na hora do serviço. Assim, ligar um checklist a um serviço vale para a próxima OS que começar, mesmo com o orçamento aprovado antes;
    - **o que a cópia guarda:** o serviço e o nome dele, o modelo, a versão e o nome dele, e cada item com o texto, "obrigatório" e "a verificar". O banco recusa mudar esse texto e apagar a cópia (teste nº 2 da fase).
241. **Revisão aprovada no meio do serviço:**
    - com a OS em execução, aguardando peças ou pronta, aprovar uma revisão nova dá a cópia aos serviços novos na hora;
    - a cópia do serviço que saiu do orçamento fica guardada e aparece na aba Execução, marcada; ela não trava o pronto e não sai no comprovante.
242. **A marcação** (aba Execução):
    - **cada item:** pendente, feito ou não se aplica; a observação; e, no não se aplica, a justificativa, obrigatória (teste nº 4). O banco também recusa o "não se aplica" sem justificativa;
    - **quem e quando:** "Quem está marcando" vem com o mecânico da OS, se ele estiver ativo; pode ficar vazio (oficina sem mecânico cadastrado), e aí grava só a hora. "Salvar marcações" grava só os itens que mudaram: o que já estava assim fica com quem marcou antes. Com um erro, nada é gravado;
    - **desfazer:** dá para voltar um item a pendente até a entrega. Fica a última marcação de cada item, com quem e quando (sem o histórico de cada mudança);
    - **depois da entrega ou do cancelamento,** a aba fica só para ver, e o banco recusa mudar a marcação e criar cópia (teste nº 6). O pagamento e os anexos continuam.
243. **A trava do pronto:**
    - **o que trava:** item obrigatório pendente (ou "não se aplica" sem justificativa) das cópias dos serviços que estão na última revisão aprovada;
    - **na tela:** "Serviço pronto" abre a janela com a lista do que falta ("serviço: item") e o campo da justificativa; sem ela, a janela não fecha. O caso de uso também recusa o pronto sem justificativa, com a lista;
    - **onde fica:** no histórico de status da OS, na linha do pronto, com a data e a hora: "Pronta com itens obrigatórios do checklist pendentes: … Justificativa: …" (o mesmo padrão da justificativa do semáforo vermelho). Sem pendência, o pronto segue como antes;
    - **salve antes:** com marcação mudada e não salva, o pronto, a entrega e o cancelamento pedem para salvar antes, como a aba Entrada faz na entrega e no cancelamento. A via do cliente não usa o checklist e não é segurada por ele.
244. **No comprovante de entrega:** a linha "Checklist", na seção "Serviço feito", com o resumo de cada serviço ("9 de 9 itens feitos", "5 de 6 itens feitos, 1 não se aplica", "3 de 9 itens feitos, 6 pendentes"). O pendente aparece como pendente. Uma linha só, como a da garantia própria (decisão 232): a OS de três serviços continua numa página, com a assinatura. O resumo fica no núcleo (`ResumoChecklist`), e a aba Execução mostra o mesmo texto em cada quadro.
245. **O banco** (migrações V8 e V9; banco na versão 9):
    - **V8 (SQL):** as tabelas `checklist_os` (uma cópia por serviço na OS) e `checklist_os_item` (os itens, com a marcação, o mecânico e a hora) e os gatilhos: o texto da cópia não muda, nada se apaga, e a OS entregue ou cancelada não ganha cópia nem muda marcação;
    - **V9 (Java):** os 7 modelos, lidos do JSON, como a V4 faz com o catálogo. O arquivo da V4 não mudou (a impressão digital dela é a mesma), e a da V9 ficou fixa num teste;
    - **ordem dos passos:** no plano, o tipo "Checklist" do catálogo entrava no passo do banco. Um teste do app exige que todo tipo de catálogo tenha formulário, então o tipo e os 7 modelos entraram no passo da tela, junto com o formulário; o passo do banco ficou só com as tabelas da OS;
    - **as duas só acrescentam:** nada do que existe muda. O teste sobe um banco na V7, com uma OS, até a V9; no 3B.7, o ensaio do MSI repete isso numa cópia dos dados reais.

### Bloco 3B.4: laudo de diagnóstico elétrico (20/10/2026)

Plano aprovado em 20/10/2026, em 6 passos. Três respostas do dono no plano: o PDF é feito na hora, as medições são texto livre e o aviso de LGPD só aparece quando a queixa parece ter dado pessoal.

246. **O PDF do laudo é feito na hora** (resposta do dono), como a via do cliente e o comprovante, sem cópia nos anexos. A SPEC 6.6.3 dizia "via do cliente e anexo da OS":
    - **por quê:** o laudo muda até a entrega, e uma cópia nos anexos ficaria velha a cada mudança. O PDF tirado de novo sai do que está gravado. Quem quiser guardar a folha assinada anexa na aba Anexos, como qualquer PDF;
    - **onde sai:**
      - na área de laudos da aba Execução, com o laudo escolhido na lista;
      - no menu Documentos da OS, em "Laudo elétrico: gerar PDF…" e "Laudo elétrico: imprimir…". Os dois ficam desligados sem laudo; com mais de um, o app pergunta qual, com o mais novo já marcado;
      - o arquivo se chama, por exemplo, `OS-2026-0003-laudo-1.pdf`, com a posição do laudo na OS;
    - **o que tem:**
      - o título e a OS, com a data e a hora do laudo;
      - o cliente, como está no cadastro na hora do PDF (anonimizado, se for o caso), o veículo e o mecânico;
      - a queixa e a tabela das medições, com a referência e "(a verificar)" quando ela ainda não foi conferida;
      - a tabela dos códigos de falha;
      - os textos do diagnóstico e o resultado ("a definir" sem ele);
      - as assinaturas do mecânico responsável e do cliente.

      A tabela e o texto em branco ficam de fora. O laudo comum cabe numa página (teste).
247. **As medições em texto livre** (resposta do dono):
    - **o valor, a unidade e a referência** são texto, como se escreve no papel ("12,1", "sem sinal", "12,4 a 12,7 V"). O app não lê o número, não converte unidade e não julga o valor. Assim ele não traz faixa "boa" ou "ruim" (decisão 219) nem inventa dado técnico. Como o núcleo não faz conta com a medição, a regra "unidades SI no núcleo" não se aplica a ela: a unidade é a do instrumento;
    - **a referência** é a "faixa opcional por medição" da SPEC: um texto, com a marca "a verificar" (regra 2.7);
    - **a linha:** o que se mediu, o valor e a condição são obrigatórios; a unidade e a referência, não. A linha sem nenhum texto fica de fora, mesmo com a condição escolhida, porque a linha nova já vem com a condição da de cima. Nos códigos de falha, o mesmo: a linha sem código e sem descrição fica de fora; o código e a situação são obrigatórios, e a descrição não.
248. **O aviso de LGPD** (resposta do dono):
    - **a dica fixa,** embaixo da queixa: "Nas palavras do cliente, sem dados pessoais (nome, telefone, documento, e-mail): o laudo é dado técnico e fica mesmo se o cliente for anonimizado.";
    - **o aviso ao gravar,** só quando a queixa parece ter dado pessoal. A SPEC avisava a cada gravação, o que logo viraria um aviso que ninguém lê. Ele aparece em amarelo, embaixo, e não impede de gravar. Diz o que achou, ex.: "A queixa parece ter dado pessoal do cliente: o nome do cliente (Ana). O laudo fica mesmo se o cliente for anonimizado; se for dado pessoal, tire da queixa e salve de novo.";
    - **o que conta:**
      - o nome do cliente: cada palavra do nome com 3 letras ou mais (menos "das" e "dos"), sem diferença de acento ou maiúscula. O nome do cliente já anonimizado não conta;
      - um telefone ou documento: 8 dígitos ou mais, separados só por ponto, traço, parênteses ou espaço, como em "(11) 91234-5678", "123.456.789-09" e no CNPJ;
      - um e-mail;
    - **o que não conta** (testado): a data ("19/10/2026"), a hora, a medição ("12,6 V"), o código de falha ("P0300"), o km ("120.500 km") e a rotação ("2.000 rpm");
    - **só a queixa é conferida,** como a SPEC pede: é o texto que vem do cliente. Os outros são texto técnico da oficina;
    - **pode errar nos dois sentidos:** o nome do cliente que também é palavra comum, ou o dado pessoal sem número nem nome (um endereço). É só aviso, e a dica fixa continua lá. A regra mora no núcleo (`DadosPessoais`), sem tela, e pode servir a outros textos livres depois.
249. **O laudo, campo a campo** (SPEC 6.6.3):
    - **data e hora:**
      - o laudo novo vem com a hora de agora, no minuto, e ela se muda até a entrega;
      - ela não pode estar no futuro (com um minuto de folga) nem antes da abertura da OS;
    - **mecânico:**
      - vem o da OS, se estiver ativo, e pode ficar vazio (oficina sem mecânico cadastrado);
      - trocar exige um mecânico ativo. O que já estava no laudo continua valendo, mesmo se foi desativado depois, como na marcação do checklist (decisão 242);
    - **queixa:** obrigatória. O laudo novo já vem com o serviço pedido da OS, que é a queixa do jeito que o cliente falou na chegada; é só ajustar;
    - **testes realizados, causa, solução e recomendações:** todos opcionais. A SPEC marcava só as recomendações como opcionais. O laudo se grava durante o diagnóstico, e a causa e a solução só existem no fim;
    - **resultado:** resolvido, resolvido em parte ou não resolvido. Fica vazio ("a definir") enquanto o diagnóstico não termina;
    - **conferência:** os erros aparecem todos de uma vez, e a janela não fecha com erro.
250. **Quando o laudo se faz, se muda e se apaga:**
    - **criar e mudar:**
      - em qualquer etapa antes da entrega, também antes de o orçamento ser aprovado, porque o diagnóstico costuma vir antes do orçamento;
      - vários por OS, um para cada diagnóstico;
    - **depois da entrega ou do cancelamento:**
      - o laudo fica só para ver, e o PDF continua saindo (teste nº 6 da fase);
      - o caso de uso recusa criar, mudar e apagar, e o banco também (gatilhos da V10);
      - a SPEC dizia "depois de ENTREGUE"; o cancelamento entrou junto, como no checklist (decisão 242);
    - **apagar:** só com a OS em andamento e com confirmação, para o laudo feito por engano. É o mesmo critério dos anexos;
    - **anonimização:** o laudo fica igual (teste nº 7). O PDF tirado depois sai com o nome anonimizado, porque o cliente vem do cadastro na hora do PDF.
251. **Onde o laudo aparece:**
    - **aba Execução:** a área "Laudos de diagnóstico elétrico", depois dos checklists, como a SPEC pede (seção 8, item 9):
      - a lista mostra a data e a hora, o mecânico, a primeira linha da queixa (até 90 letras) e o resultado;
      - os botões são "Novo laudo…", "Abrir…" (ou o duplo clique), "Gerar PDF…", "Imprimir…" e "Apagar…";
      - com a OS entregue ou cancelada, "Novo laudo…" fica desligado, "Apagar…" some e a janela abre só para ver;
    - **menu Documentos:** os dois itens da decisão 246;
    - **ficha do veículo:**
      - a seção "Laudos elétricos", entre os projetos e os anexos, com os laudos de todas as OS do veículo, do mais novo para o mais antigo;
      - o duplo clique abre a OS, como na lista das OS da ficha; o laudo se abre na aba Execução dela, e a regra do "só para ver" fica num lugar só.
252. **O banco** (migração V10, em SQL; banco na versão 10):
    - **a tabela `laudo`:**
      - a OS e o mecânico. O mecânico é opcional e fica numa coluna, para o nome vir do cadastro;
      - o conteúdo em JSON, como o do orçamento;
      - quando foi criado e quando mudou;
      - a lista da OS sai pela data e hora do laudo;
    - **os gatilhos:** a OS entregue ou cancelada não ganha laudo, e o laudo dela não muda nem se apaga. O laudo também não muda de OS;
    - **o formato do JSON** fica fixo num teste, pelo arquivo de exemplo `laudo-0.3.1.json`: mudar o nome de um campo sem querer quebra o teste. A condição, a situação e o resultado vão pelo nome em maiúsculas (`REPOUSO`, `HISTORICO`, `RESOLVIDO`), e a data e a hora como "2026-10-20T10:30";
    - **só acrescenta:** nada do que existe muda. O teste sobe um banco na V9, com uma OS, até a V10. No 3B.7, o ensaio do MSI repete isso numa cópia dos dados reais.
253. **Um editor de linhas para várias telas** (`EditorLinhas`):
    - as medições e os códigos de falha usam o mesmo editor. Ele tem colunas de texto, de escolha e de marcar, o botão de acrescentar e o ✕ de cada linha;
    - a escolha da linha nova repete a da linha de cima;
    - no 3B.6, ele serve aos pontos da ficha do chicote;
    - o editor dos itens do checklist (3B.3) ficou como estava, porque tem subir e descer.

### Bloco 3B.5: instalações do veículo (20/10/2026)

Plano aprovado em 20/10/2026, em 5 passos. Três respostas do dono no plano: o item que sai do carro se retira (e apagar fica só para o engano), a OS entregue só mostra as instalações dela (a ficha continua mudando), e o histórico fica em cada instalação.

254. **Retirar e apagar** (resposta do dono):
    - **retirar:** o campo novo `retiradaEm`, que a SPEC não tinha. "Retirar…" pede a data (vem a de hoje), que não pode estar no futuro nem ser antes da instalação. O item continua na lista, em cinza, com "retirado em dd/mm/aaaa", porque serve ao diagnóstico ("o que já foi mexido no carro");
    - **desfazer:** com a retirada escolhida, o botão vira "Desfazer retirada". Retirar o que já está retirado é recusado: para mudar a data, desfaz-se e retira-se de novo. Ao editar, a data da instalação também não pode ficar depois da retirada;
    - **apagar:** só para o cadastro feito por engano, com uma confirmação que lembra de usar "Retirar…". O histórico da instalação vai junto, como no veículo apagado.
255. **A OS entregue ou cancelada só mostra as instalações dela** (resposta do dono):
    - **na aba Execução,** ficam a lista e o "Histórico…"; os outros botões somem, e a dica manda para a ficha;
    - **a instalação é do veículo:** pela ficha, ela se muda sempre. A esquecida se registra lá, escolhendo como origem qualquer OS do veículo, também a entregue ou a cancelada, e aparece na OS;
    - **quem confere:** o caso de uso recusa a instalação nova pela OS finalizada ("registre a instalação pela ficha do veículo…"). O banco não olha a etapa da OS, porque pela ficha vale; ele confere que a OS de origem é do mesmo veículo.
256. **O histórico de cada instalação** (resposta do dono):
    - **onde fica:** no mesmo log de auditoria do veículo (tabela `auditoria`), com a entidade `instalacao`;
    - **o que entra:**
      - a criação ("Cadastro: criada");
      - cada campo mudado, com o rótulo da tela, o antes e o depois (a OS de origem pelo número, a data como dd/mm/aaaa);
      - a retirada e o desfazer ("Retirada do veículo");
    - **onde se vê:** "Histórico…", na lista das instalações (na ficha e na OS), na mesma janela do histórico do veículo. O "Histórico do veículo…" continua só com o cadastro do veículo.
257. **A instalação, campo a campo** (SPEC 6.2):
    - **obrigatórios:** o tipo e a descrição. O resto é opcional;
    - **potência ou especificação:** texto livre, como no manual ou na nota (ex.: "800 W RMS, 4 canais", "firmware 2.3"). O app não confere, pela regra de não inventar dado técnico;
    - **data da instalação:** não pode estar no futuro. Pela OS, vem a data do dia; pela ficha, vem vazia (o item informado pelo cliente costuma não ter data);
    - **OS de origem:** a escolha entre as OS do veículo, com o número, o serviço pedido e a etapa. A OS de outro veículo é recusada pelo caso de uso e pelo banco (gatilho);
    - **sem OS:** é o item informado pelo cliente ou feito antes do app. A SPEC não tem um campo para dizer quem fez, e a coluna OS da lista já mostra a diferença.
258. **Onde as instalações aparecem:**
    - **ficha do veículo:** a seção "Instalações", logo depois de "OS do veículo":
      - a lista mostra as que estão no carro primeiro, da data mais nova para a mais antiga (sem data, no fim; na mesma data, a cadastrada por último antes), e as retiradas no fim, em cinza;
      - as colunas: tipo, descrição, marca e modelo, potência ou especificação, data, OS e situação;
      - os botões: "Nova instalação…", "Editar…" (ou o duplo clique), "Histórico…", "Retirar…" e "Apagar…";
    - **aba Execução da OS:** a área "Instalações desta OS", depois dos laudos, com as mesmas colunas, menos a da OS, e "Registrar instalação…". A SPEC (seção 8, item 9) ganhou essa área;
    - **a mesma área nas duas** (`InstalacoesController`), como a lista de anexos da OS e da ficha.
259. **A janela da instalação:**
    - **o formulário dos catálogos:** a janela usa o mesmo formulário (`FormularioTela`, com `CampoForm` e `LeituraForm`), sem tela nova desenhada à mão. O construtor de `LeituraForm` passou a ser público, para a leitura ficar nos casos de uso (`FormularioInstalacao`);
    - **os erros de uma vez:** a data digitada errada e os campos obrigatórios aparecem juntos. Para isso, o núcleo separou `validarCampos()` (o tipo e a descrição) das regras das datas, que dependem de hoje e da retirada e ficam com quem grava;
    - com erro, a janela fica aberta e mostra o que corrigir.
260. **O banco** (migração V11, em SQL; banco na versão 11):
    - **a tabela `instalacao`:**
      - o veículo e a OS de origem (opcional);
      - os campos, em colunas, como os do veículo;
      - a data de retirada, e quando foi criada e mudada;
    - **os gatilhos:** a OS de origem precisa ser do mesmo veículo (ao criar e ao mudar), e a instalação não muda de veículo;
    - **a anonimização e o arquivamento** não mexem na tabela: os testes conferem que as instalações e o histórico delas ficam iguais (teste nº 7 da fase, no banco e no caso de uso);
    - **só acrescenta:** o teste sobe um banco na V10, com um veículo e uma OS, até a V11. No 3B.7, o ensaio do MSI repete isso numa cópia dos dados reais;
    - **ordem dos passos:** a ligação do caso de uso no `PrincipalController` entrou no passo das telas, e não no dos casos de uso, para não deixar um campo sem uso no meio.

### Bloco 3B.6: injeção programável (20/10/2026)

Plano aprovado em 20/10/2026, em duas entregas, com parada para aprovar entre elas (resposta do dono): A, a OS sem o veículo e "quem retirou"; B, a ficha do chicote. As outras três respostas do dono no plano são da entrega B: o PDF da ficha é feito na hora, os testes de bancada vêm do checklist da OS, e na versão nova do chicote a anterior fica só para ver.

#### Entrega A: a OS sem o veículo e "quem retirou"

261. **O que os serviços pedem do veículo** (SPEC 6.5):
    - **quem conta:** os serviços do catálogo da última revisão aprovada, cada um como está na versão do orçamento (como a garantia, decisão 231). Mudar o serviço no catálogo depois não muda a OS;
    - **a linha digitada não conta:** ela não diz se exige o veículo, e não pode travar nem liberar uma OS. A OS só com linhas digitadas segue como antes: começar pede a entrada;
    - **onde mora:** o caso de uso calcula, numa conta só, os serviços de bancada (que não exigem o veículo) e os que exigem. A tela da OS recebe isso junto com a OS (`OsAberta`), para o próximo passo e o "Serviço pronto".
262. **Começar, checklist e pronto sem o veículo:**
    - **começar:** sem a entrada, só com ao menos um serviço de bancada; sem nenhum, a mensagem de sempre pede a entrada;
    - **checklist bloqueado:** sem a entrada, o checklist de todo serviço que não é de bancada aparece na aba Execução desligado, com o aviso "Bloqueado até a entrada do veículo (aba Entrada)". O caso de uso recusa marcar os itens dele, com a mesma frase. Registrada a entrada, ele libera;
    - **pronto:** sem a entrada e com algum serviço que exige o veículo, o caso de uso recusa e diz qual serviço precisa dele. Na tela, "Serviço pronto" mostra essa mensagem direto, sem abrir a janela das pendências: a justificativa não resolveria a falta do veículo;
    - **próximo passo:** com a OS aprovada, sem a entrada e com um serviço de bancada, diz que a bancada pode começar; em execução, lembra que o pronto precisa da entrada; pronta, lembra de anotar quem retirou.
263. **A entrega sem o veículo:**
    - **a regra na prática:** a SPEC diz "quando todos os serviços da OS dispensam a entrada". Como o pronto já exige a entrada quando algum serviço precisa do veículo, a OS que chega à entrega sem a entrada é exatamente essa. Então a regra ficou pela entrada: sem ela, a entrega pede **quem retirou** (obrigatório) e recusa o km de saída;
    - **onde aparece:** na janela de entrega (o campo "Quem retirou" no lugar do km, com a explicação), na aba Entrega e pagamento ("retirado por"), no cabeçalho da OS entregue ("o veículo não passou pela oficina") e no comprovante (a linha "Retirado por" no lugar do km, e a assinatura "Quem retirou"). O comprovante continua numa página;
    - **o núcleo:** `Entrega` ganhou "quem retirou", com um construtor de compatibilidade para os usos de antes, e a regra `validarSemVeiculo()`.
264. **O banco** (migração V12, em SQL; banco na versão 12):
    - a coluna `retirado_por` na OS, vazia nas OS que já existem (elas foram entregues com o veículo);
    - o teste sobe um banco na V11, com uma OS entregue, até a V12: a entrega de antes continua igual, com "quem retirou" vazio. No 3B.7, o ensaio do MSI repete isso numa cópia dos dados reais.

#### Entrega B: a ficha do chicote

265. **A ficha, campo a campo** (SPEC 6.6.5):
    - **injeção:** do catálogo de injeções ou, com "(texto livre)", escrita. A ficha guarda o nome e, quando é do catálogo, o item; a injeção arquivada depois continua aparecendo na ficha que a usa;
    - **motor:** a versão do motor do projeto de preparação da OS; sem projeto, o campo não aparece. O caso de uso recusa a versão de outro projeto;
    - **a ficha nova** vem com a versão "v1", em confecção e, com o projeto ligado à OS, com a injeção dele e a versão de agora do motor (regra 4 da 6.6.5);
    - **fonte do pinout:** obrigatória. Os pontos ficam no editor de linhas do 3B.4 (função, conector, pino da injeção, observação e "a verificar"); na linha começada, a função é obrigatória, e a linha sem texto fica de fora. O app não confere pinagem nem bitola (decisão 224);
    - **materiais e observações:** texto livre. As fotos ficam nos anexos da OS, sem campo próprio na ficha;
    - **situação:** em confecção ou testado, escolhida na ficha. "Entregue" não se escolhe: vem com a entrega da OS (decisão 269).
266. **As versões** (resposta do dono):
    - **"Nova versão…"** abre a janela com uma cópia da ficha escolhida, a próxima versão ("v1" vira "v2"; outro nome ganha " v2") e a situação de volta a em confecção. Nada é gravado até "Gravar";
    - **a anterior** fica guardada, só para ver, em cinza, com "(versão anterior, só para ver)" na situação. O PDF dela continua saindo. O caso de uso e o banco recusam mudar ou apagar a versão que já tem uma nova (teste nº 13 da fase);
    - **uma linha só:** cada versão tem no máximo uma nova (o banco não deixa duas versões novas da mesma), e a nova é da mesma OS;
    - **apagar:** só a última versão, feita por engano, com a OS em andamento. Apagada a nova, a anterior volta a mudar.
267. **Os testes de bancada vêm do checklist da OS** (resposta do dono):
    - **quais itens:** os do checklist dos serviços de bancada da OS (os que não exigem o veículo, como a confecção do chicote), como estão marcados na aba Execução: a situação, a observação (ou, no "não se aplica", a justificativa), quem marcou e quando. Nada se anota duas vezes;
    - **onde:** a janela mostra quantos itens estão feitos; o PDF traz cada um, numa tabela. Sem checklist de bancada, o PDF diz que a OS não tem;
    - **todos os itens**, e não só continuidade, isolamento e curto: o checklist da confecção é o procedimento de bancada inteiro, e escolher só alguns pelo texto quebraria quando o dono mudasse o modelo.
268. **O PDF é feito na hora** (resposta do dono), como o laudo e o comprovante, sem cópia nos anexos:
    - **onde sai:**
      - na área das fichas, com a ficha escolhida;
      - no menu Documentos da OS, em "Ficha do chicote: gerar PDF…" e "imprimir…", desligados sem ficha; com mais de uma (ou mais de uma versão), o app pergunta qual, com a mais nova já marcada;
      - o arquivo se chama, por exemplo, `OS-2026-0002-chicote-v1.pdf`;
    - **o que tem:** a OS, a versão e a situação, o cliente e o veículo, a seção "Chicote" (situação, injeção, motor e fonte do pinout), a tabela dos pontos, os testes de bancada, os materiais e as observações, e a assinatura do responsável pela confecção;
    - **uma página:** no primeiro desenho, a assinatura da ficha comum (6 pontos e 4 itens de bancada) caiu sozinha na segunda página. A situação foi para a seção "Chicote", e os materiais e as observações ficaram numa seção só. A ficha comum, e a do teste de tela com os 12 itens do checklist, cabem numa página; a tabela de um chicote grande continua na página seguinte (teste com 60 pontos).
269. **A situação "entregue" vem com a entrega da OS:**
    - na mesma gravação da entrega, antes de a OS ficar entregue, a última versão de cada chicote fica "entregue". A versão anterior fica como era (ela já não muda);
    - na OS cancelada, as fichas ficam como estavam, só para ver;
    - assim a ficha não fica "em confecção" numa OS entregue, e o dono não precisa lembrar de marcar.
270. **Quando a ficha muda e onde aparece:**
    - **quando:** em qualquer etapa antes da entrega, como o laudo. Depois da entrega ou do cancelamento, só para ver: o caso de uso recusa, e o banco também (gatilhos da V13). O teste da ficha da OS entregue é o mesmo do nº 6, para a ficha;
    - **aba Execução:** a área "Fichas do chicote", no fim, depois das instalações. A lista mostra a versão, a injeção, quantos pontos, a situação e quando mudou; os botões são "Nova ficha…", "Abrir…" (ou o duplo clique), "Nova versão…", "Gerar PDF…", "Imprimir…" e "Apagar…". Com a versão anterior escolhida, "Nova versão…" e "Apagar…" ficam desligados;
    - **ficha do veículo:** a seção "Fichas de chicote", depois dos laudos, com as de todas as OS, da mais nova para a mais antiga; o duplo clique abre a OS;
    - **anonimização:** a ficha é dado técnico e fica igual; o PDF tirado depois sai com o nome anonimizado.
271. **O banco** (migração V13, em SQL; banco na versão 13):
    - **a tabela `ficha_chicote`:** a OS, a versão anterior (única: uma versão nova por versão), o conteúdo em JSON e quando foi criada e mudada;
    - **os gatilhos:**
      - a OS entregue ou cancelada não ganha ficha, e a ficha dela não muda nem se apaga;
      - a versão que já tem uma nova não muda nem se apaga;
      - a versão nova é da mesma OS;
      - a ficha não muda de OS nem de versão anterior;
    - **o formato do JSON** fica fixo num teste, pelo arquivo de exemplo `ficha-chicote-0.3.1.json`;
    - **só acrescenta:** o teste sobe um banco na V12, com uma OS, até a V13. No 3B.7, o ensaio do MSI repete isso numa cópia dos dados reais.

## 21/10/2026 — Fase 3B, bloco 3B.7: fechamento (versão 0.3.1)

Plano aprovado pelo dono em 21/10/2026: os itens 3 e 4 da decisão 211, a revisão final da fase, a versão 0.3.1 e o MSI com o ensaio.

272. **As correções da OS guardam o antes e o depois** (item 3 da decisão 211; resposta do dono: "na aba Histórico"):
    - **o que entra:** cada campo que mudou ao salvar a abertura (o serviço pedido, o mecânico, a previsão de entrega e as observações) ou ao corrigir a entrada (a chegada, o km ou as horas de motor, o tanque, cada item da vistoria e os objetos deixados). Os nomes são os da tela: "Km no painel" no carro e "Horas de motor" no barco, e "Vistoria: Lataria e pintura" para cada item. O mecânico aparece pelo nome, e o item repetido da vistoria ganha o número ("Vistoria: Casco (2)");
    - **o que não entra:** o primeiro registro da entrada (não é correção), salvar sem mudar nada e o item novo da vistoria que ficou em branco (a entrada gravada sem itens mostra os de Configurações em branco). O item que saiu da vistoria aparece com o "depois" vazio;
    - **onde fica:** no mesmo log de auditoria do veículo, do cliente e das instalações (tabela `auditoria`, entidade `ordem_servico`), sem migração nova. A classe `Auditoria` passou a ser pública, com os dois métodos que a OS usa;
    - **onde aparece:** na aba Histórico da OS, embaixo das etapas, na lista "Correções da abertura e da entrada": a data e a hora, o campo, o antes e o depois, com a mais nova em cima. Sem correção, a lista diz que a abertura e a entrada estão como foram anotadas;
    - **anonimização:** o antes e o depois dos textos livres da OS ficam, como os próprios textos (manual, item 6.6). O mecânico é da oficina, não do cliente.
273. **Na anonimização, os anexos que ficam trocam o nome do arquivo** (item 4 da decisão 211; resposta do dono: "só o nome"):
    - **quais:** os das OS do cliente e os dos veículos dele que não são de OS, os mesmos de onde saem os documentos pessoais. O de outro cliente não muda;
    - **o que muda:** o nome original (o que aparece na lista dos anexos e na janela "Editar…") vira "arquivo anonimizado", com a extensão da cópia (".jpg", ".png" ou ".pdf"). É o único lugar com o nome original: a cópia na pasta de anexos já tem o nome tirado do conteúdo (SHA-256), e ela não muda;
    - **o que fica:** a descrição (escolha do dono: ela diz o que é a foto, como "Porta direita"), o tipo, a validade e o arquivo em si. O manual (item 6.6) avisa que a descrição e o que aparece na foto ou no PDF não mudam;
    - **quando:** na mesma gravação da anonimização, depois de apagar os documentos pessoais. A janela de confirmação diz isso, e a mensagem da ficha do veículo diz quantos nomes mudaram.
274. **Revisão final da Fase 3B** (feita por mim, nos pontos de risco; escolha do dono):
    - **migrações V8 a V13:** cada uma tem o teste "sobre um banco na versão anterior", e o da V8 sobe um banco na V7, com uma OS, até a 13. Faltava o caminho do backup antigo: um backup do 0.3.0 (banco na 7, com os anexos dentro do zip) restaurado no 0.3.1. Teste novo: ele restaura, sobe até a 13, a OS continua lá e o SQLite confere ("ok", sem chave estrangeira quebrada);
    - **backup e restauração:** as tabelas novas vão no próprio banco, e a pasta `anexos-dos-backups` é limpa quando o backup que usava um arquivo sai dos 30 (bloco 3B.0);
    - **sem rede e sem gravar na injeção:** nenhum `pom.xml` nem script mudou na fase, e o código novo não usa rede, USB nem porta serial (o `DependenciasSemRedeTest` continua passando);
    - **anonimização:** o laudo, a ficha do chicote, a instalação e as correções da OS seguem a regra do manual (item 6.6). Achado: o "quem retirou" (V12) fica, porque o banco não deixa mudar a OS entregue (gatilho da V6). Ficou documentado no manual e para o dono decidir (decisão 275);
    - **regras da OS sem o veículo e da trava do pronto:** achado na revisão aprovada depois do pronto (decisão 275).
275. **Ficam para o dono decidir** (não mexi, porque mudam regra ou banco):
    - **o "quem retirou" na anonimização:** o nome continua na OS entregue e no comprovante tirado depois. Proposta: na próxima mudança do banco (fase 4), uma migração que deixe a anonimização trocar esse campo por "(apagado na anonimização)" nas OS do cliente. Até lá, o manual sugere escrever "o próprio cliente" quando for ele. Os dados reais não têm OS entregue sem o veículo;
    - **revisão aprovada com a OS pronta:** a OS continua pronta (SPEC 6.5, regra 3), e os serviços novos ganham o checklist, mas a entrega não confere os itens obrigatórios deles nem, na OS de bancada sem a entrada, se algum precisa do veículo. Na tela, o quadro do serviço novo mostra os pendentes, e o "Retomar o serviço" volta a OS para a execução (o pronto seguinte passa pela trava). Proposta: a entrega pedir para retomar o serviço quando houver item obrigatório pendente ou serviço que precisa do veículo sem a entrada.
276. **Versão 0.3.1** (a que a SPEC previa para o fim da Fase 3B):
    - os 6 `pom.xml` passam para 0.3.1, e o CHANGELOG fecha a seção "[0.3.1] — Fase 3B";
    - **o README** ficou completo (pedido do dono): o que o app faz por área, os dados e o backup, como instalar e atualizar, as fases feitas e as próximas, e os comandos;
    - **o manual, item 2, ganhou "Atualizar do 0.3.0 para o 0.3.1"** e um jeito de voltar que funciona. O texto do 0.2.0 dizia para instalar a versão antiga e restaurar o backup "antes de atualizar o banco" pela aba Backup. Não dá certo: a versão antiga não abre dados já atualizados (ela avisa e para), e o 0.3.0 recusa os backups do formato 2, que o 0.3.1 faz ("feito por uma versão mais nova"), sem mexer em nada. O caminho de volta passa a ser o backup feito na versão antiga antes de atualizar ("Fazer backup agora"), restaurado nela com o `calibra.db` atual renomeado (não apagado). Conferido no código do 0.3.0; não ensaiado neste PC, porque instalar a versão antiga por cima não estava no que o dono liberou.
277. **O MSI 0.3.1 e o ensaio** (como no 0.3.0, com o OK do dono): o MSI foi gerado, instalado por cima do 0.3.0 neste PC e ensaiado. Os 29 testes de tela passaram no app instalado; ele abre em 2,0 a 2,1 s (3,5 s na primeira, com o banco criado do zero); e a atualização da versão 7 para a 13 passou numa cópia dos dados reais, apagada no fim. Os dados reais não foram tocados: continuam na versão 7 até o dono abrir o 0.3.1. Os números estão em `docs/fase3b/ensaio-instalador.md`.

## 21/10/2026 — Versão 0.3.2: as duas escolhas da decisão 275

O dono vai usar o app agora e quer que ele esteja 100% usável. Escolheu corrigir as duas melhorias da decisão 275 numa versão 0.3.2, antes de entrarem dados reais (plano aprovado em 21/10/2026).

278. **A trava da entrega** (item 2 da decisão 275):
    - **a regra:** a OS pronta não é entregue enquanto houver:
      - item obrigatório pendente num checklist criado depois do pronto, que é o que a revisão aprovada com a OS já pronta trouxe;
      - ou, sem a entrada, um serviço que precisa do veículo.

      O que estava pendente no pronto e foi justificado ali não trava de novo. A etapa continua a mesma (SPEC 6.5, regra 3: a revisão nova não muda a etapa);
    - **como destravar:** marcar os itens com a OS pronta (a aba Execução continua aberta) ou "Retomar o serviço" e fazer o pronto de novo, com a justificativa; sem o veículo, registrar a entrada;
    - **"depois do pronto"** compara a hora em que a cópia do checklist foi criada com a do pronto. O banco grava a hora por segundo, e a comparação é estrita: na vida real, aprovar uma revisão leva bem mais que um segundo depois do pronto, e o checklist copiado no começo do serviço nunca fica com a mesma hora do pronto. O teste de tela espera um segundo entre os dois;
    - **onde aparece:** `CasosOficina.travasDaEntrega`. A entrega recusa com a lista, e a tela não abre a janela da entrega: ela mostra o que falta embaixo, e o próximo passo da OS pronta avisa.
279. **O "quem retirou" sai na anonimização** (item 1 da decisão 275):
    - **o que muda:** a anonimização do cliente troca o "quem retirou" das OS dele por "(apagado na anonimização)", na mesma gravação. Vale também para a OS entregue, que é a única que tem esse campo preenchido. O comprovante tirado depois sai com o texto novo;
    - **o banco (migração V14, em SQL; banco na versão 14):** o gatilho da V6 recusava qualquer mudança na OS entregue ou cancelada. Ele foi refeito com uma única exceção: trocar o "quem retirou" por esse texto, com todas as outras colunas iguais, conferidas uma a uma. Outro texto no "quem retirou", ou a troca junto com outra mudança, continua recusado (teste). A migração não muda dado nenhum;
    - **a coluna nova futura:** se uma fase seguinte acrescentar uma coluna à OS, a migração dela refaz este gatilho com a coluna na lista.
280. **Versão 0.3.2:**
    - os 6 `pom.xml` passam para 0.3.2. O CHANGELOG ganha a seção "[0.3.2]", e o README e o manual dizem o que mudou;
    - **o manual, item 2,** passa a pedir "Fazer backup agora" antes de qualquer atualização (é o caminho de volta, decisão 276). Ele explica a atualização do 0.3.0 para o 0.3.2 (banco da 7 para a 14, o caso dos dados deste PC) e a do 0.3.1 para o 0.3.2 (só a V14);
    - o MSI 0.3.2 é ensaiado como o 0.3.1: instalado por cima, com os testes de tela no instalado e a atualização numa cópia dos dados reais, apagada no fim.
281. **O MSI 0.3.2 e o ensaio:**
    - o MSI foi instalado por cima do 0.3.1 neste PC;
    - os 29 testes de tela passaram no app instalado, e ele abre em 2,0 a 2,1 s;
    - a atualização da versão 7 para a 14 passou numa cópia dos dados reais, apagada no fim;
    - os dados reais não foram tocados: sobem para a 14 quando o dono abrir o 0.3.2;
    - os números estão em `docs/fase3b/ensaio-instalador.md`.

## 21/10/2026 — Versão 0.3.3: a marca GarageTech

Pedido do dono: o app passa a se apresentar como **GarageTech — Gestão e Tecnologia Automotiva**, com o logo, o ícone e as cores da imagem aprovada (`docs/marca/garagetech-identidade.png`) e uma tela de abertura com o logo grande. Plano aprovado em 21/10/2026, com estas respostas do dono:
- GarageTech também no Windows (atalho, menu Iniciar, "Aplicativos instalados");
- a versão 0.3.3;
- o azul da marca no app.

282. **Só a apresentação muda; o nome técnico fica.**
    - **O que muda:** o nome comercial passa a ser GarageTech onde ele aparece para o usuário:
      - o título da janela, a barra lateral e a barra de baixo;
      - as mensagens e os títulos de janelas;
      - os textos de tela;
      - os rodapés e títulos dos PDFs;
      - o instalador.
    - **O que fica "Calibra", e por quê:**
      - **o código:** os pacotes `br.com.calibra`, os módulos e as classes. Renomear não muda nada para quem usa, e arrisca quebrar referências;
      - **a pasta dos dados:** `%LOCALAPPDATA%\Calibra`, com o `calibra.db` e os backups `calibra-backup-*.zip`. Mudar faria o app novo não achar os dados nem os backups;
      - **a pasta do programa e o UpgradeCode:** com eles, a instalação por cima substitui a antiga;
      - **as sementes do catálogo:** as migrações conferem a soma delas (o checksum), e mudar o texto impediria qualquer banco de abrir;
      - **as linhas de log:** os scripts de ensaio as procuram;
      - **o histórico:** as decisões, a SPEC e os relatórios dos ensaios.
    - **Como foi feito:** não houve troca automática de "Calibra" por "GarageTech". Cada ocorrência foi conferida, e a busca final só deixou nome técnico ou histórico.
    - **Os dois testes que conferiam o nome** (`MigracoesTest` e `RelatorioMemoriaTest`) passaram a esperar "GarageTech", porque a exigência mudou. Nenhum teste foi afrouxado.
    - **Os PDFs e os dados já gravados não mudam:** só os documentos gerados daqui em diante saem com o nome novo.
283. **A tela de abertura usa o `-splash` do próprio Java**, sem framework novo:
    - **quando aparece:** o lançador mostra a imagem no instante do clique, antes de o Java carregar. É o momento mais demorado (o Java e o JavaFX), e uma janela do próprio app só apareceria depois dele;
    - **as escalas da tela:** o Java escolhe sozinho a imagem da escala do Windows (`abertura@125pct.jpg` neste PC, que está em 125%). Conferido com uma foto da tela: a imagem aparece no tamanho exato e centralizada;
    - **quando some:** ela fecha logo depois de a janela principal aparecer, sem espera artificial. Também fecha antes de qualquer janela de erro da abertura (banco que não abre, dados de versão mais nova, erro inesperado): ela não esconde a mensagem e não fica presa. Quem fecha é `TelaDeAbertura.fechar()`, que pode ser chamado mais de uma vez e não faz nada sem a abertura (os testes);
    - **onde liga:** no instalador (`--java-options -splash:$APPDIR\abertura.jpg`, com as imagens na pasta do app) e no `javafx:run`;
    - **o teste do app instalado** não repassa a opção (lá ninguém fecharia a abertura) e confere que a imagem está no pacote;
    - **JPEG de qualidade 95:** em PNG, cada escala teria quase 1 MB. O fundo é um degradê, e o JPEG não deixa marca visível.
284. **A marca dentro do app:**
    - **título:** "GarageTech — Gestão e Tecnologia Automotiva" no Painel e "GarageTech — <tela>" nas outras. A versão foi para a barra de baixo ("GarageTech 0.3.3 · núcleo … · offline");
    - **barra lateral:** o logo para fundo claro, sem fundo, com a frase como texto. O logo é carregado no tamanho dos pixels da tela, para ficar nítido em 125%, 150%…;
    - **cores:** o azul de destaque `#1565C0` virou o azul da marca `#1362F9`, com contraste de 5:1 com texto branco. O azul escuro `#0D47A1` virou `#0F4CC2`. O `-fx-accent` e o `-fx-focus-color` do JavaFX passaram para `#1362F9` (seleção em listas e foco). O `#0386FE` fica para o próprio logo: tem pouco contraste com o branco (3,6:1). As cores dos gráficos do mapa do compressor não mudaram: são cores técnicas da curva.
285. **GarageTech no Windows** (resposta do dono):
    - **o que muda no jpackage:** `--name GarageTech`, que dá o nome do atalho, do menu Iniciar, de "Aplicativos instalados" e do `GarageTech.exe`; também `--vendor GarageTech`, a descrição e o grupo do menu. O MSI sai como `dist\GarageTech-<versão>.msi`;
    - **o que fica:** a pasta do programa (`--install-dir Programs\Calibra`) e o UpgradeCode. Instalar por cima do Calibra 0.3.2 o substitui, sem duas entradas (ensaio no item 288);
    - **o `testar-instalado.ps1`** acha o `.cfg` pelo nome novo ou pelo antigo;
    - **atalho fixado na barra de tarefas:** se havia um do Calibra, ele aponta para o `.exe` antigo. Fixe de novo o GarageTech (o manual diz isso).
286. **Os arquivos da marca saem da imagem aprovada, sem redesenho:**
    - **o script:** `scripts\gerar-icone.ps1` (antes, desenhava o manômetro) recorta a imagem aprovada, com o System.Drawing do Windows;
    - **o que sai:**
      - o ícone: o símbolo sem o fundo, sobre o quadrado escuro do ícone de app aprovado, nos mesmos arquivos de antes;
      - a marca da barra lateral;
      - as 5 imagens da abertura.
    - **as transparências** saem pelo método "cor para transparência", com o fundo de cada ponto tirado dos cantos da área (o painel principal tem degradê);
    - **conferido a olho:** o ícone de 16 a 256 px, a marca e a abertura, sem distorção e na proporção original;
    - **os originais do designer:** se um dia chegarem, entram no lugar dos recortes (`docs/marca/identidade-visual.md`).
287. **Versão 0.3.3 e documentos:**
    - **versão:** os 6 `pom.xml` e o CHANGELOG ("[0.3.3]");
    - **README:** o título "GarageTech — Gestão e Tecnologia Automotiva", a imagem da abertura e a nota do nome anterior;
    - **manual:** o nome do produto trocado, os caminhos e nomes de arquivo mantidos (com a explicação) e "Atualizar do 0.3.2 para o 0.3.3";
    - **SPEC:** a nota no topo e na seção 12;
    - **documento novo:** `docs/marca/identidade-visual.md`;
    - **CLAUDE.md:** só o nome do MSI, na linha do comando do instalador.
288. **O MSI 0.3.3 e o ensaio:**
    - **a instalação:** o MSI foi instalado por cima do Calibra 0.3.2 neste PC, e o mesmo UpgradeCode fez o Windows trocar um pelo outro:
      - ficou uma entrada só, "GarageTech 0.3.3";
      - os atalhos viraram GarageTech;
      - o `GarageTech.exe` entrou no lugar do `Calibra.exe`, na mesma pasta;
    - **o app instalado:** os 29 testes de tela passaram, e ele abre em 1,9 a 2,1 s;
    - **a tela de abertura e a janela:** duas fotos da tela mostram o logo grande na abertura, depois a janela com o título, o ícone e a barra lateral novos;
    - **a atualização:** da versão 7 para a 14, numa cópia dos dados reais, apagada no fim;
    - **os dados reais:** intocados;
    - **o que os testes pegaram:** o id `marca` do logo colidia com o campo da marca do veículo. Corrigido (`logoDaMarca`) antes do MSI final;
    - os números estão em `docs/fase3b/ensaio-instalador.md`.

## 21/10/2026 — Fase 4, bloco 4.0: as pendências da fase 2

Plano da fase 4 (mapa base) aprovado em 21/10/2026, com estas respostas do dono:
- as injeções que ele usa: FuelTech, Injepro, Pandoo e Octtane;
- a tabela para colar (nível B) fica para quando ele mandar um texto real copiado do software;
- o modelo de VE começa com os valores da SPEC, "a validar";
- as paradas só onde precisa dele: a conferência do mapa de um projeto real (4.4) e o fim da fase, com o MSI.

O bloco 4.0 fecha as pendências das decisões 152 e 160.

289. **Unidades preferidas (Configurações › Unidades):** pressão (kPa, bar ou psi), temperatura (°C ou °F), potência (cv, hp ou kW) e vazão de bico (cm³/min, lb/h ou g/min).
    - **o que muda:** a unidade escolhida vem primeiro; as outras continuam ao lado (ex.: com psi, "34,95 psia · 241,0 kPa · 2,41 bar"). Assim o mecânico lê na unidade dele e ainda confere na do software da injeção ou da ficha da peça;
    - **o padrão:** "Várias, como sempre", com o texto de antes, sem mudança para quem não mexer;
    - **onde vale:** em tudo que passa pela `Apresentacao` (relatórios), ou seja, na tela de resultado e nos PDFs. O núcleo continua em SI;
    - **g/min:** a vazão do bico em cm³/min × a densidade do fluido de teste do bico (a mesma da ficha). Ela só aparece com essa escolha;
    - **onde fica:** o record `UnidadesPreferidas` (núcleo), a chave `unidades` da configuração (JSON; campo que falta vira "várias") e a aba nova da tela de Configurações;
    - **testes:** `ApresentacaoTest` (o texto de sempre e cada escolha) e `ConfiguracaoRepositorioTest.unidadesPreferidas`.
290. **O cálculo roda em segundo plano** (SPEC 10; fecha a decisão 160).
    - **como:** o "Calcular" lê a tela, e a conta roda numa `Task`, numa linha de execução só dos cálculos (`CalculoEmFundo`). Ela é separada da fila do backup e das fotos, para um cálculo nunca esperar um backup comprido;
    - **na tela:** enquanto a conta roda, o botão "Calcular" fica desligado e um indicador de progresso aparece ao lado dele;
    - **mudança no meio da conta:** se um campo muda enquanto ela roda, o resultado chega já marcado "desatualizado";
    - **erros:** os de digitação e o "não convergiu" aparecem como antes; um erro inesperado vai para o log e para a janela de erro;
    - **o "Sugerir" (bico, bomba, injeção e turbo)** continua na thread da tela: são poucas contas, de milissegundos. Se o gerador do mapa base deixar alguma pesada, ela vai para a mesma linha de execução;
    - **o mapa base** (bloco 4.4) usa o mesmo `CalculoEmFundo`;
    - **os testes de tela** esperam o cálculo terminar (`TelaDeTeste.esperarCalculo`). O ensaio completo mede do clique até a tela montada: 35 a 116 ms, abaixo da meta de 1 s.
291. **"Atualizando os dados…" na tela de abertura** (fecha o resto da decisão 160).
    - **quando:** só numa atualização de verdade, com um banco que já tem dados e uma migração a aplicar. O texto aparece antes do backup e da migração. Banco novo e banco em dia abrem sem ele;
    - **onde:** embaixo do "Gestão e Tecnologia Automotiva", centralizado, em cinza-claro sobre o fundo escuro da imagem;
    - **como:** `Migracoes.migrar` recebe o aviso como parâmetro, porque o calibra-dados não conhece a tela. A `Partida` o repassa, e a `TelaDeAbertura` escreve com a API da tela de abertura do Java (`SplashScreen.createGraphics` e `update`). Sem tela de abertura (os testes), não faz nada;
    - **testes:**
      - `MigracoesTest.avisaSoNaAtualizacao`: o aviso vem antes do backup e não aparece no banco novo nem no banco em dia;
      - `PartidaTest`: a segunda abertura não avisa;
      - `TelaDeAberturaTest`: o desenho numa cópia da imagem fica embaixo do logo e centralizado. A imagem sai em `target/captura-abertura-atualizando.png`;
    - **no app de verdade:** a tela de abertura com o aviso vai ser conferida no ensaio do MSI 0.4.0, que atualiza o banco.

## 22/10/2026 — Fase 4, bloco 4.1: o perfil completo da injeção

292. **O perfil da injeção (SPEC 9) fica dentro da injeção do catálogo, num record próprio (`PerfilInjecao`).**
    - **os campos:**
      - a impedância de bico aceita;
      - os bicos por saída;
      - o modelo de combustível (ms, VE ou modelo físico);
      - as tabelas;
      - a temperatura de referência do ar;
      - se a tabela em ms inclui o tempo morto;
      - o formato de importação e exportação (texto livre);
      - a proteção contra água (texto livre, ex.: IP67).

      Tudo é opcional, porque cada campo vem do manual ou do software da injeção (SPEC 2.7);
    - **injeção antiga:** a cadastrada antes da fase 4 não tem "perfil" no JSON e é lida com o perfil vazio. Nada se perde, e nada precisa de migração. Os cálculos salvos guardam a injeção do mesmo jeito;
    - **as tabelas:** cada uma tem o nome no software, a função, a carga (MAP em kPa ou TPS em %), a unidade, os pontos de rotação e de carga e a orientação. A orientação diz o que fica nas colunas e se as linhas crescem para baixo ou para cima, para a folha de transcrição sair igual à tela da injeção. O núcleo confere os eixos: de 2 a 64 pontos, crescentes, rotação até 20.000 rpm, MAP até 1.000 kPa e TPS de 0 a 100%;
    - **na tela, três vagas fixas:** combustível, ignição e lambda alvo, as tabelas que o mapa base preenche. As "demais tabelas" (partida, aquecimento, aceleração rápida, correções) vão para o checklist da SPEC 7.5. Cada vaga começa pela carga do eixo: em branco, a vaga não existe, e os outros campos dela ficam escondidos;
    - **os eixos são digitados ou colados** como uma lista de números: separados por espaço, tabulação, ponto e vírgula ou um por linha, no formato brasileiro (`NumeroBr.lerLista`). Assim, uma linha ou uma coluna copiada do software entra direto;
    - **o que o perfil ainda não faz:**
      - **bicos por saída e impedância** são informação: aparecem na folha e no checklist, para o mecânico conferir com o bico. A regra das saídas do semáforo não muda, e o app não classifica o bico em alta ou baixa impedância, porque a SPEC não dá o limite;
      - **o formato de importação** fica como texto até o nível B, que espera um texto real copiado do software (resposta do dono em 21/10/2026);
    - **testes:**
      - `PerfilInjecaoTest`;
      - `NumeroBrTest.leListaDeNumeros`;
      - `JsonTest.injecaoComPerfilEInjecaoAntiga`;
      - `FormulariosCatalogoTest.injecaoComPerfilEixosEmTextoEVagasDasTabelas` e a ida e volta do exemplo com o perfil;
      - `CatalogosUiTest.injecaoComPerfil`, que cadastra pela janela de verdade.

## 22/10/2026 — Fase 4, bloco 4.2: os modelos de VE e de ignição no catálogo

293. **Dois catálogos novos: "Modelos de VE" e "Modelos de ignição"** (SPEC 7.5 e 8.5).
    - **a tabela comum (`Tabela`, núcleo):** os pontos de rotação e de carga, do menor para o maior, e as células (vazia = null). Ela é usada pelos modelos, pelo mapa base e, depois, pelo editor. Ela interpola em duas direções (bilinear) e **nunca extrapola**. Uma célula vazia não vira zero: se a conta precisar dela, o resultado fica vazio. O modelo de VE, que precisa de valor no mapa todo, usa a borda fora dos eixos (`interpolarNaBorda`), e o gerador avisa;
    - **a orientação da tela da injeção:** a `OrientacaoTabela` leva a tabela para a ordem da tela (`naTela`) e traz de volta (`daTela`). Isso serve para colar uma tabela copiada do software e, no bloco 4.6, para a folha de transcrição;
    - **modelo de VE:** a família (válvulas por cilindro e comandos; em branco, qualquer), com eixos relativos:
      - **rotação relativa** = rpm ÷ rpm do torque máximo, porque o pico da VE fica perto do torque máximo (SPEC 7.5);
      - **carga relativa** = MAP ÷ 100 kPa.

      Assim o mesmo modelo serve para motores de rotações e pressões diferentes. Na tela, a VE fica em %; no núcleo, em fração;
    - **os 4 modelos que vêm com o app** (migração V15, `sementes/modelos-ve.json`), todos "a verificar" e com a fonte "SPEC 7.5, valores iniciais, a validar":
      - 4 válvulas com comando original, leve ou médio; 4 válvulas com comando agressivo;
      - 2 válvulas com comando original, leve ou médio; 2 válvulas com comando agressivo.
    - **como os valores foram montados, só com o que a SPEC dá:**
      - **marcha lenta:** 50%, o meio da faixa de 40% a 60%;
      - **pico na plena carga:** 97% com 4 válvulas e 88% com 2 (o meio das faixas de 95% a 99% e de 80% a 95%);
      - **entre a marcha lenta e a plena carga:** linear na carga, entre 35 kPa (a carga da marcha lenta) e 100 kPa;
      - **acima de 100 kPa:** o valor da plena carga;
      - **na rotação:** a mesma VE em todas as colunas. A forma na rotação vem do projeto, que manda nos pontos do torque e da potência (bloco 4.3);
      - **comando agressivo** ("reduz a VE em baixa"): 20% a menos até 0,4 × a rotação de torque, o que leva a marcha lenta a 40% (o limite de baixo da faixa), voltando ao normal na rotação de torque;
      - **3 e 5 válvulas:** nenhum modelo semente bate. O mecânico escolhe um na tela do mapa base ou cadastra o dele;
    - **modelo de ignição:** a família (texto livre), o combustível do mapa (código), a temperatura do ar do mapa e o avanço em rpm × kPa. O avanço é digitado ou colado como está na tela da injeção, dizendo o que fica nas colunas e o sentido das linhas. **O app não traz nenhum** (SPEC 7.5: "o app não inventa avanço");
    - **os dois catálogos não têm preço** (não são peças);
    - **no formulário:**
      - os eixos e as células são texto: o eixo numa linha e as células uma linha por linha da tabela, separadas por tabulação (`GradeTexto`), para colar do software ou do Excel;
      - a janela de texto das células é pequena. O editor de tabela de verdade é o bloco 4.5;
    - **testes:**
      - `TabelaTest`: a interpolação, as células vazias, as quatro orientações e os modelos;
      - `NumeroBrTest.leGradeDeNumeros`;
      - `SementesDosModelosDeVeTest`: o checksum da V15 e os valores da SPEC em cada modelo;
      - `FormulariosCatalogoTest.modelosDeVeEIgnicaoComoTexto` e a ida e volta dos exemplos;
      - `CatalogosUiTest.modelosDeVeEIgnicao`, na janela de verdade.

      O banco vai para a versão 15.

## 22/10/2026 — Fase 4, bloco 4.3: o gerador do mapa base (núcleo 1.2.0)

294. **O gerador do mapa base (`GeradorMapaBase`, núcleo)** segue a SPEC 7.5. As contas de cada célula são as da SPEC 7.1, as mesmas do dimensionamento. O núcleo vai para a versão 1.2.0, e as contas do dimensionamento não mudaram.
    - **a entrada:**
      - o cálculo do projeto: motor, meta, combustível, bico, VEs, lambda e padrões. O gerador refaz o dimensionamento para pegar o MAP, a Tm e a P_atm;
      - a injeção, com o perfil (opcional);
      - o modelo de VE;
      - a fonte da ignição (opcional);
      - as regras;
      - a mola da wastegate (opcional).

      **Sem bico ou sem a rotação de torque máximo, não gera**, e a mensagem diz o que falta;
    - **os eixos:**
      - os da tabela do perfil, só por MAP;
      - a ignição e o lambda sem tabela própria seguem a de combustível;
      - sem perfil, os padrão da SPEC: de 500 em 500 rpm até a máxima (a máxima entra no fim) e de 20 kPa até 1,1 × o MAP máximo, arredondado para cima de 10 em 10 (aspirado: até 105 kPa);
    - **a VE de cada célula** = VE do modelo (rpm ÷ rpm do torque, MAP ÷ 100 kPa) × o fator do projeto:
      - no torque máximo, o fator é a VE do torque ÷ a VE do modelo ali;
      - na potência máxima, a VE da potência ÷ a VE do modelo ali;
      - entre as duas, linear; antes do torque e depois da potência, o da ponta.

      Assim a célula da potência máxima, no MAP do dimensionamento, tem a VE do projeto, e o modelo dá a forma do resto;
    - **o lambda** (SPEC 7.5):
      - 1,00 até 70 kPa;
      - de 70 a 100 kPa, linear até o lambda de plena carga: no aspirado, o alvo do projeto; no turbo, o lambda aspirado de plena carga do combustível;
      - acima de 100 kPa, o alvo do projeto, que vem do lambda sob pressão do combustível e pode ser editado;
      - acima de 90 kPa, 0,03 mais rico; em NÁUTICO e CIRCUITO/ENDURANCE, mais 0,02.

      A SPEC só fala do 1,00 "em RUA"; nos outros usos, o mapa usa o mesmo valor e avisa (informação);
    - **o combustível:**
      - **em ms:** o TI de cada célula com o ar na temperatura de referência do perfil (sem ela, 25 °C e um alerta amarelo). O tempo morto entra se o perfil disser que a tabela inclui; sem saber, entra (mais rico) e há um alerta amarelo;
      - **por VE e no modelo físico:** a VE em %, e o lambda vai na tabela dele.

      **Teste dos casos golden:** com o ar de referência na Tm e sem a riqueza extra, a célula (rpm da potência, MAP do dimensionamento) dá o TI e o DC do dimensionamento: 14,81 ms e 86% no caso B; 15,19 ms e 70% no caso C;
    - **o DC de cada célula** usa a temperatura do ar esperada nela, porque a injeção corrige a tabela por ela:
      - até 100 kPa, o ar ambiente mais o acréscimo do aspirado;
      - acima, linear até a Tm no MAP do dimensionamento, e continua subindo depois.

      Dois ajustes vieram de conferir um mapa de verdade:
      - **com a temperatura de referência**, o DC da célula da potência dava 95% contra os 86% do dimensionamento;
      - **onde se julga o DC:** o semáforo do DC vale até a rotação de potência e o MAP máximo. Acima da potência, o mapa mantém a VE (mais rico, de propósito), e o DC de lá vai só como informação. Senão, todo motor com o limitador acima da potência teria vermelho;
    - **a ignição** (o app não inventa avanço):
      - **sem fonte:** a tabela vazia e o vermelho "ignição sem modelo";
      - **com fonte:** o avanço interpolado da fonte, sem estender: fora dos eixos dela, a célula fica vazia, com vermelho;
      - **reduções configuráveis:**
        - por pressão: graus a cada 10 kPa acima de 100;
        - por temperatura do ar: graus a cada 10 °C acima da temperatura da fonte;
        - por troca de combustível: graus fixos.

        Nunca somam avanço;
      - **a fonte antes das reduções** fica gravada junto, para a tela mostrar a diferença célula a célula;
      - **a SPEC não dá número para as reduções:** elas começam vazias, e o mapa avisa em amarelo enquanto não forem configuradas;
    - **as travas:**
      - a pressão alvo do turbo é o menor valor entre a mola da wastegate e 50% da prevista (MAP máximo − P_atm);
      - o limitador fica 500 rpm abaixo da máxima;
      - os cortes de proteção vão para o checklist (bloco 4.6);
    - **os alertas (`AlertaMapa`),** com a cor do semáforo. O vermelho só passa com justificativa, como no dimensionamento.
      - **vermelhos:**
        - ignição sem fonte ou incompleta;
        - DC acima do amarelo;
        - eixo de carga da injeção abaixo do MAP máximo (a injeção repetiria a última linha: pobre);
        - tabela da injeção por TPS (esta versão só gera por MAP);
        - duas bancadas por cilindro (o mapa soma a vazão das duas, e a divisão se configura na injeção);
      - **amarelos:**
        - o perfil incompleto ou sem injeção;
        - o eixo de rotação curto;
        - o modelo de VE de outra família;
        - as reduções de ignição não configuradas;
        - o TI efetivo na marcha lenta abaixo do mínimo;
    - **a memória:**
      - **as contas comuns,** como o bico, os fatores da VE e as travas, ficam no resultado;
      - **as de cada célula** saem na hora, pela mesma conta (`memoriaDaCelula`), para a tela mostrar ao clicar;
    - **testes (`GeradorMapaBaseTest`, 19):**
      - os casos golden B e C;
      - os pontos de torque e de potência;
      - a troca do modelo;
      - as regras de lambda (também náutico e aspirado);
      - os eixos padrão;
      - as travas;
      - a ignição sem fonte, com fonte, fora da fonte e com as reduções;
      - o perfil (eixos, tempo morto, VE) e os alertas;
      - "trocar o bico não muda a VE" (SPEC 7.6);
      - DC e duas bancadas;
      - as recusas;
      - de 3 a 12 cilindros.
    - **para a Parada 1 (o dono confere):**
      - **os números das reduções de ignição;**
      - **o lambda da carga parcial** fora de RUA;
      - **se alguma das injeções dele usa tabela de uma dimensão** (combustível só por MAP, com correção por rotação à parte). O gerador desta versão faz tabelas de rotação × MAP.
