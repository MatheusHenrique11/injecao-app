package br.com.calibra.dados.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.oficina.ChecklistModelo;
import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.core.orcamento.ServicosSugeridos;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Migracoes;

/**
 * Os modelos de checklist que vêm com o app (migração V9, bloco 3B.3): os 7 da SPEC 6.6.2, com os textos dela, todos
 * "a verificar" e com todos os itens obrigatórios. A chave de cada modelo é a que os serviços sugeridos usam.
 */
class SementesDosChecklistsTest {

    @TempDir
    Path pasta;

    /**
     * O checksum da V9. Se este teste falhar, o arquivo dos checklists mudou: depois que a V9 chegar aos bancos (MSI
     * 0.3.1), migração aplicada não se edita, e modelo novo ou corrigido entra numa migração nova.
     */
    @Test
    void checksumDaV9NaoMuda() {
        assertEquals(377_876_481, Sementes.checksum(Sementes.CHECKLISTS));
    }

    @Test
    void osSeteModelosDaSpec() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        CatalogoRepositorio catalogo = new CatalogoRepositorio(base.relogio);
        List<ItemCatalogo<ChecklistModelo>> modelos =
                base.banco.ler(c -> catalogo.listar(c, TipoCatalogo.CHECKLIST, "", false));
        Map<String, Integer> itensPorChave = new HashMap<>();
        modelos.forEach(m -> itensPorChave.put(m.chaveSemente(), m.dados().itens().size()));
        assertEquals(Map.of(ServicosSugeridos.CHECKLIST_SOM, 9, ServicosSugeridos.CHECKLIST_VIDRO_E_TRAVA, 6,
                ServicosSugeridos.CHECKLIST_PARTIDA_E_ALTERNADOR, 6, ServicosSugeridos.CHECKLIST_ILUMINACAO, 5,
                ServicosSugeridos.CHECKLIST_CHICOTE, 12, ServicosSugeridos.CHECKLIST_INSTALACAO_INJECAO, 11,
                ServicosSugeridos.CHECKLIST_DIAGNOSTICO, 5), itensPorChave,
                "as chaves que os serviços sugeridos usam, com os itens da SPEC");

        for (ItemCatalogo<ChecklistModelo> modelo : modelos) {
            String nome = modelo.info().nome();
            assertTrue(modelo.info().aVerificar(), nome);
            assertTrue(modelo.info().fonte().startsWith("SPEC 6.6.2"), nome);
            assertEquals(nome, modelo.dados().nome(), nome);
            assertEquals(modelo.info().fonte(), modelo.dados().fonte(), nome);
            assertNull(modelo.info().precoReferencia(), nome);
            assertEquals(List.of(), modelo.dados().validar(), nome);
            assertTrue(modelo.dados().itens().stream().allMatch(i -> i.obrigatorio() && i.aVerificar()),
                    nome + ": todos os itens obrigatórios e a verificar");
        }

        ChecklistModelo som = modelos.stream().filter(m -> m.chaveSemente().equals(ServicosSugeridos.CHECKLIST_SOM))
                .findFirst().orElseThrow().dados();
        assertEquals("Som", som.nome());
        assertEquals("Medir a tensão da bateria antes de começar", som.itens().getFirst().texto(), "na ordem da SPEC");
        assertEquals("Bitola do cabo compatível com a potência (anote na observação a tabela usada)",
                som.itens().get(5).texto(), "o \"campo para anotar\" da SPEC é a observação do item");
        List<String> textos = modelos.stream().flatMap(m -> m.dados().itens().stream()).map(ChecklistModelo.Item::texto)
                .toList();
        assertTrue(textos.stream().noneMatch(t -> t.contains("campo para anotar") || t.contains("(a verificar)")),
                textos::toString);
    }

    /** A V9 sobre um banco na V8 (com um serviço): só acrescenta os modelos; o que já existe não muda. */
    @Test
    void v9SobreUmBancoNaV8() throws Exception {
        BaseDeTeste base = BaseDeTeste.semMigrar(pasta);
        Flyway.configure().dataSource(base.banco.fonte()).locations(Migracoes.LOCAL_PADRAO).target("8").load()
                .migrate();
        CatalogoRepositorio catalogo = new CatalogoRepositorio(base.relogio);
        ServicoPadrao som = new ServicoPadrao("Instalação de som completa", 4.0, "Tabela da oficina", null,
                CategoriaServico.SOM, "", true, null, null);
        long servico = base.banco.transacao(c -> catalogo.inserir(c, TipoCatalogo.SERVICO, som,
                new InfoCatalogo("Instalação de som completa", "Tabela da oficina", null, false, null, ""), null).id());

        Migracoes.Resultado resultado = Migracoes.migrar(base.banco, base.backup);

        assertEquals("8", resultado.versaoAntes());
        assertEquals(BaseDeTeste.VERSAO_BANCO, resultado.versaoDepois());
        assertEquals(7, base.banco.ler(c -> catalogo.listar(c, TipoCatalogo.CHECKLIST, "", false)).size());
        ItemCatalogo<ServicoPadrao> depois = base.banco.ler(c -> catalogo.buscar(c, TipoCatalogo.SERVICO, servico))
                .orElseThrow();
        assertEquals(som, depois.dados(), "o serviço fica como estava, sem checklist");
        assertEquals(1, depois.versao());
    }
}
