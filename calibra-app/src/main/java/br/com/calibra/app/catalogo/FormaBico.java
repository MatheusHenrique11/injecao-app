package br.com.calibra.app.catalogo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.com.calibra.app.catalogo.LeituraForm.LinhaTabela;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.core.unidades.Conversoes;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Bico: vazão, pressão e fluido do teste, impedância e a tabela de tempo morto (tensão × pressão). Na tela a
 * pressão fica em bar, como na calculadora; no núcleo, em kPa.
 */
final class FormaBico implements FormaCatalogo<Bico> {

    static final String VAZAO = "vazao";
    static final String PRESSAO = "pressaoTeste";
    static final String DENSIDADE = "densidadeTeste";
    static final String IMPEDANCIA = "impedancia";
    static final String TEMPO_MORTO = "tempoMorto";

    private static final int TENSAO = 0;
    private static final int PRESSAO_BAR = 1;
    private static final int TEMPO = 2;

    @Override
    public TipoCatalogo<Bico> tipo() {
        return TipoCatalogo.BICO;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(
                CampoForm.numero(VAZAO, "Vazão nominal (cm³/min)", true),
                CampoForm.numero(PRESSAO, "Pressão do teste (bar)", true)
                        .comDica("Pressão diferencial em que o fabricante mediu a vazão."),
                CampoForm.numero(DENSIDADE, "Densidade do fluido de teste (kg/L)", true)
                        .comDica("0,70 é o fluido de teste padrão da SPEC; confira na ficha do fabricante."),
                CampoForm.numero(IMPEDANCIA, "Impedância (Ω)", false),
                CampoForm.tabela(TEMPO_MORTO, "Tempo morto", List.of("Tensão (V)", "Pressão (bar)", "Tempo (ms)"))
                        .comDica("Um ponto por linha, da tabela do fabricante. Valor único: preencha só o tempo, "
                                + "ele vale para qualquer tensão e pressão."));
    }

    @Override
    public void novo(ValoresForm valores) {
        valores.texto(DENSIDADE, TextoForm.numero(Parametros.padrao().densidadeFluidoTesteKgL()));
    }

    @Override
    public void escrever(Bico bico, ValoresForm valores) {
        valores.texto(VAZAO, TextoForm.numero(bico.vazaoNominalCm3Min()))
                .texto(PRESSAO, TextoForm.numero(Conversoes.kpaParaBar(bico.pressaoTesteKpa())))
                .texto(DENSIDADE, TextoForm.numero(bico.densidadeFluidoTesteKgL()))
                .texto(IMPEDANCIA, TextoForm.numero(bico.impedanciaOhm()));
        List<List<String>> linhas = new ArrayList<>();
        for (TempoMorto.Ponto ponto : bico.tempoMorto().pontos()) {
            linhas.add(List.of(TextoForm.numero(ponto.tensaoV()),
                    TextoForm.numero(Conversoes.kpaParaBar(ponto.pressaoKpa())), TextoForm.numero(ponto.tempoMs())));
        }
        valores.tabela(TEMPO_MORTO, linhas);
    }

    @Override
    public Bico ler(LeituraForm leitura, InfoCatalogo info) {
        Double vazao = leitura.numero(VAZAO, true);
        Double pressaoBar = leitura.numero(PRESSAO, true);
        Double densidade = leitura.numero(DENSIDADE, true);
        Double impedancia = leitura.numero(IMPEDANCIA, false);
        TempoMorto tempoMorto = tempoMorto(leitura);
        if (leitura.temErros()) {
            return null;
        }
        return new Bico(info.nome(), vazao, Conversoes.barParaKpa(pressaoBar), densidade, tempoMorto, impedancia,
                info.fonte(), info.data());
    }

    /** Uma linha só com o tempo = valor único; senão, cada linha precisa de tensão, pressão e tempo. */
    private static TempoMorto tempoMorto(LeituraForm leitura) {
        List<LinhaTabela> linhas = leitura.tabela(TEMPO_MORTO);
        if (linhas.isEmpty()) {
            leitura.erro("Informe o tempo morto: uma linha só com o tempo ou a tabela do fabricante.");
            return null;
        }
        LinhaTabela primeira = linhas.getFirst();
        if (linhas.size() == 1 && primeira.celula(TENSAO) == null && primeira.celula(PRESSAO_BAR) == null) {
            if (primeira.celula(TEMPO) == null) {
                leitura.erro("Tempo morto: preencha o tempo (ms).");
                return null;
            }
            return TempoMorto.fixo(primeira.celula(TEMPO));
        }
        List<TempoMorto.Ponto> pontos = new ArrayList<>();
        for (LinhaTabela linha : linhas) {
            if (!linha.completa(3)) {
                leitura.erro("Tempo morto, linha " + linha.numero() + ": preencha tensão, pressão e tempo "
                        + "(ou deixe uma linha só, com o tempo).");
                continue;
            }
            pontos.add(new TempoMorto.Ponto(linha.celula(TENSAO), Conversoes.barParaKpa(linha.celula(PRESSAO_BAR)),
                    linha.celula(TEMPO)));
        }
        return pontos.isEmpty() ? null : new TempoMorto(pontos);
    }

    @Override
    public List<String> validar(Bico bico) {
        List<String> erros = new ArrayList<>();
        if (!(bico.vazaoNominalCm3Min() > 0)) {
            erros.add("A vazão nominal do bico deve ser maior que zero.");
        }
        if (!(bico.pressaoTesteKpa() > 0)) {
            erros.add("A pressão do teste deve ser maior que zero.");
        }
        if (!(bico.densidadeFluidoTesteKgL() > 0)) {
            erros.add("A densidade do fluido de teste deve ser maior que zero.");
        }
        if (bico.impedanciaOhm() != null && !(bico.impedanciaOhm() > 0)) {
            erros.add("A impedância deve ser maior que zero.");
        }
        Set<String> vistos = new HashSet<>();
        for (TempoMorto.Ponto ponto : bico.tempoMorto().pontos()) {
            if (!(ponto.tempoMs() >= 0)) {
                erros.add("O tempo morto não pode ser negativo.");
            }
            if (bico.tempoMorto().eFixo() && Double.isNaN(ponto.tensaoV())) {
                continue;
            }
            if (!(ponto.tensaoV() > 0) || !(ponto.pressaoKpa() > 0)) {
                erros.add("Tempo morto: a tensão e a pressão devem ser maiores que zero.");
            }
            String chave = TextoForm.numero(ponto.tensaoV()) + " V e "
                    + TextoForm.numero(Conversoes.kpaParaBar(ponto.pressaoKpa())) + " bar";
            if (!vistos.add(chave)) {
                erros.add("Tempo morto: o ponto " + chave + " aparece duas vezes.");
            }
        }
        return erros.stream().distinct().toList();
    }

    @Override
    public List<String> colunas() {
        return List.of("Vazão", "Pressão do teste", "Tempo morto");
    }

    @Override
    public List<String> resumo(Bico bico) {
        TempoMorto tempoMorto = bico.tempoMorto();
        String morto = tempoMorto.eFixo()
                ? TextoForm.numero(tempoMorto.pontos().getFirst().tempoMs()) + " ms"
                : "tabela (" + tempoMorto.pontos().size() + " pontos)";
        return List.of(TextoForm.coluna(bico.vazaoNominalCm3Min(), "cm³/min"),
                TextoForm.coluna(Conversoes.kpaParaBar(bico.pressaoTesteKpa()), "bar"), morto);
    }
}
