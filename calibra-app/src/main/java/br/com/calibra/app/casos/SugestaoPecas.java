package br.com.calibra.app.casos;

import static br.com.calibra.core.util.NumeroBr.formatar;
import static br.com.calibra.core.util.NumeroBr.percentual;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.dimensionamento.SelecaoBico;
import br.com.calibra.core.dimensionamento.SelecaoBomba;
import br.com.calibra.core.dimensionamento.SelecaoInjecao;
import br.com.calibra.core.memoria.Aviso;
import br.com.calibra.core.memoria.NivelAviso;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Recurso;
import br.com.calibra.core.unidades.Conversoes;
import br.com.calibra.dados.catalogo.ItemCatalogo;

/**
 * Os botões "Sugerir" do dimensionamento (SPEC 7.3): escolhem no catálogo, com as regras já testadas no núcleo
 * ({@link SelecaoBico}, {@link SelecaoBomba} e {@link SelecaoInjecao}), e explicam a escolha em português. Só
 * os itens ativos entram (quem chama passa a lista). Sem JavaFX.
 */
public final class SugestaoPecas {

    /**
     * O que a sugestão achou.
     *
     * @param item vazio se nenhum item do catálogo serve
     * @param mensagem a escolha (ou a falta dela), numa frase
     * @param avisos avisos amarelos da escolha (ex.: avaliar duas bancadas)
     */
    public record Sugestao<T>(Optional<ItemCatalogo<T>> item, String mensagem, List<String> avisos) {

        public Sugestao {
            avisos = List.copyOf(avisos);
        }
    }

    private SugestaoPecas() {
    }

    /**
     * O menor bico que mantém o DC até o máximo, já com o tempo morto, na rotação de potência máxima.
     *
     * @param entrada a entrada da tela (o bico dela, se houver, é ignorado)
     * @throws EntradaInvalidaException se a entrada não fecha ou o motor tem duas bancadas
     */
    public static Sugestao<Bico> bico(List<ItemCatalogo<Bico>> bicos, EntradaDimensionamento entrada) {
        if (bicos.isEmpty()) {
            return new Sugestao<>(Optional.empty(), "Nenhum bico cadastrado no catálogo.", List.of());
        }
        Resultado<Optional<SelecaoBico.Escolha>> resultado = SelecaoBico.escolher(
                bicos.stream().map(ItemCatalogo::dados).toList(), entrada.comBico(null).comBicoSecundario(null));
        List<String> avisos = amarelos(resultado);
        if (resultado.valor().isEmpty()) {
            return new Sugestao<>(Optional.empty(), "Nenhum bico do catálogo atende.", avisos);
        }
        SelecaoBico.Escolha escolha = resultado.valor().get();
        ItemCatalogo<Bico> item = doCatalogo(bicos, escolha.bico());
        return new Sugestao<>(Optional.of(item), "Sugerido: " + item.info().nome() + " (DC "
                + percentual(escolha.dutyCycle(), 1) + " na potência máxima, já com o tempo morto).", avisos);
    }

    /**
     * A menor bomba cuja vazão, na pressão exigida e na tensão de trabalho, cobre a necessidade com a folga verde.
     *
     * @throws EntradaInvalidaException se a entrada não fecha
     */
    public static Sugestao<Bomba> bomba(List<ItemCatalogo<Bomba>> bombas, EntradaDimensionamento entrada) {
        if (bombas.isEmpty()) {
            return new Sugestao<>(Optional.empty(), "Nenhuma bomba cadastrada no catálogo.", List.of());
        }
        if (entrada.tensaoV() == null) {
            return new Sugestao<>(Optional.empty(), "Informe a tensão de trabalho para ler as curvas das bombas.",
                    List.of());
        }
        Dimensionamento d = CalculoDimensionamento.calcular(entrada.comBomba(null).comBombaDisponivel(null)).valor();
        Dimensionamento.BombaNecessaria necessaria = d.bomba();
        Resultado<Optional<SelecaoBomba.Escolha>> resultado = SelecaoBomba.escolher(
                bombas.stream().map(ItemCatalogo::dados).toList(), necessaria.vazaoLh(), necessaria.pressaoKpa(),
                entrada.tensaoV(), entrada.parametros().limiares());
        List<String> avisos = amarelos(resultado);
        String condicao = formatar(necessaria.vazaoLh(), 0) + " L/h a " + formatar(
                Conversoes.kpaParaBar(necessaria.pressaoKpa()), 1) + " bar e " + formatar(entrada.tensaoV(), 1) + " V";
        if (resultado.valor().isEmpty()) {
            return new Sugestao<>(Optional.empty(), "Nenhuma bomba do catálogo atende " + condicao + ".", avisos);
        }
        SelecaoBomba.Escolha escolha = resultado.valor().get();
        ItemCatalogo<Bomba> item = doCatalogo(bombas, escolha.bomba());
        return new Sugestao<>(Optional.of(item), "Sugerida: " + item.info().nome() + " (" + formatar(
                escolha.vazaoDisponivelLh(), 0) + " L/h na condição pedida; precisa de " + condicao + ", folga "
                + percentual(escolha.folga(), 0) + ").", avisos);
    }

    /**
     * A primeira injeção, em ordem de nome, com saídas, cilindros em sequencial e recursos para o motor.
     *
     * @param gruposSimultaneo grupos de saídas no modo simultâneo (Configurações)
     * @param exigidos recursos que o projeto pede (ex.: sensor de etanol)
     */
    public static Sugestao<Injecao> injecao(List<ItemCatalogo<Injecao>> injecoes, ConfiguracaoMotor motor,
            int gruposSimultaneo, Set<Recurso> exigidos) {
        if (injecoes.isEmpty()) {
            return new Sugestao<>(Optional.empty(), "Nenhuma injeção cadastrada no catálogo.", List.of());
        }
        List<Injecao> atendem = SelecaoInjecao.queAtendem(injecoes.stream().map(ItemCatalogo::dados).toList(), motor,
                gruposSimultaneo, exigidos);
        String pedidos = exigidos.isEmpty() ? ""
                : " (recursos pedidos: " + String.join(", ", exigidos.stream().sorted()
                        .map(r -> r.rotulo().toLowerCase(Locale.ROOT)).toList()) + ")";
        if (atendem.isEmpty()) {
            List<String> faltas = new ArrayList<>();
            for (ItemCatalogo<Injecao> item : injecoes) {
                faltas.add(item.info().nome() + ": " + String.join("; ",
                        SelecaoInjecao.faltas(item.dados(), motor, gruposSimultaneo, exigidos)));
            }
            return new Sugestao<>(Optional.empty(), "Nenhuma injeção do catálogo atende este motor" + pedidos + ".",
                    faltas);
        }
        ItemCatalogo<Injecao> item = doCatalogo(injecoes, atendem.getFirst());
        String outras = atendem.size() == 1 ? ""
                : " Também atendem: " + String.join(", ", atendem.subList(1, atendem.size()).stream()
                        .map(injecao -> doCatalogo(injecoes, injecao).info().nome()).toList()) + ".";
        return new Sugestao<>(Optional.of(item), "Sugerida: " + item.info().nome() + pedidos + "." + outras,
                List.of());
    }

    private static List<String> amarelos(Resultado<?> resultado) {
        return resultado.avisos().stream().filter(aviso -> aviso.nivel() == NivelAviso.AMARELO)
                .map(Aviso::mensagem).toList();
    }

    /** O item do catálogo de onde saíram os dados escolhidos pelo núcleo. */
    private static <T> ItemCatalogo<T> doCatalogo(List<ItemCatalogo<T>> itens, T dados) {
        return itens.stream().filter(item -> item.dados().equals(dados)).findFirst().orElseThrow();
    }
}
