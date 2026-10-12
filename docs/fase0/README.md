# Fase 0 — andamento

Atualizado em 08/10/2026.

| # | Passo | Situação | Onde |
|---|---|---|---|
| 1 | Instalar Temurin 25, Scene Builder 25 e Git; repositório com o projeto Maven | Feito, com os 5 módulos `calibra-*` da SPEC. Repositório privado no GitHub: `MatheusHenrique11/injecao-app` (primeiro envio em 12/10/2026) | raiz do projeto |
| 2 | Manuais das injeções e anotações | FuelTech e Pandoo baixados e anotados. Injepro precisa ser baixado pelo navegador | [injecoes.md](injecoes.md), `manuais/` |
| 3 | Três motores de referência | EA888 preenchido com o SSP 606. AP 2.0 e V8 com campos a preencher | [motores-de-referencia.md](motores-de-referencia.md) |
| 4 | Planilha da cadeia de cálculo + exemplos | Refeita com as fórmulas da SPEC 7.1/7.2 e os golden A a D (inclui o exemplo do plano = golden B e o da Garrett = golden A). Aba Conferencia mostra OK/FALHA com 1% | [cadeia-de-calculo.xlsx](cadeia-de-calculo.xlsx) |
| 5 | Copiar tabela do FTManager e anotar o formato | Precisa do FTManager instalado; roteiro e ficha prontos | [formato-tabelas.md](formato-tabelas.md) |
| 6 | Preços com 5 a 10 oficinas | Roteiro e tabela prontos | [pesquisa-oficinas.md](pesquisa-oficinas.md) |
| 7 | Especificação ANP do etanol e teor na gasolina | E32 verificado. EHC: norma vigente identificada (ANP 907/2022), faltam os números do anexo | [combustiveis.md](combustiveis.md) |
| 8 | Nome do app e marca da oficina | Decisão sua | — |

A planilha é gerada por `gerar-planilha.ps1`. Para regerar depois de mudar valores no script:

```
powershell -ExecutionPolicy Bypass -File docs\fase0\gerar-planilha.ps1
```

Editar direto no Excel/LibreOffice também serve; nesse caso, o script deixa de ser a fonte.
