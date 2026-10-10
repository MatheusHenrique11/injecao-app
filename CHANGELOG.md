# Changelog

Formato: [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versões: semântica (0.x.y).

## [0.1.0] — 09/10/2026 — Fase 1

### Adicionado
- **Núcleo de cálculo** (`calibra-core`):
  - todas as fórmulas da SPEC 7.1, cada uma com teste;
  - cadeia de dimensionamento com iteração MAP ↔ Tm;
  - bico necessário e bico informado (TI, DC com tempo morto, vazão mínima, duas bancadas, marcha lenta);
  - bomba, esforço (BMEP, torque, velocidade do pistão), saídas exigidas, sensor MAP, ponto de operação do compressor e recomendações.
- Combustíveis da SPEC 7.2 (E32 vigente desde 01/08/2026, E25, etanol hidratado, anidro, E85, metanol), com vigência, fonte e versão.
- Configuração e validação do motor de 3 a 12 cilindros (SPEC 6.3), com sugestões de ordem de ignição e ângulos marcadas "confirmar no manual do motor".
- Semáforo da SPEC 7.4, com justificativa obrigatória para vermelho.
- Memória de cálculo com fórmula, entradas, resultado, avisos e versão do núcleo (1.0.0).
- Escolha de bico, bomba e injeção em catálogos (SPEC 7.3), pronta no núcleo para a fase 2.
- PDF da memória de cálculo (`calibra-relatorios`, PDFBox 3.0.8).
- Tela da calculadora (`calibra-app`, FXML): motor, meta e peças, resultado com semáforo, justificativas e geração do PDF.
- **Testes:** golden A–D (1%), coerência, 3 a 12 cilindros e zero rede (regra 2.1), mais um teste de interface opcional (tag `ui`).

### Fica para depois
- Mapa do compressor e escolha de turbo com mapa (fase 2, com o digitalizador).
- Catálogos editáveis, banco de dados, instalador, módulos Java (JPMS) e logs em arquivo (fase 2).

## [0.0.1] — 08/10/2026 — Fase 0

### Adicionado
- Projeto Maven multimódulo: `calibra-core`, `calibra-ecu`, `calibra-dados`, `calibra-relatorios`, `calibra-app`.
- Janela vazia "Calibra 0.0.1" (JavaFX 25.0.4).
- Testes de fumaça (JUnit 6.1.3) que conferem a regra de dependência: só o `calibra-app` enxerga o JavaFX.
- Documentação: `docs/SPEC.md`, `docs/decisoes.md`, `docs/manual.md`, `CLAUDE.md`, `README.md`.
- Planilha de conferência das fórmulas da spec com os casos golden A a D (`docs/fase0/cadeia-de-calculo.xlsx`).
