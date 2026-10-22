package br.com.calibra.core.mapa;

import static br.com.calibra.core.memoria.MemoriaCalculo.e;
import static br.com.calibra.core.util.NumeroBr.formatar;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.ToDoubleFunction;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Formulas;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.memoria.MemoriaCalculo;
import br.com.calibra.core.memoria.NivelAviso;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.ModeloCombustivel;
import br.com.calibra.core.pecas.PerfilInjecao;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.LimiaresSemaforo;
import br.com.calibra.core.unidades.Conversoes;

/**
 * O gerador do mapa base (SPEC 7.5; decisão 294). Conservador por padrão: na dúvida, mais rico e menos avanço; nunca
 * inventa avanço. As contas de cada célula são as da SPEC 7.1 (as mesmas do dimensionamento), com a VE do modelo da
 * família ajustada ao projeto e a temperatura de referência do ar do perfil da injeção.
 *
 * <p>VE de uma célula = VE do modelo (rpm ÷ rpm de torque, MAP ÷ 100 kPa) × fator do projeto. O fator faz o mapa bater
 * com o dimensionamento: no torque máximo, VE do torque ÷ VE do modelo ali; na potência máxima, VE da potência ÷ VE do
 * modelo ali; entre as duas rotações, linear; antes do torque e depois da potência, o da ponta.
 */
public final class GeradorMapaBase {

    /** Passo dos eixos padrão (SPEC 7.5). */
    static final double PASSO_RPM = 500;
    static final double PASSO_KPA = 10;
    static final double PRIMEIRA_CARGA_KPA = 20;
    static final double ULTIMA_CARGA_ASPIRADO_KPA = 105;
    static final double FOLGA_CARGA = 1.1;

    private GeradorMapaBase() {
    }

    /**
     * Gera o mapa base.
     *
     * @throws EntradaInvalidaException em português, quando falta algo que o mapa precisa (o bico, a rotação de
     *     torque máximo) ou quando o próprio dimensionamento recusa a entrada
     */
    public static Resultado<MapaBase> gerar(EntradaMapaBase entrada) {
        return new Geracao(entrada).gerar();
    }

    /** A memória de cálculo de uma célula de combustível, para a tela mostrar a conta de qualquer célula. */
    public static Resultado<CelulaMapa> memoriaDaCelula(EntradaMapaBase entrada, double rotacao, double cargaKpa) {
        Geracao geracao = new Geracao(entrada);
        MemoriaCalculo memoria = new MemoriaCalculo();
        return memoria.resultado(geracao.celula(rotacao, cargaKpa, memoria));
    }

    /** Os eixos padrão da SPEC 7.5: de 500 em 500 rpm até a máxima; de 20 kPa até 1,1 × o MAP máximo, de 10 em 10. */
    static List<Double> rotacoesPadrao(int rotacaoMaxima) {
        List<Double> rotacoes = new ArrayList<>();
        for (double rpm = PASSO_RPM; rpm < rotacaoMaxima; rpm += PASSO_RPM) {
            rotacoes.add(rpm);
        }
        rotacoes.add((double) rotacaoMaxima);
        return rotacoes;
    }

    /** No aspirado, até 105 kPa; no sobrealimentado, até 1,1 × o MAP máximo, arredondado para cima de 10 em 10. */
    static List<Double> cargasPadrao(boolean sobrealimentado, double mapMaximoKpa) {
        double ultima = sobrealimentado
                ? Math.ceil(FOLGA_CARGA * mapMaximoKpa / PASSO_KPA - 1e-9) * PASSO_KPA
                : ULTIMA_CARGA_ASPIRADO_KPA;
        List<Double> cargas = new ArrayList<>();
        for (double kpa = PRIMEIRA_CARGA_KPA; kpa < ultima - 1e-9; kpa += PASSO_KPA) {
            cargas.add(kpa);
        }
        cargas.add(ultima);
        return cargas;
    }

    private static final class Geracao {

        private final EntradaMapaBase entrada;
        private final EntradaDimensionamento dim;
        private final ConfiguracaoMotor motor;
        private final Meta meta;
        private final Combustivel combustivel;
        private final RegrasMapaBase regras;
        private final PerfilInjecao perfil;
        private final ModeloVe modelo;
        private final Dimensionamento dimensionamento;
        private final MemoriaCalculo memoria = new MemoriaCalculo();
        private final List<AlertaMapa> alertas = new ArrayList<>();

        private final boolean sobrealimentado;
        private final double mapAncoraKpa;
        private final double mapMaximoKpa;
        private final double rpmTorque;
        private final double rpmPotencia;
        private final int injecoes;
        private final double volumeCilindroM3;
        private final double afr;
        private final double vazaoBicoKgS;
        private final double tempoMortoMs;
        private final double temperaturaReferenciaC;
        private final boolean tempoMortoNaTabela;
        private final ModeloCombustivel modeloCombustivel;
        private final double fatorTorque;
        private final double fatorPotencia;
        private final double lambdaCem;
        private final double lambdaPlena;

        Geracao(EntradaMapaBase entrada) {
            this.entrada = entrada;
            this.dim = entrada.dimensionamento();
            this.motor = dim.motor();
            this.meta = dim.meta();
            this.combustivel = motor.combustivel();
            this.regras = entrada.regras();
            this.perfil = entrada.injecao() == null ? PerfilInjecao.vazio() : entrada.injecao().perfil();
            this.modelo = entrada.modeloVe();
            List<String> faltam = new ArrayList<>();
            if (dim.bico() == null) {
                faltam.add("O mapa base precisa do bico: escolha ou digite o bico no Dimensionamento e salve o "
                        + "cálculo.");
            }
            if (meta.rotacaoTorqueMaximo() == null) {
                faltam.add("O mapa base precisa da rotação de torque máximo (meta do projeto): é ela que posiciona "
                        + "o modelo de VE.");
            }
            faltam.addAll(regras.validar().stream().map(erro -> "Regras do mapa base: " + erro).toList());
            if (!faltam.isEmpty()) {
                throw new EntradaInvalidaException(faltam);
            }
            this.dimensionamento = CalculoDimensionamento.calcular(dim).valor();
            Dimensionamento.LadoDoAr ar = dimensionamento.ladoDoAr();
            this.sobrealimentado = ar.sobrealimentado();
            this.rpmTorque = meta.rotacaoTorqueMaximo();
            this.rpmPotencia = meta.rotacaoPotenciaMaxima();
            this.injecoes = motor.modoInjecao().injecoesPorCiclo();

            this.mapAncoraKpa = memoria.passo("MAP do dimensionamento (âncora da VE)", "MAP necessário na potência "
                    + "máxima, do cálculo do projeto", ar.mapKpa(), "kPa");
            this.mapMaximoKpa = memoria.passo("MAP máximo previsto", "maior entre o MAP necessário e a P_atm local",
                    dimensionamento.sensorMap().mapMaximoPrevistoKpa(), "kPa", e("MAP", ar.mapKpa(), "kPa"),
                    e("P_atm", ar.patmKpa(), "kPa"));
            this.volumeCilindroM3 = memoria.passo("Volume de um cilindro", "V_cil = π/4 × D² × S",
                    motor.volumeCilindroCc() / 1e6, "m³", e("D", motor.diametroMm(), "mm"),
                    e("S", motor.cursoMm(), "mm"));
            this.afr = memoria.passo("AFR estequiométrico", combustivel.nome(), combustivel.afrEstequiometrico(),
                    ":1");
            this.vazaoBicoKgS = vazaoBico();
            this.tempoMortoMs = tempoMorto(dim.bico());
            this.modeloCombustivel = modeloCombustivel();
            this.tempoMortoNaTabela = tempoMortoNaTabela();
            this.temperaturaReferenciaC = temperaturaReferencia();

            double cargaAncora = mapAncoraKpa / ModeloVe.KPA_DA_CARGA_RELATIVA;
            double veModeloTorque = memoria.passo("VE do modelo no torque máximo",
                    "modelo(rpm ÷ rpm_torque = 1; MAP ÷ 100 kPa)", modelo.ve(1.0, cargaAncora), "fração",
                    e("MAP ÷ 100", cargaAncora, ""));
            double veModeloPotencia = memoria.passo("VE do modelo na potência máxima",
                    "modelo(rpm_potência ÷ rpm_torque; MAP ÷ 100 kPa)", modelo.ve(rpmPotencia / rpmTorque,
                            cargaAncora), "fração", e("rpm_potência ÷ rpm_torque", rpmPotencia / rpmTorque, ""),
                    e("MAP ÷ 100", cargaAncora, ""));
            this.fatorTorque = memoria.passo("Fator da VE no torque máximo", "fator = VE do torque ÷ VE do modelo",
                    dim.veNoTorque() / veModeloTorque, "", e("VE do torque", dim.veNoTorque(), ""),
                    e("VE do modelo", veModeloTorque, ""));
            this.fatorPotencia = memoria.passo("Fator da VE na potência máxima",
                    "fator = VE da potência ÷ VE do modelo", dim.ve() / veModeloPotencia, "",
                    e("VE da potência", dim.ve(), ""), e("VE do modelo", veModeloPotencia, ""));

            this.lambdaCem = memoria.passo("Lambda em " + formatar(regras.plenaCargaKpa(), 0) + " kPa",
                    sobrealimentado ? "lambda de plena carga do aspirado (combustível)"
                            : "lambda alvo de plena carga do projeto",
                    sobrealimentado ? combustivel.lambdaAspiradoPlenaCarga() : dim.lambdaAlvo(), "");
            this.lambdaPlena = memoria.passo("Lambda acima de " + formatar(regras.plenaCargaKpa(), 0) + " kPa",
                    "lambda alvo do projeto na potência máxima", dim.lambdaAlvo(), "");
        }

        Resultado<MapaBase> gerar() {
            avisosDoPerfilEDoModelo();
            EixosDaTabela eixosCombustivel = eixos(FuncaoTabela.COMBUSTIVEL, null);
            EixosDaTabela eixosLambda = eixos(FuncaoTabela.LAMBDA_ALVO, eixosCombustivel);
            EixosDaTabela eixosIgnicao = eixos(FuncaoTabela.IGNICAO, eixosCombustivel);

            List<List<CelulaMapa>> celulas = new ArrayList<>();
            for (double carga : eixosCombustivel.cargas()) {
                List<CelulaMapa> linha = new ArrayList<>();
                for (double rotacao : eixosCombustivel.rotacoes()) {
                    linha.add(celula(rotacao, carga, new MemoriaCalculo()));
                }
                celulas.add(linha);
            }
            Tabela ve = tabela(eixosCombustivel, celulas, CelulaMapa::ve);
            Tabela dutyCycle = tabela(eixosCombustivel, celulas, CelulaMapa::dutyCycle);
            Tabela combustivelTabela = modeloCombustivel == ModeloCombustivel.MS
                    ? tabela(eixosCombustivel, celulas, CelulaMapa::tiTabelaMs)
                    : tabela(eixosCombustivel, celulas, c -> c.ve() * 100.0);
            Tabela lambda = Tabela.preencher(eixosLambda.rotacoes(), eixosLambda.cargas(),
                    (rotacao, carga) -> lambda(carga));
            conferirDutyCycle(celulas);
            conferirMarchaLenta();

            Tabela ignicaoFonte;
            Tabela ignicao;
            FonteIgnicao fonte = entrada.ignicao();
            if (fonte == null) {
                ignicaoFonte = Tabela.vazia(eixosIgnicao.rotacoes(), eixosIgnicao.cargas());
                ignicao = ignicaoFonte;
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.IGNICAO_SEM_FONTE, Cor.VERMELHO, "Ignição sem modelo: "
                        + "o app não inventa avanço. A tabela de ignição ficou vazia; escolha um modelo da família "
                        + "ou digite a tabela antes de dar partida."));
            } else {
                ignicaoFonte = Tabela.preencher(eixosIgnicao.rotacoes(), eixosIgnicao.cargas(), (rotacao, carga) -> {
                    OptionalDouble valor = fonte.avanco().interpolar(rotacao, carga);
                    return valor.isPresent() ? valor.getAsDouble() : null;
                });
                ignicao = reduzir(ignicaoFonte, fonte);
            }

            MapaBase.Travas travas = travas();
            MapaBase mapa = new MapaBase(modeloCombustivel, tempoMortoNaTabela, temperaturaReferenciaC, ve, lambda,
                    combustivelTabela, dutyCycle, ignicaoFonte, ignicao, travas, alertas);
            return memoria.resultado(mapa);
        }

        // ---------- Uma célula de combustível (SPEC 7.1) ----------

        CelulaMapa celula(double rotacao, double cargaKpa, MemoriaCalculo nota) {
            double ve = nota.passo("VE da célula", "VE = modelo(rpm ÷ rpm_torque; MAP ÷ 100 kPa) × fator",
                    modelo.ve(rotacao / rpmTorque, cargaKpa / ModeloVe.KPA_DA_CARGA_RELATIVA) * fator(rotacao),
                    "fração", e("rpm", rotacao, "rpm"), e("MAP", cargaKpa, "kPa"), e("rpm_torque", rpmTorque, "rpm"),
                    e("fator", fator(rotacao), ""));
            double lambda = nota.passo("Lambda alvo da célula", "regras do mapa base (SPEC 7.5)", lambda(cargaKpa), "",
                    e("MAP", cargaKpa, "kPa"));
            double tK = Conversoes.cParaK(temperaturaReferenciaC);
            double massaArKg = nota.passo("Massa de ar por cilindro", Formulas.TEXTO_MASSA_AR,
                    Formulas.massaArCilindroKg(ve, cargaKpa * 1000.0, volumeCilindroM3, tK) * 1000.0, "g",
                    e("VE", ve, ""), e("MAP", cargaKpa * 1000.0, "Pa"), e("V_cil", volumeCilindroM3, "m³"),
                    e("T_ref", tK, "K")) / 1000.0;
            double massaCombustivelKg = nota.passo("Combustível por injeção", Formulas.TEXTO_MASSA_COMBUSTIVEL,
                    Formulas.massaCombustivelPorInjecao(massaArKg, afr, lambda, injecoes) * 1e6, "mg",
                    e("m_ar_cil", massaArKg * 1000.0, "g"), e("AFR_est", afr, ":1"), e("lambda", lambda, ""),
                    e("injecoesPorCiclo", injecoes, "")) / 1e6;
            double tiEfetivo = nota.passo("Tempo de injeção efetivo", "TI_ef = m_f_inj / mdot_bico",
                    Formulas.tempoInjecaoMs(massaCombustivelKg, vazaoBicoKgS, 0.0), "ms",
                    e("m_f_inj", massaCombustivelKg * 1e6, "mg"), e("mdot", vazaoBicoKgS * 1000.0, "g/s"));
            double tiTabela = nota.passo("Tempo na tabela", tempoMortoNaTabela ? Formulas.TEXTO_TEMPO_INJECAO
                    : "TI_tabela = TI_ef (a injeção soma o tempo morto)",
                    tiEfetivo + (tempoMortoNaTabela ? tempoMortoMs : 0.0), "ms", e("TI_ef", tiEfetivo, "ms"),
                    e("t_morto", tempoMortoMs, "ms"));
            // O DC é o tempo que o bico fica aberto de verdade: com o ar na temperatura esperada na célula (a injeção
            // corrige a tabela por ela), e não na de referência da tabela.
            double tArC = temperaturaDoAr(cargaKpa);
            double tiNoAr = nota.passo("TI efetivo com o ar esperado na célula", "TI_ar = TI_ef × T_ref / T_ar",
                    tiEfetivo * tK / Conversoes.cParaK(tArC), "ms", e("TI_ef", tiEfetivo, "ms"), e("T_ref", tK, "K"),
                    e("T_ar", Conversoes.cParaK(tArC), "K"));
            double dc = nota.passo("DC do bico na célula", Formulas.TEXTO_DC,
                    Formulas.dutyCycle(tiNoAr + tempoMortoMs, injecoes, rotacao), "fração",
                    e("TI com tempo morto", tiNoAr + tempoMortoMs, "ms"), e("injecoesPorCiclo", injecoes, ""),
                    e("RPM", rotacao, "rpm"));
            return new CelulaMapa(rotacao, cargaKpa, ve, lambda, tiEfetivo, tiTabela, dc);
        }

        /** O fator do projeto na rotação: o do torque, o da potência e linear entre os dois. */
        private double fator(double rotacao) {
            if (rotacao <= rpmTorque) {
                return fatorTorque;
            }
            if (rotacao >= rpmPotencia) {
                return fatorPotencia;
            }
            double fracao = (rotacao - rpmTorque) / (rpmPotencia - rpmTorque);
            return fatorTorque + fracao * (fatorPotencia - fatorTorque);
        }

        /** As regras de lambda da SPEC 7.5, com a riqueza extra acima de 90 kPa. */
        private double lambda(double cargaKpa) {
            double base;
            if (cargaKpa <= regras.cargaParcialAteKpa()) {
                base = regras.lambdaCargaParcial();
            } else if (cargaKpa <= regras.plenaCargaKpa()) {
                double fracao = (cargaKpa - regras.cargaParcialAteKpa())
                        / (regras.plenaCargaKpa() - regras.cargaParcialAteKpa());
                base = regras.lambdaCargaParcial() + fracao * (lambdaCem - regras.lambdaCargaParcial());
            } else {
                base = sobrealimentado ? lambdaPlena : lambdaCem;
            }
            if (cargaKpa > regras.riquezaExtraAcimaKpa()) {
                base -= regras.riquezaExtra();
                if (meta.uso() == Uso.NAUTICO || meta.uso() == Uso.CIRCUITO_ENDURANCE) {
                    base -= regras.riquezaExtraUsoContinuo();
                }
            }
            return base;
        }

        // ---------- Bico, perfil e eixos ----------

        /** A vazão real (no combustível e na pressão de base) das bancadas que abrem juntas, em kg/s. */
        private double vazaoBico() {
            Bico primario = dim.bico();
            Bico secundario = dim.bicoSecundario();
            double densidade = combustivel.densidadeKgL();
            double real = vazaoReal(primario);
            if (secundario != null) {
                real += vazaoReal(secundario);
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.DUAS_BANCADAS, Cor.VERMELHO, "Duas bancadas por "
                        + "cilindro: o mapa base soma a vazão das duas, como se abrissem juntas. A entrada da "
                        + "bancada secundária (por carga ou pressão) se configura na injeção; esta versão não gera "
                        + "a divisão."));
            }
            return memoria.passo("Vazão do bico (massa)", "mdot = Q_real × rho_comb", real * densidade / 60000.0
                    * 1000.0, "g/s", e("Q_real", real, "cm³/min"), e("rho_comb", densidade, "kg/L")) / 1000.0;
        }

        private double vazaoReal(Bico bico) {
            double naPressao = Formulas.corrigirPorPressao(bico.vazaoNominalCm3Min(), bico.pressaoTesteKpa(),
                    dim.pressaoBaseKpa());
            return memoria.passo("Vazão real do bico " + bico.nome(),
                    "Q_real = Q_nom × raiz(dP_base / dP_teste) × raiz(rho_teste / rho_comb)",
                    Formulas.vazaoNoCombustivel(naPressao, bico.densidadeFluidoTesteKgL(), combustivel.densidadeKgL()),
                    "cm³/min", e("Q_nom", bico.vazaoNominalCm3Min(), "cm³/min"),
                    e("dP_base", dim.pressaoBaseKpa(), "kPa"), e("dP_teste", bico.pressaoTesteKpa(), "kPa"));
        }

        private double tempoMorto(Bico bico) {
            OptionalDouble valor = bico.tempoMorto().valorMs(dim.tensaoV(), dim.pressaoBaseKpa());
            if (valor.isEmpty()) {
                throw new EntradaInvalidaException(bico.nome() + ": a tabela de tempo morto não cobre a condição "
                        + "pedida (informe uma tensão dentro da tabela).");
            }
            return memoria.passo("Tempo morto do bico", "da tabela do bico, na tensão e na pressão de base",
                    valor.getAsDouble(), "ms");
        }

        private ModeloCombustivel modeloCombustivel() {
            if (entrada.injecao() == null) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.PERFIL_INCOMPLETO, Cor.AMARELO, "Sem injeção "
                        + "escolhida: o mapa sai nos eixos padrão da SPEC, em ms, com o tempo morto somado e o ar de "
                        + "referência a " + formatar(PerfilInjecao.TEMPERATURA_REFERENCIA_PADRAO_C, 0) + " °C. "
                        + "Para transcrever, escolha a injeção com o perfil dela."));
                return ModeloCombustivel.MS;
            }
            if (perfil.modeloCombustivel() == null) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.PERFIL_INCOMPLETO, Cor.AMARELO, "O perfil da "
                        + "injeção não diz o modelo de combustível: o mapa sai em ms. Confira no manual dela."));
                return ModeloCombustivel.MS;
            }
            return perfil.modeloCombustivel();
        }

        private boolean tempoMortoNaTabela() {
            if (perfil.tabelaMsIncluiTempoMorto() != null) {
                return perfil.tabelaMsIncluiTempoMorto();
            }
            if (entrada.injecao() != null && modeloCombustivel == ModeloCombustivel.MS) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.PERFIL_INCOMPLETO, Cor.AMARELO, "O perfil da "
                        + "injeção não diz se a tabela em ms inclui o tempo morto: o mapa soma o tempo morto (fica "
                        + "mais rico, " + formatar(tempoMortoMs, 2) + " ms a mais em cada célula). Confira no manual."));
            }
            return true;
        }

        private double temperaturaReferencia() {
            Double doPerfil = perfil.temperaturaReferenciaArC();
            if (doPerfil == null && entrada.injecao() != null) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.PERFIL_INCOMPLETO, Cor.AMARELO, "O perfil da "
                        + "injeção não diz a temperatura de referência da correção por ar: o mapa usa "
                        + formatar(PerfilInjecao.TEMPERATURA_REFERENCIA_PADRAO_C, 0) + " °C (padrão da SPEC). "
                        + "Confira no manual."));
            }
            return memoria.passo("Temperatura de referência do ar", doPerfil == null ? "padrão da SPEC 7.5"
                    : "do perfil da injeção", doPerfil == null ? PerfilInjecao.TEMPERATURA_REFERENCIA_PADRAO_C
                            : doPerfil, "°C");
        }

        private record EixosDaTabela(List<Double> rotacoes, List<Double> cargas) {
        }

        /**
         * Os eixos da tabela: os do perfil (só por MAP); sem eles, os da tabela de combustível; sem ela, os padrão
         * da SPEC. Eixo curto e tabela por TPS viram alerta.
         */
        private EixosDaTabela eixos(FuncaoTabela funcao, EixosDaTabela daTabelaDeCombustivel) {
            EixosDaTabela padrao = daTabelaDeCombustivel != null ? daTabelaDeCombustivel
                    : new EixosDaTabela(rotacoesPadrao(motor.rotacaoMaxima()), cargasPadrao(sobrealimentado, mapMaximoKpa));
            PerfilTabela tabela = perfil.tabela(funcao).orElse(null);
            if (tabela == null) {
                return padrao;
            }
            if (tabela.tipoCarga() == TipoCarga.TPS_PERCENTUAL) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.TABELA_POR_TPS, Cor.VERMELHO, "A tabela de "
                        + funcao.rotulo().toLowerCase() + " da injeção (\"" + tabela.nome() + "\") é por TPS: o "
                        + "gerador desta versão só faz por MAP. A tabela saiu nos eixos de MAP, só como referência; "
                        + "monte a da injeção pelo assistente dela."));
                return padrao;
            }
            double ultimaCarga = tabela.cargas().getLast();
            if (ultimaCarga < mapMaximoKpa) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.EIXO_CURTO, Cor.VERMELHO, "O eixo de carga da tabela de "
                        + funcao.rotulo().toLowerCase() + " vai até " + formatar(ultimaCarga, 0) + " kPa, abaixo do "
                        + "MAP máximo previsto (" + formatar(mapMaximoKpa, 0) + " kPa): acima dele, a injeção repete "
                        + "a última linha. Limite a pressão do turbo ou mude o eixo na injeção."));
            }
            double ultimaRotacao = tabela.rotacoes().getLast();
            if (ultimaRotacao < motor.rotacaoMaxima()) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.EIXO_CURTO, Cor.AMARELO, "O eixo de rotação da tabela "
                        + "de " + funcao.rotulo().toLowerCase() + " vai até " + formatar(ultimaRotacao, 0)
                        + " rpm, abaixo da rotação máxima (" + motor.rotacaoMaxima() + " rpm): acima dele, a injeção "
                        + "repete a última coluna."));
            }
            return new EixosDaTabela(tabela.rotacoes(), tabela.cargas());
        }

        private void avisosDoPerfilEDoModelo() {
            if (!modelo.serve(motor.valvulasPorCilindro(), motor.comando())) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.MODELO_VE, Cor.AMARELO, "O modelo de VE \""
                        + modelo.nome() + "\" é de outra família (o motor tem " + motor.valvulasPorCilindro()
                        + " válvulas por cilindro e comando " + motor.comando().rotulo().toLowerCase() + ")."));
            }
            double rotacaoRelativa = motor.rotacaoMaxima() / rpmTorque;
            double cargaRelativa = FOLGA_CARGA * mapMaximoKpa / ModeloVe.KPA_DA_CARGA_RELATIVA;
            if (rotacaoRelativa > modelo.ve().rotacoes().getLast() || cargaRelativa > modelo.ve().cargas().getLast()) {
                memoria.aviso(NivelAviso.INFORMACAO, "O mapa passa dos eixos do modelo de VE: lá, vale a VE da "
                        + "borda do modelo (ela não continua subindo).");
            }
            if (meta.uso() != Uso.RUA) {
                memoria.aviso(NivelAviso.INFORMACAO, "A SPEC define o lambda " + formatar(regras.lambdaCargaParcial(),
                        2) + " abaixo de " + formatar(regras.cargaParcialAteKpa(), 0) + " kPa em RUA; em "
                        + meta.uso().rotulo() + ", o mapa usa o mesmo valor (editável em Configurações › Mapa base).");
            }
        }

        // ---------- Conferências do combustível ----------

        /**
         * O maior DC onde o dimensionamento vale: até a rotação de potência máxima e até o MAP máximo previsto. Acima
         * da potência, o mapa mantém a VE (fica mais rico, de propósito), e o DC de lá vai só como informação.
         */
        private void conferirDutyCycle(List<List<CelulaMapa>> celulas) {
            CelulaMapa pior = null;
            CelulaMapa piorAcima = null;
            for (List<CelulaMapa> linha : celulas) {
                for (CelulaMapa celula : linha) {
                    if (celula.cargaKpa() > mapMaximoKpa + 1e-9 || celula.rotacao() > motor.rotacaoMaxima() + 1e-9) {
                        continue;
                    }
                    if (celula.rotacao() <= rpmPotencia + 1e-9) {
                        pior = pior == null || celula.dutyCycle() > pior.dutyCycle() ? celula : pior;
                    } else {
                        piorAcima = piorAcima == null || celula.dutyCycle() > piorAcima.dutyCycle() ? celula : piorAcima;
                    }
                }
            }
            if (piorAcima != null && (pior == null || piorAcima.dutyCycle() > pior.dutyCycle())) {
                memoria.aviso(NivelAviso.INFORMACAO, "Acima da potência máxima, o mapa mantém a VE (fica mais rico): "
                        + "lá, o DC do bico chega a " + formatar(piorAcima.dutyCycle() * 100, 0) + "% em "
                        + formatar(piorAcima.rotacao(), 0) + " rpm e " + formatar(piorAcima.cargaKpa(), 0) + " kPa.");
            }
            if (pior == null) {
                return;
            }
            LimiaresSemaforo limiares = dim.parametros().limiares();
            Cor cor = pior.dutyCycle() > limiares.dcAmareloAte() ? Cor.VERMELHO
                    : pior.dutyCycle() > limiares.dcVerdeAte() ? Cor.AMARELO : Cor.VERDE;
            memoria.passo("Maior DC do bico no mapa", "maior DC até a rotação de potência e o MAP máximo previsto",
                    pior.dutyCycle(), "fração", e("rpm", pior.rotacao(), "rpm"), e("MAP", pior.cargaKpa(), "kPa"));
            if (cor != Cor.VERDE) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.DC_ALTO, cor, "DC do bico de "
                        + formatar(pior.dutyCycle() * 100, 0) + "% em " + formatar(pior.rotacao(), 0) + " rpm e "
                        + formatar(pior.cargaKpa(), 0) + " kPa (verde até " + formatar(limiares.dcVerdeAte() * 100, 0)
                        + "%): o bico chega no limite nessa parte do mapa."));
            }
        }

        /** O TI efetivo na marcha lenta informada no cálculo (SPEC 7.3), na célula dela. */
        private void conferirMarchaLenta() {
            if (dim.marchaLenta() == null) {
                return;
            }
            CelulaMapa lenta = celula(motor.rotacaoMarchaLenta(), dim.marchaLenta().mapKpa(), new MemoriaCalculo());
            double minimo = dim.parametros().tiMinimoMarchaLentaMs();
            memoria.passo("TI efetivo na marcha lenta (mapa)", "célula da rotação de marcha lenta e do MAP de marcha "
                    + "lenta", lenta.tiEfetivoMs(), "ms", e("rpm", lenta.rotacao(), "rpm"),
                    e("MAP", lenta.cargaKpa(), "kPa"), e("TI mínimo", minimo, "ms"));
            if (lenta.tiEfetivoMs() < minimo) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.TI_BAIXO, Cor.AMARELO, "TI efetivo de "
                        + formatar(lenta.tiEfetivoMs(), 2) + " ms na marcha lenta, abaixo de " + formatar(minimo, 1)
                        + " ms: bico grande para a carga baixa, a marcha lenta pode oscilar."));
            }
        }

        // ---------- Ignição (SPEC 7.5: o app não inventa avanço) ----------

        private Tabela reduzir(Tabela daFonte, FonteIgnicao fonte) {
            int vazias = 0;
            int total = 0;
            for (List<Double> linha : daFonte.valores()) {
                for (Double valor : linha) {
                    total++;
                    if (valor == null) {
                        vazias++;
                    }
                }
            }
            if (vazias > 0) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.IGNICAO_INCOMPLETA, Cor.VERMELHO, vazias + " de "
                        + total + " células da ignição ficaram vazias, fora dos eixos da fonte (\"" + fonte.descricao()
                        + "\"): o app não estende o avanço. Preencha essas células antes de dar partida."));
            }
            List<String> faltam = regras.reducoesNaoConfiguradas();
            if (!faltam.isEmpty()) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.REDUCOES_IGNICAO, Cor.AMARELO, "Reduções de ignição "
                        + "não configuradas (" + String.join(", ", faltam) + "): nessas partes, o avanço sai igual ao "
                        + "da fonte. Configure em Configurações › Mapa base."));
            }
            boolean trocaDeCombustivel = !fonte.combustivel().isEmpty()
                    && !fonte.combustivel().equalsIgnoreCase(combustivel.codigo());
            if (fonte.combustivel().isEmpty() && regras.reducaoIgnicaoTrocaCombustivel() != null) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.REDUCOES_IGNICAO, Cor.AMARELO, "A fonte da ignição "
                        + "não diz o combustível em que foi feita: sem a redução por troca de combustível."));
            }
            if (fonte.temperaturaArReferenciaC() == null && regras.reducaoIgnicaoPorTemperatura() != null) {
                alertas.add(new AlertaMapa(AlertaMapa.Codigo.REDUCOES_IGNICAO, Cor.AMARELO, "A fonte da ignição "
                        + "não diz a temperatura do ar em que foi feita: sem a redução por temperatura."));
            }
            memoria.passo("Redução por troca de combustível", "da fonte (" + (fonte.combustivel().isEmpty() ? "?"
                    : fonte.combustivel()) + ") para o projeto (" + combustivel.codigo() + ")",
                    trocaDeCombustivel && regras.reducaoIgnicaoTrocaCombustivel() != null
                            ? regras.reducaoIgnicaoTrocaCombustivel() : 0.0, "°");
            return Tabela.preencher(daFonte.rotacoes(), daFonte.cargas(), (rotacao, carga) -> {
                OptionalDouble valor = daFonte.interpolar(rotacao, carga);
                if (valor.isEmpty()) {
                    return null;
                }
                return valor.getAsDouble() - reducao(carga, fonte, trocaDeCombustivel);
            });
        }

        /** As reduções conservadoras de uma célula, em graus. Nunca somam avanço. */
        private double reducao(double cargaKpa, FonteIgnicao fonte, boolean trocaDeCombustivel) {
            double total = 0.0;
            if (regras.reducaoIgnicaoPorPressao() != null && cargaKpa > ModeloVe.KPA_DA_CARGA_RELATIVA) {
                total += regras.reducaoIgnicaoPorPressao() * (cargaKpa - ModeloVe.KPA_DA_CARGA_RELATIVA) / 10.0;
            }
            if (regras.reducaoIgnicaoPorTemperatura() != null && fonte.temperaturaArReferenciaC() != null) {
                double acima = temperaturaDoAr(cargaKpa) - fonte.temperaturaArReferenciaC();
                total += regras.reducaoIgnicaoPorTemperatura() * Math.max(0.0, acima) / 10.0;
            }
            if (trocaDeCombustivel && regras.reducaoIgnicaoTrocaCombustivel() != null) {
                total += regras.reducaoIgnicaoTrocaCombustivel();
            }
            return Math.max(0.0, total);
        }

        /**
         * A temperatura do ar no coletor estimada para a carga: até 100 kPa (ou no aspirado), a do ar ambiente mais o
         * acréscimo do aspirado; acima, linear até a Tm do dimensionamento no MAP dele, continuando a subir depois.
         */
        private double temperaturaDoAr(double cargaKpa) {
            double aspirado = dim.temperaturaAmbienteC() + dim.parametros().acrescimoTmAspiradoC();
            if (!sobrealimentado || cargaKpa <= ModeloVe.KPA_DA_CARGA_RELATIVA
                    || mapAncoraKpa <= ModeloVe.KPA_DA_CARGA_RELATIVA) {
                return aspirado;
            }
            double tm = dimensionamento.ladoDoAr().tmC();
            return aspirado + (tm - aspirado) * (cargaKpa - ModeloVe.KPA_DA_CARGA_RELATIVA)
                    / (mapAncoraKpa - ModeloVe.KPA_DA_CARGA_RELATIVA);
        }

        // ---------- Travas (SPEC 7.5) ----------

        private MapaBase.Travas travas() {
            int limitador = (int) Math.round(memoria.passo("Limitador de rotação", "limitador = rotação máxima − "
                    + "margem", motor.rotacaoMaxima() - regras.margemLimitadorRpm(), "rpm",
                    e("rotação máxima", motor.rotacaoMaxima(), "rpm"), e("margem", regras.margemLimitadorRpm(), "rpm")));
            if (!sobrealimentado) {
                return new MapaBase.Travas(Double.NaN, null, Double.NaN, limitador);
            }
            double prevista = memoria.passo("Pressão do turbo prevista", "MAP máximo − P_atm",
                    mapMaximoKpa - dimensionamento.ladoDoAr().patmKpa(), "kPa",
                    e("MAP máximo", mapMaximoKpa, "kPa"), e("P_atm", dimensionamento.ladoDoAr().patmKpa(), "kPa"));
            double fracao = regras.fracaoPressaoPrevista() * prevista;
            Double mola = entrada.pressaoMolaWastegateKpa();
            double alvo = memoria.passo("Pressão do turbo alvo (trava)", "menor entre a mola da wastegate e "
                    + formatar(regras.fracaoPressaoPrevista() * 100, 0) + "% da prevista",
                    mola == null ? fracao : Math.min(mola, fracao), "kPa", e("prevista", prevista, "kPa"),
                    e("fração da prevista", fracao, "kPa"), e("mola", mola == null ? Double.NaN : mola, "kPa"));
            if (mola == null) {
                memoria.aviso(NivelAviso.INFORMACAO, "Sem a pressão da mola da wastegate: a pressão alvo ficou em "
                        + formatar(regras.fracaoPressaoPrevista() * 100, 0) + "% da prevista.");
            }
            return new MapaBase.Travas(prevista, mola, alvo, limitador);
        }

        private static Tabela tabela(EixosDaTabela eixos, List<List<CelulaMapa>> celulas,
                ToDoubleFunction<CelulaMapa> valor) {
            List<List<Double>> linhas = celulas.stream()
                    .map(linha -> linha.stream().map(c -> valor.applyAsDouble(c)).toList()).toList();
            return new Tabela(eixos.rotacoes(), eixos.cargas(), linhas);
        }
    }
}
