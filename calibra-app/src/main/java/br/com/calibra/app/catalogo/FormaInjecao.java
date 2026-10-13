package br.com.calibra.app.catalogo;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Recurso;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/** Injeção programável: marca e modelo (que formam o nome), saídas, cilindros em sequencial e recursos. */
final class FormaInjecao implements FormaCatalogo<Injecao> {

    static final String MARCA = "marca";
    static final String MODELO = "modelo";
    static final String SAIDAS_BICO = "saidasBico";
    static final String SAIDAS_IGNICAO = "saidasIgnicao";
    static final String MAX_SEQUENCIAL = "maxCilindrosSequencial";

    static String recurso(Recurso recurso) {
        return "recurso." + recurso.name();
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
        return campos;
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
        if (leitura.temErros()) {
            return null;
        }
        return new Injecao(marca, modelo, saidasBico, saidasIgnicao, maxSequencial, recursos, info.fonte(),
                info.data());
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
        return List.of("Saídas de bico", "Saídas de ignição", "Recursos");
    }

    @Override
    public List<String> resumo(Injecao injecao) {
        List<String> recursos = injecao.recursos().stream().sorted().map(Recurso::rotulo).toList();
        return List.of(String.valueOf(injecao.saidasBico()), String.valueOf(injecao.saidasIgnicao()),
                recursos.isEmpty() ? "—" : String.join(", ", recursos));
    }
}
