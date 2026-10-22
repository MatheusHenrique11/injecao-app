# Identidade visual — GarageTech

**GarageTech — Gestão e Tecnologia Automotiva.** É o nome comercial do app desde a versão 0.3.3 (21/10/2026). Antes, ele se chamava Calibra. As decisões estão em [docs/decisoes.md](../decisoes.md), itens 282 a 287.

## A marca aprovada

A imagem aprovada pelo dono é [garagetech-identidade.png](garagetech-identidade.png), com 1536 × 1024 pixels e quatro painéis:

| Painel | Área (pixels) | O que tem |
|---|---|---|
| Principal | 0,0 a 1536,640 | o logo grande sobre fundo escuro: o símbolo, o nome e a frase |
| Fundo claro | 0,641 a 518,1024 | o logo para fundo claro |
| Monocromático | 519,641 a 1018,1024 | o logo branco sobre preto |
| Ícones de app | 1019,641 a 1536,1024 | o ícone escuro e o claro |

**Cores:**

| Cor | Código | Uso |
|---|---|---|
| Preto | `#000000` | fundo, solidez |
| Grafite | `#171717` | fundo e texto escuro (a frase da barra lateral) |
| Azul principal | `#0386FE` | destaque e brilho do logo; no app, só em detalhe sem texto em cima (pouco contraste com o branco) |
| Azul complementar | `#1362F9` | o azul do app: botão e aba escolhidos, números do Painel, seleção em listas e foco (contraste de 5:1 com o texto branco) |
| Branco | `#FFFFFF` | contraste e leitura |

No app, um azul mais escuro, `#0F4CC2`, faz a borda de foco e o texto azul sobre fundo claro. É o mesmo tom do azul complementar.

## Os arquivos do app

Todos saem de `scripts\gerar-icone.ps1`, recortados da imagem aprovada, sem redesenho. O script usa o System.Drawing do próprio Windows: não precisa de programa novo.

| Arquivo | O que é | Quem usa |
|---|---|---|
| `calibra-app\src\main\resources\br\com\calibra\app\icones\calibra-16.png` a `calibra-256.png` | o ícone: o símbolo do painel principal, sem o fundo, sobre o quadrado escuro do ícone de app aprovado | a janela e a barra de tarefas (`CalibraApp.icones`) |
| `calibra-app\src\main\instalador\calibra.ico` | o mesmo ícone, de 16 a 256 px | o `.exe`, o atalho e "Aplicativos instalados" (`--icon` do jpackage) |
| `calibra-app\src\main\resources\br\com\calibra\app\icones\marca-garagetech.png` | o logo para fundo claro (o símbolo e o nome), sem o fundo, com 348 px de largura | a barra lateral (`PrincipalController.mostrarMarca`), em 174 px de tela, nítido em qualquer escala |
| `calibra-app\src\main\instalador\abertura.jpg` e `abertura@125pct.jpg` a `abertura@200pct.jpg` | o painel principal, de 768 × 320 (100%) a 1536 × 640 (200%) | a tela de abertura: a opção `-splash` do Java, que escolhe sozinha a imagem da escala da tela |

Os arquivos do ícone mantêm os nomes de antes (`calibra-*`): o código e o instalador os procuram assim.

**O fundo do logo** sai pelo método "cor para transparência": cada pixel vira a cor do logo com a transparência que, sobre o fundo, dá o pixel original. Assim, a borda fica lisa, sem contorno.

**A frase** "Gestão e Tecnologia Automotiva" vai como texto na barra lateral. Na largura da barra, a da imagem ficaria ilegível.

**Gerar de novo** (depois de trocar a imagem aprovada, por exemplo):

```
powershell -ExecutionPolicy Bypass -File scripts\gerar-icone.ps1
```

O script confere se a imagem tem 1536 × 1024 pixels. Se o desenho mudar, as áreas no começo do script precisam ser medidas de novo.

**Arquivos originais do designer:** se um dia chegarem (PNG transparente, SVG ou ICO), basta pôr cada um no lugar do arquivo da tabela, com o mesmo nome e um tamanho parecido, sem rodar o script.

## Onde a marca aparece

- **Ao abrir o app:** a tela de abertura com o logo grande aparece no instante do clique, antes de o Java carregar. Ela fecha quando a janela principal aparece, ou antes de qualquer janela de erro da abertura (`TelaDeAbertura.fechar`).
- **Título da janela:** "GarageTech — Gestão e Tecnologia Automotiva" no Painel e "GarageTech — Oficina" (o nome da tela) nas outras telas.
- **Barra lateral:** o logo e a frase.
- **Barra de baixo:** "GarageTech 0.3.3 · núcleo … · offline".
- **Windows:** "GarageTech" no atalho, no menu Iniciar, em "Aplicativos instalados" e no `GarageTech.exe`, com o ícone da marca.
- **Mensagens, janelas e PDFs:** os rodapés dos PDFs e as mensagens de backup e erro trazem "GarageTech".

## O que continua com o nome Calibra (e por quê)

Ficou com o nome antigo tudo o que não aparece como marca para o usuário, ou que mudaria dados e instalações que já existem:

- **o código:** os pacotes `br.com.calibra`, os módulos `calibra-*` e as classes (`CalibraApp`…);
- **a pasta dos dados:** `%LOCALAPPDATA%\Calibra`, com o `calibra.db`, os backups `calibra-backup-*.zip` e a pasta `Calibra-dev` do desenvolvimento. Mudar perderia o acesso aos dados;
- **a pasta do programa:** `%LOCALAPPDATA%\Programs\Calibra`, e o UpgradeCode do instalador. É o que faz a versão nova substituir a antiga, sem duplicar;
- **os arquivos `calibra.css`, `calibra.ico` e `calibra-*.png`;**
- **as sementes do catálogo** (`catalogo.json` e `checklists.json`): as migrações conferem a soma delas (o checksum), e mudar o texto impediria os dados de abrir;
- **as linhas de log** ("Calibra … abrindo", "Calibra fechando."), que os scripts de ensaio procuram;
- **o histórico:** as decisões, a SPEC e os relatórios dos ensaios contam a história como foi.
