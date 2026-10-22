package br.com.calibra.app.catalogo;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import br.com.calibra.core.mapa.FuncaoTabela;
import br.com.calibra.core.mapa.OrientacaoTabela;
import br.com.calibra.core.mapa.PerfilTabela;
import br.com.calibra.core.mapa.TipoCarga;
import br.com.calibra.core.pecas.ImpedanciaBico;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.ModeloCombustivel;
import br.com.calibra.core.pecas.PerfilInjecao;
import br.com.calibra.core.pecas.Recurso;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Injeção programável: marca e modelo (que formam o nome), saídas, cilindros em sequencial, recursos e, desde a fase
 * 4, o perfil (SPEC 9; decisão 292). As tabelas do perfil são três vagas fixas (combustível, ignição e lambda alvo),
 * as que o mapa base preenche; cada vaga começa pela carga do eixo, e em branco ela não existe.
 */
final class FormaInjecao implements FormaCatalogo<Injecao> {

    static final String MARCA = "marca";
    static final String MODELO = "modelo";
    static final String SAIDAS_BICO = "saidasBico";
    static final String SAIDAS_IGNICAO = "saidasIgnicao";
    static final String MAX_SEQUENCIAL = "maxCilindrosSequencial";
    static final String IMPEDANCIA = "impedanciaAceita";
    static final String BICOS_POR_SAIDA = "bicosPorSaida";
    static final String MODELO_COMBUSTIVEL = "modeloCombustivel";
    static final String TEMPERATURA_REFERENCIA = "temperaturaReferenciaArC";
    static final String TEMPO_MORTO_NA_TABELA = "tabelaMsIncluiTempoMorto";
    static final String FORMATO = "formatoImportacaoExportacao";
    static final String PROTECAO_AGUA = "protecaoAgua";

    /** Sim ou não, para a pergunta do tempo morto (em branco: o manual não diz). */
    enum SimNao {
        SIM("Sim"),
        NAO("Não");

        private final String rotulo;

        SimNao(String rotulo) {
            this.rotulo = rotulo;
        }

        String rotulo() {
            return rotulo;
        }
    }

    static String recurso(Recurso recurso) {
        return "recurso." + recurso.name();
    }

    /** A chave de um campo da vaga de uma tabela (ex.: "tabela.COMBUSTIVEL.rotacoes"). */
    static String tabela(FuncaoTabela funcao, String campo) {
        return "tabela." + funcao.name() + "." + campo;
    }

    @Override
    public TipoCatalogo<Injecao> tipo() {
        return TipoCatalogo.INJECAO;
    }

    @Override
    public List<CampoForm> campos() {
        List<CampoForm> campos = new ArrayList<>(List.of(
                CampoForm.texto(MARCA, "Marca", true),
                CampoForm.texto(MODELO, "Modelo", true),
                CampoForm.inteiro(SAIDAS_BICO, "Saídas de bico", true),
                CampoForm.inteiro(SAIDAS_IGNICAO, "Saídas de ignição", true),
                CampoForm.inteiro(MAX_SEQUENCIAL, "Cilindros em sequencial (máximo)", false)
                        .comDica("Em branco, a escolha confere só as saídas.")));
        for (Recurso recurso : Recurso.values()) {
            campos.add(CampoForm.marcar(recurso(recurso), recurso.rotulo()));
        }
        campos.addAll(List.of(
                CampoForm.escolha(IMPEDANCIA, "Impedância de bico aceita", false, ImpedanciaBico.values(),
                        ImpedanciaBico::rotulo)
                        .comDica("Daqui para baixo, o perfil da injeção: tudo vem do manual ou do software dela. "
                                + "Em branco, o app não sabe."),
                CampoForm.inteiro(BICOS_POR_SAIDA, "Bicos por saída (máximo)", false),
                CampoForm.escolha(MODELO_COMBUSTIVEL, "Modelo de combustível", false, ModeloCombustivel.values(),
                        ModeloCombustivel::rotulo)
                        .comDica("Em ms, o mapa base leva o tempo de injeção; por VE, leva a VE e o lambda alvo."),
                CampoForm.escolha(TEMPO_MORTO_NA_TABELA, "A tabela em ms já inclui o tempo morto?", false,
                        SimNao.values(), SimNao::rotulo)
                        .mostrarSe(MODELO_COMBUSTIVEL, ModeloCombustivel.MS.name())
                        .comDica("Em branco, o mapa base soma o tempo morto (fica mais rico) e avisa."),
                CampoForm.numero(TEMPERATURA_REFERENCIA, "Temperatura de referência do ar (°C)", false)
                        .comDica("A temperatura em que a tabela de combustível vale (a correção por ar parte dela). "
                                + "Em branco, o mapa base usa 25 °C e avisa."),
                CampoForm.area(FORMATO, "Formato de importação e exportação")
                        .comDica("Como o software importa e exporta tabelas (ex.: cola células separadas por "
                                + "tabulação)."),
                CampoForm.texto(PROTECAO_AGUA, "Proteção contra água", false)
                        .comDica("Como diz o fabricante (ex.: IP67).")));
        for (FuncaoTabela funcao : FuncaoTabela.values()) {
            campos.addAll(camposDaTabela(funcao));
        }
        return campos;
    }

    private static List<CampoForm> camposDaTabela(FuncaoTabela funcao) {
        String titulo = "Tabela de " + funcao.rotulo().toLowerCase();
        String[] usada = {TipoCarga.MAP_KPA.name(), TipoCarga.TPS_PERCENTUAL.name()};
        String chaveCarga = tabela(funcao, "carga");
        return List.of(
                CampoForm.escolha(chaveCarga, titulo + ": carga", false, TipoCarga.values(), TipoCarga::rotulo)
                        .comDica("Em branco, o perfil não tem esta tabela e o mapa base usa os eixos padrão."),
                CampoForm.texto(tabela(funcao, "nome"), titulo + ": nome no software", true)
                        .mostrarSe(chaveCarga, usada),
                CampoForm.texto(tabela(funcao, "unidade"), titulo + ": unidade das células", false)
                        .mostrarSe(chaveCarga, usada).comDica("Ex.: ms, %, °."),
                CampoForm.area(tabela(funcao, "rotacoes"), titulo + ": pontos de rotação (rpm)")
                        .mostrarSe(chaveCarga, usada)
                        .comDica("Cole a linha ou a coluna do software, ou digite separando por espaço "
                                + "(ex.: 500 1.000 1.500)."),
                CampoForm.area(tabela(funcao, "cargas"), titulo + ": pontos de carga")
                        .mostrarSe(chaveCarga, usada).comDica("Do menor para o maior, em kPa absolutos ou em %."),
                CampoForm.escolha(tabela(funcao, "colunas"), titulo + ": colunas", true,
                        OrientacaoTabela.EixoDasColunas.values(), OrientacaoTabela.EixoDasColunas::rotulo)
                        .mostrarSe(chaveCarga, usada),
                CampoForm.escolha(tabela(funcao, "linhas"), titulo + ": linhas", true,
                        OrientacaoTabela.OrdemDasLinhas.values(), OrientacaoTabela.OrdemDasLinhas::rotulo)
                        .mostrarSe(chaveCarga, usada)
                        .comDica("Como aparece na tela da injeção, para a folha de transcrição sair igual."));
    }

    @Override
    public void escrever(Injecao injecao, ValoresForm valores) {
        valores.texto(MARCA, injecao.marca())
                .texto(MODELO, injecao.modelo())
                .texto(SAIDAS_BICO, String.valueOf(injecao.saidasBico()))
                .texto(SAIDAS_IGNICAO, String.valueOf(injecao.saidasIgnicao()))
                .texto(MAX_SEQUENCIAL, TextoForm.inteiro(injecao.maxCilindrosSequencial()));
        for (Recurso recurso : Recurso.values()) {
            valores.texto(recurso(recurso), String.valueOf(injecao.recursos().contains(recurso)));
        }
        PerfilInjecao perfil = injecao.perfil();
        valores.texto(IMPEDANCIA, nome(perfil.impedanciaAceita()))
                .texto(BICOS_POR_SAIDA, TextoForm.inteiro(perfil.bicosPorSaida()))
                .texto(MODELO_COMBUSTIVEL, nome(perfil.modeloCombustivel()))
                .texto(TEMPO_MORTO_NA_TABELA, perfil.tabelaMsIncluiTempoMorto() == null ? ""
                        : (perfil.tabelaMsIncluiTempoMorto() ? SimNao.SIM : SimNao.NAO).name())
                .texto(TEMPERATURA_REFERENCIA, TextoForm.numero(perfil.temperaturaReferenciaArC()))
                .texto(FORMATO, perfil.formatoImportacaoExportacao())
                .texto(PROTECAO_AGUA, perfil.protecaoAgua());
        for (PerfilTabela tabela : perfil.tabelas()) {
            FuncaoTabela funcao = tabela.funcao();
            valores.texto(tabela(funcao, "carga"), tabela.tipoCarga().name())
                    .texto(tabela(funcao, "nome"), tabela.nome())
                    .texto(tabela(funcao, "unidade"), tabela.unidade())
                    .texto(tabela(funcao, "rotacoes"), pontos(tabela.rotacoes()))
                    .texto(tabela(funcao, "cargas"), pontos(tabela.cargas()))
                    .texto(tabela(funcao, "colunas"), tabela.orientacao().colunas().name())
                    .texto(tabela(funcao, "linhas"), tabela.orientacao().linhas().name());
        }
    }

    @Override
    public Injecao ler(LeituraForm leitura, InfoCatalogo info) {
        String marca = leitura.textoObrigatorio(MARCA);
        String modelo = leitura.textoObrigatorio(MODELO);
        Integer saidasBico = leitura.inteiro(SAIDAS_BICO, true);
        Integer saidasIgnicao = leitura.inteiro(SAIDAS_IGNICAO, true);
        Integer maxSequencial = leitura.inteiro(MAX_SEQUENCIAL, false);
        Set<Recurso> recursos = EnumSet.noneOf(Recurso.class);
        for (Recurso recurso : Recurso.values()) {
            if (leitura.marcado(recurso(recurso))) {
                recursos.add(recurso);
            }
        }
        PerfilInjecao perfil = perfil(leitura);
        if (leitura.temErros()) {
            return null;
        }
        return new Injecao(marca, modelo, saidasBico, saidasIgnicao, maxSequencial, recursos, perfil, info.fonte(),
                info.data());
    }

    private static PerfilInjecao perfil(LeituraForm leitura) {
        ImpedanciaBico impedancia = leitura.escolha(IMPEDANCIA, ImpedanciaBico.class, false);
        Integer bicosPorSaida = leitura.inteiro(BICOS_POR_SAIDA, false);
        ModeloCombustivel modelo = leitura.escolha(MODELO_COMBUSTIVEL, ModeloCombustivel.class, false);
        SimNao tempoMorto = leitura.escolha(TEMPO_MORTO_NA_TABELA, SimNao.class, false);
        Double temperatura = leitura.numero(TEMPERATURA_REFERENCIA, false);
        List<PerfilTabela> tabelas = new ArrayList<>();
        for (FuncaoTabela funcao : FuncaoTabela.values()) {
            PerfilTabela tabela = tabela(leitura, funcao);
            if (tabela != null) {
                tabelas.add(tabela);
            }
        }
        return new PerfilInjecao(impedancia, bicosPorSaida, modelo, tabelas, temperatura,
                tempoMorto == null ? null : tempoMorto == SimNao.SIM, leitura.texto(FORMATO),
                leitura.texto(PROTECAO_AGUA));
    }

    /** A tabela da vaga, ou null se a vaga ficou em branco (ou com erro, que já foi anotado na leitura). */
    private static PerfilTabela tabela(LeituraForm leitura, FuncaoTabela funcao) {
        TipoCarga carga = leitura.escolha(tabela(funcao, "carga"), TipoCarga.class, false);
        if (carga == null) {
            return null;
        }
        String titulo = "Tabela de " + funcao.rotulo().toLowerCase();
        String nome = leitura.textoObrigatorio(tabela(funcao, "nome"));
        List<Double> rotacoes = pontos(leitura, tabela(funcao, "rotacoes"), titulo + ", pontos de rotação");
        List<Double> cargas = pontos(leitura, tabela(funcao, "cargas"), titulo + ", pontos de carga");
        OrientacaoTabela.EixoDasColunas colunas = leitura.escolha(tabela(funcao, "colunas"),
                OrientacaoTabela.EixoDasColunas.class, true);
        OrientacaoTabela.OrdemDasLinhas linhas = leitura.escolha(tabela(funcao, "linhas"),
                OrientacaoTabela.OrdemDasLinhas.class, true);
        if (rotacoes == null || cargas == null || colunas == null || linhas == null) {
            return null;
        }
        return new PerfilTabela(nome, funcao, carga, leitura.texto(tabela(funcao, "unidade")), rotacoes, cargas,
                new OrientacaoTabela(colunas, linhas));
    }

    private static List<Double> pontos(LeituraForm leitura, String chave, String qual) {
        try {
            return NumeroBr.lerLista(leitura.texto(chave));
        } catch (NumberFormatException erro) {
            leitura.erro(qual + ", " + erro.getMessage());
            return null;
        }
    }

    @Override
    public List<String> validar(Injecao injecao) {
        List<String> erros = new ArrayList<>();
        if (injecao.saidasBico() < 1) {
            erros.add("A injeção precisa de pelo menos uma saída de bico.");
        }
        if (injecao.saidasIgnicao() < 0) {
            erros.add("As saídas de ignição não podem ser negativas.");
        }
        if (injecao.maxCilindrosSequencial() != null && injecao.maxCilindrosSequencial() < 1) {
            erros.add("Os cilindros em sequencial devem ser pelo menos 1 (ou deixe em branco).");
        }
        erros.addAll(injecao.perfil().validar());
        return erros;
    }

    @Override
    public boolean nomeDigitado() {
        return false;
    }

    @Override
    public String nomeDerivado(Injecao injecao) {
        return injecao.nome();
    }

    @Override
    public List<String> colunas() {
        return List.of("Saídas de bico", "Saídas de ignição", "Recursos", "Perfil");
    }

    @Override
    public List<String> resumo(Injecao injecao) {
        List<String> recursos = injecao.recursos().stream().sorted().map(Recurso::rotulo).toList();
        return List.of(String.valueOf(injecao.saidasBico()), String.valueOf(injecao.saidasIgnicao()),
                recursos.isEmpty() ? "—" : String.join(", ", recursos), resumoPerfil(injecao.perfil()));
    }

    /** "ms · combustível e ignição", "VE", "tabelas: lambda alvo" ou "—" (sem perfil). */
    static String resumoPerfil(PerfilInjecao perfil) {
        List<String> partes = new ArrayList<>();
        if (perfil.modeloCombustivel() != null) {
            partes.add(perfil.modeloCombustivel() == ModeloCombustivel.MS ? "ms"
                    : perfil.modeloCombustivel().rotulo());
        }
        List<String> tabelas = perfil.tabelas().stream().map(t -> t.funcao().rotulo().toLowerCase()).toList();
        if (!tabelas.isEmpty()) {
            partes.add(tabelas.size() == 1 ? "tabela de " + tabelas.getFirst()
                    : "tabelas de " + String.join(", ", tabelas.subList(0, tabelas.size() - 1)) + " e "
                            + tabelas.getLast());
        }
        return partes.isEmpty() ? "—" : String.join(" · ", partes);
    }

    private static String pontos(List<Double> valores) {
        return valores.stream().map(TextoForm::numero).collect(Collectors.joining(" "));
    }

    private static String nome(Enum<?> valor) {
        return valor == null ? "" : valor.name();
    }
}
