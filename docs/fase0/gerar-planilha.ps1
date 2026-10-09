param([string]$Saida = (Join-Path $PSScriptRoot 'cadeia-de-calculo.xlsx'))
# Gera a planilha de conferência das fórmulas da SPEC (seções 7.1, 7.2 e 7.6) sem depender do Excel.
# Rodar: powershell -ExecutionPolicy Bypass -File docs\fase0\gerar-planilha.ps1
Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem

function Esc([string]$t) { $t.Replace('&','&amp;').Replace('<','&lt;').Replace('>','&gt;').Replace('"','&quot;') }

# estilos: 0 normal, 1 cabeçalho, 2 entrada, 3 cálculo, 4 entrada %, 5 cálculo %, 6 título, 7 seção, 8 conferência, 9 observação
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
<xf fontId="3" applyFont="1"/>
</cellXfs>
</styleSheet>
'@

$colunas = [char[]](65..90) | ForEach-Object { [string]$_ }

function Celula($ref, $v, $s) {
    if ($null -eq $v -or "$v" -eq '') { return "" }
    if ($v -is [double] -or $v -is [int]) { return "<c r=`"$ref`" s=`"$s`"><v>$([string]::Format([Globalization.CultureInfo]::InvariantCulture,'{0}',$v))</v></c>" }
    $t = [string]$v
    if ($t.StartsWith('=')) { return "<c r=`"$ref`" s=`"$s`"><f>$(Esc $t.Substring(1))</f></c>" }
    return "<c r=`"$ref`" s=`"$s`" t=`"inlineStr`"><is><t xml:space=`"preserve`">$(Esc $t)</t></is></c>"
}

# linha: @(numero, estiloDosValores, estiloDoRotulo, valores a partir da coluna A)
# colunas 0..($nRotulos-1) = rótulo; até $ultimaValor = valores; depois = observação
function Planilha($linhas, $larguras, $nRotulos = 2, $ultimaValor = 5) {
    $sb = New-Object Text.StringBuilder
    [void]$sb.Append('<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetViews><sheetView workbookViewId="0"/></sheetViews><cols>')
    for ($i = 0; $i -lt $larguras.Count; $i++) { [void]$sb.Append("<col min=`"$($i+1)`" max=`"$($i+1)`" width=`"$($larguras[$i])`" customWidth=`"1`"/>") }
    [void]$sb.Append('</cols><sheetData>')
    foreach ($l in $linhas) {
        $n = $l[0]; $sv = $l[1]; $sr = $l[2]; $vals = $l[3]
        [void]$sb.Append("<row r=`"$n`">")
        for ($i = 0; $i -lt $vals.Count; $i++) {
            $s = if ($i -lt $nRotulos) { $sr } elseif ($i -le $ultimaValor) { $sv } else { 9 }
            [void]$sb.Append((Celula "$($colunas[$i])$n" $vals[$i] $s))
        }
        [void]$sb.Append('</row>')
    }
    [void]$sb.Append('</sheetData></worksheet>')
    $sb.ToString()
}

$casos = 'C','D','E','F'   # colunas da aba Cadeia: golden A, B, C, D
# mesma fórmula nas 4 colunas; {c} vira a letra da coluna
function F4([string]$f) { $casos | ForEach-Object { $f.Replace('{c}', $_) } }
function Linha4($n, $sv, $rotulo, $unidade, $f, $obs = '') { $script:cad += ,@($n, $sv, 0, (@($rotulo, $unidade) + (F4 $f) + @($obs))) }

# ---------------- Combustiveis (SPEC 7.2) ----------------
$comb = @(
 @(1,0,6,@('Combustíveis (SPEC 7.2)')),
 @(2,0,9,@('Constantes da spec','Valor','Unidade')),
 @(3,2,0,@('AFR estequiométrico gasolina A',14.7,':1')),
 @(4,2,0,@('AFR estequiométrico etanol',9.0,':1')),
 @(5,2,0,@('Densidade gasolina A',0.74,'kg/L')),
 @(6,2,0,@('Densidade etanol anidro',0.789,'kg/L')),
 @(7,4,0,@('Etanol no EHC (fração em massa)',0.93,'m/m')),
 @(8,2,0,@('Densidade EHC',0.809,'kg/L')),
 @(10,1,1,@('Combustível','Etanol (vol)','Fração mássica','AFR esteq.','Densidade kg/L','Lambda sob pressão','Lambda aspirado','Origem')),
 @(11,3,0,@('Gasolina C comum E32',0.32,'=B11*$B$6/(B11*$B$6+(1-B11)*$B$5)','=(1-C11)*$B$3+C11*$B$4','=B11*$B$6+(1-B11)*$B$5',0.80,0.87,'Calculado (vigente desde 01/08/2026)')),
 @(12,3,0,@('Gasolina premium E25',0.25,'=B12*$B$6/(B12*$B$6+(1-B12)*$B$5)','=(1-C12)*$B$3+C12*$B$4','=B12*$B$6+(1-B12)*$B$5',0.80,0.87,'Calculado')),
 @(13,3,0,@('Etanol hidratado (EHC)','-','=$B$7','=$B$4*$B$7','=$B$8',0.80,0.87,'Calculado: 9,0 x fração de etanol')),
 @(14,3,0,@('Etanol anidro','-',1,9.0,0.789,0.80,0.87,'Valor da spec')),
 @(15,3,0,@('E85','-','-',9.75,0.78,0.80,0.87,'Valor da spec')),
 @(16,3,0,@('Metanol','-','-',6.42,0.792,0.65,0.70,'Valor da spec (lambda com aviso amarelo)')),
 @(18,0,9,@('Conferência: a spec traz E32 = 12,80 e 0,756; E25 = 13,21 e 0,752; EHC = 8,37.'))
)

# ---------------- Cadeia (SPEC 7.1) ----------------
$cad = @(
 @(1,0,6,@('Cadeia de cálculo - fórmulas da SPEC 7.1')),
 @(2,0,9,@('Colunas C a F = casos golden A a D da SPEC 7.6. Amarelo = entrada; azul = cálculo. A iteração MAP-Tm está na aba Iteracao; a conferência de 1% na aba Conferencia.')),
 @(3,1,1,@('Item','Unidade','A Garrett','B 2.0 turbo EHC','C 1.0 3 cil E32','D V12 aspirado E32','Observação')),
 @(4,0,7,@('ENTRADAS')),
 @(5,2,0,@('Potência no virabrequim','cv','=650/0.98632',400,150,450,'A: 650 hp')),
 @(6,2,0,@('Caminho da vazão de ar','-','BSFC','K_AR','K_AR','K_AR','K_AR = padrão; BSFC = opcional (decisão 8)')),
 @(7,2,0,@('k_ar','hp por lb/min',10.5,10.5,10.5,10.5)),
 @(8,2,0,@('A/F (caminho BSFC)',':1',11.5,'','','')),
 @(9,2,0,@('BSFC (caminho BSFC)','lb/hp·h',0.46,'','','')),
 @(10,2,0,@('AFR estequiométrico',':1','','=Combustiveis!D13','=Combustiveis!D11','=Combustiveis!D11')),
 @(11,2,0,@('Lambda alvo','λ','',0.80,0.80,0.87)),
 @(12,2,0,@('Cilindros (1 bico por cilindro)','-','',4,3,12)),
 @(13,2,0,@('Diâmetro','mm','','=SQRT(2000*1000/(PI()/4*92.8*4))',74.5,89.0,'B: diâmetro que dá 2.000 cc (decisão 7)')),
 @(14,2,0,@('Curso','mm','',92.8,76.4,80.4)),
 @(15,2,0,@('Cilindrada informada (só A)','pol³',348,'','','')),
 @(16,2,0,@('Rotação','rpm',6000,7000,5500,6500)),
 @(17,2,0,@('VE','-',0.80,0.95,0.95,0.90)),
 @(18,2,0,@('Sobrealimentação','-','FIXO','SIM','SIM','NAO','FIXO = Tm informado (exemplo Garrett)')),
 @(19,2,0,@('Tm fixo (só A)','°C','=(130-32)*5/9','','','','130 °F')),
 @(20,2,0,@('Temperatura ambiente','°C','',25,25,25)),
 @(21,2,0,@('Altitude','m',0,600,600,600)),
 @(22,2,0,@('Perda depois do compressor','psi',2,2,2,2)),
 @(23,2,0,@('Perda antes do compressor','psi',1,1,1,1)),
 @(24,2,0,@('eta_c','-','',0.72,0.72,'')),
 @(25,2,0,@('eps do intercooler','-','',0.80,0.80,'')),
 @(26,2,0,@('Acréscimo de Tm no aspirado','°C','','','',10)),
 @(27,2,0,@('DC máximo','-','',0.80,0.80,0.80)),
 @(28,2,0,@('Densidade do combustível','kg/L','','=Combustiveis!E13','=Combustiveis!E11','=Combustiveis!E11')),
 @(29,2,0,@('Densidade do fluido de teste','kg/L','',0.70,0.70,0.70)),
 @(30,2,0,@('Bico nominal','cc/min','',1050,440,260)),
 @(31,2,0,@('Pressão de teste do bico','bar','',3,3,3)),
 @(32,2,0,@('Pressão de trabalho (diferencial)','bar','',3,3,3)),
 @(33,2,0,@('Tempo morto','ms','',1.0,1.0,1.0)),
 @(34,2,0,@('Bico alternativo 1','cc/min','',1300,'','')),
 @(35,2,0,@('Pressão do alternativo 1','bar','',3,'','')),
 @(36,2,0,@('Bico alternativo 2','cc/min','',1050,'','')),
 @(37,2,0,@('Pressão do alternativo 2','bar','',4,'','')),
 @(38,0,7,@('CÁLCULOS'))
)
Linha4 39 3 'Potência' 'hp' '={c}5*0.98632'
Linha4 40 3 'Potência' 'kW' '={c}5*0.7355'
Linha4 41 3 'Vazão de ar Wa' 'lb/min' '=IF({c}6="BSFC",{c}39*{c}8*{c}9/60,{c}39/{c}7)'
Linha4 42 3 'Combustível mf' 'lb/h' '=IF({c}6="BSFC",{c}39*{c}9,60*{c}41/({c}10*{c}11))'
Linha4 43 3 'Combustível mf' 'kg/h' '={c}42*0.453592'
Linha4 44 3 'BSFC (conferência)' 'lb/hp·h' '={c}42/{c}39'
Linha4 45 3 'Cilindrada' 'cc' '=IF(ISNUMBER({c}15),{c}15/61.0237*1000,PI()/4*{c}13^2*{c}14*{c}12/1000)'
Linha4 46 3 'Cilindrada' 'pol³' '={c}45/1000*61.0237'
Linha4 47 3 'P_atm' 'kPa' '=101.325*(1-2.25577E-5*{c}21)^5.25588'
Linha4 48 3 'P_atm' 'psia' '={c}47/6.894757'
Linha4 49 3 'Tm inicial da iteração' '°C' '=IF({c}18="FIXO",{c}19,IF({c}18="NAO",{c}20+{c}26,50))'
# colunas da aba Iteracao para cada caso: Tm, MAP, PR, T2
$it = @{}
for ($j = 0; $j -lt 4; $j++) { $it[$casos[$j]] = @($colunas[1+4*$j], $colunas[2+4*$j], $colunas[3+4*$j], $colunas[4+4*$j]) }
$ult = 18
function FIt([string]$f) { $casos | ForEach-Object { $c = $_; $f.Replace('{tm}',$it[$c][0]).Replace('{map}',$it[$c][1]).Replace('{pr}',$it[$c][2]).Replace('{t2}',$it[$c][3]).Replace('{c}',$c) } }
$cad += ,@(50, 3, 0, (@('Tm final','°C') + (FIt "=Iteracao!{tm}$ult") + @('15 passos; ver aba Iteracao')))
$cad += ,@(51, 3, 0, (@('Convergiu (|ΔTm| < 0,01 °C)','-') + (FIt "=IF(ABS(Iteracao!{tm}$ult-Iteracao!{tm}$($ult-1))<0.01,""SIM"",""NÃO"")")))
$cad += ,@(52, 3, 0, (@('MAP necessário','psia') + (FIt "=Iteracao!{map}$ult")))
Linha4 53 3 'MAP necessário' 'kPa' '={c}52*6.894757'
$cad += ,@(54, 3, 0, (@('Razão de pressão PR','-') + (FIt "=IF({c}18=""NAO"","""",Iteracao!{pr}$ult)")))
$cad += ,@(55, 3, 0, (@('T2 (saída do compressor)','°C') + (FIt "=IF({c}18=""SIM"",Iteracao!{t2}$ult,"""")")))
Linha4 56 3 'MAP ÷ P_atm local' '-' '={c}53/{c}47'
Linha4 57 3 'Vazão por bico (massa)' 'g/min' '=IFERROR({c}43*1000/60/({c}12*{c}27),"")'
Linha4 58 3 'Vazão por bico (combustível)' 'cc/min' '=IFERROR({c}57/{c}28,"")'
Linha4 59 3 'Vazão nominal (fluido de teste)' 'cc/min' '=IFERROR({c}58*SQRT({c}28/{c}29),"")'
Linha4 60 3 'Volume por cilindro' 'L' '=IFERROR({c}45/1000/{c}12,"")'
Linha4 61 3 'Ar por cilindro m_ar_cil' 'g' '=IFERROR({c}17*{c}53*1000*{c}60/1000/(287.05*({c}50+273.15))*1000,"")'
Linha4 62 3 'Combustível por injeção m_f_inj' 'mg' '=IFERROR({c}61/({c}10*{c}11)*1000,"")' 'Sequencial: 1 injeção por ciclo'
Linha4 63 3 'Vazão real do bico' 'cc/min' '=IFERROR({c}30*SQRT({c}32/{c}31)*SQRT({c}29/{c}28),"")' 'Corrige pressão e densidade'
Linha4 64 3 'TI efetivo' 'ms' '=IFERROR({c}62/1000000/({c}63*{c}28/60000)*1000,"")'
Linha4 65 3 'TI com tempo morto' 'ms' '=IFERROR({c}64+{c}33,"")'
Linha4 66 3 'Ciclo (120000 / rpm)' 'ms' '=120000/{c}16'
Linha4 67 5 'DC do bico' '%' '=IFERROR({c}65/{c}66,"")'
Linha4 68 5 'DC com bico alternativo 1' '%' '=IFERROR(({c}62/1000000/({c}34*SQRT({c}35/{c}31)*SQRT({c}29/{c}28)*{c}28/60000)*1000+{c}33)/{c}66,"")'
Linha4 69 5 'DC com bico alternativo 2' '%' '=IFERROR(({c}62/1000000/({c}36*SQRT({c}37/{c}31)*SQRT({c}29/{c}28)*{c}28/60000)*1000+{c}33)/{c}66,"")'
Linha4 70 3 'BMEP' 'bar' '=1200*{c}40/({c}45/1000*{c}16)'
Linha4 71 3 'Torque' 'N·m' '=9549*{c}40/{c}16'
Linha4 72 3 'Velocidade média do pistão' 'm/s' '=IFERROR(2*{c}14/1000*{c}16/60,"")'
$cad += ,@(73, 0, 7, @('SEMÁFORO (SPEC 7.4)'))
Linha4 74 0 'DC do bico' '-' '=IF(ISNUMBER({c}67),IF({c}67<=0.8,"VERDE",IF({c}67<=0.85,"AMARELO","VERMELHO")),"")'
Linha4 75 0 'BMEP' '-' '=IF({c}18="FIXO","",IF({c}70<=IF({c}18="NAO",13,22),"VERDE",IF({c}70<=IF({c}18="NAO",13,22)*1.15,"AMARELO","VERMELHO")))' 'Vermelho só com pistões ou bielas originais'
Linha4 76 0 'Velocidade do pistão' '-' '=IF(ISNUMBER({c}72),IF({c}72<=20,"VERDE",IF({c}72<=25,"AMARELO","VERMELHO")),"")'
Linha4 77 0 'Aspirado: MAP ÷ P_atm local' '-' '=IF({c}18="NAO",IF({c}56<=0.9,"VERDE",IF({c}56<=0.98,"AMARELO","VERMELHO")),"")'
Linha4 78 3 'Aspirado: MAP ÷ P_atm ao nível do mar' '-' '=IF({c}18="NAO",{c}53/101.325,"")'

# ---------------- Iteracao (MAP <-> Tm) ----------------
$cabIt = @('k')
foreach ($c in $casos) { $nome = @{C='A';D='B';E='C';F='D'}[$c]; $cabIt += @("Tm °C ($nome)","MAP psia ($nome)","PR ($nome)","T2 °C ($nome)") }
$iter = @(
 @(1,0,6,@('Iteração MAP - Tm (SPEC 7.1)')),
 @(2,0,9,@('Começa em Tm = 50 °C (sobrealimentado). Para FIXO e NAO, Tm não muda. Critério: |ΔTm| < 0,01 °C.')),
 @(3,1,1,$cabIt)
)
for ($k = 0; $k -le ($ult - 4); $k++) {
    $r = 4 + $k
    $vals = @($k)
    foreach ($c in $casos) {
        $tm = $it[$c][0]; $map = $it[$c][1]; $pr = $it[$c][2]; $t2 = $it[$c][3]
        if ($k -eq 0) { $fTm = "=Cadeia!${c}49" }
        else { $fTm = "=IF(Cadeia!${c}18=""SIM"",${t2}$($r-1)-Cadeia!${c}25*(${t2}$($r-1)-Cadeia!${c}20),${tm}$($r-1))" }
        $fMap = "=Cadeia!${c}41*639.6*(460+(${tm}$r*9/5+32))/(Cadeia!${c}17*Cadeia!${c}16/2*Cadeia!${c}46)"
        $fPr = "=(${map}$r+Cadeia!${c}22)/(Cadeia!${c}48-Cadeia!${c}23)"
        $fT2 = "=IF(Cadeia!${c}18=""SIM"",(Cadeia!${c}20+273.15)*(1+(${pr}$r^0.2857-1)/Cadeia!${c}24)-273.15,"""")"
        $vals += @($fTm, $fMap, $fPr, $fT2)
    }
    $iter += ,@($r, 3, 0, $vals)
}

# ---------------- Conferencia (SPEC 7.6, tolerância 1%) ----------------
$esperados = @(
 @('Wa (lb/min)',41,57.31,37.57,14.09,42.27),
 @('MAP (psia)',52,25.89,'','',''),
 @('PR',54,2.04,2.91,2.78,''),
 @('Combustível (kg/h)',43,'',152.8,37.5,103.3),
 @('BSFC',44,'','',0.558,0.513),
 @('Cilindrada (cc)',45,'','',999,6002),
 @('Vazão por bico (g/min)',57,'',796,260,179),
 @('Vazão por bico (cc/min de combustível)',58,'',984,344,237),
 @('Vazão nominal (cc/min)',59,'',1058,358,247),
 @('P_atm (kPa)',47,'',94.3,94.3,''),
 @('MAP (kPa)',53,'',241,229,96.6),
 @('T2 (°C)',55,'',173,165,''),
 @('Tm (°C)',50,'',55,53,35),
 @('TI efetivo (ms)',64,'',13.81,14.19,14.01),
 @('TI com tempo morto (ms)',65,'',14.81,15.19,15.01),
 @('Ciclo (ms)',66,'',17.14,21.82,18.46),
 @('DC',67,'',0.86,0.70,0.81),
 @('DC bico 1.300 cc a 3 bar',68,'',0.71,'',''),
 @('DC bico 1.050 cc a 4 bar',69,'',0.76,'',''),
 @('BMEP (bar)',70,'',25.2,24.1,10.2),
 @('Torque (N·m)',71,'',401,192,486),
 @('Velocidade do pistão (m/s)',72,'',21.7,14.0,17.4),
 @('Aspirado: MAP ÷ P_atm a 600 m',56,'','','',1.024),
 @('Aspirado: MAP ÷ P_atm ao nível do mar',78,'','','',0.953),
 @('Semáforo DC',74,'','VERMELHO','VERDE','AMARELO'),
 @('Semáforo BMEP',75,'','AMARELO','AMARELO','VERDE'),
 @('Semáforo pistão',76,'','AMARELO','VERDE','VERDE'),
 @('Semáforo aspirado (600 m)',77,'','','','VERMELHO')
)
$cabConf = @('Item')
foreach ($nome in 'A','B','C','D') { $cabConf += @("Esperado $nome","Calculado $nome","$nome") }
$conf = @(
 @(1,0,6,@('Conferência dos casos golden (SPEC 7.6) - tolerância de 1%')),
 @(3,1,1,$cabConf)
)
$r = 4
foreach ($e in $esperados) {
    $vals = @($e[0])
    for ($j = 0; $j -lt 4; $j++) {
        $c = $casos[$j]; $exp = $e[2+$j]
        $colEsp = $colunas[1+3*$j]; $colCalc = $colunas[2+3*$j]
        if ("$exp" -eq '') { $vals += @('','','') ; continue }
        $vals += @($exp, "=Cadeia!$c$($e[1])", "=IF(ISNUMBER($colEsp$r),IF(ABS($colCalc$r/$colEsp$r-1)<=0.01,""OK"",""FALHA""),IF($colCalc$r=$colEsp$r,""OK"",""FALHA""))")
    }
    $conf += ,@($r, 3, 0, $vals)
    $r++
}
$fim = $r - 1
$conf += ,@(($r + 1), 0, 8, @('Resultado', "=IF(COUNTIF(B4:M$fim,""FALHA"")=0,""TODOS OK"",""HÁ FALHAS"")", "=COUNTIF(B4:M$fim,""OK"")&"" OK / ""&COUNTIF(B4:M$fim,""FALHA"")&"" FALHA"""))

# ---------------- Leia-me ----------------
$leia = @(
 @(1,0,6,@('Planilha de conferência - SPEC Calibra')),
 @(3,0,0,@('Objetivo: conferir as fórmulas da SPEC (seção 7.1) e os casos golden (7.6) antes de escrever o calibra-core.')),
 @(4,0,0,@('Amarelo = entrada; azul = cálculo (não editar); verde = conferência.')),
 @(5,0,0,@('Abas: Cadeia (fórmulas, colunas = golden A a D), Iteracao (MAP-Tm), Combustiveis (SPEC 7.2), Conferencia (esperado x calculado, 1%).')),
 @(6,0,0,@('Golden B usa 2.000 cc (a spec não dá o diâmetro). Golden A usa o caminho por BSFC; B, C e D usam k_ar. Ver docs/decisoes.md.')),
 @(7,0,0,@('Esta planilha é gerada por gerar-planilha.ps1. Para mudar de verdade, editar o script e gerar de novo.'))
)

$abas = @(
 @('Leia-me', (Planilha $leia @(120) 1 0)),
 @('Cadeia', (Planilha $cad @(38,14,16,18,18,20,48))),
 @('Iteracao', (Planilha $iter (@(5) + (@(11) * 16)) 1 16)),
 @('Combustiveis', (Planilha $comb @(32,12,14,12,14,16,14,40) 1 6)),
 @('Conferencia', (Planilha $conf (@(38) + (@(12,12,7) * 4)) 1 12))
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
