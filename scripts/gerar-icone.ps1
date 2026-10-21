# Gera os arquivos da marca GarageTech (docs\decisoes.md, item 282) a partir da imagem aprovada pelo dono
# (docs\marca\garagetech-identidade.png, 1536 x 1024, com os 4 painéis da identidade visual). Nada é redesenhado:
# tudo é recortado da imagem aprovada, com o System.Drawing do Windows (como antes, sem programa novo).
#   - o ícone do app: o símbolo do painel principal sobre o quadrado escuro do ícone de app aprovado. Os PNG da janela
#     (16 a 256 px) e o .ico do instalador (jpackage) ficam com os nomes de antes (calibra-*.png e calibra.ico): o
#     código e o instalador os procuram assim;
#   - a marca da barra lateral: o logo da versão para fundo claro (o símbolo e o nome), com o fundo transparente;
#   - a tela de abertura (a opção -splash do Java): o painel principal, em 100% e nas escalas do Windows. O Java
#     escolhe sozinho o arquivo da escala da tela ("abertura@125pct.jpg", "abertura@150pct.jpg"...).
# Com os arquivos originais do designer (PNG transparente, SVG ou ICO), basta pôr no lugar destes
# (docs\marca\identidade-visual.md).
#
# Uso (na raiz do projeto):  powershell -ExecutionPolicy Bypass -File scripts\gerar-icone.ps1
param(
    [string]$Imagem = 'docs\marca\garagetech-identidade.png',
    [string]$Pngs = 'calibra-app\src\main\resources\br\com\calibra\app\icones',
    [string]$Ico = 'calibra-app\src\main\instalador\calibra.ico',
    [string]$Instalador = 'calibra-app\src\main\instalador'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# As áreas da imagem aprovada, em pixels (medidas em 21/10/2026):
#   - o painel principal, o de cima: fundo escuro e logo grande;
#   - o símbolo dele (engrenagem, GT e circuito), com folga;
#   - o logo da versão para fundo claro: o símbolo e o nome, sem a frase.
$AreaPainel = New-Object System.Drawing.Rectangle 0, 0, 1536, 640
$AreaSimbolo = New-Object System.Drawing.Rectangle 476, 95, 600, 298
$AreaLogoClaro = New-Object System.Drawing.Rectangle 50, 690, 430, 227
# O degradê do quadrado do ícone de app escuro da imagem aprovada (de cima para baixo).
$IconeCima = [System.Drawing.Color]::FromArgb(255, 12, 24, 36)
$IconeBaixo = [System.Drawing.Color]::FromArgb(255, 3, 7, 13)

Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;

public static class MarcaGarageTech {

    // Recorta a area e tira o fundo, como o "cor para transparencia" dos editores de imagem: cada pixel vira a cor da
    // arte com a transparencia que, sobre o fundo, da o pixel original. O fundo de cada ponto vem dos 4 cantos da
    // area (o painel escuro tem degrade). Abaixo de 4% de opacidade, e ruido do fundo: fica transparente.
    public static Bitmap SemFundo(Bitmap origem, Rectangle area) {
        Color[] cantos = {
            Media(origem, area.Left, area.Top), Media(origem, area.Right - 3, area.Top),
            Media(origem, area.Left, area.Bottom - 3), Media(origem, area.Right - 3, area.Bottom - 3)
        };
        Bitmap saida = new Bitmap(area.Width, area.Height, PixelFormat.Format32bppArgb);
        for (int y = 0; y < area.Height; y++) {
            double fy = y / (double) (area.Height - 1);
            for (int x = 0; x < area.Width; x++) {
                double fx = x / (double) (area.Width - 1);
                Color p = origem.GetPixel(area.Left + x, area.Top + y);
                double[] pixel = { p.R, p.G, p.B };
                double[] fundo = new double[3];
                double alfa = 0;
                for (int c = 0; c < 3; c++) {
                    double cima = Canal(cantos[0], c) * (1 - fx) + Canal(cantos[1], c) * fx;
                    double baixo = Canal(cantos[2], c) * (1 - fx) + Canal(cantos[3], c) * fx;
                    fundo[c] = cima * (1 - fy) + baixo * fy;
                    double d = pixel[c] - fundo[c];
                    double a = d > 0 ? d / Math.Max(1, 255 - fundo[c]) : (d < 0 ? -d / Math.Max(1, fundo[c]) : 0);
                    alfa = Math.Max(alfa, a);
                }
                if (alfa < 0.04) {
                    saida.SetPixel(x, y, Color.FromArgb(0, 0, 0, 0));
                    continue;
                }
                alfa = Math.Min(1, alfa);
                int[] cor = new int[3];
                for (int c = 0; c < 3; c++) {
                    cor[c] = (int) Math.Round(Math.Max(0, Math.Min(255, fundo[c] + (pixel[c] - fundo[c]) / alfa)));
                }
                saida.SetPixel(x, y, Color.FromArgb((int) Math.Round(alfa * 255), cor[0], cor[1], cor[2]));
            }
        }
        return saida;
    }

    // Recorta a area, sem mexer nas cores.
    public static Bitmap Recortar(Bitmap origem, Rectangle area) {
        return origem.Clone(area, PixelFormat.Format32bppArgb);
    }

    // Muda o tamanho com a interpolacao de melhor qualidade do Windows, sem borda escura nas beiradas.
    public static Bitmap Redimensionar(Image origem, int largura, int altura) {
        Bitmap saida = new Bitmap(largura, altura, PixelFormat.Format32bppArgb);
        using (Graphics g = Graphics.FromImage(saida))
        using (ImageAttributes atributos = new ImageAttributes()) {
            g.CompositingMode = CompositingMode.SourceCopy;
            g.CompositingQuality = CompositingQuality.HighQuality;
            g.InterpolationMode = InterpolationMode.HighQualityBicubic;
            g.PixelOffsetMode = PixelOffsetMode.HighQuality;
            g.SmoothingMode = SmoothingMode.HighQuality;
            atributos.SetWrapMode(WrapMode.TileFlipXY);
            g.DrawImage(origem, new Rectangle(0, 0, largura, altura), 0, 0, origem.Width, origem.Height,
                GraphicsUnit.Pixel, atributos);
        }
        return saida;
    }

    static double Canal(Color cor, int c) {
        return c == 0 ? cor.R : (c == 1 ? cor.G : cor.B);
    }

    // A media de um quadrado de 3 x 3 pixels (o fundo tem um pouco de ruido).
    static Color Media(Bitmap origem, int x, int y) {
        int r = 0, g = 0, b = 0;
        for (int dy = 0; dy < 3; dy++) {
            for (int dx = 0; dx < 3; dx++) {
                Color p = origem.GetPixel(x + dx, y + dy);
                r += p.R;
                g += p.G;
                b += p.B;
            }
        }
        return Color.FromArgb(r / 9, g / 9, b / 9);
    }
}
'@

$raiz = Split-Path -Parent $PSScriptRoot
$arquivoImagem = Join-Path $raiz $Imagem
$pastaPngs = Join-Path $raiz $Pngs
$arquivoIco = Join-Path $raiz $Ico
$pastaInstalador = Join-Path $raiz $Instalador
if (-not (Test-Path $arquivoImagem)) {
    throw "Imagem da marca não encontrada: $arquivoImagem"
}
New-Item -ItemType Directory -Force $pastaPngs | Out-Null
New-Item -ItemType Directory -Force $pastaInstalador | Out-Null

$aprovada = New-Object System.Drawing.Bitmap $arquivoImagem
if ($aprovada.Width -ne 1536 -or $aprovada.Height -ne 1024) {
    throw "A imagem da marca devia ter 1536 x 1024 pixels (tem $($aprovada.Width) x $($aprovada.Height)): as áreas mudam."
}

# Desenha o ícone num tamanho (cada tamanho do zero, a partir do símbolo grande, para ficar nítido).
function Desenhar([int]$lado, $simbolo) {
    $icone = New-Object System.Drawing.Bitmap $lado, $lado, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($icone)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $s = $lado / 256.0

    # O quadrado de cantos arredondados, com o degradê escuro do ícone de app aprovado.
    $x = 8 * $s; $y = 8 * $s; $l = 240 * $s; $d = 96 * $s
    $fundo = New-Object System.Drawing.Drawing2D.GraphicsPath
    $fundo.AddArc($x, $y, $d, $d, 180, 90)
    $fundo.AddArc($x + $l - $d, $y, $d, $d, 270, 90)
    $fundo.AddArc($x + $l - $d, $y + $l - $d, $d, $d, 0, 90)
    $fundo.AddArc($x, $y + $l - $d, $d, $d, 90, 90)
    $fundo.CloseFigure()
    $degrade = New-Object System.Drawing.Drawing2D.LinearGradientBrush (New-Object System.Drawing.PointF 0, $y),
        (New-Object System.Drawing.PointF 0, ($y + $l)), $IconeCima, $IconeBaixo
    $g.FillPath($degrade, $fundo)

    # O símbolo no meio: 82% da largura do quadrado (92% nos tamanhos pequenos, para continuar legível).
    $fracao = if ($lado -le 32) { 0.92 } else { 0.82 }
    $largura = $l * $fracao
    $altura = $largura * $simbolo.Height / $simbolo.Width
    $g.DrawImage($simbolo, (New-Object System.Drawing.RectangleF ((128 * $s) - $largura / 2),
        ((128 * $s) - $altura / 2), $largura, $altura))
    $g.Dispose()
    return $icone
}

# .ico com as imagens em PNG (aceito pelo Windows desde o Vista).
function GravarIco([string]$arquivo, $imagens) {
    $fluxo = [System.IO.File]::Create($arquivo)
    $escrita = New-Object System.IO.BinaryWriter $fluxo
    $escrita.Write([UInt16]0)
    $escrita.Write([UInt16]1)
    $escrita.Write([UInt16]$imagens.Count)
    $posicao = 6 + 16 * $imagens.Count
    foreach ($imagem in $imagens) {
        $tamanho = if ($imagem.Lado -ge 256) { 0 } else { $imagem.Lado }
        $escrita.Write([byte]$tamanho)
        $escrita.Write([byte]$tamanho)
        $escrita.Write([byte]0)
        $escrita.Write([byte]0)
        $escrita.Write([UInt16]1)
        $escrita.Write([UInt16]32)
        $escrita.Write([UInt32]$imagem.Bytes.Length)
        $escrita.Write([UInt32]$posicao)
        $posicao += $imagem.Bytes.Length
    }
    foreach ($imagem in $imagens) {
        $escrita.Write($imagem.Bytes)
    }
    $escrita.Close()
}

# JPEG de qualidade 95 (a tela de abertura é uma imagem de fundo em degradê: em PNG, cada escala teria quase 1 MB).
function GravarJpeg($imagem, [string]$arquivo) {
    $codificador = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() |
        Where-Object { $_.MimeType -eq 'image/jpeg' }
    $parametros = New-Object System.Drawing.Imaging.EncoderParameters 1
    $parametros.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter (
        [System.Drawing.Imaging.Encoder]::Quality, [long]95)
    $imagem.Save($arquivo, $codificador, $parametros)
}

# ---------- O ícone ----------
$simbolo = [MarcaGarageTech]::SemFundo($aprovada, $AreaSimbolo)
$imagens = @()
foreach ($lado in 16, 24, 32, 48, 64, 128, 256) {
    $desenho = Desenhar $lado $simbolo
    $memoria = New-Object System.IO.MemoryStream
    $desenho.Save($memoria, [System.Drawing.Imaging.ImageFormat]::Png)
    $bytes = $memoria.ToArray()
    if ($lado -in 16, 32, 48, 64, 128, 256) {
        [System.IO.File]::WriteAllBytes((Join-Path $pastaPngs "calibra-$lado.png"), $bytes)
    }
    $imagens += [pscustomobject]@{ Lado = $lado; Bytes = $bytes }
    $desenho.Dispose()
}
GravarIco $arquivoIco $imagens
$simbolo.Dispose()

# ---------- A marca da barra lateral (348 px: o dobro dos 174 px da barra, nítida em tela de alta resolução) ----------
$logo = [MarcaGarageTech]::SemFundo($aprovada, $AreaLogoClaro)
$marca = [MarcaGarageTech]::Redimensionar($logo, 348, [int][Math]::Round(348 * $logo.Height / $logo.Width))
$marca.Save((Join-Path $pastaPngs 'marca-garagetech.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$marca.Dispose()
$logo.Dispose()

# ---------- A tela de abertura: 768 x 320 em 100%, e as escalas do Windows até a imagem original (200%) ----------
$painel = [MarcaGarageTech]::Recortar($aprovada, $AreaPainel)
foreach ($escala in 100, 125, 150, 175, 200) {
    $largura = [int](768 * $escala / 100)
    $altura = [int](320 * $escala / 100)
    $nome = if ($escala -eq 100) { 'abertura.jpg' } else { "abertura@$($escala)pct.jpg" }
    $copia = if ($escala -eq 200) { $painel } else { [MarcaGarageTech]::Redimensionar($painel, $largura, $altura) }
    GravarJpeg $copia (Join-Path $pastaInstalador $nome)
    if ($escala -ne 200) {
        $copia.Dispose()
    }
}
$painel.Dispose()
$aprovada.Dispose()
Write-Host "Marca gerada: o ícone ($arquivoIco e os PNG), a marca da barra lateral e a tela de abertura ($pastaInstalador)."
