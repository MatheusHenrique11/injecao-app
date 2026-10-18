# Ensaio do instalador 0.3.0 (fase 3, bloco 3.4)

18/10/2026, Windows 11, no PC da oficina (o mesmo do desenvolvimento).

**MSI final:** `dist\Calibra-0.3.0.msi`, 62,1 MB, SHA-256 `10B04C3EF39577E536630F6C702A04F509B9A936852291D19E91703C9B1CB801`. Gerado pelo `scripts\gerar-instalador.ps1`, que:
- roda os testes;
- monta o Java embutido: 15 módulos, a partir de java.base, java.desktop, java.sql, javafx.controls, javafx.fxml e jdk.localedata, sem o cliente HTTP;
- confere o pacote antes de gerar: 5 de 5 itens OK. O item novo, "Páginas para imprimir", desenhou as 4 páginas do PDF de exemplo em A4 (595 × 841 pontos a 72 DPI) no Java embutido.

**Dados:** o app instalado só abriu pastas de teste. A atualização do banco foi ensaiada numa **cópia** dos dados reais, com o OK do dono, e a cópia foi apagada no fim. Os dados de verdade, em `%LOCALAPPDATA%\Calibra`, não foram tocados: o `calibra.db` tem o mesmo SHA-256 antes e depois (`43D156D1…B749`), e a pasta continua com 68 arquivos.

## Resultado dos 5 testes

| # | Teste | Resultado |
|---|---|---|
| 1 | Instalar por cima (0.2.0 → 0.3.0) | **Passou.** `msiexec /i dist\Calibra-0.3.0.msi /passive` levou 21 s, sem pedir clique (código 0). Ficou uma entrada só em "Aplicativos instalados" ("Calibra 0.3.0"). A pasta do programa ficou só com os jars 0.3.0, e os atalhos do menu Iniciar e da área de trabalho continuam lá. |
| 2 | Abrir em menos de 5 s | **Passou**, a partir da segunda abertura. A primeira depois de instalar, com o banco de teste criado do zero (as 7 migrações e as sementes) e o disco "frio", levou 5,9 s ("Tela aberta em", no log) e 8,0 s do comando até essa linha. Na fase 2, a primeira abertura também foi a exceção (5,9 s). Nas três seguintes: 2,9 a 3,0 s no log e 3,3 a 3,4 s do comando até a linha. |
| 3 | Testes de tela no app instalado | **Passou: 18 de 18**, com o Java embutido e os jars da pasta de instalação (`scripts\testar-instalado.ps1`). Entre eles: a OS da Nova OS à entrega, com a via e o comprovante em PDF, a ficha do veículo, o catálogo de serviços, o projeto de preparação criado pela OS e o ensaio da fase 2 (cadastro, orçamento aprovado, backup e restauração). Os recálculos levaram de 43 a 77 ms. |
| 4 | Atualização do banco numa cópia dos dados reais | **Passou.** Veja os números abaixo. |
| 5 | Dados reais intocados | **Passou.** O mesmo SHA-256 e os mesmos 68 arquivos, antes e depois de tudo. |

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

## A revisão final

O agente de revisão parou logo no começo, por limite de uso da conta. A revisão dos pontos de maior risco foi feita no próprio bloco (decisão 203):
- migrações, backup e restauração;
- anonimização e arquivos;
- pagamento e status;
- a regra de não usar rede;
- o código novo do bloco 3.3.

| Achado | O que foi feito |
|---|---|
| O km (ou as horas) gravado com duas casas aparece com uma na aba Entrada, e a trava dos documentos via mudança onde não havia: a via do cliente ficava presa até salvar de novo. | **Corrigido**, com teste de interface. |
| Faltava teste de que a anonimização apaga o documento pessoal também da OS entregue. | **Teste novo.** O código já fazia certo. |
| Os backups feitos antes da anonimização continuam com os dados do cliente, e os textos livres da OS não mudam. | **Documentado** no manual (item 6.6). Fica para o dono decidir numa fase seguinte. |

## Como repetir

```
powershell -ExecutionPolicy Bypass -File scripts\gerar-instalador.ps1
msiexec /i dist\Calibra-0.3.0.msi /passive
powershell -ExecutionPolicy Bypass -File scripts\testar-instalado.ps1
```

Para abrir o app instalado sem tocar nos dados de verdade, abra pelo PowerShell com `$env:JAVA_TOOL_OPTIONS='-Dcalibra.pasta=C:\alguma\pasta\de\teste'`.

## O que fica com você (critério de pronto da fase 3)

O app instalado já é o 0.3.0. **Na primeira vez que você abrir**, ele faz o backup "antes de atualizar o banco" e atualiza os seus dados para a versão 7, como no ensaio. Depois:

- 3 carros reais pela OS, cada um com entrada, orçamento aprovado, execução e entrega; um deles de preparação, ligado ao projeto;
- imprimir a via do cliente e o comprovante de entrega na sua impressora (ou na "Microsoft Print to PDF");
- ler o texto da via do cliente em Configurações › OS e ajustar, se quiser. É um modelo, não um texto jurídico revisado.
