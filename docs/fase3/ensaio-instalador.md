# Ensaio do instalador 0.3.0 (fase 3, bloco 3.4)

Windows 11, no PC da oficina (o mesmo do desenvolvimento). Ensaio em 18/10/2026; em 19/10/2026, o MSI refeito com as correções da revisão pelo agente (decisões 205 a 212) e os testes no app instalado repetidos.

**MSI final (19/10/2026):** `dist\Calibra-0.3.0.msi`, 62,1 MB, SHA-256 `96CCA4444FD9CA4E261A866ACCB057BE8BACA714E328931EE5FE0643820090E7`. Gerado pelo `scripts\gerar-instalador.ps1`, que:
- roda os testes;
- monta o Java embutido: 15 módulos, a partir de java.base, java.desktop, java.sql, javafx.controls, javafx.fxml e jdk.localedata, sem o cliente HTTP;
- confere o pacote antes de gerar: 5 de 5 itens OK. O item novo, "Páginas para imprimir", desenhou as 4 páginas do PDF de exemplo em A4 (595 × 841 pontos a 72 DPI) no Java embutido.

O MSI do ensaio de 18/10 (SHA-256 `10B04C3E…B801`) foi substituído por este, com a mesma versão 0.3.0.

**Dados:** o app instalado só abriu pastas de teste. A atualização do banco foi ensaiada numa **cópia** dos dados reais, com o OK do dono, e a cópia foi apagada no fim. Os dados de verdade, em `%LOCALAPPDATA%\Calibra`, não foram tocados por nenhum teste (detalhes no fim).

## Resultado dos 5 testes

| # | Teste | Resultado |
|---|---|---|
| 1 | Instalar por cima (0.2.0 → 0.3.0) | **Passou** (18/10). `msiexec /i dist\Calibra-0.3.0.msi /passive` levou 21 s, sem pedir clique (código 0). Ficou uma entrada só em "Aplicativos instalados" ("Calibra 0.3.0"). A pasta do programa ficou só com os jars 0.3.0, e os atalhos do menu Iniciar e da área de trabalho continuam lá. Em 19/10, o MSI final entrou no lugar do de 18/10 (mesma versão: desinstalar e instalar, ver abaixo). |
| 2 | Abrir em menos de 5 s | **Passou**, a partir da segunda abertura. A primeira, com o banco de teste criado do zero (as 7 migrações, as sementes e o backup do dia), leva de 5,9 a 7,0 s ("Tela aberta em", no log); na fase 2 a primeira também foi a exceção. Nas seguintes: 2,9 a 3,0 s em 18/10 e 3,1 a 3,2 s em 19/10 (3,4 a 3,6 s do comando até a linha no log). Logo depois dos testes, com o PC ocupado, chegou a 4,3 s (4,8 s do comando), ainda abaixo de 5 s. |
| 3 | Testes de tela no app instalado | **Passou: 18 de 18**, nos dois dias, com o Java embutido e os jars da pasta de instalação (`scripts\testar-instalado.ps1`). Entre eles: a OS da Nova OS à entrega, com a via e o comprovante em PDF, a ficha do veículo, o catálogo de serviços, o projeto de preparação criado pela OS e o ensaio da fase 2 (cadastro, orçamento aprovado, backup e restauração). Os recálculos levaram de 39 a 77 ms. |
| 4 | Atualização do banco numa cópia dos dados reais | **Passou** (18/10). Veja os números abaixo. |
| 5 | Dados reais intocados | **Passou.** Nenhum teste abriu a pasta real. A única mudança nela foi a primeira abertura do 0.3.0 pelo dono, prevista (ver "Os seus dados"). |

### Teste 4 em detalhe: a atualização na cópia

O programa de contagem só mostra números, nunca nome de cliente, e recusa a pasta de dados real.

1. **A cópia:** a pasta `%LOCALAPPDATA%\Calibra` foi copiada para uma pasta temporária: 68 arquivos, banco com o mesmo SHA-256. A pasta de backup configurada era a padrão, então o ensaio não gravou nada fora da cópia.
2. **Antes (versão 5):**
   - clientes, veículos, projetos e orçamentos: 0;
   - catálogo: 6 combustíveis e 25 motores e montagens (as sementes), com 31 versões;
   - nenhuma configuração salva e nenhum cálculo.
3. **Abrir o 0.3.0 instalado na cópia:**
   - primeiro o backup "antes de atualizar o banco", na pasta de backups da própria cópia;
   - depois as migrações V6 e V7, em 46 ms ("Banco migrado de v5 até a v7");
   - a tela abriu em 6,1 s, contando o backup e a atualização, e o app fechou normalmente.
4. **Depois (versão 7):**
   - o mesmo catálogo (6 + 25, com 31 versões), as mesmas configurações e cálculos;
   - 0 OS, porque não havia orçamento;
   - nenhum orçamento sem OS e nenhum status de OS diferente do esperado;
   - a conferência do SQLite deu "ok", sem chave estrangeira quebrada;
   - os 5 backups antigos continuaram, mais o novo.
5. **Fim:** a cópia foi apagada.

Os dados reais ainda não têm cliente nem orçamento. Por isso, a regra "cada projeto com orçamento ganha uma OS" ficou conferida pelo `MigracaoV6Test`, com dados fictícios (5 projetos com orçamentos em todas as situações), e não pela cópia.

### 19/10: o MSI final no lugar do de 18/10

- **Trocar o instalado:** os dois MSI têm a mesma versão e o mesmo código de produto, então o Windows não instala um por cima do outro. O de 18/10 foi desinstalado (`msiexec /x`, 9 s, código 0) e o final instalado (`msiexec /i … /passive`, 24 s, código 0). A pasta de dados ficou onde estava. Ficou uma entrada só ("Calibra 0.3.0", de 19/10), só os jars 0.3.0, o jar do app igual ao do build e os dois atalhos.
- **Fechar pelo X:** o `Calibra.exe` que o atalho abre é o lançador; o Java e a janela ficam num segundo `Calibra.exe`. O script do ensaio pegava o primeiro da lista e às vezes mandava o "fechar" para o lançador, que não tem janela, e o app só saía à força. Corrigido o script (ele não fica no repositório): fechando pela janela, as 4 aberturas seguidas terminaram com "Calibra fechando." no log.

## Os seus dados

- **Até o fim do ensaio (18/10):** o mesmo SHA-256 (`43D156D1…B749`) e os mesmos 68 arquivos do começo.
- **18/10, 20:52:** o 0.3.0 instalado foi aberto com os dados de verdade e fechado 17 s depois. O log do app mostra o caminho previsto: o backup "antes de atualizar o banco" (`calibra-backup-2026-10-18-205227.zip`), as migrações V6 e V7 em 28 ms, a tela em 3,5 s e o fechamento normal. Nesse horário não rodava nenhum teste: eu esperava a revisão, e o agente só lia o código no Git. O banco real está na versão 7 desde então.
- **19/10, durante a troca do MSI e os testes:** o banco ficou com o mesmo SHA-256 do começo do dia (`2F138B6F…DC11`) e a pasta com os mesmos 69 arquivos; a última mudança nela continua sendo a de 18/10, às 20:52:44.

## As revisões

**18/10, no próprio bloco (decisão 203):** o agente de revisão parou logo no começo, por limite de uso da conta, e os pontos de maior risco foram revisados no bloco: migrações, backup e restauração, anonimização e arquivos, pagamento e status, a regra de não usar rede e o código novo do bloco 3.3.

| Achado | O que foi feito |
|---|---|
| O km (ou as horas) gravado com duas casas aparece com uma na aba Entrada, e a trava dos documentos via mudança onde não havia: a via do cliente ficava presa até salvar de novo. | **Corrigido**, com teste de interface. |
| Faltava teste de que a anonimização apaga o documento pessoal também da OS entregue. | **Teste novo.** O código já fazia certo. |
| Os backups feitos antes da anonimização continuam com os dados do cliente, e os textos livres da OS não mudam. | **Documentado** no manual (item 6.6). Fica para o dono decidir numa fase seguinte. |

**18/10, pelo agente (decisões 205 a 212), pedida pelo dono:** 15 achados, todos conferidos no código.

| Achado | O que foi feito |
|---|---|
| A aba do projeto juntava os orçamentos de todas as OS e travava a segunda etapa da preparação. | **Corrigido:** mostra o da OS em andamento; o próximo abre outra OS. O orçamento da OS entregue ou cancelada fica só para ver. |
| Mudança não salva na aba Entrada se perdia na entrega, e depois a via ficava presa. | **Corrigido:** entregar e cancelar pedem para salvar antes; a OS finalizada não fica presa. |
| Corrigir só a vistoria regravava o km antigo no veículo. | **Corrigido.** |
| A Nova OS em preenchimento sumia ao abrir outra OS (contra a decisão 183). | **Corrigido.** |
| O recebido a mais ficava escondido atrás do "Pago". | **Corrigido:** aparece na OS, no comprovante e no texto do WhatsApp. |
| Garantia sem limite (um 9000 no lugar de 90 ficaria gravado). | **Corrigido:** até 3650 dias. |
| Depois da anonimização, a placa voltava pela edição, e o cliente ainda ganhava OS nova pelo projeto e documento pessoal novo. | **Corrigido.** |
| Arquivo de documento aberto em outro programa não saía da pasta e ficava para sempre. | **Corrigido:** avisado na hora e apagado na próxima abertura do app. |
| O backup falhava se um anexo fosse removido no meio, e levava cópias pela metade. | **Corrigido.** |
| O tamanho dos backups (todos os anexos em cada um), a cópia de fotos grandes travando a tela, o histórico das correções da OS e o nome dos arquivos que ficam na anonimização. | **Para o dono decidir** (decisão 211), com uma proposta para cada. |
| Na atualização para a 0.3.0, a OS de um orçamento aprovado com revisão nova em rascunho nasce "Aberta". | **Documentado** (decisão 212). Não afeta os dados deste PC. |

Cada correção tem teste; depois delas, os 741 testes do `mvn -q verify` e os 18 de tela passaram.

## Como repetir

```
powershell -ExecutionPolicy Bypass -File scripts\gerar-instalador.ps1
msiexec /i dist\Calibra-0.3.0.msi /passive
powershell -ExecutionPolicy Bypass -File scripts\testar-instalado.ps1
```

Com a mesma versão já instalada, desinstale antes (Configurações do Windows → Aplicativos, ou `msiexec /x` com o código do produto). Para abrir o app instalado sem tocar nos dados de verdade, abra pelo PowerShell com `$env:JAVA_TOOL_OPTIONS='-Dcalibra.pasta=C:\alguma\pasta\de\teste'`.

## O que fica com você (critério de pronto da fase 3)

O app instalado é o 0.3.0 final, e os seus dados já estão na versão 7 (você abriu o app em 18/10). Falta:

- 3 carros reais pela OS, cada um com entrada, orçamento aprovado, execução e entrega; um deles de preparação, ligado ao projeto;
- imprimir a via do cliente e o comprovante de entrega na sua impressora (ou na "Microsoft Print to PDF");
- ler o texto da via do cliente em Configurações › OS e ajustar, se quiser. É um modelo, não um texto jurídico revisado;
- decidir os 4 pontos da decisão 211.
