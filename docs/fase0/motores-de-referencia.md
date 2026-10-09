# Motores de referência

Três motores para testar a cadeia de cálculo. Cada dado tem fonte; o que não tem fonte confirmada fica como **não verificado**.

Os valores de potência e preparação abaixo são exemplos para testar a planilha, não meta de acerto.

## 1. VW AP 2.0 turbo (8V)

| Item | Valor | Fonte | Status |
|---|---|---|---|
| Cilindrada | 1984 cm³ | — | Não verificado |
| Diâmetro × curso | 82,5 × 92,8 mm | — | Não verificado |
| Taxa de compressão (original) | ~10:1 | — | Não verificado (varia por versão) |
| Ordem de ignição | 1-3-4-2 | — | Não verificado |
| Injeção original | — | — | Preencher |
| Preparação de exemplo | turbo, EHC, 300 hp | Exemplo | — |
| Injetores de exemplo | 1000 cm³/min a 3 bar | Exemplo | Trocar pelo dado de caracterização real |
| Tempo morto | — | Folha do fabricante do injetor | Preencher |
| Turbo / mapa do compressor | — | — | Preencher |

## 2. VW/Audi EA888 2.0 TSI/TFSI (3ª geração)

| Item | Valor | Fonte | Status |
|---|---|---|---|
| Cilindrada | 1984 cm³ | SSP 606 (Audi), p. 6 | Verificado |
| Diâmetro × curso | 82,5 × 92,8 mm | SSP 606 | Verificado |
| Taxa de compressão | 9,6:1 (CNCB, CNCD); 9,3:1 (CJXC) | SSP 606 | Verificado |
| Ordem de ignição | 1-3-4-2 | SSP 606 | Verificado |
| Potência original | 132 kW (CNCB), 165 kW (CNCD), 221 kW (CJXC) | SSP 606 | Verificado |
| Combustível original | RON 95 (CNCB/CNCD), RON 98 (CJXC) | SSP 606 | Verificado |
| Injeção | direta + indireta (MPI) na 3ª geração | SSP 606 | Conferir qual código tem MPI |
| Preparação de exemplo | gasolina C E32, 380 hp | Exemplo | — |
| Injetores de exemplo | 650 cm³/min a 3 bar (indireta) | Exemplo | Preencher |

Material local: `OneDrive\Documentos\Material Automotivo\EA888\` (SSP 606, 436, 645, 384).

Atenção: com injeção direta original, uma injeção programável comum controla só a parte indireta (MPI). Decidir se o app vai tratar o EA888 só com injetores indiretos adicionais.

## 3. V8 5.0 (modelo genérico)

Escolher o motor real (por exemplo Ford Coyote 5.0 ou GM LS) e preencher.

| Item | Valor | Fonte | Status |
|---|---|---|---|
| Motor | — | — | Escolher |
| Cilindrada | 5,0 L (exemplo) | — | Preencher |
| Diâmetro × curso | — | — | Preencher |
| Taxa de compressão | — | — | Preencher |
| Ordem de ignição | — | — | Preencher |
| Preparação de exemplo | gasolina C E32, 500 hp | Exemplo | — |
| Injetores de exemplo | 550 cm³/min a 3 bar | Exemplo | Preencher |

## Campos que toda ficha precisa ter (para o módulo `dominio`)

Cilindrada, número de cilindros, diâmetro, curso, taxa de compressão, ordem de ignição, tipo de comando, combustível, injetores (vazão + pressão de teste + tempo morto por tensão), regulador (referenciado ou não), turbo (mapa do compressor), rotação máxima, e para cada valor: fonte, data e status.
