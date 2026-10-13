package br.com.calibra.app.catalogo;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import br.com.calibra.core.combustivel.AfrFixo;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.combustivel.Composicao;
import br.com.calibra.core.combustivel.EtanolHidratado;
import br.com.calibra.core.combustivel.GasolinaComEtanol;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Combustível (SPEC 7.2): código, composição (de onde sai o AFR estequiométrico), densidade e lambdas padrão. O
 * teor de etanol aparece em % na tela e vira fração no núcleo. Editar cria versão nova; o cálculo guarda a usada.
 */
final class FormaCombustivel implements FormaCatalogo<Combustivel> {

    static final String CODIGO = "codigo";
    static final String COMPOSICAO = "composicao";
    static final String ETANOL_VOLUME = "etanolVolumePct";
    static final String ETANOL_MASSA = "etanolMassaPct";
    static final String AFR = "afr";
    static final String DENSIDADE = "densidade";
    static final String LAMBDA_SOB_PRESSAO = "lambdaSobPressao";
    static final String LAMBDA_ASPIRADO = "lambdaAspirado";
    static final String AVISO_LAMBDA = "avisoLambda";
    static final String VIGENTE_DESDE = "vigenteDesde";

    /** Código curto, sem espaço: é ele que o motor guarda. */
    private static final Pattern FORMATO_CODIGO = Pattern.compile("[A-Z0-9_-]{1,15}");

    /** De onde sai o AFR estequiométrico (as três formas da SPEC 7.2). */
    enum TipoComposicao {
        GASOLINA_COM_ETANOL("Gasolina com etanol"),
        ETANOL_HIDRATADO("Etanol hidratado"),
        AFR_FIXO("AFR fixo (valor tabelado)");

        private final String rotulo;

        TipoComposicao(String rotulo) {
            this.rotulo = rotulo;
        }

        String rotulo() {
            return rotulo;
        }

        static TipoComposicao de(Composicao composicao) {
            return switch (composicao) {
                case GasolinaComEtanol _ -> GASOLINA_COM_ETANOL;
                case EtanolHidratado _ -> ETANOL_HIDRATADO;
                case AfrFixo _ -> AFR_FIXO;
            };
        }
    }

    @Override
    public TipoCatalogo<Combustivel> tipo() {
        return TipoCatalogo.COMBUSTIVEL;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(
                CampoForm.texto(CODIGO, "Código", true).travarNaEdicao()
                        .comDica("Curto e único (ex.: E32). Não muda depois de criado: os motores guardam o código."),
                CampoForm.escolha(COMPOSICAO, "Composição", true, TipoComposicao.values(), TipoComposicao::rotulo),
                CampoForm.numero(ETANOL_VOLUME, "Etanol na gasolina (% em volume)", true)
                        .mostrarSe(COMPOSICAO, TipoComposicao.GASOLINA_COM_ETANOL.name()).comDica("32 = E32."),
                CampoForm.numero(ETANOL_MASSA, "Etanol (% em massa)", true)
                        .mostrarSe(COMPOSICAO, TipoComposicao.ETANOL_HIDRATADO.name()).comDica("A água não queima."),
                CampoForm.numero(AFR, "AFR estequiométrico", true)
                        .mostrarSe(COMPOSICAO, TipoComposicao.AFR_FIXO.name()),
                CampoForm.numero(DENSIDADE, "Densidade (kg/L)", true),
                CampoForm.numero(LAMBDA_SOB_PRESSAO, "Lambda padrão em plena carga, sobrealimentado", true),
                CampoForm.numero(LAMBDA_ASPIRADO, "Lambda padrão em plena carga, aspirado", true),
                CampoForm.area(AVISO_LAMBDA, "Aviso sobre a lambda")
                        .comDica("Aparece em amarelo no cálculo (ex.: metanol, bem mais rico)."),
                CampoForm.data(VIGENTE_DESDE, "Vigente desde"));
    }

    @Override
    public void escrever(Combustivel combustivel, ValoresForm valores) {
        valores.texto(CODIGO, combustivel.codigo())
                .texto(COMPOSICAO, TipoComposicao.de(combustivel.composicao()).name())
                .texto(DENSIDADE, TextoForm.numero(combustivel.densidadeKgL()))
                .texto(LAMBDA_SOB_PRESSAO, TextoForm.numero(combustivel.lambdaSobPressao()))
                .texto(LAMBDA_ASPIRADO, TextoForm.numero(combustivel.lambdaAspiradoPlenaCarga()))
                .texto(AVISO_LAMBDA, combustivel.avisoLambda())
                .texto(VIGENTE_DESDE, TextoForm.data(combustivel.vigenteDesde()));
        switch (combustivel.composicao()) {
            case GasolinaComEtanol g -> valores.texto(ETANOL_VOLUME, TextoForm.numero(g.fracaoVolumetricaEtanol() * 100));
            case EtanolHidratado h -> valores.texto(ETANOL_MASSA, TextoForm.numero(h.fracaoMassicaEtanol() * 100));
            case AfrFixo a -> valores.texto(AFR, TextoForm.numero(a.afr()));
        }
    }

    @Override
    public void duplicar(ValoresForm valores) {
        valores.texto(CODIGO, "");
    }

    @Override
    public Combustivel ler(LeituraForm leitura, InfoCatalogo info) {
        String codigo = leitura.textoObrigatorio(CODIGO).toUpperCase(Locale.ROOT);
        if (!codigo.isEmpty() && !FORMATO_CODIGO.matcher(codigo).matches()) {
            leitura.erro("O código do combustível usa só letras, números, - e _ (até 15), sem espaço (ex.: E32).");
        }
        Composicao composicao = composicao(leitura);
        Double densidade = leitura.numero(DENSIDADE, true);
        Double lambdaSobPressao = leitura.numero(LAMBDA_SOB_PRESSAO, true);
        Double lambdaAspirado = leitura.numero(LAMBDA_ASPIRADO, true);
        String aviso = leitura.texto(AVISO_LAMBDA);
        LocalDate vigenteDesde = leitura.data(VIGENTE_DESDE);
        if (densidade != null && !(densidade > 0)) {
            leitura.erro("A densidade do combustível deve ser maior que zero.");
        }
        if ((lambdaSobPressao != null && !(lambdaSobPressao > 0)) || (lambdaAspirado != null && !(lambdaAspirado > 0))) {
            leitura.erro("Os lambdas padrão devem ser maiores que zero.");
        }
        if (leitura.temErros()) {
            return null;
        }
        return new Combustivel(codigo, info.nome(), composicao, densidade, lambdaSobPressao, lambdaAspirado,
                aviso.isEmpty() ? null : aviso, vigenteDesde, info.fonte(), 1);
    }

    /** A composição escolhida, com o valor do campo dela; as faixas são conferidas aqui, em %. */
    private static Composicao composicao(LeituraForm leitura) {
        TipoComposicao tipo = leitura.escolha(COMPOSICAO, TipoComposicao.class, true);
        if (tipo == null) {
            return null;
        }
        return switch (tipo) {
            case GASOLINA_COM_ETANOL -> gasolina(leitura);
            case ETANOL_HIDRATADO -> hidratado(leitura);
            case AFR_FIXO -> afrFixo(leitura);
        };
    }

    private static Composicao gasolina(LeituraForm leitura) {
        Double pct = leitura.numero(ETANOL_VOLUME, true);
        if (pct == null) {
            return null;
        }
        if (!(pct >= 0 && pct <= 100)) {
            leitura.erro("O etanol na gasolina deve ficar entre 0 e 100% (32 = E32).");
            return null;
        }
        return new GasolinaComEtanol(pct / 100.0);
    }

    private static Composicao hidratado(LeituraForm leitura) {
        Double pct = leitura.numero(ETANOL_MASSA, true);
        if (pct == null) {
            return null;
        }
        if (!(pct > 0 && pct <= 100)) {
            leitura.erro("O etanol em massa deve ficar entre 0 e 100% (93 = 93%).");
            return null;
        }
        return new EtanolHidratado(pct / 100.0);
    }

    private static Composicao afrFixo(LeituraForm leitura) {
        Double afr = leitura.numero(AFR, true);
        if (afr == null) {
            return null;
        }
        if (!(afr > 0)) {
            leitura.erro("O AFR estequiométrico deve ser maior que zero.");
            return null;
        }
        return new AfrFixo(afr);
    }

    /** As faixas já são conferidas na leitura (o record do núcleo recusa valores fora delas). */
    @Override
    public List<String> validar(Combustivel combustivel) {
        return List.of();
    }

    @Override
    public List<String> colunas() {
        return List.of("Código", "AFR estequiométrico", "Densidade", "Lambda sob pressão / aspirado");
    }

    @Override
    public List<String> resumo(Combustivel combustivel) {
        return List.of(combustivel.codigo(), NumeroBr.formatar(combustivel.afrEstequiometrico(), 0, 2),
                TextoForm.coluna(combustivel.densidadeKgL(), "kg/L"),
                TextoForm.numero(combustivel.lambdaSobPressao()) + " / "
                        + TextoForm.numero(combustivel.lambdaAspiradoPlenaCarga()));
    }
}
