package br.com.calibra.dados.config;

import java.util.Objects;
import java.util.function.Supplier;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.mapa.RegrasMapaBase;
import br.com.calibra.core.oficina.PadroesOs;
import br.com.calibra.core.orcamento.PadroesOrcamento;
import br.com.calibra.core.unidades.UnidadesPreferidas;

/**
 * Uma configuração guardada na tabela {@code configuracao}: o nome, o tipo e o valor padrão.
 *
 * @param padrao valor quando nada foi salvo; também completa campos que faltarem num JSON antigo
 */
public record Chave<T>(String nome, Class<T> tipo, Supplier<T> padrao) {

    /** Dados da oficina para os PDFs. */
    public static final Chave<DadosOficina> OFICINA = new Chave<>("oficina", DadosOficina.class, DadosOficina::vazia);

    /** Padrões de cálculo e limiares do semáforo (SPEC 7.1 e 7.4) para os cálculos novos. */
    public static final Chave<Parametros> CALCULO = new Chave<>("calculo", Parametros.class, Parametros::padrao);

    /** O que um orçamento novo traz preenchido (desconto, prazo, validade, condições e valor da hora). */
    public static final Chave<PadroesOrcamento> ORCAMENTO =
            new Chave<>("orcamento", PadroesOrcamento.class, PadroesOrcamento::padrao);

    /** Padrões das OS (bloco 3.3): garantia da entrega, itens da vistoria e texto da via do cliente. */
    public static final Chave<PadroesOs> OS = new Chave<>("os", PadroesOs.class, PadroesOs::padrao);

    /** As unidades preferidas na tela e nos PDFs (fase 4, decisão 289). */
    public static final Chave<UnidadesPreferidas> UNIDADES =
            new Chave<>("unidades", UnidadesPreferidas.class, UnidadesPreferidas::padrao);

    /** As regras do mapa base: lambda, riqueza extra, reduções de ignição e travas (fase 4, decisão 294). */
    public static final Chave<RegrasMapaBase> MAPA_BASE =
            new Chave<>("mapa_base", RegrasMapaBase.class, RegrasMapaBase::padrao);

    /** Pasta de backup escolhida pelo usuário. */
    public static final Chave<ConfiguracaoBackup> BACKUP =
            new Chave<>("backup", ConfiguracaoBackup.class, ConfiguracaoBackup::padrao);

    public Chave {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(padrao, "padrao");
    }
}
