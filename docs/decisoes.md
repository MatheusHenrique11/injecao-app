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
