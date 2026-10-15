# Ensaio do instalador 0.2.0 (fase 2, bloco 2.6)

14 e 15/10/2026, Windows 11, no PC de desenvolvimento.

**MSI final:** `dist\Calibra-0.2.0.msi`, 61,9 MB, SHA-256 `5AF0708943C1EC63DA395E89D4535699ED16AFF6B0200FA7F12F84B5686878BB`. Ele foi gerado pelo `scripts\gerar-instalador.ps1`, que roda os testes, monta o Java embutido (15 módulos, sem o cliente HTTP) e confere o pacote antes de gerar (4 de 4 itens OK).

**Dados:** o app instalado rodou com uma pasta de dados de teste (`JAVA_TOOL_OPTIONS=-Dcalibra.pasta=<pasta do ensaio>`), e os testes de interface usam pastas temporárias. Os dados de verdade, em `%LOCALAPPDATA%\Calibra`, não foram tocados: o `calibra.db` tem o mesmo SHA-256 antes e depois (`2F0A8ADD…C667`), e a pasta continua com 64 arquivos.

## Resultado dos 5 testes

| # | Teste | Resultado |
|---|---|---|
| 1 | Instalar e abrir em menos de 5 s | **Passou.** O log grava "Tela aberta em" 2,1 a 3,4 s. Do comando até essa linha aparecer no log: 3,1 a 4,9 s; a exceção foi 5,9 s na primeira abertura depois de instalar, com o disco ainda "frio". |
| 2 | Do cadastro ao orçamento aprovado; recalcular em menos de 1 s | **Passou.** O ensaio `FluxoCompletoUiTest` rodou no Java embutido do MSI instalado. Os recálculos levaram de 46 a 74 ms (pelo Maven: 111 a 187 ms). |
| 3 | Backup, mudar os dados, restaurar: os dados voltam | **Passou,** no mesmo ensaio, pela aba Backup e pelas três janelas da restauração. |
| 4 | Instalar por cima (0.1.99 → 0.2.0) | **Passou.** Ficou uma entrada só em "Aplicativos instalados" ("Calibra 0.2.0"), com os atalhos. O banco de teste não mudou (mesmo SHA-256) e abriu na versão 5. |
| 5 | Desinstalar e reinstalar | **Passou.** Desinstalado: nenhuma entrada, nenhum atalho e nenhuma pasta do programa; a pasta de dados ficou. Reinstalado: abriu com o mesmo banco ("Banco aberto", não "criado"). |

### Teste 2 e 3 em detalhe (no app instalado)

O `scripts\testar-instalado.ps1` roda os 9 testes de interface com o Java embutido, os jars da pasta de instalação e as opções do atalho (`Calibra.cfg`). Os 9 passaram. O resumo do ensaio, como saiu:

```
Java: C:\Users\mathe\AppData\Local\Programs\Calibra\runtime · 25.0.4.1+1-LTS
Calibra 0.2.0 lido de file:/C:/Users/mathe/AppData/Local/Programs/Calibra/app/calibra-app-0.2.0.jar
Recálculos (ms, do clique em Calcular até a tela montada): [53, 52, 46, 69, 57]
Orçamento ORC-2026-0001 · total R$ 2.025,00 · PDF de 3319 bytes
Aprovado pela janela de resposta (WhatsApp); projeto em Aprovado
Backup feito pela tela: calibra-backup-2026-10-15-205148.zip
Depois do backup: revisão 2 do orçamento aberta e cor do veículo = Vermelho
Aviso da restauração: Os dados voltaram a ser os do backup de 15/10/2026 20:51 · feito por você · 21 KB. Os dados de antes foram guardados no backup de segurança: …\calibra-backup-2026-10-15-205149.zip
Backup de segurança (os dados de antes da restauração): calibra-backup-2026-10-15-205149.zip
Depois de restaurar: 1 projeto, orçamento aprovado sem a revisão 2, veículo sem cor (como no backup)
```

O caminho do ensaio, todo pelas telas e com dados fictícios:

1. Configurações: dados da oficina e valor da hora.
2. Catálogo: um bico com tabela de tempo morto e preço de R$ 450,00.
3. Assistente: cliente com consentimento, veículo, motor do caso B e meta de 400 cv.
4. Dimensionamento com o bico do catálogo; cálculo salvo com a justificativa do vermelho.
5. Orçamento:
   - 4 bicos e 2,5 h de mão de obra, com 10% de desconto: R$ 2.025,00;
   - PDF conferido pelo texto, e texto do WhatsApp;
   - marcado como enviado e aprovado pela janela "Aprovado pelo cliente".

A janela de escolher arquivo do Windows não dá para automatizar; o PDF foi gravado pelo mesmo código, sem essa janela. As imagens de cada passo ficam em `calibra-app\target\testar-instalado\target\ensaio-*.png`.

## O que o ensaio e a revisão final acharam

| Achado | O que foi feito |
|---|---|
| As janelas de aviso cortavam o texto com "...": a confirmação da restauração perdia "O que estiver digitado e não salvo nas telas será perdido", e o aviso do fim escondia o caminho do backup de segurança. | **Corrigido** (decisão 155). Todas as janelas de aviso mostram o texto inteiro, e o ensaio confere isso. |
| O MSI deixa instalar uma versão mais antiga por cima (o 0.1.99 substituiu o 0.2.0). | **Documentado** (decisão 161 e manual). Os dados ficam protegidos: a versão mais antiga não abre um banco atualizado por uma mais nova e não mexe em nada. |
| Revisão: trocar o câmbio do veículo não deixava o cálculo nem o orçamento desatualizados. | **Corrigido** (decisão 156), com teste. |
| Revisão: o checksum das sementes (V4) mudaria numa cópia do repositório com quebras de linha CRLF, e a atualização seguinte não abriria os bancos. | **Corrigido** (decisão 157). O número continua o mesmo que os bancos guardam (-451915068), e um teste o fixa. |
| Revisão: com o relógio do PC atrasado, todo backup novo era apagado na hora. | **Corrigido** (decisão 158), com teste. |
| Revisão: falha ao gerar PDF não ia para o log. | **Corrigido** (decisão 159). |
| Revisão: o cálculo roda na thread da tela, e a SPEC 10 pede `Task`. | **Fica para a fase 3** (decisão 160). Hoje leva menos de 0,1 s no app instalado. |

## Como repetir

```
mvn -q install -DskipTests
powershell -ExecutionPolicy Bypass -File scripts\gerar-instalador.ps1
powershell -ExecutionPolicy Bypass -File scripts\gerar-instalador.ps1 -VersaoMsi 0.1.99 -PularTestes
msiexec /i dist\Calibra-0.1.99.msi      (depois abrir uma vez)
msiexec /i dist\Calibra-0.2.0.msi       (por cima)
powershell -ExecutionPolicy Bypass -File scripts\testar-instalado.ps1
```

Para abrir o app instalado sem tocar nos dados de verdade, abra pelo PowerShell com `$env:JAVA_TOOL_OPTIONS='-Dcalibra.pasta=C:\alguma\pasta\de\teste'`.

## O que fica com você (critério de pronto da fase 2)

No app instalado pelo MSI, com os seus dados:

- um projeto real, do cadastro até o orçamento aprovado;
- a restauração de um backup.

Faça um "Fazer backup agora" antes de começar.
