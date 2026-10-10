# Manual do usuário — Calibra

Versão do app: 0.1.1 (fase 1.1). Este manual cresce a cada fase.

## 1. O que o Calibra faz

Transforma a meta do cliente (motor, potência, uso, combustível e injeção) em peças dimensionadas, orçamento, mapa base conservador e relatório final. O acerto fino continua sendo feito pelo calibrador, no software da própria injeção. O Calibra nunca grava nada na injeção e funciona sem internet.

## 2. Instalação

O instalador (MSI) chega na fase 2. Até lá, o app abre pelo terminal, na pasta do projeto:

```
mvn -q install -DskipTests
mvn -q -pl calibra-app javafx:run
```

## 3. Onde ficam os dados

`%LOCALAPPDATA%\Calibra\`: banco de dados, anexos, backups e logs (a partir da fase 2). Desinstalar o app não apaga essa pasta. Na fase 1 só existe `cache\`, com o cache de fontes usado para gerar os PDFs.

## 4. Calculadora

A calculadora dimensiona as peças para uma meta de potência e gera o PDF da memória de cálculo. Ela não guarda nada: para refazer uma conta, preencha de novo (ou guarde o PDF).

### 4.1 Aba "1. Motor"

- **Número de cilindros:** de 3 a 12. Ao trocar, o app sugere de novo a ordem de ignição, os ângulos dos PMS e o arranjo. Se você já tinha editado esses campos, ele pergunta antes. **As sugestões precisam ser conferidas no manual do motor.**
- **Ordem de ignição:** números separados por traço (ex.: `1-3-4-2`). Começa sempre no cilindro 1.
- **Ângulos dos PMS:** um por cilindro, na ordem de ignição, separados por ponto e vírgula (ex.: `0; 180; 360; 540`). Devem crescer, de 0 a 719°.
- **Modos de injeção e ignição:** com número ímpar de cilindros, só aparecem os modos possíveis (centelha perdida e semissequencial precisam de pares de cilindros a 360°). Injeção ou ignição sequencial exigem sensor de fase.
- **Combustível:** os combustíveis da spec, com AFR, densidade e vigência (a gasolina comum está E32 desde 01/08/2026). A lambda alvo da outra aba é preenchida pelo combustível e pela aspiração.
- **Conferir motor:** mostra os erros (que bloqueiam o cálculo) e os avisos amarelos (que só pedem atenção).

### 4.2 Aba "2. Meta e peças"

- **Meta:** potência em cv, medida no virabrequim ou na roda. Na roda, informe a perda de transmissão; o app converte: virabrequim = roda ÷ (1 − perda).
- **VE na potência máxima:** sem padrão, você informa. Use fração (0,95 = 95%). Referência a validar: 4 válvulas 0,95 a 0,99; 2 válvulas 0,80 a 0,95.
- **Pressão de base do combustível:** diferencial no bico, com regulador referenciado ao coletor. Sem padrão.
- **Parâmetros:** já vêm com os padrões da spec (k_ar 10,5, DC máximo 80%, eficiências do compressor e do intercooler, perdas de 2 e 1 psi). Só mude se souber por quê.
- **k_ar (cv por lb/min de ar):** diz quanta potência o motor tira de cada lb/min de ar. O padrão de 10,5 serve para motores modernos de 4 válvulas. Motor antigo de 2 válvulas (AP, Opala, Omega, Família II 8V) costuma ficar perto de 8,5 a 9,5: a referência vem dos dados de fábrica do Omega 4.1 e 3.0 e ainda precisa ser validada no dinamômetro. k_ar menor = mais ar e mais combustível para a mesma potência, ou seja, bico e bomba maiores. O app aceita de 8,0 a 11.
- **Bico (opcional):** com o bico preenchido, o app calcula o tempo de injeção, o DC com tempo morto e a vazão mínima que mantém o DC em 80%.
- **Marcha lenta (opcional):** com MAP e VE de marcha lenta, o app avisa se o bico fica grande demais para a lenta (tempo efetivo abaixo de 1,0 ms) e sugere duas bancadas.
- **Bomba, sensor MAP e injeção (opcionais):** entram no semáforo. A vazão da bomba deve ser lida na curva do fabricante na **pressão exigida** que aparece no resultado.

**Números:** use vírgula para decimais (`3,5`). O ponto é separador de milhar (`1.050` = mil e cinquenta). O app recusa `3.5` para não virar 35 sem ninguém perceber.

### 4.3 Calcular e ler o resultado

Clique em **Calcular** (ou tecle Enter). A aba "3. Resultado" mostra:

1. **Semáforo:** verde, amarelo ou vermelho, sempre com a cor escrita e a explicação.
2. **Resultados** em várias unidades (ex.: 241,0 kPa · 2,41 bar · 34,95 psia).
3. **Recomendações:** bico, bomba, sensor MAP, saídas da injeção, intercooler e radiador de óleo.
4. **Avisos.**
5. **Memória de cálculo:** cada conta, com fórmula, entradas e resultado.

Se você mudar qualquer campo depois de calcular, o resultado fica marcado como **desatualizado** e o PDF é bloqueado até calcular de novo.

### 4.4 PDF da memória de cálculo

Clique em **Gerar PDF da memória…**. Antes, escreva a justificativa de cada item vermelho, no campo que aparece embaixo dele. Sem isso, o PDF não sai. O PDF leva: dados do motor e da meta, semáforo com as justificativas (com data e hora), resultados, recomendações, avisos e a memória completa, com a versão do app e do núcleo de cálculo.

## 5. Clientes, veículos, motores e projetos

*(fase 2)*

## 6. Orçamento

*(fase 2)*

## 7. Mapa base

*(fase 3)*

## 8. Acerto, relatório e biblioteca

*(fase 4)*

## 9. Backup e restauração

*(fase 2)*

## 10. Semáforo de segurança

Cada indicador importante sai em verde, amarelo ou vermelho. **Vermelho só segue com justificativa escrita**, gravada com data e hora.

| Indicador | Verde | Amarelo | Vermelho |
|---|---|---|---|
| DC do bico (com tempo morto) na potência máxima | até 80% | 80% a 85% | acima de 85% |
| Folga da bomba | 30% ou mais | 15% a 30% | menos de 15% |
| Mapa do compressor | — | sempre amarelo na fase 1 (sem mapa cadastrado): confira o ponto no mapa do fabricante | — |
| BMEP (aspirado 13 bar; sobrealimentado 22 bar) | até a referência | até 15% acima | mais de 15% acima com pistões ou bielas originais (com os dois forjados, amarelo) |
| Velocidade média do pistão | até 20 m/s | 20 a 25 m/s | acima de 25 m/s |
| Saídas da injeção | suficientes | — | insuficientes |
| Faixa do sensor MAP | 1,2 × o MAP máximo ou mais | 1,0 a 1,2 × | abaixo do MAP máximo |
| Aspirado: MAP necessário ÷ pressão atmosférica local | até 0,90 | 0,90 a 0,98 | acima de 0,98 |

Os resultados são **estimativas** para dimensionar peças e preparar o mapa base. O acerto final é do calibrador, com wideband, no dinamômetro, na rua ou na água.
