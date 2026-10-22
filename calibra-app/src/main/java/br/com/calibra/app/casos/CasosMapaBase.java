package br.com.calibra.app.casos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.mapa.AlertaMapa;
import br.com.calibra.core.mapa.EntradaMapaBase;
import br.com.calibra.core.mapa.FonteIgnicao;
import br.com.calibra.core.mapa.GeradorMapaBase;
import br.com.calibra.core.mapa.JustificativaMapa;
import br.com.calibra.core.mapa.MapaBase;
import br.com.calibra.core.mapa.ModeloIgnicao;
import br.com.calibra.core.mapa.ModeloVe;
import br.com.calibra.core.mapa.RegrasMapaBase;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.memoria.VersaoNucleo;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.catalogo.CatalogoRepositorio;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.ResumoItem;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.projeto.CalculoRepositorio;
import br.com.calibra.dados.projeto.CalculoSalvo;
import br.com.calibra.dados.projeto.MapaBaseRepositorio;
import br.com.calibra.dados.projeto.MapaBaseSalvo;
import br.com.calibra.dados.projeto.MotorRepositorio;
import br.com.calibra.dados.projeto.PapelPeca;
import br.com.calibra.dados.projeto.PecaUsada;
import br.com.calibra.dados.projeto.ProjetoRepositorio;
import br.com.calibra.dados.projeto.ResumoCalculo;
import br.com.calibra.dados.projeto.ResumoMapaBase;
import br.com.calibra.dados.projeto.VersaoMotor;

/**
 * O mapa base do projeto (fase 4, bloco 4.4; decisão 296): monta a entrada do gerador com o último cálculo e as peças
 * do catálogo nas versões de agora, salva em versões (com as justificativas dos vermelhos) e diz quando um mapa salvo
 * ficou desatualizado. Salvar com o projeto APROVADO leva o projeto a MAPA_BASE.
 */
public final class CasosMapaBase {

    /**
     * O que a tela escolheu para gerar.
     *
     * @param injecaoItemId opcional: a injeção do catálogo (o perfil dela)
     * @param modeloVeItemId o modelo de VE do catálogo
     * @param modeloIgnicaoItemId opcional: o modelo de ignição do catálogo, como fonte do avanço
     * @param ignicaoDigitada opcional: a tabela de ignição digitada no projeto (só sem o modelo de ignição)
     * @param pressaoMolaWastegateKpa opcional: a mola da wastegate (relativa)
     */
    public record Escolhas(Long injecaoItemId, long modeloVeItemId, Long modeloIgnicaoItemId,
            FonteIgnicao ignicaoDigitada, Double pressaoMolaWastegateKpa) {
    }

    /**
     * Um mapa salvo e o que mudou desde ele (como no cálculo: "desatualizado — gerar de novo").
     *
     * @param motivos vazio = em dia com o projeto
     */
    public record SituacaoMapa(ResumoMapaBase mapa, List<String> motivos) {

        public SituacaoMapa {
            motivos = List.copyOf(motivos);
        }

        public boolean desatualizado() {
            return !motivos.isEmpty();
        }
    }

    private final BancoDados banco;
    private final Clock relogio;
    private final String versaoApp;
    private final ProjetoRepositorio projetos;
    private final MotorRepositorio motores;
    private final CalculoRepositorio calculos;
    private final MapaBaseRepositorio mapas;
    private final CatalogoRepositorio catalogo;

    public CasosMapaBase(BancoDados banco, Clock relogio, String versaoApp) {
        this.banco = Objects.requireNonNull(banco, "banco");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
        this.versaoApp = Objects.requireNonNull(versaoApp, "versaoApp");
        this.projetos = new ProjetoRepositorio(relogio);
        this.motores = new MotorRepositorio(relogio);
        this.calculos = new CalculoRepositorio(relogio);
        this.mapas = new MapaBaseRepositorio(relogio);
        this.catalogo = new CatalogoRepositorio(relogio);
    }

    /** O cálculo que o mapa usa: o último salvo no projeto (vazio se ainda não há). */
    public Optional<CalculoSalvo> calculoDoProjeto(long projetoId) {
        return banco.ler(c -> calculos.ultimo(c, projetoId));
    }

    /** Por que o último cálculo não serve para o mapa (o motor mudou depois dele, por exemplo); vazio se serve. */
    public List<String> motivosDoCalculo(long projetoId) {
        return banco.ler(c -> {
            VersaoMotor atual = motores.atual(c, projetoId).orElseThrow();
            List<ResumoCalculo> doProjeto = calculos.doProjeto(c, projetoId);
            return doProjeto.isEmpty() ? List.of() : CasosProjeto.motivos(doProjeto.getFirst(), atual);
        });
    }

    /**
     * O modelo de VE da família do motor: o primeiro ativo que serve (válvulas e comando); sem nenhum, o primeiro que
     * serve para qualquer motor.
     */
    public Optional<ItemCatalogo<ModeloVe>> modeloVeSugerido(ConfiguracaoMotor motor) {
        List<ItemCatalogo<ModeloVe>> modelos = banco.ler(c -> catalogo.listar(c, TipoCatalogo.MODELO_VE, "", false));
        return modelos.stream()
                .filter(m -> m.dados().serve(motor.valvulasPorCilindro(), motor.comando()))
                .min(Comparator.comparing(m -> m.dados().valvulasPorCilindro() == null ? 1 : 0));
    }

    /** A entrada do gerador com o cálculo e as peças do catálogo nas versões de agora. */
    public EntradaMapaBase entrada(long projetoId, long calculoId, Escolhas escolhas, RegrasMapaBase regras) {
        return banco.ler(c -> entrada(c, projetoId, calculoId, escolhas, regras));
    }

    /** Gera o mapa (sem salvar): para a tela mostrar antes de salvar. */
    public Resultado<MapaBase> gerar(long projetoId, long calculoId, Escolhas escolhas, RegrasMapaBase regras) {
        return GeradorMapaBase.gerar(entrada(projetoId, calculoId, escolhas, regras));
    }

    /**
     * Gera de novo, com as versões de agora, e salva a versão seguinte do mapa do projeto. Todo vermelho precisa de
     * justificativa escrita (regra 2.4). Com o projeto APROVADO, ele passa para MAPA_BASE; nos outros status, fica.
     *
     * @return o id do mapa salvo
     * @throws EntradaInvalidaException se o cálculo não for o último do projeto, se o motor mudou depois dele ou se
     *     faltar justificativa
     */
    public long salvar(long projetoId, long calculoId, Escolhas escolhas, RegrasMapaBase regras,
            List<JustificativaMapa> justificativas) {
        long ultimo = calculoDoProjeto(projetoId).map(CalculoSalvo::id).orElse(-1L);
        if (ultimo != calculoId) {
            throw new EntradaInvalidaException("Há um cálculo mais novo no projeto: gere o mapa de novo com ele.");
        }
        List<String> motivos = motivosDoCalculo(projetoId);
        if (!motivos.isEmpty()) {
            throw new EntradaInvalidaException("O último cálculo está desatualizado (" + String.join("; ", motivos)
                    + "). Calcule e salve de novo no Dimensionamento antes do mapa base.");
        }
        EntradaMapaBase entrada = entrada(projetoId, calculoId, escolhas, regras);
        Resultado<MapaBase> resultado = GeradorMapaBase.gerar(entrada);
        List<JustificativaMapa> necessarias = justificativasDosVermelhos(resultado.valor(), justificativas);
        return banco.transacao(c -> {
            CalculoSalvo calculo = calculos.buscar(c, calculoId).orElseThrow();
            long id = mapas.salvar(c, projetoId, calculoId, calculo.versaoMotorId(), entrada, resultado, necessarias,
                    pecasUsadas(c, escolhas), versaoApp);
            if (projetos.buscar(c, projetoId).orElseThrow().status() == StatusProjeto.APROVADO) {
                projetos.mudarStatus(c, projetoId, StatusProjeto.MAPA_BASE);
            }
            return id;
        });
    }

    /** Os mapas salvos do projeto, do mais novo para o mais antigo, com o que mudou desde cada um. */
    public List<SituacaoMapa> mapas(long projetoId, RegrasMapaBase regrasAtuais) {
        return banco.ler(c -> {
            VersaoMotor motor = motores.atual(c, projetoId).orElseThrow();
            List<ResumoCalculo> doProjeto = calculos.doProjeto(c, projetoId);
            Long ultimoCalculo = doProjeto.isEmpty() ? null : doProjeto.getFirst().id();
            List<ResumoMapaBase> salvos = mapas.doProjeto(c, projetoId);
            Set<Long> ids = salvos.stream().flatMap(m -> m.pecas().stream()).map(PecaUsada::itemId)
                    .collect(Collectors.toCollection(HashSet::new));
            Map<Long, ResumoItem> itens = catalogo.resumos(c, ids);
            List<SituacaoMapa> situacoes = new ArrayList<>();
            for (ResumoMapaBase mapa : salvos) {
                List<String> motivos = new ArrayList<>();
                if (mapa.versaoMotorId() != motor.id()) {
                    motivos.add("o motor mudou: este mapa usou a v" + mapa.numeroVersaoMotor() + " e o motor está na v"
                            + motor.numero());
                }
                if (ultimoCalculo != null && mapa.calculoId() != ultimoCalculo) {
                    motivos.add("há um cálculo mais novo no projeto (#" + ultimoCalculo + "; este mapa usou o #"
                            + mapa.calculoId() + ")");
                }
                motivos.addAll(CasosProjeto.motivosDasPecas(mapa.pecas(), itens, Optional.empty()));
                if (VersaoNucleo.anterior(mapa.versaoNucleo(), VersaoNucleo.ULTIMA_MUDANCA_DO_MAPA_BASE)) {
                    motivos.add("as contas do mapa base mudaram: " + mapa.versaoNucleo() + " → " + VersaoNucleo.ATUAL);
                }
                if (!mapas.buscar(c, mapa.id()).orElseThrow().entrada().regras().equals(regrasAtuais)) {
                    motivos.add("as regras do mapa base mudaram em Configurações");
                }
                situacoes.add(new SituacaoMapa(mapa, motivos));
            }
            return situacoes;
        });
    }

    public Optional<MapaBaseSalvo> mapa(long mapaId) {
        return banco.ler(c -> mapas.buscar(c, mapaId));
    }

    public LocalDateTime agora() {
        return LocalDateTime.now(relogio).withNano(0);
    }

    // ===================== Regras internas =====================

    private EntradaMapaBase entrada(Connection c, long projetoId, long calculoId, Escolhas escolhas,
            RegrasMapaBase regras) throws SQLException {
        CalculoSalvo calculo = calculos.buscar(c, calculoId)
                .filter(salvo -> salvo.projetoId() == projetoId)
                .orElseThrow(() -> new OperacaoRecusadaException("Cálculo #" + calculoId + " não é deste projeto."));
        Injecao injecao = escolhas.injecaoItemId() == null ? null
                : item(c, TipoCatalogo.INJECAO, escolhas.injecaoItemId()).dados();
        ModeloVe modeloVe = item(c, TipoCatalogo.MODELO_VE, escolhas.modeloVeItemId()).dados();
        FonteIgnicao ignicao = escolhas.modeloIgnicaoItemId() != null
                ? FonteIgnicao.doModelo(item(c, TipoCatalogo.MODELO_IGNICAO, escolhas.modeloIgnicaoItemId()).dados())
                : escolhas.ignicaoDigitada();
        return new EntradaMapaBase(calculo.entrada(), injecao, modeloVe, ignicao, regras,
                escolhas.pressaoMolaWastegateKpa());
    }

    private List<PecaUsada> pecasUsadas(Connection c, Escolhas escolhas) throws SQLException {
        List<PecaUsada> pecas = new ArrayList<>();
        if (escolhas.injecaoItemId() != null) {
            pecas.add(new PecaUsada(PapelPeca.INJECAO, escolhas.injecaoItemId(),
                    item(c, TipoCatalogo.INJECAO, escolhas.injecaoItemId()).versao()));
        }
        pecas.add(new PecaUsada(PapelPeca.MODELO_VE, escolhas.modeloVeItemId(),
                item(c, TipoCatalogo.MODELO_VE, escolhas.modeloVeItemId()).versao()));
        if (escolhas.modeloIgnicaoItemId() != null) {
            ItemCatalogo<ModeloIgnicao> modelo = item(c, TipoCatalogo.MODELO_IGNICAO, escolhas.modeloIgnicaoItemId());
            pecas.add(new PecaUsada(PapelPeca.MODELO_IGNICAO, modelo.id(), modelo.versao()));
        }
        return pecas;
    }

    private <T> ItemCatalogo<T> item(Connection c, TipoCatalogo<T> tipo, long id) throws SQLException {
        return catalogo.buscar(c, tipo, id).orElseThrow(() -> new OperacaoRecusadaException(
                "O " + tipo.singular() + " #" + id + " não está no catálogo."));
    }

    /** As justificativas dos alertas vermelhos, uma por código; falta uma, nada é salvo. */
    private List<JustificativaMapa> justificativasDosVermelhos(MapaBase mapa, List<JustificativaMapa> dadas) {
        Set<AlertaMapa.Codigo> vermelhos = new LinkedHashSet<>();
        mapa.vermelhos().forEach(alerta -> vermelhos.add(alerta.codigo()));
        List<JustificativaMapa> necessarias = new ArrayList<>();
        List<String> faltando = new ArrayList<>();
        for (AlertaMapa.Codigo codigo : vermelhos) {
            Optional<JustificativaMapa> dada = dadas.stream().filter(j -> j.alerta() == codigo).findFirst();
            if (dada.isPresent()) {
                necessarias.add(dada.get());
            } else {
                faltando.add(codigo.rotulo());
            }
        }
        if (!faltando.isEmpty()) {
            throw new EntradaInvalidaException("Para salvar, escreva a justificativa de: " + String.join(", ", faltando)
                    + " (regra 2.4: vermelho só avança com justificativa).");
        }
        return necessarias;
    }
}
