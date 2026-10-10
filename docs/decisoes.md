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
