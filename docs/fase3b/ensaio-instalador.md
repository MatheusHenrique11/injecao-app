# Ensaio do instalador 0.3.1 (Fase 3B, bloco 3B.7)

Windows 11, no PC da oficina (o mesmo do desenvolvimento), em 21/10/2026. O dono liberou:

- instalar o 0.3.1 por cima do 0.3.0;
- rodar os testes de tela no app instalado, com pastas de teste;
- ensaiar a atualização do banco numa **cópia** dos dados reais, só com números, apagando a cópia no fim.

**MSI:** `dist\Calibra-0.3.1.msi`, 62,4 MB, SHA-256 `ADBE60A05FF1B45BD0FBD4A249C36A1652F9913FA20E57B8712406238D3B01AE`. Gerado pelo `scripts\gerar-instalador.ps1`, que:
- roda os testes (894 no `mvn -q verify`, todos passando);
- monta o Java embutido: 15 módulos, a partir de java.base, java.desktop, java.sql, javafx.controls, javafx.fxml e jdk.localedata, sem o cliente HTTP;
- confere o pacote antes de gerar: 5 de 5 itens OK. O do banco agora diz "banco na versão 13".

Antes do MSI, os 29 testes de tela passaram no código, inclusive o `FluxoCompletoUiTest`.

**Dados:** o app instalado só abriu pastas de teste e a cópia. Os dados de verdade, em `%LOCALAPPDATA%\Calibra`, não foram tocados (detalhes no fim).

## Resultado dos 5 testes

| # | Teste | Resultado |
|---|---|---|
| 1 | Instalar por cima (0.3.0 → 0.3.1) | **Passou.** `msiexec /i dist\Calibra-0.3.1.msi /passive` levou 11 s, sem pedir clique (código 0). Ficou uma entrada só em "Aplicativos instalados" ("Calibra 0.3.1"). A pasta do programa ficou só com os jars 0.3.1 (16 jars), o jar do app igual ao do build, e os atalhos do menu Iniciar e da área de trabalho continuam lá. |
| 2 | Abrir em menos de 5 s | **Passou**, também na primeira abertura. Numa pasta de teste vazia: 3,5 s na primeira, criando o banco do zero com as 13 migrações e o backup do dia (4,8 s do comando até a linha "Tela aberta em", no log); 2,0 a 2,1 s nas três seguintes (2,3 a 2,5 s do comando). As 4 fecharam pelo X, com "Calibra fechando." no log. |
| 3 | Testes de tela no app instalado | **Passou: 29 de 29**, com o Java embutido e os jars da pasta de instalação (`scripts\testar-instalado.ps1`). Entre os 11 novos da fase: o checklist na execução, o laudo, as instalações, a ficha do chicote, o serviço de bancada sem o veículo, a garantia própria e os kits. Os recálculos levaram de 37 a 67 ms. |
| 4 | Atualização do banco numa cópia dos dados reais | **Passou.** Veja os números abaixo. |
| 5 | Dados reais intocados | **Passou.** O mesmo SHA-256 e os mesmos 69 arquivos do começo ao fim (ver "Os seus dados"). |

### Teste 4 em detalhe: a atualização na cópia

O programa de contagem só mostra números, nunca nome de cliente, e recusa a pasta de dados real.

1. **A cópia:** a pasta `%LOCALAPPDATA%\Calibra` foi copiada para uma pasta temporária: 69 arquivos, banco com o mesmo SHA-256. A pasta de backup configurada era a padrão, então o ensaio não gravou nada fora da cópia.
2. **Antes (versão 7):**
   - clientes, veículos, projetos, orçamentos, OS, recebimentos, anexos e mecânicos: 0;
   - catálogo: 6 combustíveis e 25 motores e montagens (as sementes), com 31 versões;
   - nenhuma configuração salva e nenhum cálculo;
   - 20 tabelas e 16 gatilhos.
3. **Abrir o 0.3.1 instalado na cópia:**
   - primeiro o backup "antes de atualizar o banco", na pasta de backups da própria cópia: formato 2, app 0.3.1, banco na versão 7;
   - depois as migrações V8 a V13, em 55 ms ("Banco migrado de v7 até a v13");
   - a tela abriu em 3,3 s, contando o backup e a atualização, e o app fechou pelo X normalmente, sem nenhum erro no log.
4. **Depois (versão 13):**
   - 13 migrações registradas, nenhuma com falha;
   - o mesmo catálogo, mais os 7 modelos de checklist da V9 (38 versões);
   - as mesmas configurações e cálculos;
   - as tabelas novas vazias (checklists da OS, laudos, instalações e fichas do chicote) e nenhuma OS com "quem retirou";
   - 25 tabelas e 36 gatilhos;
   - a conferência do SQLite deu "ok", sem chave estrangeira quebrada;
   - os 6 backups antigos continuaram, mais o novo.
5. **Fim:** a cópia e a pasta de teste foram apagadas.

Os dados reais ainda não têm cliente nem OS. Por isso, as migrações com dados foram conferidas pelos testes, com dados fictícios:
- o da V8 sobe um banco na versão 7, com uma OS, até a 13;
- o novo da revisão final (decisão 274) restaura um backup do 0.3.0 (banco na 7, formato antigo) e sobe até a 13.

## Os seus dados

Do começo ao fim do ensaio, o banco teve o mesmo SHA-256 (`2F138B6F…DC11`), e a pasta, os mesmos 69 arquivos. A última mudança nela continua sendo a de 18/10, às 20:52:44 (a primeira abertura do 0.3.0). O banco real continua na versão 7: ele só sobe para a 13 quando **você** abrir o 0.3.1, com o backup "antes de atualizar o banco" antes.

## A revisão final

Feita por mim, nos pontos de risco (decisões 274 a 276).

| Ponto | O que foi feito |
|---|---|
| As migrações V8 a V13 sobre um banco antigo | Cada uma já tinha teste. **Teste novo:** o backup do 0.3.0 (banco na 7, com as fotos dentro do zip) restaura no 0.3.1 e sobe até a 13. |
| Backup e restauração com as tabelas novas | Elas vão no próprio banco. A pasta `anexos-dos-backups` é limpa quando o backup sai dos 30. |
| Sem rede e sem gravar na injeção | Nenhum `pom.xml` nem script mudou na fase. O código novo não usa rede, USB nem porta serial. |
| O "quem retirou" na anonimização | Ele fica, porque a OS entregue não muda. **Documentado** no manual (item 6.6) e **para o dono decidir** (decisão 275). |
| A revisão aprovada com a OS pronta | A entrega não confere o checklist dos serviços novos. **Para o dono decidir** (decisão 275). Até lá, use "Retomar o serviço" antes de entregar. |
| O jeito de voltar para a versão anterior, no manual | Estava errado: a versão antiga não abre dados atualizados nem restaura os backups da nova. **Corrigido** no manual (item 2). O caminho de volta é o backup feito na versão antiga antes de atualizar (decisão 276). |

## Versão 0.3.2 (21/10/2026, à noite)

Ela traz as duas correções da decisão 275 (decisões 278 a 280): a trava da entrega e o "quem retirou" na anonimização, com a migração V14. O ensaio foi o mesmo, com o mesmo OK do dono.

**MSI:** `dist\Calibra-0.3.2.msi`, 62,4 MB, SHA-256 `828BC4A534D5A65A756E7941242336BA43DA292BE9209E01CF705E5FB78CDB74`. Antes dele, passaram os 898 testes do `mvn -q verify` e os 29 de tela no código. A conferência do pacote deu 5 de 5, com "banco na versão 14".

| # | Teste | Resultado |
|---|---|---|
| 1 | Instalar por cima (0.3.1 → 0.3.2) | **Passou.** 11 s, código 0. Uma entrada só ("Calibra 0.3.2"), só os jars 0.3.2 (16 jars), o jar do app igual ao do build e os dois atalhos. |
| 2 | Abrir em menos de 5 s | **Passou.** 3,3 s na primeira, com o banco criado do zero (as 14 migrações); 2,0 a 2,1 s nas três seguintes (2,3 a 2,5 s do comando). As 4 fecharam pelo X. |
| 3 | Testes de tela no app instalado | **Passou: 29 de 29.** O do checklist agora também confere a entrega travada pela revisão aprovada depois do pronto. Os recálculos levaram de 39 a 55 ms. |
| 4 | Atualização do banco numa cópia dos dados reais | **Passou.** Da versão 7 para a 14 (detalhes abaixo). |
| 5 | Dados reais intocados | **Passou.** O mesmo SHA-256 (`2F138B6F…DC11`) e os mesmos 69 arquivos; a última mudança continua sendo a de 18/10, às 20:52:44. |

**A cópia, de novo:** 69 arquivos, com o banco igual ao real.
- **Antes:** versão 7, com os mesmos números do ensaio do 0.3.1.
- **Ao abrir o 0.3.2 instalado:**
  - o backup "antes de atualizar o banco";
  - as migrações V8 a V14 ("Banco migrado de v7 até a v14");
  - a tela em 3,1 s e o fechamento pelo X.
- **Depois:**
  - versão 14, com 14 migrações e nenhuma falha;
  - os 7 modelos de checklist no catálogo (38 versões);
  - 25 tabelas e 36 gatilhos (a V14 refaz um, sem criar outro);
  - SQLite "ok", sem chave estrangeira quebrada;
  - 7 backups (os 6 antigos e o novo);
  - nenhum erro no log.

A cópia e a pasta de teste foram apagadas no fim.

O app instalado agora é o 0.3.2. Os seus dados sobem da versão 7 para a 14 quando você abrir o app, com o backup antes.

## Como repetir

```
powershell -ExecutionPolicy Bypass -File scripts\gerar-instalador.ps1
msiexec /i dist\Calibra-0.3.2.msi /passive
mvn -q install -DskipTests
powershell -ExecutionPolicy Bypass -File scripts\testar-instalado.ps1
```

Para abrir o app instalado sem tocar nos dados de verdade, abra pelo PowerShell com `$env:JAVA_TOOL_OPTIONS='-Dcalibra.pasta=C:\alguma\pasta\de\teste'`. Os programas do ensaio ficam fora do repositório, como no 0.3.0:
- a contagem, que só mostra números e recusa a pasta real;
- a abertura, que fecha pelo X mirando o `Calibra.exe` que tem a janela.

## O que fica com você (critério de pronto da Fase 3B)

- **Abrir o 0.3.2 com os seus dados:** o app instalado já é o 0.3.2. Na primeira abertura, ele atualiza os dados da versão 7 para a 14, com o backup antes. Hoje os seus dados só têm o que vem com o app (nenhum cliente, OS ou configuração salva), então não precisa de backup extra antes. Nas próximas atualizações, faça antes "Fazer backup agora" na versão antiga (manual, item 2).
- **Antes da primeira OS:** em Configurações, preencha a Oficina (nome, CNPJ, telefone e logo, que vão para os PDFs), confira a garantia padrão e o texto da via do cliente (aba OS) e cadastre os mecânicos. Em Catálogos › Serviços, use "Serviços sugeridos…" e preencha as horas ou o preço de cada serviço que a oficina faz.
- **As 4 OS reais pelo app instalado,** cada uma com o checklist cumprido:
  - uma de som;
  - uma de vidro ou trava;
  - uma de partida ou alternador;
  - uma de injeção programável, com chicote, instalação ou os dois.

  Ao menos uma com o laudo em PDF, e a de injeção com a ficha do chicote em PDF.
- **Os modelos de checklist:** confira os 7 em Catálogos › Checklists. Eles vêm "a verificar", com todos os itens obrigatórios.
