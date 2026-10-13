package br.com.calibra.app.catalogo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.com.calibra.app.catalogo.LeituraForm.LinhaTabela;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.unidades.Conversoes;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/** Bomba: a curva do fabricante, pressão × tensão × vazão. Na tela a pressão fica em bar; no núcleo, em kPa. */
final class FormaBomba implements FormaCatalogo<Bomba> {

    static final String CURVA = "curva";

    private static final int PRESSAO_BAR = 0;
    private static final int TENSAO = 1;
    private static final int VAZAO = 2;

    @Override
    public TipoCatalogo<Bomba> tipo() {
        return TipoCatalogo.BOMBA;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(CampoForm.tabela(CURVA, "Curva da bomba", List.of("Pressão (bar)", "Tensão (V)", "Vazão (L/h)"))
                .comDica("Um ponto por linha, da curva do fabricante (pressão manométrica na saída). O cálculo "
                        + "não estende a curva: fora dos pontos, ele avisa."));
    }

    @Override
    public void escrever(Bomba bomba, ValoresForm valores) {
        List<List<String>> linhas = new ArrayList<>();
        for (Bomba.Ponto ponto : bomba.curva()) {
            linhas.add(List.of(TextoForm.numero(Conversoes.kpaParaBar(ponto.pressaoKpa())),
                    TextoForm.numero(ponto.tensaoV()), TextoForm.numero(ponto.vazaoLh())));
        }
        valores.tabela(CURVA, linhas);
    }

    @Override
    public Bomba ler(LeituraForm leitura, InfoCatalogo info) {
        List<LinhaTabela> linhas = leitura.tabela(CURVA);
        if (linhas.isEmpty()) {
            leitura.erro("Informe a curva da bomba: pelo menos um ponto de pressão, tensão e vazão.");
            return null;
        }
        List<Bomba.Ponto> curva = new ArrayList<>();
        for (LinhaTabela linha : linhas) {
            if (!linha.completa(3)) {
                leitura.erro("Curva da bomba, linha " + linha.numero() + ": preencha pressão, tensão e vazão.");
                continue;
            }
            curva.add(new Bomba.Ponto(Conversoes.barParaKpa(linha.celula(PRESSAO_BAR)), linha.celula(TENSAO),
                    linha.celula(VAZAO)));
        }
        if (leitura.temErros()) {
            return null;
        }
        return new Bomba(info.nome(), curva, info.fonte(), info.data());
    }

    @Override
    public List<String> validar(Bomba bomba) {
        List<String> erros = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (Bomba.Ponto ponto : bomba.curva()) {
            if (!(ponto.pressaoKpa() >= 0)) {
                erros.add("Curva da bomba: a pressão não pode ser negativa.");
            }
            if (!(ponto.tensaoV() > 0)) {
                erros.add("Curva da bomba: a tensão deve ser maior que zero.");
            }
            if (!(ponto.vazaoLh() >= 0)) {
                erros.add("Curva da bomba: a vazão não pode ser negativa.");
            }
            String chave = TextoForm.numero(Conversoes.kpaParaBar(ponto.pressaoKpa())) + " bar em "
                    + TextoForm.numero(ponto.tensaoV()) + " V";
            if (!vistos.add(chave)) {
                erros.add("Curva da bomba: o ponto " + chave + " aparece duas vezes.");
            }
        }
        return erros.stream().distinct().toList();
    }

    @Override
    public List<String> colunas() {
        return List.of("Tensões", "Pressões", "Vazão");
    }

    @Override
    public List<String> resumo(Bomba bomba) {
        return List.of(
                TextoForm.lista(bomba.curva().stream().map(Bomba.Ponto::tensaoV).toList(), "V"),
                TextoForm.faixa(bomba.curva().stream().map(p -> Conversoes.kpaParaBar(p.pressaoKpa())).toList(), "bar"),
                TextoForm.faixa(bomba.curva().stream().map(Bomba.Ponto::vazaoLh).toList(), "L/h"));
    }
}
