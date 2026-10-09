# Calibra (nome provisório)

App desktop para Windows, 100% offline, para oficinas de preparação. Ele dimensiona as peças, faz o orçamento e gera o mapa base conservador de injeção programável para motores 4 tempos de 3 a 12 cilindros. O acerto fino continua com o calibrador.

A especificação completa está em [docs/SPEC.md](docs/SPEC.md).

## Situação

Fase 0 (preparação): estrutura vazia que compila e testa. Ainda não há funções de produto.

## Módulos

| Módulo | O que faz | Depende de |
|---|---|---|
| `calibra-core` | Cálculo puro (sem JavaFX, JDBC ou arquivos) | — |
| `calibra-ecu` | Perfis de injeção; exportar e importar tabelas | core |
| `calibra-dados` | SQLite, migrações, anexos, backup | core |
| `calibra-relatorios` | PDFs | core |
| `calibra-app` | Telas JavaFX e casos de uso | todos |

## Requisitos

- Eclipse Temurin 25 LTS (`java -version` → 25.0.x)
- Apache Maven 3.9.x (`mvn -v`)
- Git
- WiX Toolset (só a partir da fase 2, para o instalador)

## Comandos

```
mvn -q verify                          # compila e roda os testes
mvn -q install -DskipTests             # instala os módulos no repositório local
mvn -q -pl calibra-app javafx:run      # abre o app (depois do install)
```

## Documentação

- [docs/SPEC.md](docs/SPEC.md): especificação
- [docs/decisoes.md](docs/decisoes.md): decisões técnicas
- [docs/manual.md](docs/manual.md): manual do usuário
- [docs/fase0/](docs/fase0/): levantamentos da fase 0 (injeções, combustíveis, motores, planilha de conferência)
- [CHANGELOG.md](CHANGELOG.md)
