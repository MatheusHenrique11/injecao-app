# Gera o ícone do Calibra: um manômetro branco sobre o quadrado azul do app, com a faixa final em laranja e o
# ponteiro amarelo. Desenhado por código (nenhuma imagem baixada): os PNG da janela, em vários tamanhos, e o .ico
# do instalador (jpackage), com as mesmas imagens.
#
# Uso (na raiz do projeto):  powershell -ExecutionPolicy Bypass -File scripts\gerar-icone.ps1
param(
    [string]$Pngs = 'calibra-app\src\main\resources\br\com\calibra\app\icones',
    [string]$Ico = 'calibra-app\src\main\instalador\calibra.ico'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$raiz = Split-Path -Parent $PSScriptRoot
$pastaPngs = Join-Path $raiz $Pngs
$arquivoIco = Join-Path $raiz $Ico
New-Item -ItemType Directory -Force $pastaPngs | Out-Null
New-Item -ItemType Directory -Force (Split-Path -Parent $arquivoIco) | Out-Null

function Cor([int]$r, [int]$g, [int]$b) {
    return [System.Drawing.Color]::FromArgb(255, $r, $g, $b)
}

function Caneta($cor, [double]$largura) {
    $caneta = New-Object System.Drawing.Pen $cor, ([single]$largura)
    $caneta.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $caneta.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    return $caneta
}

# Desenha o ícone em um tamanho (cada tamanho é desenhado do zero, para ficar nítido).
function Desenhar([int]$lado) {
    $imagem = New-Object System.Drawing.Bitmap $lado, $lado, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($imagem)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $s = $lado / 256.0

    # Fundo: quadrado de cantos arredondados, no azul do app (#1565C0).
    $x = 8 * $s; $y = 8 * $s; $l = 240 * $s; $d = 104 * $s
    $fundo = New-Object System.Drawing.Drawing2D.GraphicsPath
    $fundo.AddArc($x, $y, $d, $d, 180, 90)
    $fundo.AddArc($x + $l - $d, $y, $d, $d, 270, 90)
    $fundo.AddArc($x + $l - $d, $y + $l - $d, $d, $d, 0, 90)
    $fundo.AddArc($x, $y + $l - $d, $d, $d, 90, 90)
    $fundo.CloseFigure()
    $g.FillPath((New-Object System.Drawing.SolidBrush (Cor 21 101 192)), $fundo)

    # Manômetro: arco de 240 graus (de 8 para 4 horas, por cima), com o fim em laranja.
    $cx = 128 * $s; $cy = 146 * $s; $r = 80 * $s
    $g.DrawArc((Caneta ([System.Drawing.Color]::White) (20 * $s)), $cx - $r, $cy - $r, 2 * $r, 2 * $r, 150, 190)
    $g.DrawArc((Caneta (Cor 255 112 67) (20 * $s)), $cx - $r, $cy - $r, 2 * $r, 2 * $r, 340, 50)

    # Ponteiro amarelo, quase no fim da escala, e o eixo branco.
    $angulo = 312 * [Math]::PI / 180
    $comprimento = 66 * $s
    $g.DrawLine((Caneta (Cor 255 193 7) (14 * $s)), $cx, $cy, $cx + $comprimento * [Math]::Cos($angulo),
        $cy + $comprimento * [Math]::Sin($angulo))
    $eixo = 15 * $s
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)), $cx - $eixo, $cy - $eixo,
        2 * $eixo, 2 * $eixo)

    $g.Dispose()
    return $imagem
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

$imagens = @()
foreach ($lado in 16, 24, 32, 48, 64, 128, 256) {
    $desenho = Desenhar $lado
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
Write-Host "Ícone gerado: $arquivoIco e os PNG em $pastaPngs"
