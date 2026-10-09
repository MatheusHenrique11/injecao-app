# Injeção (nome provisório)

Aplicativo de apoio ao cálculo de mapa base para injeção programável.

## Módulos

| Módulo | Responsabilidade | Depende de |
|---|---|---|
| `dominio` | Modelos: motor, peças, injetores, combustível, unidades | — |
| `calculo` | Núcleo de cálculo. Sem interface, revisado fórmula por fórmula, com testes | `dominio` |
| `catalogo` | Catálogo versionado (combustíveis, injetores, turbos), com fonte e data em cada item | `dominio` |
| `exportacao` | Saída das tabelas por injeção (nível B: formato do software; nível C: texto/planilha) | `dominio` |
| `persistencia` | Banco de dados e backup diário | `dominio` |
| `app` | Interface JavaFX (telas em FXML, editadas no Scene Builder) | todos |

## Requisitos

- Temurin JDK 25
- Maven 3.9+

## Comandos

```
mvn verify                 # compila e roda os testes
mvn -pl app -am javafx:run # abre o aplicativo
```
