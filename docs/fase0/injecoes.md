# Injeções programáveis — anotações da fase 0

Levantado em 08/10/2026. Os PDFs ficam em `manuais/` (fora do Git).

## FuelTech FT450 / FT550 / FT550 LITE / FT600

Fonte: manual oficial em inglês, versão 2.9 (`manuais/FuelTech_FT450_FT550_FT600_EN.pdf`), baixado de fueltech.net/pages/manuals. A versão em português não está no mesmo caminho (deu 404); baixar pelo site da FuelTech BR.

**Saídas (pinagem do manual, seções 4.1 a 4.3)**

| Modelo | Azuis | Cinzas | Amarelas | Total |
|---|---|---|---|---|
| FT450 | 6 | 4 | — | 10 |
| FT550 / LITE | 12 | 8 | 4 | 24 |
| FT600 | 16 | 8 | 8 | 32 |

- Azul: injetor, relé, solenoide. Aciona só pelo negativo, 5 A por saída. Não serve para ignição.
- Cinza: ignição por padrão; pode ser injetor (exige Peak and Hold) ou auxiliar. 1 A.
- Amarela: push-pull, para borboleta eletrônica, motor de passo ou ignição acionada por 12 V. Como injetor, exige Peak and Hold.
- Limite de injetores de alta impedância (>10 Ω) somando todas as azuis: 8 na FT450, 24 na FT550/FT600. Entre 7 e 10 Ω: 6 e 16.
- FT450 pode usar a azul 6 como 5ª saída de ignição.

**Eixos da tabela principal de injeção (seção 17.1)**

- Modo básico (padrão): tabela 2D de MAP × tempo de injeção, ou TPS × tempo.
- Modo avançado: tabela 3D de MAP (ou TPS) × RPM, até 32 × 32 células.
- MAP vai de −14,5 psi (−1,0 bar) até a pressão escolhida. TPS de 0 a 100 % em passos de 10 %.
- No FTManager os pontos dos eixos são editáveis ("Edit axis"), até 32 pontos de RPM.

**Modelo de combustível**

- A tabela guarda **tempo de injeção em ms**, não VE nem massa. Isso define o que o app tem de exportar: tempo em ms já com o tempo morto incluído.
- O mapa base da FuelTech pede: vazão total dos injetores (lb/h, soma da bancada), taxa de compressão (baixa/média/alta), combustível (Pump gas, Race gas, E85, Ethanol) e perfil de comando.
- Tempo morto padrão: 1,00 ms para alta impedância; 0,60 ms para baixa impedância com Peak and Hold. O próprio manual diz que são valores genéricos.
- Correções percentuais (overall trim etc.) são aplicadas descontando o tempo morto.
- Flex fuel: há menu de identificação de teor de etanol (sensor).

**Importar e exportar tabelas**

- Na ECU e no FTManager: Ctrl+C e Ctrl+V nas tabelas; "Copy map to another file"; "Import settings" de outro mapa; datalog exporta texto e Excel.
- **A conferir no passo do FTManager:** o formato exato do que vai para a área de transferência (separador, vírgula decimal, se inclui os eixos). Ver [formato-tabelas.md](formato-tabelas.md).

## Pandoo

Fonte: manuais oficiais em update.pandoo.com.br (`manuais/Pandoo_*.pdf`): EFI-4, Box Inject v0.58 e Pulser Plus v0.08.

- EFI-4: até 5 saídas de ignição (A a E), 6 fios de saída auxiliar (máx. 0,5 A cada, sempre via relé). Tem configuração de tempo morto dos bicos.
- Pulser Plus: 2 saídas auxiliares configuráveis (padrão: bomba e ventoinha); vazão dos bicos escolhida por bancada; tem tempo morto.
- Box Inject: 1 a 8 cilindros.
- **Pendente:** eixos da tabela e forma de importar/exportar. Os manuais não falam em copiar e colar; conferir no software ECU Manager da Pandoo.

## Injepro

- Manual S2000 oficial: https://www.injepro.com/wt_admin/uploads/Manual%20S2000.pdf
- **Pendente:** o servidor da Injepro fechou a conexão no download automático. Baixar pelo navegador na página de downloads (injepro.com/downloads) e preencher: saídas, eixos, modelo de combustível, importação.
- Pelo resumo da busca, a S2000 atende de 1 a 4 cilindros, com ignição sequencial até 2 cilindros. Confirmar no manual.

## Resumo para o módulo `exportacao`

| Injeção | Unidade da tabela | Eixos | Copiar/colar | Status |
|---|---|---|---|---|
| FuelTech FT450/550/600 | ms | MAP ou TPS × RPM, até 32×32 | Sim (Ctrl+C/V) | Formato do texto a conferir |
| Pandoo | a conferir | a conferir | a conferir | Pendente |
| Injepro | a conferir | a conferir | a conferir | Baixar manual |
