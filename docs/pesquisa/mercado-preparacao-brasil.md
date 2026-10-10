# Pesquisa — mercado de preparação no Brasil e motores que o Calibra precisa cobrir

Feita em 09/10/2026. Fontes no fim de cada seção.

Os dados de motor vêm de fichas técnicas, manuais e matérias de revistas. Os dados de montagem (strokers, "1.9" etc.) vêm de fóruns e blogs de preparação e precisam ser conferidos com uma retífica antes de virar dado do app (regra 5 da spec: dado sem fonte confirmada fica marcado "a verificar").

## Resumo

1. **O Calibra não está preso ao 2.0.** O núcleo aceita qualquer motor de 3 a 12 cilindros, com qualquer diâmetro e curso. O "2.0" que aparece nos testes é só o caso de referência B da spec, e ele tem exatamente a geometria do **AP 2.0** e do **EA888 2.0 TSI** (82,5 × 92,8 mm). O caso C da spec é o **VW 1.0 TSI** (74,5 × 76,4 mm).
2. **O mercado tem duas frentes bem diferentes:**
   - **Motores antigos ou aspirados** (AP, Opala, Família II, Marea, 5 cilindros VW): injeção programável (FuelTech, Injepro, Pandoo) e acerto no dinamômetro. É aqui que o Calibra se encaixa.
   - **Motores turbo modernos de fábrica** (TSI, EA888, T-Jet): o mercado é de **reprogramação da central original** ("stage 1/2/3"), quase sem injeção programável.
3. **O AP ainda é a base mais popular**, pela oferta de peças. A preparação dele é feita por **montagem**: bloco + virabrequim + pistão + biela. Os nomes populares ("1.9", "2.1", "2.2") são **ambíguos**: o mesmo nome vale para combinações diferentes. O app precisa calcular pela geometria (diâmetro, curso, biela), nunca pelo nome.
4. **Opala 4.1 de 6 cilindros** é a referência da arrancada traseira: em versões turbo de arrancada passa de 4.785 cm³ e de 1.000 cv. Na arrancada nacional (CBA), o maior grupo é o de **tração dianteira turbo** (DTA, DTB, DTC).
5. **Seu projeto (i30 CW, G4GC):** pelas contas do próprio Calibra, 0,5–0,6 bar levam o motor de 145 cv para **cerca de 205 a 225 cv**, e o torque na rotação de potência vai de 186 para **cerca de 245 a 260 N·m** (estimativa, ver seção 4). O câmbio automático de 4 marchas (A4CF1, pelas fichas) já tem trancos. Faz sentido resolver o câmbio antes do turbo, e o app pode ajudar se souber o torque que cada câmbio aguenta.
6. **Faltam dados públicos de tamanho do nicho.** Não achei faturamento do mercado de preparação. A pesquisa de preços com 5 a 10 oficinas (fase 0) continua sendo a fonte mais confiável para o modelo de negócio.

## 1. Como o mercado funciona

**Duas frentes**

| | Motores antigos / aspirados | Turbo moderno de fábrica |
|---|---|---|
| Exemplos | AP, Opala 4.1, Família II, Marea 5 cil., VW 2.5 5 cil., Zetec, Fire | EA211 1.0/1.4 TSI, EA888 2.0 TSI, T-Jet, GSE turbo |
| Gerenciamento | Injeção programável (FuelTech, Injepro, Pandoo) | Reprogramação da central original |
| Como se vende | Projeto: peças + instalação + acerto no dinamômetro | "Stage 1" (só software), "Stage 2" (intake, downpipe), "Stage 3" (turbo maior) |
| Onde o Calibra entra | Em cheio: dimensionamento, orçamento, mapa base | Pouco: não há mapa base para a central original; no máximo dimensionamento e orçamento |

Exemplos anunciados de reprogramação no Brasil (sem condição de medição informada):
- Passat TSI B8 (EA888): stage 1 com 320 cv, stage 3 com 400+ cv.
- Golf 1.0 TSI: stage 1 com 150 cv.
- UP! TSI: torque de 16,8 para 22,2 kgfm no stage 1.

**Quem fornece**
- **FuelTech:** Porto Alegre, desde 2003. Tem a escola FT Education (mais de 80 mil alunos, segundo a empresa) e uma rede de oficinas parceiras credenciadas. Módulos FT550 e FT600 aparecem no varejo entre R$ 7 mil e R$ 13 mil.
- **Injepro:** Cascavel (PR), desde 2011, com clientes no Mercosul, na América do Norte e na Europa.
- **Pandoo** e outras marcas nacionais.

**Arrancada (o laboratório do mercado)**
- **Campeonato nacional:** a CBA organiza o Campeonato Nacional de Arrancada, com 19 categorias em 2025: tração dianteira e traseira, força livre, 4x4, Pro Mod.
- **Maior grupo:** o de tração dianteira turbo (DTA, DTB, DTC). Também há categorias street de tração traseira (STT, TST).
- **Recordes:** no Fueltech Velopark, os recordes recentes ficam perto de 4,5 s (força livre dianteira) e 4,2 s (força livre traseira).
- **Campeonatos estaduais:** há campeonatos das federações (PR, RS etc.).

**Tamanho do mercado**
- Não achei número confiável de faturamento do nicho de preparação.
- Contexto geral: frota de mais de 60 milhões de veículos, envelhecida. O aftermarket brasileiro pode dobrar até 2040, chegando a cerca de US$ 25 bilhões (McKinsey, citada pela Valeo).

**Prática comum citada nas fontes (turbo de baixa pressão em motor aspirado)**
- **Pressão:** 0,5–0,6 bar com motor original é visto como possível para uso leve, mas com risco maior de desgaste.
- **Gerenciamento:** a recomendação é injeção programável e sonda wideband, e atrasar a ignição quando entra pressão.
- **Cuidados:** controlar a temperatura do óleo e a detonação.
- **Taxa de compressão:** baixar a taxa ou usar combustível de octanagem maior quando a pressão sobe. O etanol aguenta mais.

Fontes:
- [FuelTech — o que é](https://www.fueltech.com.br/blogs/news/o-que-e-fueltech)
- [FuelTech — oficinas parceiras](https://www.fueltech.com.br/pages/oficinas-parceiras)
- [FT Education](https://www.fteducation.com.br/)
- [Injepro (LinkedIn)](https://br.linkedin.com/company/injepro)
- [FuelTech (Wikipedia)](https://en.wikipedia.org/wiki/FuelTech)
- [Valeo — aftermarket automotivo](https://www.valeoservice.com.br/pt-br/newsroom/aftermarket-automotivo)
- [Mercado Livre — injeção programável](https://lista.mercadolivre.com.br/inje%C3%A7%C3%A3o-programavel)
- [CBA — downloads 2025](https://www.cba.org.br/campeonato/downloads/319/793/2025)
- [CBA — Dianteira Turbo C](https://cba.org.br/upload/downloads//726/4-categoria-dianteira-turbo-ldquo-c-rdquo-dtc-.pdf)
- [Atleta Pro — recordes no Velopark](https://www.atletapro.com.br/recordes-arrancada-velopark-copa-ft/)
- [FGARS — regulamentos](https://www.fgars.org/modalidades/arrancada/regulamentos/)
- [Armada Performance — Passat TSI B8](https://armadaperformance.com.br/reprogramacao-de-ecu-vw-passat-tsi-b8/)
- [Armada Performance — Golf 1.0 TSI](https://armadaperformance.com.br/reprogramacao-de-ecu-vw-golf-1-0-tsi-mk7/)
- [Strike — UP! TSI](https://en.strikebrasil.com/veiculos/reprogramacao-vw-up-tsi-1-0/)
- [ItsTurbo — de aspirado a turbo](https://www.itsturbo.com.br/blog/posts/guia-completo-como-turbinar-carro-aspirado-c918e0d96ffb/)
- [Preparados — taxa de aspirado vs turbo](https://preparados.com.br/topic/19982/taxa-de-aspirado-vs-turbo)

## 2. Motores mais preparados — dados de partida

A cilindrada foi conferida por mim com π/4 × D² × S × n. Os outros dados vêm das fontes. Ordem de ignição e altura de bloco **não** foram confirmadas nesta pesquisa: ficam como "a verificar".

### 2.1 VW AP (linha AP)

| Versão | Diâmetro × curso (mm) | Cilindrada | Observações |
|---|---|---|---|
| AP 1.6 | 81 × 77,4 | 1.595 cm³ | Mesmo bloco do 1.8; pistão mais alto (maior altura de compressão) |
| AP 1.8 | 81 × 86,4 | 1.781 cm³ | Mesmo bloco do 1.6 |
| AP 2.0 | 82,5 × 92,8 | 1.984 cm³ | Bloco diferente só na parte de baixo (folga do virabrequim) |

- **Biela:** todo AP usa biela de **144 mm** (centro a centro).
- **Agregados:** distribuidores e outros agregados não trocam entre bloco 1.6/1.8 e bloco 2.0.

**Montagens comuns.** Os nomes populares variam, então o app deve calcular pela geometria:

| Nome popular | Montagem citada | Diâmetro × curso | Cilindrada |
|---|---|---|---|
| "Milenove" (1.9) | Pistões do 2.0 (82,5) + virabrequim do 1.8 | 82,5 × 86,4 | 1.847 cm³ |
| "1.9" (outra receita) | Bloco 1.8 + virabrequim do 2.0 + pistão 81 (bloco retrabalhado) | 81 × 92,8 | 1.913 cm³ |
| 2.0 sobremedida | Bloco 2.0 + pistão 83 | 83 × 92,8 | 2.008 cm³ |
| ~2.1 | Virabrequim 96 + pistão 83 | 83 × 96 | 2.078 cm³ |
| ~2.1 | Virabrequim 95,5 (do AP 1.9 diesel, segundo fórum) + pistão 84 | 84 × 95,5 | 2.117 cm³ |
| ~2.2 | Virabrequim 99 + pistão 85 (cálculo meu) | 85 × 99 | 2.247 cm³ |

**Por que o app precisa da geometria completa:**
- Curso maior com a mesma biela de 144 mm exige pistão com menor altura de compressão.
- Curso maior também piora a **relação biela/curso**: 1,55 no 2.0 original, 1,45 com curso de 99 mm.
- Curso maior sobe a velocidade do pistão, que o Calibra já confere no semáforo.
- Com bielas forjadas, o bloco 1.8 exige mais desbaste.

Fontes:
- [Luciano Preparação — alterando a cilindrada do AP](http://lucianopreparacao.blogspot.com/p/alterando-cilindrada-de-seu-ap.html)
- [Luciano Preparação — ficha técnica do AP](http://lucianopreparacao.blogspot.com/p/ficha-tecnica-motor-ap.html)
- [Preparados — AP 1.9 forjado](https://preparados.com.br/topic/14289/ajuda-montagem-do-motor-ap-1-9-forjado)
- [Preparados — bloco 1.8 com virabrequim do 2.0](https://preparados.com.br/topic/22088/motor-ap-bloco-1-8-com-virabrequim-do-2-0)
- [Preparados — virabrequim de 96 mm](https://preparados.com.br/topic/3958/virabrequim-vw-96mm-de-curso)
- [Clube do Gol Quadrado — AP 1.7/1.9/2.1/2.2](https://www.golquadrado.com.br/forum/viewtopic.php?t=16385)
- [Carmaniacs — reabrindo bloco 1.6/1.8 para 2.0](http://carmaniacs.com.br/forummm/index.php?showtopic=16082)
- [Fusca4ever — decidindo como melhorar o motor](http://www.fusca4ever.com.br/forum/se%C3%A3%C2%A7%C3%A3%C2%A3o-t%C3%A3%C2%A9cnica/dicas-t%C3%A3%C2%A9cnicas/4972-tutorial-decidindo-como-melhorar-o-motor)
- [Volkswagen AP (Wikipedia)](https://pt.wikipedia.org/wiki/Volkswagen_AP)

### 2.2 GM: Opala, Omega e Família II

| Motor | Diâmetro × curso (mm) | Cilindrada | Taxa | Potência de série | Observações |
|---|---|---|---|---|---|
| Opala 151 (2.5, 4 cil.) | 101,6 × 76,2 | 2.471 cm³ | — | — | A partir de 1974; antes 98,42 × 82,55 |
| Opala 250 (4.1, 6 cil.) | 98,4 × 89,66 | 4.093 cm³ | 7,5:1 | 140 cv brutos | Curso aumentado em 1971 (antes 3.8) |
| Opala 250-S | 98,4 × 89,66 | 4.093 cm³ | 7,8:1 | 171 cv brutos | Comando de desempenho, molas duplas, volante de 9 kg |
| Opala 6 cil. de arrancada | — | 4.785 cm³ | — | mais de 1.000 cv | Turbo; acima de 7.000 rpm |
| Omega 4.1 (C41GE) | 98,4 × 89,7 | 4.093 cm³ | 8,5:1 | 168 cv a 4.500 rpm; 29,1 kgfm a 3.500 rpm | Injeção multiponto sequencial; dutos de admissão individuais |
| Omega 3.0 (C30NE, Opel) | 95 × 69,8 | 2.969 cm³ | 9,2:1 | 165 cv a 5.800 rpm; 23,4 kgfm a 4.200 rpm | 6 cilindros em linha |
| Família II C20NE (2.0 8V) | 86 × 86 | 1.998 cm³ | 9,2:1 | 115 PS | |
| Família II C20XE (2.0 16V) | 86 × 86 | 1.998 cm³ | 10,5:1 | 150 PS | Bloco closed deck, citado para turbo (C20XE/C20YE) |
| Família II C22NE (2.2) | 86 × 94,6 | 2.198 cm³ | — | 116–123 hp | Principalmente Brasil |
| Família II 2.4 (Flexpower 8V/16V) | 87,5 × 100 | 2.405 cm³ | — | até 150 hp | Bloco alto: mais torque, relação biela/curso pior |

Fontes:
- [Autoentusiastas — motores de 4 e 6 cilindros do Opala](https://autoentusiastas.com.br/2019/11/os-motores-de-4-e-6-cilindros-do-opala/)
- [Vintage Clássicos — 250 x 250-S](https://vintageclassicos.com.br/opala-4100-a-diferenca-entre-os-motores-250-e-250-s/)
- [Futcarros — Omega 4.1](https://futcarros.com.br/omega-4-1/)
- [Garagem do Alan — Omega CD 4.1 1996](https://garagemdoalan.com.br/chevrolet-omega-cd-4-1-at-1996-preco-consumo-desempenho-e-ficha-tecnica.html)
- [Heycar — Omega CD 3.0 1993](https://heycar.com.br/ficha-tecnica/ficha-tecnica-completa-do-chevrolet-omega-cd-3-0-1993)
- [Ficha Completa — Omega CD 3.0 1994](https://www.fichacompleta.com.br/en/carros/chevrolet/omega-cd-3-0-1994)
- [GM Family II engine (Wikipedia)](https://en.wikipedia.org/wiki/GM_Family_II_engine)
- [Flatout — preparação do motor Família 2](https://flatout.com.br/preparacao-do-motor-familia-2-da-gm/)
- [Clube do Opala Marília — dicas](http://clubedoopalamarilia.blogspot.com/p/dicas.html)

### 2.3 Cinco cilindros: Fiat e VW

| Motor | Diâmetro × curso (mm) | Cilindrada | Taxa | Potência de série | Observações |
|---|---|---|---|---|---|
| Fiat 2.0 20V (Marea) | 82 × 75,65 | 1.998 cm³ | — | 142 cv | Família Pratola Serra |
| Fiat 2.0 20V Turbo (Marea Turbo) | 82 × 75,65 | 1.998 cm³ | 8,5:1 | 182 cv; 27 kgfm | Turbo Garrett, pressão máxima 1,2 kgf/cm²; no Fiat Coupé, 220 cv |
| Fiat 2.4 20V (Marea / Stilo Abarth) | 83 × 90,4 | 2.446 cm³ | — | 157–165 cv | |
| VW 2.5 20V (Jetta 2008–2011) | 82,5 × 92,8 | 2.480 cm³ | 10,0:1 | 170 cv; 24,5 kgfm a 4.250 rpm | Aspirado, injeção multiponto sequencial |

O VW 2.5 tem a mesma geometria do Audi 2.5 TFSI do RS3 (400 PS), mas são motores diferentes: **peças não devem ser consideradas intercambiáveis** sem conferência.

Fontes:
- [Fiat Pratola Serra (Wikipedia)](https://en.wikipedia.org/wiki/Fiat_Pratola_Serra_modular_engines)
- [Auto-data — Marea 2.0 20V Turbo](https://www.auto-data.net/en/fiat-marea-185-2.0-i-20v-turbo-182hp-7203)
- [Autopapo — Marea Sedan e Weekend](https://autopapo.com.br/noticia/fiat-marea-sedan-weekend-dupla-injusticada/)
- [car.blog.br — Marea Turbo](https://www.car.blog.br/2018/12/fiat-marea-turbo-fotos-consumo-e-preco.html)
- [Notícias Automotivas — Jetta Variant 2.5](https://www.noticiasautomotivas.com.br/avaliacao-completa-da-volkswagen-jetta-variant-2-5/)
- [Carros na Web — Jetta 2.5](https://www.carrosnaweb.com.br/fichadetalhe.asp?codigo=926)
- [Auto-data — Jetta V 2.5](https://www.auto-data.net/en/volkswagen-jetta-v-2.5i-170hp-46498)
- [Audi straight-five (Wikipedia)](https://en.wikipedia.org/wiki/Audi_straight-five_engine)

### 2.4 VW modernos (referência dos casos de teste da spec)

| Motor | Diâmetro × curso (mm) | Cilindrada | Taxa | Observações |
|---|---|---|---|---|
| EA211 1.0 (MPI e TSI, 3 cil.) | 74,5 × 76,4 | 999 cm³ | 10,5:1 | Mesma geometria do caso golden C |
| EA888 2.0 TSI (3ª geração) | 82,5 × 92,8 | 1.984 cm³ | 9,6:1 (9,3:1 no CJXC) | Mesma geometria do AP 2.0 e do caso golden B (SSP 606 que você tem) |

Fontes:
- [VW EA211 (Wikipedia)](https://en.wikipedia.org/wiki/Volkswagen_EA211_engine)
- SSP 606 da Audi (`OneDrive\Documentos\Material Automotivo\EA888`)

### 2.5 Hyundai G4GC (seu i30 CW)

| Item | Valor | Fonte |
|---|---|---|
| Motor | Beta II 2.0 16V CVVT | Manual de oficina Hyundai |
| Diâmetro × curso | 82 × 93,5 mm | Manual de oficina Hyundai |
| Cilindrada | 1.975 cm³ | Manual de oficina Hyundai |
| Taxa | 10,1:1 | Manual de oficina e Carros na Web |
| Potência e torque (i30 2.0 no Brasil) | 145 cv a 6.000 rpm; 19 kgfm (cerca de 186 N·m) a 4.600 rpm | Carros na Web |
| Construção | Bloco de ferro fundido, cabeçote de alumínio 16V | Engineswork |
| Câmbio do i30 2.0 automático | A4CF1, 4 marchas com conversor de torque | Fichas técnicas |

**Atenção ao etanol:** o i30 2.0 vendido aqui era, pelas fichas, movido a gasolina. Isso fica a confirmar no seu carro, mas importa: para rodar com etanol, o sistema de combustível precisa ser compatível.

Fontes:
- [Manual de oficina do G4GC (PDF)](https://www.newtonnet.co.uk/coupe/GK-HMA-WorkshopManual/PDF/EM.pdf)
- [Hyundai Beta (Wikipedia)](https://en.wikipedia.org/wiki/Hyundai_Beta_engine)
- [Carros na Web — i30 2.0](https://www.carrosnaweb.com.br/fichadetalhe.asp?codigo=1056)
- [Heycar — i30 2.0 AT 2012](https://heycar.com.br/ficha-tecnica/ficha-tecnica-completa-do-hyundai-i30-2-0-at-2012)
- [Engineswork — G4GC](https://engineswork.com/engines/hyundai-elantra-g4gc-2-0-engine.html)
- [Carro Club — fórum i30, câmbio e embreagem](https://www.carroclub.com.br/carros/hyundai/i30/forum/?categ=Cambio+e+Embreagem)

### 2.6 Outros motores citados no mercado (sem dados conferidos nesta pesquisa)

- **Ford:** Zetec, Sigma, Duratec 2.0, V8 302 (Maverick).
- **Fiat:** Fire 1.0/1.4 e T-Jet, E.torQ 1.6/1.8, GSE turbo.
- **Honda:** séries B e K.
- **Mitsubishi:** 4G63.
- **Renault:** F4R.
- **Swaps:** V8 GM LS.

Ficam para um segundo levantamento, conforme os seus clientes pedirem.

## 3. O que isso muda no Calibra

**O que já funciona hoje:**
- **Qualquer motor:** o núcleo calcula qualquer motor de 3 a 12 cilindros com o diâmetro e o curso informados. Opala e Omega 6 cilindros, Marea e Jetta 5 cilindros, G4GC, AP de qualquer montagem: todos passam pela mesma cadeia.
- **Cilindros ímpares:** o semáforo e as regras da 6.3 já tratam os 5 cilindros. Neles, não existe centelha perdida nem semissequencial.

**O que falta para guiar projetos de verdade (propostas, nada implementado):**

| Proposta | Por quê | Fase sugerida |
|---|---|---|
| **A. Meta pela pressão do turbo** | O preparador pensa assim: "o cliente quer 0,5 bar". O app estimaria a potência, o torque, o bico e a bomba para aquela pressão, fazendo a mesma conta da Garrett ao contrário. Hoje é preciso tentar potências até achar a pressão. | Fase 1.1 (calculadora) |
| **B. Casos de teste de motores do mercado** | Além dos 4 casos da spec: AP 1.6/1.8/1.9, Opala 4.1, Omega 3.0, Marea Turbo, Jetta 2.5, G4GC, 1.0 TSI. O teste confere a cilindrada contra a ficha e roda a cadeia inteira. | Fase 1.1 |
| **C. Catálogo de motores e montagens** | Escolher "AP 2.0" ou "Opala 4.1" preenche diâmetro, curso, biela, taxa e ordem de ignição, cada dado com fonte e "a verificar". Montagens (bloco + virabrequim + pistão + biela) com a cilindrada calculada, porque os nomes populares são ambíguos. | Fase 2 (junto dos outros catálogos) |
| **D. Torque × câmbio** | Comparar o torque estimado com o limite do câmbio cadastrado (com fonte), com aviso no semáforo. O seu i30 é o exemplo. | Fase 2 |
| **E. Geometria da montagem** | Biela, relação biela/curso e taxa de compressão calculada a partir dos volumes (câmara, junta, cabeça do pistão), para strokers e para converter aspirado em turbo. | Fase 2 ou 3 |

A spec não prevê catálogo de motores nem limite de câmbio. Por isso, **C e D mudam a spec e precisam do seu aval**.

## 4. Seu projeto — conta feita com o próprio Calibra

**Premissas:**
- G4GC 82 × 93,5 mm, gasolina E32, lambda 0,80;
- potência máxima a 6.000 rpm;
- ar a 25 °C, intercooler ar-ar (padrões da spec);
- VE 0,90: **chute meu, a confirmar no dinamômetro**.

| Pressão do turbo | Ao nível do mar | A 600 m |
|---|---|---|
| ~0,5 bar | cerca de 210 cv | cerca de 200 cv |
| ~0,6 bar | cerca de 225 cv | cerca de 210–215 cv |

- **Torque:** na rotação de potência, sobe para cerca de **245–260 N·m**. De série são **186 N·m a 4.600 rpm**, um aumento de 30% a 40%. O pico de torque com turbo costuma vir mais baixo no giro e ser maior que isso.
- **Bico:** na faixa de **375–400 cm³/min nominais**, sem tempo morto.
- **Bomba:** cerca de **70–75 L/h**.

**Leitura de mecânico:**
- **Câmbio:** o A4CF1 foi feito para o torque de série e já dá trancos. O fórum do i30 aponta como suspeitos o óleo, o coxim e o corpo de válvulas. Resolver o câmbio, ou converter para manual, antes do turbo é coerente com os números.
- **Taxa e pressão:** com taxa de 10,1:1 e 0,5–0,6 bar, o cuidado com ignição e detonação vale em dobro.

O acerto final continua sendo do preparador.
