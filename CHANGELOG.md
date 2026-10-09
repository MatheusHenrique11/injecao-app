# Changelog

Formato: [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versões: semântica (0.x.y).

## [0.0.1] — 08/10/2026 — Fase 0

### Adicionado
- Projeto Maven multimódulo: `calibra-core`, `calibra-ecu`, `calibra-dados`, `calibra-relatorios`, `calibra-app`.
- Janela vazia "Calibra 0.0.1" (JavaFX 25.0.4).
- Testes de fumaça (JUnit 6.1.3) que conferem a regra de dependência: só o `calibra-app` enxerga o JavaFX.
- Documentação: `docs/SPEC.md`, `docs/decisoes.md`, `docs/manual.md`, `CLAUDE.md`, `README.md`.
- Planilha de conferência das fórmulas da spec com os casos golden A a D (`docs/fase0/cadeia-de-calculo.xlsx`).
