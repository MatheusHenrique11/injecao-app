package br.com.calibra.app.catalogo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.combustivel.GasolinaComEtanol;
import br.com.calibra.core.mapa.FuncaoTabela;
import br.com.calibra.core.mapa.ModeloIgnicao;
import br.com.calibra.core.mapa.ModeloVe;
import br.com.calibra.core.mapa.OrientacaoTabela;
import br.com.calibra.core.mapa.PerfilTabela;
import br.com.calibra.core.mapa.Tabela;
import br.com.calibra.core.mapa.TipoCarga;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.Comando;
import br.com.calibra.core.motor.ModeloMotor;
import br.com.calibra.core.oficina.ChecklistModelo;
import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemMaterial;
import br.com.calibra.core.orcamento.Kit;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.pecas.CalibracaoImagem;
import br.com.calibra.core.pecas.Cambio;
import br.com.calibra.core.pecas.ImpedanciaBico;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Intercooler;
import br.com.calibra.core.pecas.MapaCompressor;
import br.com.calibra.core.pecas.ModeloCombustivel;
import br.com.calibra.core.pecas.PerfilInjecao;
import br.com.calibra.core.pecas.Recurso;
import br.com.calibra.core.pecas.Sensor;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.core.pecas.TipoIntercooler;
import br.com.calibra.core.pecas.TipoSensor;
import br.com.calibra.core.pecas.Turbo;
import br.com.calibra.core.pecas.UnidadeVazao;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Um item fictício de cada tipo de catálogo, com os dados e a informação comum coerentes (mesmo nome, fonte e
 * data). Valores de teste, não de peças reais.
 */
public final class ExemplosCatalogo {

    /** Os dados de um item e a informação comum dele. */
    public record Exemplo<T>(TipoCatalogo<T> tipo, T dados, InfoCatalogo info) {
    }

    static final String FONTE = "Ficha fictícia de teste";
    static final LocalDate DATA = LocalDate.of(2026, 10, 1);

    private ExemplosCatalogo() {
    }

    static InfoCatalogo info(String nome) {
        return new InfoCatalogo(nome, FONTE, DATA, false, new Dinheiro(45_000), "Dados fictícios.");
    }

    public static Exemplo<Bico> bicoComTabela() {
        TempoMorto tabela = new TempoMorto(List.of(
                new TempoMorto.Ponto(12.0, 300.0, 1.10), new TempoMorto.Ponto(14.0, 300.0, 0.90),
                new TempoMorto.Ponto(12.0, 400.0, 1.20), new TempoMorto.Ponto(14.0, 400.0, 1.00)));
        return new Exemplo<>(TipoCatalogo.BICO,
                new Bico("Bico teste 440", 440.0, 300.0, 0.70, tabela, 12.0, FONTE, DATA), info("Bico teste 440"));
    }

    public static Exemplo<Bico> bicoFixo() {
        return new Exemplo<>(TipoCatalogo.BICO,
                new Bico("Bico teste 630", 630.0, 300.0, 0.70, TempoMorto.fixo(1.05), null, FONTE, DATA),
                info("Bico teste 630"));
    }

    public static Exemplo<Bomba> bomba() {
        List<Bomba.Ponto> curva = List.of(new Bomba.Ponto(300.0, 12.0, 190.0), new Bomba.Ponto(500.0, 12.0, 160.0),
                new Bomba.Ponto(300.0, 13.5, 230.0), new Bomba.Ponto(500.0, 13.5, 200.0));
        return new Exemplo<>(TipoCatalogo.BOMBA, new Bomba("Bomba teste 230", curva, FONTE, DATA),
                info("Bomba teste 230"));
    }

    public static Exemplo<Injecao> injecao() {
        OrientacaoTabela orientacao = new OrientacaoTabela(OrientacaoTabela.EixoDasColunas.ROTACAO,
                OrientacaoTabela.OrdemDasLinhas.CRESCE_PARA_CIMA);
        PerfilInjecao perfil = new PerfilInjecao(ImpedanciaBico.ALTA, 2, ModeloCombustivel.MS, List.of(
                new PerfilTabela("Mapa principal", FuncaoTabela.COMBUSTIVEL, TipoCarga.MAP_KPA, "ms",
                        List.of(500.0, 1000.0, 2500.0, 7500.0), List.of(20.0, 60.5, 100.0, 250.0), orientacao),
                new PerfilTabela("Avanço", FuncaoTabela.IGNICAO, TipoCarga.TPS_PERCENTUAL, "°",
                        List.of(1000.0, 8000.0), List.of(0.0, 100.0),
                        new OrientacaoTabela(OrientacaoTabela.EixoDasColunas.CARGA,
                                OrientacaoTabela.OrdemDasLinhas.CRESCE_PARA_BAIXO))),
                22.5, Boolean.FALSE, "Cola células separadas por tabulação.", "IP67");
        return new Exemplo<>(TipoCatalogo.INJECAO, new Injecao("Marca Teste", "ECU 8", 8, 8, 8,
                Set.of(Recurso.DETONACAO, Recurso.WIDEBAND), perfil, FONTE, DATA), info("Marca Teste ECU 8"));
    }

    public static Exemplo<Combustivel> combustivel() {
        return new Exemplo<>(TipoCatalogo.COMBUSTIVEL, new Combustivel("E30T", "Gasolina teste E30",
                new GasolinaComEtanol(0.30), 0.755, 0.80, 0.87, null, null, FONTE, 1), info("Gasolina teste E30"));
    }

    public static Exemplo<Intercooler> intercooler() {
        return new Exemplo<>(TipoCatalogo.INTERCOOLER,
                new Intercooler("Intercooler teste", TipoIntercooler.AR_AR, 0.75, FONTE, DATA), info("Intercooler teste"));
    }

    public static Exemplo<Sensor> sensor() {
        return new Exemplo<>(TipoCatalogo.SENSOR, new Sensor("MAP teste 4 bar", TipoSensor.MAP, 10.0, 400.0, FONTE, DATA),
                info("MAP teste 4 bar"));
    }

    public static Exemplo<Cambio> cambio() {
        return new Exemplo<>(TipoCatalogo.CAMBIO, new Cambio("Câmbio teste", TipoCambio.MANUAL, 350.0, FONTE, DATA),
                info("Câmbio teste"));
    }

    public static Exemplo<ModeloMotor> motor() {
        return new Exemplo<>(TipoCatalogo.MOTOR, new ModeloMotor("Motor teste 2.0", false, 4, 82.5, 92.8, 144.0, 10.0, 2,
                1984.0, Aspiracao.ASPIRADO, "Descrição de teste.", FONTE, DATA), info("Motor teste 2.0"));
    }

    /** Mapa pequeno e fictício (kg/s × PR), com a referência da Garrett. */
    public static MapaCompressor mapaFicticio() {
        return new MapaCompressor(UnidadeVazao.LB_MIN, 545.0 / 1.8, 96.18,
                List.of(ponto(0.10, 1.3), ponto(0.16, 2.0), ponto(0.22, 2.8)),
                List.of(ponto(0.38, 1.3), ponto(0.42, 2.0), ponto(0.44, 2.8)),
                List.of(new MapaCompressor.Ilha(0.74, List.of(ponto(0.20, 1.8), ponto(0.36, 1.8), ponto(0.30, 2.6)))),
                List.of(new MapaCompressor.LinhaRotacao(120_000, List.of(ponto(0.17, 2.3), ponto(0.43, 2.0)))),
                150_000.0);
    }

    private static MapaCompressor.PontoMapa ponto(double vazao, double pr) {
        return new MapaCompressor.PontoMapa(vazao, pr);
    }

    /**
     * Mapa fictício para o caso golden B (kg/s × PR, referência 298 K e 100 kPa). O ponto de potência do caso B fica
     * perto de 0,32 kg/s corrigidos a PR 2,9: dentro do mapa grande (até PR 3,4), acima do pequeno (até PR 2,5).
     */
    public static MapaCompressor mapaCasoB(boolean grande, double eficiencia) {
        double topo = grande ? 3.4 : 2.5;
        return new MapaCompressor(UnidadeVazao.KG_S, 298.0, 100.0,
                List.of(ponto(0.08, 1.3), ponto(0.18, 2.5), ponto(0.24, topo)),
                List.of(ponto(0.40, 1.3), ponto(0.46, 2.5), ponto(0.50, topo)),
                List.of(new MapaCompressor.Ilha(eficiencia, List.of(ponto(0.22, 2.4), ponto(0.40, 2.4), ponto(0.38, 3.2),
                        ponto(0.26, 3.2)))),
                List.of(new MapaCompressor.LinhaRotacao(100_000, List.of(ponto(0.10, 1.8), ponto(0.45, 1.6))),
                        new MapaCompressor.LinhaRotacao(160_000, List.of(ponto(0.24, topo), ponto(0.50, topo - 0.2)))),
                180_000.0);
    }

    public static Exemplo<Turbo> turbo() {
        CalibracaoImagem calibracao = new CalibracaoImagem(100, 10, 700, 60, 500, 1.0, 100, 3.0);
        return new Exemplo<>(TipoCatalogo.TURBO, new Turbo("Turbo teste 60", mapaFicticio(), 0.63, "turbo-1.png",
                calibracao, FONTE, DATA), info("Turbo teste 60"));
    }

    /**
     * Serviço sem preço próprio: entra no orçamento pelas horas × o valor da hora, com o nome na linha (sem
     * descrição), e tem garantia própria de 180 dias.
     */
    public static Exemplo<ServicoPadrao> servicoPorHora() {
        return new Exemplo<>(TipoCatalogo.SERVICO, new ServicoPadrao("Limpeza de bicos teste", 1.5, FONTE, DATA,
                CategoriaServico.PREPARACAO, "", true, 180, null),
                new InfoCatalogo("Limpeza de bicos teste", FONTE, DATA, false, null, "Dados fictícios."));
    }

    /**
     * Serviço com preço próprio (e sem horas): entra no orçamento como 1 serviço pelo preço, com a descrição na linha,
     * e fica com a garantia da OS.
     */
    public static Exemplo<ServicoPadrao> servicoComPreco() {
        return new Exemplo<>(TipoCatalogo.SERVICO, new ServicoPadrao("Diagnóstico teste", null, FONTE, DATA,
                CategoriaServico.DIAGNOSTICO_ELETRICO, "Diagnóstico com scanner e relatório (teste)", true, null,
                null),
                new InfoCatalogo("Diagnóstico teste", FONTE, DATA, false, new Dinheiro(25_000), "Dados fictícios."));
    }

    /** Item de material para o orçamento (Fase 3B), com preço de referência. */
    public static Exemplo<ItemMaterial> item() {
        return new Exemplo<>(TipoCatalogo.ITEM, new ItemMaterial("Relé teste 12 V 40 A", "un", FONTE, DATA),
                new InfoCatalogo("Relé teste 12 V 40 A", FONTE, DATA, false, Dinheiro.ler("18,50"), "Dados fictícios."));
    }

    /**
     * Kit (Fase 3B) com 2 × um item e 1 × um serviço: um pacote. Ele aponta para o catálogo, então fica fora de
     * {@link #umDeCada()}: quem grava passa os ids de um item e de um serviço que existam.
     */
    public static Exemplo<Kit> kit(long itemId, long servicoId) {
        return new Exemplo<>(TipoCatalogo.KIT, new Kit("Kit teste de relé", List.of(
                new Kit.Linha(Kit.TipoLinha.ITEM, itemId, "Relé teste 12 V 40 A", new BigDecimal("2")),
                new Kit.Linha(Kit.TipoLinha.SERVICO, servicoId, "Limpeza de bicos teste", BigDecimal.ONE)), FONTE,
                DATA), new InfoCatalogo("Kit teste de relé", FONTE, DATA, false, null, "Dados fictícios."));
    }

    /** Modelo de checklist (Fase 3B): dois itens obrigatórios, um deles "a verificar", e um opcional. Sem preço. */
    public static Exemplo<ChecklistModelo> checklist() {
        return new Exemplo<>(TipoCatalogo.CHECKLIST, new ChecklistModelo("Checklist teste de som", List.of(
                new ChecklistModelo.Item("Medir a tensão da bateria", true, true),
                new ChecklistModelo.Item("Teste de ruído com o motor ligado", true, false),
                new ChecklistModelo.Item("Fotos de antes e depois", false, false)), FONTE, DATA),
                new InfoCatalogo("Checklist teste de som", FONTE, DATA, false, null, "Dados fictícios."));
    }

    /** Modelo de VE (fase 4): 2 cargas × 3 rotações relativas, 4 válvulas e comando original ou leve. Sem preço. */
    public static Exemplo<ModeloVe> modeloVe() {
        Tabela ve = new Tabela(List.of(0.2, 1.0, 1.5), List.of(0.35, 1.0),
                List.of(List.of(0.5, 0.52, 0.55), List.of(0.9, 0.97, 0.95)));
        return new Exemplo<>(TipoCatalogo.MODELO_VE, new ModeloVe("VE teste 4 válvulas", 4,
                Set.of(Comando.ORIGINAL, Comando.LEVE), ve, FONTE, DATA),
                new InfoCatalogo("VE teste 4 válvulas", FONTE, DATA, true, null, "Dados fictícios."));
    }

    /** Modelo de ignição (fase 4): 3 cargas × 2 rotações, em E32, feito com o ar a 30 °C. Sem preço. */
    public static Exemplo<ModeloIgnicao> modeloIgnicao() {
        Tabela avanco = new Tabela(List.of(1000.0, 6000.0), List.of(30.0, 100.0, 200.0),
                List.of(List.of(18.0, 36.5), List.of(12.0, 30.0), List.of(8.0, 18.0)));
        return new Exemplo<>(TipoCatalogo.MODELO_IGNICAO, new ModeloIgnicao("Ignição teste AP turbo",
                "VW AP 2.0 8v turbo", "E32", 30.0, avanco, FONTE, DATA),
                new InfoCatalogo("Ignição teste AP turbo", FONTE, DATA, false, null, "Dados fictícios."));
    }

    /** Um exemplo de cada tipo, na ordem das abas (menos o kit, que aponta para o catálogo: {@link #kit}). */
    public static List<Exemplo<?>> umDeCada() {
        return List.of(bicoComTabela(), bomba(), injecao(), turbo(), combustivel(), intercooler(), sensor(), cambio(),
                motor(), modeloVe(), modeloIgnicao(), servicoPorHora(), item(), checklist());
    }
}
