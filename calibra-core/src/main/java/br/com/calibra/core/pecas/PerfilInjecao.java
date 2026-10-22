package br.com.calibra.core.pecas;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import br.com.calibra.core.mapa.FuncaoTabela;
import br.com.calibra.core.mapa.PerfilTabela;

/**
 * O perfil completo da injeção (SPEC 9; fase 4): o que o mapa base e a folha de transcrição precisam saber do
 * software dela. Tudo opcional: cada campo vem do manual ou do software da injeção, cadastrado pelo mecânico com
 * fonte e data (SPEC 2.7). Uma injeção cadastrada antes da fase 4 tem o perfil vazio.
 *
 * @param impedanciaAceita a impedância de bico que as saídas aceitam
 * @param bicosPorSaida quantos bicos uma saída aguenta
 * @param modeloCombustivel como a injeção calcula o combustível
 * @param tabelas as tabelas que o mapa base preenche, no máximo uma por função
 * @param temperaturaReferenciaArC a temperatura do ar em que a tabela de combustível vale (a correção por ar parte
 *     dela); vazia, o mapa base usa {@link #TEMPERATURA_REFERENCIA_PADRAO_C} e avisa
 * @param tabelaMsIncluiTempoMorto se o tempo da tabela em ms já inclui o tempo morto do bico
 * @param formatoImportacaoExportacao como o software importa e exporta tabelas (texto livre)
 * @param protecaoAgua a proteção contra água (ex.: o grau IP), como diz o fabricante
 */
public record PerfilInjecao(
        ImpedanciaBico impedanciaAceita,
        Integer bicosPorSaida,
        ModeloCombustivel modeloCombustivel,
        List<PerfilTabela> tabelas,
        Double temperaturaReferenciaArC,
        Boolean tabelaMsIncluiTempoMorto,
        String formatoImportacaoExportacao,
        String protecaoAgua) {

    /** A temperatura de referência da correção por ar quando o perfil não diz qual é (SPEC 7.5). */
    public static final double TEMPERATURA_REFERENCIA_PADRAO_C = 25.0;

    public PerfilInjecao {
        tabelas = tabelas == null ? List.of() : List.copyOf(tabelas);
        formatoImportacaoExportacao = formatoImportacaoExportacao == null ? "" : formatoImportacaoExportacao.strip();
        protecaoAgua = protecaoAgua == null ? "" : protecaoAgua.strip();
    }

    /** O perfil de uma injeção cadastrada sem ele. */
    public static PerfilInjecao vazio() {
        return new PerfilInjecao(null, null, null, List.of(), null, null, "", "");
    }

    /** A tabela que faz este papel no software, se o perfil tiver. */
    public Optional<PerfilTabela> tabela(FuncaoTabela funcao) {
        return tabelas.stream().filter(tabela -> tabela.funcao() == funcao).findFirst();
    }

    /** O que está errado no perfil, em português; vazio se estiver certo. */
    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (bicosPorSaida != null && bicosPorSaida < 1) {
            erros.add("Bicos por saída: pelo menos 1 (ou deixe em branco).");
        }
        if (temperaturaReferenciaArC != null && !(temperaturaReferenciaArC >= -40 && temperaturaReferenciaArC <= 150)) {
            erros.add("Temperatura de referência do ar: de −40 a 150 °C (ou deixe em branco).");
        }
        Set<FuncaoTabela> vistas = EnumSet.noneOf(FuncaoTabela.class);
        for (PerfilTabela tabela : tabelas) {
            if (!vistas.add(tabela.funcao())) {
                erros.add("Só uma tabela de " + tabela.funcao().rotulo().toLowerCase() + " por injeção.");
            }
            erros.addAll(tabela.validar());
        }
        return erros;
    }
}
