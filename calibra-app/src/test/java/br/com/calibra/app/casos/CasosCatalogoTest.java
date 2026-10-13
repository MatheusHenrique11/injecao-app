package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosCatalogo.Gravacao;
import br.com.calibra.app.catalogo.ExemplosCatalogo;
import br.com.calibra.app.catalogo.ExemplosCatalogo.Exemplo;
import br.com.calibra.app.catalogo.FormasCatalogo;
import br.com.calibra.app.catalogo.FormularioCatalogo;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.combustivel.GasolinaComEtanol;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.catalogo.VersaoCatalogo;

/** Casos de uso dos catálogos, com o banco de verdade numa pasta temporária. Dados fictícios. */
class CasosCatalogoTest {

    @TempDir
    Path raiz;

    private final RelogioAjustavel relogio = new RelogioAjustavel(LocalDateTime.of(2026, 10, 12, 9, 0));
    private CasosCatalogo casos;

    @BeforeEach
    void abrirBanco() throws IOException {
        PastasDados pastas = new PastasDados(raiz);
        pastas.criar();
        BancoDados banco = new BancoDados(pastas.banco());
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
