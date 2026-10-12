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
