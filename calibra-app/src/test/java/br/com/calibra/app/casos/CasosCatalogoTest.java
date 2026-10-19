package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosCatalogo.Gravacao;
import br.com.calibra.app.catalogo.ExemplosCatalogo;
import br.com.calibra.app.catalogo.ExemplosCatalogo.Exemplo;
import br.com.calibra.app.catalogo.FormasCatalogo;
import br.com.calibra.app.catalogo.FormularioCatalogo;
import br.com.calibra.app.catalogo.FormularioSugeridos;
import br.com.calibra.app.catalogo.FormularioSugeridos.Linha;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.combustivel.GasolinaComEtanol;
import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemMaterial;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.Kit;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.core.orcamento.ServicosSugeridos;
import br.com.calibra.core.orcamento.ServicosSugeridos.Sugestao;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.Sql;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.catalogo.VersaoCatalogo;

/** Casos de uso dos catálogos, com o banco de verdade numa pasta temporária. Dados fictícios. */
class CasosCatalogoTest {

    /** Um serviço como o 0.3.0 grava (o mesmo formato de {@code servico-0.3.0.json}, no calibra-dados). */
    private static final String SERVICO_030 =
            "{\"nome\":\"Limpeza de bicos\",\"horas\":1.5,\"fonte\":\"Tabela da oficina\",\"data\":\"2026-10-17\"}";

    @TempDir
    Path raiz;

    private final RelogioAjustavel relogio = new RelogioAjustavel(LocalDateTime.of(2026, 10, 12, 9, 0));
    private BancoDados banco;
    private CasosCatalogo casos;

    @BeforeEach
    void abrirBanco() throws IOException {
        PastasDados pastas = new PastasDados(raiz);
        pastas.criar();
        banco = new BancoDados(pastas.banco());
        Migracoes.migrar(banco, new Backup(pastas, banco, "0.2.0-teste", relogio));
        casos = new CasosCatalogo(banco, relogio);
    }

    @Test
    void cadaTipoGravaVersaoNovaAoEditarEGuardaAAntiga() {
        ExemplosCatalogo.umDeCada().forEach(this::conferirVersoes);
    }

    private <T> void conferirVersoes(Exemplo<T> exemplo) {
        String nome = exemplo.tipo().codigo();
        ItemCatalogo<T> criado = casos.criar(exemplo.tipo(), exemplo.dados(), exemplo.info());
        assertEquals(1, criado.versao(), nome);

        InfoCatalogo conferir = new InfoCatalogo(exemplo.info().nome(), exemplo.info().fonte(), exemplo.info().data(),
                true, exemplo.info().precoReferencia(), "Conferir na bancada.");
        Gravacao<T> gravacao = casos.salvar(exemplo.tipo(), criado.id(), exemplo.dados(), conferir);
        assertTrue(gravacao.versaoNova(), nome);
        assertEquals(2, gravacao.item().versao(), nome);
        assertTrue(casos.buscar(exemplo.tipo(), criado.id()).orElseThrow().info().aVerificar(), nome);

        List<VersaoCatalogo<T>> versoes = casos.versoes(exemplo.tipo(), criado.id());
        assertEquals(List.of(2, 1), versoes.stream().map(VersaoCatalogo::versao).toList(), nome);
        assertEquals(exemplo.info(), versoes.getLast().info(), nome);
        assertEquals(exemplo.tipo().comVersao(exemplo.dados(), 1), versoes.getLast().dados(), nome);
    }

    @Test
    void salvarSemMudancaNaoCriaVersao() {
        Exemplo<Bico> bico = ExemplosCatalogo.bicoFixo();
        ItemCatalogo<Bico> criado = casos.criar(TipoCatalogo.BICO, bico.dados(), bico.info());
        Gravacao<Bico> gravacao = casos.salvar(TipoCatalogo.BICO, criado.id(), bico.dados(), bico.info());
        assertFalse(gravacao.versaoNova());
        assertEquals(1, gravacao.item().versao());
        assertEquals(1, casos.versoes(TipoCatalogo.BICO, criado.id()).size());
        assertEquals(TempoMorto.fixo(1.05), casos.buscar(TipoCatalogo.BICO, criado.id()).orElseThrow().dados()
                .tempoMorto(), "o tempo morto de valor único volta igual do banco");
    }

    /** Abrir um item que veio com o app e gravar sem mexer em nada não cria versão. */
    @Test
    void sementesPassamPelaTelaSemMudar() {
        for (TipoCatalogo<?> tipo : TipoCatalogo.todos()) {
            reabrirSemMudar(tipo);
        }
    }

    /**
     * Teste nº 1 da Fase 3B: o serviço gravado pelo 0.3.0 (JSON sem os campos da auto elétrica) aparece como Outro e,
     * aberto e salvo sem mexer, não ganha versão nova.
     */
    @Test
    void servicoDo030PassaPelaTelaSemMudar() {
        long id = banco.transacao(c -> {
            try (PreparedStatement item = c.prepareStatement("INSERT INTO catalogo_item (tipo, versao_atual, nome, "
                    + "fonte, data, a_verificar, observacoes, dados_json, busca, criado_em, atualizado_em) VALUES "
                    + "('SERVICO', 1, 'Limpeza de bicos', 'Tabela da oficina', '2026-10-17', 0, '', ?, "
                    + "'limpeza de bicos tabela da oficina', '2026-10-18T20:52', '2026-10-18T20:52')")) {
                item.setString(1, SERVICO_030);
                item.executeUpdate();
            }
            long novo = Sql.ultimoId(c);
            try (PreparedStatement versao = c.prepareStatement("INSERT INTO catalogo_versao (item_id, versao, nome, "
                    + "fonte, data, a_verificar, observacoes, dados_json, criada_em) VALUES (?, 1, 'Limpeza de bicos', "
                    + "'Tabela da oficina', '2026-10-17', 0, '', ?, '2026-10-18T20:52')")) {
                versao.setLong(1, novo);
                versao.setString(2, SERVICO_030);
                versao.executeUpdate();
            }
            return novo;
        });
        ServicoPadrao lido = casos.buscar(TipoCatalogo.SERVICO, id).orElseThrow().dados();
        assertEquals(CategoriaServico.OUTRO, lido.categoria());
        assertTrue(lido.exigeEntradaDoVeiculo());

        reabrirSemMudar(TipoCatalogo.SERVICO);
        assertEquals(1, casos.versoes(TipoCatalogo.SERVICO, id).size());
    }

    /**
     * Kit (Fase 3B, SPEC 6.6.4): cada linha precisa apontar para um item ou um serviço do catálogo (o serviço não vale
     * como item); o nome guardado em cada linha é o de agora, e gravar de novo sem mudar não cria versão.
     */
    @Test
    void kitApontaParaItensEServicosDoCatalogo() {
        ItemCatalogo<ItemMaterial> rele = casos.criar(TipoCatalogo.ITEM, ExemplosCatalogo.item().dados(),
                ExemplosCatalogo.item().info());
        Exemplo<ServicoPadrao> hora = ExemplosCatalogo.servicoPorHora();
        ItemCatalogo<ServicoPadrao> servico = casos.criar(TipoCatalogo.SERVICO, hora.dados(), hora.info());
        Exemplo<Kit> exemplo = ExemplosCatalogo.kit(rele.id(), servico.id());
        Kit comNomeVelho = new Kit(exemplo.dados().nome(), List.of(
                new Kit.Linha(Kit.TipoLinha.ITEM, rele.id(), "Nome antigo", new BigDecimal("2")),
                exemplo.dados().linhas().get(1)), exemplo.dados().fonte(), exemplo.dados().data());

        ItemCatalogo<Kit> kit = casos.criar(TipoCatalogo.KIT, comNomeVelho, exemplo.info());
        assertEquals(exemplo.dados(), kit.dados(), "o nome de cada linha vem do catálogo");
        assertFalse(casos.salvar(TipoCatalogo.KIT, kit.id(), exemplo.dados(), exemplo.info()).versaoNova());
        assertNull(kit.info().precoReferencia(), "o kit não tem preço próprio");

        EntradaInvalidaException trocado = assertThrows(EntradaInvalidaException.class, () -> casos.criar(
                TipoCatalogo.KIT, ExemplosCatalogo.kit(servico.id(), rele.id()).dados(), exemplo.info()));
        assertEquals(List.of("Linha 1 (Relé teste 12 V 40 A): esse item não está no catálogo; escolha de novo.",
                "Linha 2 (Limpeza de bicos teste): esse serviço não está no catálogo; escolha de novo."),
                trocado.erros(), "os ids trocados: o serviço não vale como item, e o item não vale como serviço");
        assertThrows(EntradaInvalidaException.class, () -> casos.criar(TipoCatalogo.KIT,
                ExemplosCatalogo.kit(99_999, servico.id()).dados(), exemplo.info()), "id que não existe");
        assertEquals(1, casos.listar(TipoCatalogo.KIT, "", true).size(), "nada mais foi gravado");
    }

    /**
     * O kit no orçamento (Fase 3B): uma linha para cada item e serviço, com a versão de agora do catálogo e a
     * quantidade do kit; o serviço pela regra dele (horas × o valor da hora, ou o preço próprio); o arquivado entra com
     * aviso.
     */
    @Test
    void linhasDoKitParaOOrcamento() {
        ItemCatalogo<ItemMaterial> rele = casos.criar(TipoCatalogo.ITEM, ExemplosCatalogo.item().dados(),
                ExemplosCatalogo.item().info());
        Exemplo<ServicoPadrao> hora = ExemplosCatalogo.servicoPorHora();
        ItemCatalogo<ServicoPadrao> limpeza = casos.criar(TipoCatalogo.SERVICO, hora.dados(), hora.info());
        Exemplo<ServicoPadrao> preco = ExemplosCatalogo.servicoComPreco();
        ItemCatalogo<ServicoPadrao> diagnostico = casos.criar(TipoCatalogo.SERVICO, preco.dados(), preco.info());
        Kit dados = new Kit("Alimentação teste", List.of(
                new Kit.Linha(Kit.TipoLinha.ITEM, rele.id(), "", new BigDecimal("2")),
                new Kit.Linha(Kit.TipoLinha.SERVICO, limpeza.id(), "", new BigDecimal("2")),
                new Kit.Linha(Kit.TipoLinha.SERVICO, diagnostico.id(), "", BigDecimal.ONE)), "Tabela da oficina", null);
        ItemCatalogo<Kit> kit = casos.criar(TipoCatalogo.KIT, dados, new InfoCatalogo("Alimentação teste",
                "Tabela da oficina", null, false, null, ""));

        CasosCatalogo.LinhasDoKit linhas = casos.linhasDoKit(kit.id(), Dinheiro.ler("180,00"));
        assertEquals(List.of(), linhas.avisos());
        assertEquals(List.of(
                new ItemOrcamento(TipoItem.PECA, "Relé teste 12 V 40 A", new BigDecimal("2"), "un",
                        Dinheiro.ler("18,50"), rele.id(), 1, false),
                new ItemOrcamento(TipoItem.SERVICO, "Limpeza de bicos teste", new BigDecimal("3"), "h",
                        Dinheiro.ler("180,00"), limpeza.id(), 1, false),
                new ItemOrcamento(TipoItem.SERVICO, "Diagnóstico com scanner e relatório (teste)", BigDecimal.ONE,
                        "serviço", Dinheiro.ler("250,00"), diagnostico.id(), 1, false)), linhas.itens(),
                "2 relés; 2 × 1,5 h = 3 h; o diagnóstico pelo preço, com a descrição dele");
        assertEquals(Dinheiro.ler("827,00"), new Orcamento(linhas.itens(), Desconto.NENHUM, 1, "", 15, "", "").total(),
                "2 × R$ 18,50 + 3 h × R$ 180 + R$ 250");

        casos.arquivar(rele.id(), true);
        CasosCatalogo.LinhasDoKit semValorHora = casos.linhasDoKit(kit.id(), null);
        assertEquals(List.of("Kit Alimentação teste: o item \"Relé teste 12 V 40 A\" está arquivado no catálogo; "
                + "confira a linha."), semValorHora.avisos());
        assertEquals(3, semValorHora.itens().size(), "o arquivado entra mesmo assim");
        assertNull(semValorHora.itens().get(1).valorUnitario(), "sem o valor da hora, o serviço por hora fica em branco");

        assertEquals(Set.of(rele.id()), casos.doTipo(TipoCatalogo.ITEM, List.of(rele.id(), limpeza.id(), 99_999L)));
    }

    /** Serviços sugeridos (SPEC 6.6.1): todos numa gravação só, cada um na versão 1, com a categoria da lista. */
    @Test
    void servicosSugeridosEntramTodosDeUmaVez() {
        List<ItemCatalogo<ServicoPadrao>> gravados = casos.adicionarServicosSugeridos(sugeridos(
                new Linha(sugestao("Reparo ou troca de alternador"), true, "2", ""),
                new Linha(sugestao("Confecção de chicote para injeção programável"), true, "", "1.800,00")));
        assertEquals(2, gravados.size());
        List<ItemCatalogo<ServicoPadrao>> lista = casos.ativos(TipoCatalogo.SERVICO);
        assertEquals(List.of("Confecção de chicote para injeção programável", "Reparo ou troca de alternador"),
                lista.stream().map(item -> item.info().nome()).toList());
        assertEquals(List.of(CategoriaServico.INJECAO_PROGRAMAVEL, CategoriaServico.PARTIDA_CARGA),
                lista.stream().map(item -> item.dados().categoria()).toList());
        assertEquals(List.of(1, 1), lista.stream().map(ItemCatalogo::versao).toList());
        assertFalse(lista.getFirst().dados().exigeEntradaDoVeiculo(), "o chicote é de bancada");
    }

    /**
     * O mesmo nome (sem diferença de acento ou maiúscula, também arquivado) não entra de novo, e nada da lista é
     * gravado: nem os outros.
     */
    @Test
    void sugeridoComNomeQueJaExisteNaoGravaNada() {
        Exemplo<ServicoPadrao> exemplo = ExemplosCatalogo.servicoPorHora();
        ServicoPadrao parecido = new ServicoPadrao("TROCA DE REGULADOR (MAQUINA DE VIDRO)", 1.0, "Tabela da oficina",
                null, CategoriaServico.VIDROS_TRAVAS, "", true, null);
        ItemCatalogo<ServicoPadrao> antigo = casos.criar(TipoCatalogo.SERVICO, parecido, new InfoCatalogo(
                parecido.nome(), "Tabela da oficina", null, false, null, ""));
        casos.arquivar(antigo.id(), true);

        EntradaInvalidaException repetido = assertThrows(EntradaInvalidaException.class,
                () -> casos.adicionarServicosSugeridos(sugeridos(
                        new Linha(sugestao("Troca de motor de vidro elétrico"), true, "1,5", ""),
                        new Linha(sugestao("Troca de regulador (máquina de vidro)"), true, "2", ""))));
        assertEquals(List.of("Já existe um serviço chamado \"Troca de regulador (máquina de vidro)\" no catálogo."),
                repetido.erros());
        assertEquals(List.of(antigo.id()), casos.listar(TipoCatalogo.SERVICO, "", true).stream().map(ItemCatalogo::id)
                .toList(), "nada foi gravado");

        EntradaInvalidaException duasVezes = assertThrows(EntradaInvalidaException.class,
                () -> casos.adicionarServicosSugeridos(List.of(novo(exemplo), novo(exemplo))));
        assertEquals(List.of("Já existe um serviço chamado \"Limpeza de bicos teste\" no catálogo."),
                duasVezes.erros(), "o mesmo nome duas vezes na lista");
        assertEquals(1, casos.listar(TipoCatalogo.SERVICO, "", true).size());
    }

    @Test
    void sugeridoForaDasRegrasNaoGrava() {
        ServicoPadrao semHoras = new ServicoPadrao("Teste de bateria e do sistema de carga", 0.0, "Tabela da oficina",
                null, CategoriaServico.PARTIDA_CARGA, "", true, null);
        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class,
                () -> casos.adicionarServicosSugeridos(List.of(new FormularioSugeridos.Novo(semHoras,
                        new InfoCatalogo(semHoras.nome(), "Tabela da oficina", null, false, null, "")))));
        assertEquals(List.of("Teste de bateria e do sistema de carga: As horas do serviço devem ser maiores que zero."),
                erro.erros());
        assertEquals(List.of("Marque ao menos um serviço e preencha as horas ou o preço dele."),
                assertThrows(EntradaInvalidaException.class, () -> casos.adicionarServicosSugeridos(List.of()))
                        .erros());
        assertTrue(casos.listar(TipoCatalogo.SERVICO, "", true).isEmpty());
    }

    private static Sugestao sugestao(String nome) {
        return ServicosSugeridos.todos().stream().filter(s -> s.nome().equals(nome)).findFirst().orElseThrow();
    }

    private static List<FormularioSugeridos.Novo> sugeridos(Linha... linhas) {
        FormularioSugeridos.Leitura leitura = FormularioSugeridos.ler(List.of(linhas));
        assertEquals(List.of(), leitura.erros());
        return leitura.servicos();
    }

    private static FormularioSugeridos.Novo novo(Exemplo<ServicoPadrao> exemplo) {
        return new FormularioSugeridos.Novo(exemplo.dados(), exemplo.info());
    }

    private <T> void reabrirSemMudar(TipoCatalogo<T> tipo) {
        FormularioCatalogo<T> formulario = FormasCatalogo.de(tipo);
        for (ItemCatalogo<T> item : casos.ativos(tipo)) {
            FormularioCatalogo.Leitura<T> lido = formulario.ler(formulario.escrever(item.dados(), item.info()));
            assertEquals(List.of(), lido.erros(), item.info().nome());
            assertFalse(casos.salvar(tipo, item.id(), lido.dados(), lido.info()).versaoNova(), item.info().nome());
        }
    }

    @Test
    void arquivadoSaiDasEscolhasMasContinuaGuardado() {
        Exemplo<Bico> bico = ExemplosCatalogo.bicoComTabela();
        ItemCatalogo<Bico> criado = casos.criar(TipoCatalogo.BICO, bico.dados(), bico.info());
        casos.arquivar(criado.id(), true);

        assertTrue(casos.ativos(TipoCatalogo.BICO).isEmpty());
        assertEquals(1, casos.listar(TipoCatalogo.BICO, "", true).size());
        ItemCatalogo<Bico> guardado = casos.buscar(TipoCatalogo.BICO, criado.id()).orElseThrow();
        assertTrue(guardado.arquivado());
        assertEquals(bico.dados(), guardado.dados(), "um cálculo antigo ainda acha a peça");

        casos.arquivar(criado.id(), false);
        assertEquals(List.of(criado.id()), casos.ativos(TipoCatalogo.BICO).stream().map(ItemCatalogo::id).toList());
    }

    @Test
    void combustiveisVemDasSementes() {
        assertEquals(6, casos.combustiveis().size());
        assertEquals("METANOL", casos.combustivel("METANOL").orElseThrow().dados().codigo());
        assertTrue(casos.combustivel("E99").isEmpty());
    }

    @Test
    void editarCombustivelCriaVersaoComONumeroNoRegistro() {
        ItemCatalogo<Combustivel> e32 = casos.combustivel("E32").orElseThrow();
        Combustivel atual = e32.dados();
        Combustivel trinta = new Combustivel(atual.codigo(), atual.nome(), new GasolinaComEtanol(0.30), atual.densidadeKgL(),
                atual.lambdaSobPressao(), atual.lambdaAspiradoPlenaCarga(), atual.avisoLambda(), atual.vigenteDesde(),
                atual.fonte(), 1);

        Gravacao<Combustivel> gravacao = casos.salvar(TipoCatalogo.COMBUSTIVEL, e32.id(), trinta, e32.info());
        assertEquals(2, gravacao.item().dados().versao());
        assertEquals(2, casos.combustivel("E32").orElseThrow().dados().versao());
        Combustivel primeira = casos.versoes(TipoCatalogo.COMBUSTIVEL, e32.id()).getLast().dados();
        assertEquals(1, primeira.versao());
        assertEquals(new GasolinaComEtanol(0.32), primeira.composicao());
    }

    @Test
    void codigoDoCombustivelEUnicoENaoMuda() {
        ItemCatalogo<Combustivel> e32 = casos.combustivel("E32").orElseThrow();
        Combustivel atual = e32.dados();
        EntradaInvalidaException repetido = assertThrows(EntradaInvalidaException.class,
                () -> casos.criar(TipoCatalogo.COMBUSTIVEL, atual, e32.info()));
        assertEquals("Já existe um combustível com o código E32 (Gasolina C comum E32). Use outro código.",
                repetido.getMessage());

        Combustivel outroCodigo = new Combustivel("E30", atual.nome(), atual.composicao(), atual.densidadeKgL(),
                atual.lambdaSobPressao(), atual.lambdaAspiradoPlenaCarga(), atual.avisoLambda(), atual.vigenteDesde(),
                atual.fonte(), 1);
        EntradaInvalidaException mudou = assertThrows(EntradaInvalidaException.class,
                () -> casos.salvar(TipoCatalogo.COMBUSTIVEL, e32.id(), outroCodigo, e32.info()));
        assertTrue(mudou.getMessage().startsWith("O código do combustível não muda depois de criado"), mudou.getMessage());
        assertEquals(1, casos.combustivel("E32").orElseThrow().versao());
    }

    @Test
    void itemForaDasRegrasNaoEGravado() {
        Exemplo<Bico> bico = ExemplosCatalogo.bicoFixo();
        Bico semVazao = new Bico(bico.dados().nome(), 0, 300.0, 0.70, TempoMorto.fixo(1.0), null, bico.dados().fonte(),
                null);
        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class,
                () -> casos.criar(TipoCatalogo.BICO, semVazao, bico.info()));
        assertEquals(List.of("A vazão nominal do bico deve ser maior que zero."), erro.erros());

        InfoCatalogo semFonte = new InfoCatalogo("Sem fonte", "", null, false, null, "");
        erro = assertThrows(EntradaInvalidaException.class,
                () -> casos.criar(TipoCatalogo.BICO, bico.dados(), semFonte));
        assertEquals(List.of("Informe a fonte dos dados (manual, site do fabricante, medição na bancada...)."),
                erro.erros());
        assertTrue(casos.listar(TipoCatalogo.BICO, "", true).isEmpty());
    }
}
