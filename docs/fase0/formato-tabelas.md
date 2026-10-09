# Formato das tabelas copiadas dos softwares de injeção

Passo a passo (repetir para cada software disponível):

1. Abrir um mapa qualquer no software (FTManager, ECU Manager da Pandoo, software da Injepro).
2. Abrir a tabela principal de injeção, selecionar tudo e copiar (Ctrl+C).
3. Colar no Bloco de Notas e salvar em `docs/fase0/amostras/<software>-<versão>-<tabela>.txt`.
4. Repetir copiando só uma parte da tabela e, se houver opção, com e sem os eixos.
5. Preencher a ficha abaixo.

## Ficha

| Pergunta | FTManager | Pandoo | Injepro |
|---|---|---|---|
| Versão do software | | | |
| Tabela copiada | | | |
| Separador entre colunas (tab, ponto e vírgula, espaço) | | | |
| Separador decimal (vírgula ou ponto) | | | |
| Casas decimais | | | |
| Copia os eixos junto? Onde aparecem (1ª linha, 1ª coluna) | | | |
| Unidade dos valores (ms, %, VE) | | | |
| Unidade do eixo de carga (bar, kPa, psi, % TPS) | | | |
| Ordem das linhas (carga crescendo para baixo ou para cima) | | | |
| Colar texto editado de volta funciona? | | | |
| Colar com tamanho diferente da tabela: o que acontece? | | | |

Isto define o nível B do módulo `exportacao`. O nível C (texto/planilha para digitação manual) não depende deste formato.
