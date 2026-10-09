param([string]$Saida)
# Gera a planilha da cadeia de cálculo (.xlsx) sem depender do Excel.
Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem

function Esc([string]$t) { $t.Replace('&','&amp;').Replace('<','&lt;').Replace('>','&gt;').Replace('"','&quot;') }

# estilos: 0 normal, 1 cabeçalho, 2 entrada (amarelo), 3 cálculo, 4 entrada %, 5 cálculo %, 6 título, 7 seção, 8 conferência
$styles = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="1"><numFmt numFmtId="164" formatCode="0.000"/></numFmts>
<fonts count="4">
<font><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="14"/><name val="Calibri"/></font>
<font><i/><sz val="10"/><color rgb="FF555555"/><name val="Calibri"/></font>
</fonts>
<fills count="6">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFFFF2CC"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFDDEBF7"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFE2EFDA"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFD9D9D9"/></patternFill></fill>
</fills>
<borders count="1"><border/></borders>
<cellStyleXfs count="1"><xf/></cellStyleXfs>
<cellXfs count="10">
<xf/>
<xf fontId="1" fillId="5" applyFont="1" applyFill="1"/>
<xf numFmtId="164" fillId="2" applyFill="1" applyNumberFormat="1"/>
<xf numFmtId="164" fillId="3" applyFill="1" applyNumberFormat="1"/>
<xf numFmtId="10" fillId="2" applyFill="1" applyNumberFormat="1"/>
<xf numFmtId="10" fillId="3" applyFill="1" applyNumberFormat="1"/>
<xf fontId="2" applyFont="1"/>
<xf fontId="1" applyFont="1"/>
<xf fontId="1" fillId="4" applyFont="1" applyFill="1"/>
<xf fontId="3" applyFont="1"><alignment wrapText="1"/></xf>
</cellXfs>
</styleSheet>
'@

$colunas = 'A','B','C','D','E','F','G','H','I','J'

function Celula($ref, $v, $s) {
    if ($null -eq $v -or "$v" -eq '') { return "" }
    if ($v -is [double] -or $v -is [int]) { return "<c r=`"$ref`" s=`"$s`"><v>$([string]::Format([Globalization.CultureInfo]::InvariantCulture,'{0}',$v))</v></c>" }
    $t = [string]$v
    if ($t.StartsWith('=')) { return "<c r=`"$ref`" s=`"$s`"><f>$(Esc $t.Substring(1))</f></c>" }
    return "<c r=`"$ref`" s=`"$s`" t=`"inlineStr`"><is><t xml:space=`"preserve`">$(Esc $t)</t></is></c>"
}

# cada linha: @(numero, estiloDosValores, estiloDoRotulo, valores começando na coluna A)
function Planilha($linhas, $larguras) {
    $sb = New-Object Text.StringBuilder
    [void]$sb.Append('<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetViews><sheetView workbookViewId="0"/></sheetViews><cols>')
    for ($i = 0; $i -lt $larguras.Count; $i++) { [void]$sb.Append("<col min=`"$($i+1)`" max=`"$($i+1)`" width=`"$($larguras[$i])`" customWidth=`"1`"/>") }
    [void]$sb.Append('</cols><sheetData>')
    foreach ($l in $linhas) {
        $n = $l[0]; $sv = $l[1]; $sr = $l[2]; $vals = $l[3]
        [void]$sb.Append("<row r=`"$n`">")
        for ($i = 0; $i -lt $vals.Count; $i++) {
            $s = if ($i -le 1) { $sr } elseif ($i -le 5) { $sv } else { 9 }
            [void]$sb.Append((Celula "$($colunas[$i])$n" $vals[$i] $s))
        }
        [void]$sb.Append('</row>')
    }
    [void]$sb.Append('</sheetData></worksheet>')
    $sb.ToString()
}

# ---------------- Leia-me ----------------
$leia = @(
 @(1,0,6,@('Cadeia de cálculo do mapa base - fase 0')),
 @(3,0,0,@('Objetivo: conferir as fórmulas antes de escrevê-las em Java (módulo calculo).')),
 @(4,0,0,@('Células amarelas = entradas. Células azuis = cálculos (não editar). Linha verde = conferência.')),
 @(5,0,0,@('Aba Cadeia: coluna C reproduz o exemplo da Garrett (Turbo Tech 103). Colunas D-F são os três motores de referência.')),
 @(6,0,0,@('Aba Combustiveis: valores por combustível com fonte, data e status. "Não verificado" = conferir antes de usar.')),
 @(7,0,0,@('Aba Exemplo_documento: preencher com o exemplo resolvido do documento do projeto e comparar com a coluna correspondente.')),
 @(9,0,7,@('Fórmulas usadas (Garrett, unidades inglesas)')),
 @(10,0,0,@('Vazão de ar  Wa [lb/min] = HP x AFR x BSFC / 60')),
 @(11,0,0,@('MAP [psia] = Wa x R x (460 + Tm[°F]) / (VE x N/2 x Vd[in³]),  R = 639,6')),
 @(12,0,0,@('Razão de pressão = (MAP + perda na descarga) / (p atm - perda na admissão)')),
 @(13,0,0,@('Combustível [lb/h] = HP x BSFC;  vazão do injetor corrigida = vazão nominal x raiz(p trabalho / p teste)')),
 @(14,0,0,@('Tempo de injeção na potência máxima [ms] = uso do injetor x 120000 / rpm  (sem tempo morto)')),
 @(16,0,7,@('Aviso')),
 @(17,0,0,@('Valores dos motores D-F são EXEMPLOS para testar a cadeia; não são recomendação de acerto. Mapa base sempre abaixo da meta e acerto com wideband.'))
)

# ---------------- Combustiveis ----------------
$hoje = '08/10/2026'
$comb = @(
 @(1,0,6,@('Combustíveis')),
 @(2,0,9,@('Constantes (entradas)','', 'Unidade','Fonte','Status')),
 @(3,2,0,@('AFR estequiométrico gasolina A (sem etanol)',14.7,'kg ar/kg','Literatura (valor usual 14,6-14,7)','Não verificado')),
 @(4,2,0,@('AFR estequiométrico etanol puro',9.0,'kg ar/kg','Literatura (8,95-9,0)','Não verificado')),
 @(5,2,0,@('Densidade gasolina A a 20 °C',0.745,'g/cm³','Literatura (faixa ~0,72-0,77)','Não verificado')),
 @(6,2,0,@('Densidade etanol anidro a 20 °C',0.791,'g/cm³','Literatura / Res. ANP 907/2022 (conferir anexo)','Não verificado')),
 @(7,4,0,@('Teor alcoólico do EHC (% massa)',0.93,'% m/m','Res. ANP 907/2022, Tabela 1 (conferir anexo)','Não verificado')),
 @(8,2,0,@('Densidade EHC a 20 °C',0.809,'g/cm³','Res. ANP 907/2022 (faixa citada 807,6-811,0 kg/m³)','Não verificado')),
 @(10,0,1,@('Combustível','% etanol (vol)','Fração mássica etanol','AFR esteq.','Densidade g/cm³','Fonte','Data','Status')),
 @(11,3,0,@('Gasolina A (sem etanol)',0,'=B11*$B$6/(B11*$B$6+(1-B11)*$B$5)','=C11*$B$4+(1-C11)*$B$3','=B11*$B$6+(1-B11)*$B$5','Referência','', 'Não verificado')),
 @(12,3,0,@('Gasolina C comum/aditivada (E32)',0.32,'=B12*$B$6/(B12*$B$6+(1-B12)*$B$5)','=C12*$B$4+(1-C12)*$B$3','=B12*$B$6+(1-B12)*$B$5','Res. CNPE 9/2026 (vigente desde 01/08/2026, 180 dias)',$hoje,'Teor verificado; AFR/densidade calculados')),
 @(13,3,0,@('Gasolina C premium (E25)',0.25,'=B13*$B$6/(B13*$B$6+(1-B13)*$B$5)','=C13*$B$4+(1-C13)*$B$3','=B13*$B$6+(1-B13)*$B$5','ANP, comunicado E32 (premium mantida em 25%)',$hoje,'Teor verificado; AFR/densidade calculados')),
 @(14,3,0,@('Etanol anidro (EAC)',1,'=B14*$B$6/(B14*$B$6+(1-B14)*$B$5)','=C14*$B$4+(1-C14)*$B$3','=B14*$B$6+(1-B14)*$B$5','Literatura','', 'Não verificado')),
 @(15,3,0,@('Etanol hidratado (EHC)','-','=$B$7','=$B$4*$B$7','=$B$8','Res. ANP 907/2022 (água não queima: AFR = 9,0 x fração de etanol)','', 'Não verificado')),
 @(17,0,9,@('Obs.: E32 é temporário (180 dias a partir de 01/08/2026, prorrogável uma vez). Ao vencer, conferir se volta a E30 e atualizar a linha 12 com nova data - por isso o catálogo é versionado.'))
)

# ---------------- Cadeia ----------------
$cad = @(
 @(1,0,6,@('Cadeia de cálculo - potência alvo até injetor')),
 @(2,0,9,@('Coluna C = exemplo Garrett Turbo Tech 103 (2,0 L, 400 hp). D-F = motores de referência com valores de EXEMPLO.')),
 @(3,0,1,@('Item','Unidade','Garrett TT103','AP 2.0 turbo','EA888 2.0 (gen3)','V8 5.0 (modelo)','Observação')),
 @(4,0,7,@('ENTRADAS')),
 @(5,0,0,@('Combustível','-','Gasolina (exemplo Garrett)','EHC','Gasolina C E32','Gasolina C E32')),
 @(6,3,0,@('AFR estequiométrico',':1',14.7,'=Combustiveis!D15','=Combustiveis!D12','=Combustiveis!D12','Vem da aba Combustiveis')),
 @(7,2,0,@('Lambda alvo na potência máxima','λ','=12/14.7',0.80,0.82,0.82,'Garrett usa AFR 12 em gasolina')),
 @(8,2,0,@('Potência alvo no virabrequim','hp',400,300,380,500)),
 @(9,2,0,@('BSFC','lb/hp·h',0.55,0.75,0.55,0.50,'Etanol gasta mais massa por hp')),
 @(10,2,0,@('Cilindrada','L','=122*0.016387',1.984,1.984,5.0,'EA888: 1984 cm³ (SSP 606)')),
 @(11,2,0,@('Número de cilindros','-',4,4,4,8)),
 @(12,2,0,@('Rotação na potência máxima','rpm',7200,6500,6500,7000)),
 @(13,2,0,@('Eficiência volumétrica','-',0.95,0.90,0.95,0.95)),
 @(14,2,0,@('Temperatura no coletor','°C','=(130-32)*5/9',50,45,50,'Garrett: 130 °F')),
 @(15,2,0,@('Perda entre compressor e coletor','psi',2,2,2,2,'Intercooler e tubos')),
 @(16,2,0,@('Perda antes do compressor','psi',1,1,1,1,'Filtro e tubo de admissão')),
 @(17,2,0,@('Pressão atmosférica','psia',14.7,14.7,14.7,14.7,'Ajustar pela altitude da oficina')),
 @(18,2,0,@('Densidade do combustível','g/cm³',0.745,'=Combustiveis!E15','=Combustiveis!E12','=Combustiveis!E12')),
 @(19,2,0,@('Vazão nominal do injetor','cm³/min',700,1000,650,550,'Usar dado de caracterização do fabricante. Garrett não define injetor: 700 é escolha nossa')),
 @(20,2,0,@('Pressão de teste do injetor','bar',3,3,3,3)),
 @(21,2,0,@('Pressão de trabalho (diferencial no bico)','bar',3,4,3.5,4,'Regulador referenciado ao coletor')),
 @(22,4,0,@('Uso máximo admitido do injetor','%',0.85,0.85,0.85,0.85)),
 @(24,0,7,@('CÁLCULOS')),
 @(25,3,0,@('AFR alvo',':1','=C6*C7','=D6*D7','=E6*E7','=F6*F7')),
 @(26,3,0,@('Potência alvo','cv','=C8*1.01387','=D8*1.01387','=E8*1.01387','=F8*1.01387')),
 @(27,3,0,@('Vazão de ar Wa','lb/min','=C8*C25*C9/60','=D8*D25*D9/60','=E8*E25*E9/60','=F8*F25*F9/60','HP x AFR x BSFC / 60')),
 @(28,3,0,@('Vazão de ar','kg/h','=C27*0.45359*60','=D27*0.45359*60','=E27*0.45359*60','=F27*0.45359*60')),
 @(29,3,0,@('Cilindrada','in³','=C10/0.016387','=D10/0.016387','=E10/0.016387','=F10/0.016387')),
 @(30,3,0,@('Temperatura no coletor','°F','=C14*9/5+32','=D14*9/5+32','=E14*9/5+32','=F14*9/5+32')),
 @(31,3,0,@('MAP necessária','psia','=C27*639.6*(460+C30)/(C13*C12/2*C29)','=D27*639.6*(460+D30)/(D13*D12/2*D29)','=E27*639.6*(460+E30)/(E13*E12/2*E29)','=F27*639.6*(460+F30)/(F13*F12/2*F29)','R = 639,6')),
 @(32,3,0,@('MAP necessária','bar abs','=C31*0.0689476','=D31*0.0689476','=E31*0.0689476','=F31*0.0689476')),
 @(33,3,0,@('Pressão de turbo (manométrica)','bar','=(C31-C17)*0.0689476','=(D31-D17)*0.0689476','=(E31-E17)*0.0689476','=(F31-F17)*0.0689476')),
 @(34,3,0,@('Pressão na saída do compressor P2c','psia','=C31+C15','=D31+D15','=E31+E15','=F31+F15')),
 @(35,3,0,@('Pressão na entrada do compressor P1c','psia','=C17-C16','=D17-D16','=E17-E16','=F17-F16')),
 @(36,3,0,@('Razão de pressão do compressor','-','=C34/C35','=D34/D35','=E34/E35','=F34/F35','Ponto no mapa do compressor: (Wa, PR)')),
 @(37,3,0,@('Vazão total de combustível','lb/h','=C8*C9','=D8*D9','=E8*E9','=F8*F9','HP x BSFC')),
 @(38,3,0,@('Vazão total de combustível','cm³/min','=C37*453.59/60/C18','=D37*453.59/60/D18','=E37*453.59/60/E18','=F37*453.59/60/F18')),
 @(39,3,0,@('Vazão mínima por injetor (no uso máx.)','cm³/min','=C38/C11/C22','=D38/D11/D22','=E38/E11/E22','=F38/F11/F22')),
 @(40,3,0,@('Vazão do injetor na pressão de trabalho','cm³/min','=C19*SQRT(C21/C20)','=D19*SQRT(D21/D20)','=E19*SQRT(E21/E20)','=F19*SQRT(F21/F20)')),
 @(41,5,0,@('Uso do injetor na potência máxima','%','=C38/C11/C40','=D38/D11/D40','=E38/E11/E40','=F38/F11/F40','Acima do limite = trocar injetor')),
 @(42,3,0,@('Tempo de injeção efetivo','ms','=C41*120000/C12','=D41*120000/D12','=E41*120000/E12','=F41*120000/F12','Somar o tempo morto para o tempo comandado')),
 @(43,0,0,@('Injetor atende?','-','=IF(C41<=C22,"SIM","NÃO")','=IF(D41<=D22,"SIM","NÃO")','=IF(E41<=E22,"SIM","NÃO")','=IF(F41<=F22,"SIM","NÃO")')),
 @(45,0,8,@('Conferência Garrett','-','=IF(AND(ABS(C27-44)<0.05,ABS(C31-39.8)<0.1,ABS(C36-3.05)<0.02),"OK - bate com a Garrett","CONFERIR")','','','','Garrett: Wa = 44 lb/min, MAP = 39,8 psia, PR = 3,05'))
)

# ---------------- Exemplo_documento ----------------
$ex = @(
 @(1,0,6,@('Exemplo resolvido do documento do projeto')),
 @(2,0,9,@('Copiar aqui as entradas e os resultados do exemplo do documento. Depois lançar as mesmas entradas numa coluna da aba Cadeia e comparar.')),
 @(4,0,1,@('Item','Unidade','Valor do documento','Valor da planilha','Diferença %','','Observação')),
 @(5,2,0,@('Vazão de ar','lb/min','','','=IF(AND(ISNUMBER(C5),ISNUMBER(D5)),(D5-C5)/C5,"")')),
 @(6,2,0,@('MAP necessária','psia','','','=IF(AND(ISNUMBER(C6),ISNUMBER(D6)),(D6-C6)/C6,"")')),
 @(7,2,0,@('Razão de pressão','-','','','=IF(AND(ISNUMBER(C7),ISNUMBER(D7)),(D7-C7)/C7,"")')),
 @(8,2,0,@('Vazão de combustível','cm³/min','','','=IF(AND(ISNUMBER(C8),ISNUMBER(D8)),(D8-C8)/C8,"")')),
 @(9,2,0,@('Uso do injetor','%','','','=IF(AND(ISNUMBER(C9),ISNUMBER(D9)),(D9-C9)/C9,"")')),
 @(10,2,0,@('Tempo de injeção','ms','','','=IF(AND(ISNUMBER(C10),ISNUMBER(D10)),(D10-C10)/C10,"")'))
)

$abas = @(
 @('Leia-me', (Planilha $leia @(110))),
 @('Cadeia', (Planilha $cad @(42,12,22,16,18,18,44))),
 @('Combustiveis', (Planilha $comb @(44,14,20,12,16,52,12,34))),
 @('Exemplo_documento', (Planilha $ex @(28,12,20,18,14,4,40)))
)

$ct = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>'
$wb = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>'
$wbr = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
for ($i = 0; $i -lt $abas.Count; $i++) {
    $n = $i + 1
    $ct += "<Override PartName=`"/xl/worksheets/sheet$n.xml`" ContentType=`"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml`"/>"
    $wb += "<sheet name=`"$($abas[$i][0])`" sheetId=`"$n`" r:id=`"rId$n`"/>"
    $wbr += "<Relationship Id=`"rId$n`" Type=`"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet`" Target=`"worksheets/sheet$n.xml`"/>"
}
$ct += '</Types>'
$wb += '</sheets><calcPr calcId="0" fullCalcOnLoad="1"/></workbook>'
$wbr += "<Relationship Id=`"rId99`" Type=`"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles`" Target=`"styles.xml`"/></Relationships>"
$rels = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>'

if (Test-Path $Saida) { Remove-Item $Saida }
$zip = [IO.Compression.ZipFile]::Open($Saida, 'Create')
$utf8 = New-Object Text.UTF8Encoding $false
function Poe($caminho, $texto) {
    $e = $zip.CreateEntry($caminho)
    $w = New-Object IO.StreamWriter($e.Open(), $utf8)
    $w.Write($texto); $w.Close()
}
Poe '[Content_Types].xml' $ct
Poe '_rels/.rels' $rels
Poe 'xl/workbook.xml' $wb
Poe 'xl/_rels/workbook.xml.rels' $wbr
Poe 'xl/styles.xml' $styles
for ($i = 0; $i -lt $abas.Count; $i++) { Poe "xl/worksheets/sheet$($i+1).xml" $abas[$i][1] }
$zip.Dispose()
"Gerado: $Saida"
