package br.com.calibra.app.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.calibra.app.catalogo.ExemplosCatalogo.Exemplo;
import br.com.calibra.app.catalogo.FormularioCatalogo.Leitura;
import br.com.calibra.app.catalogo.FormularioCatalogo.LinhaHistorico;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.motor.ModeloMotor;
import br.com.calibra.core.motor.ValidadorMotor;
import br.com.calibra.core.oficina.ChecklistModelo;
import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.ItemMaterial;
import br.com.calibra.core.orcamento.Kit;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Sensor;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.core.pecas.Turbo;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.catalogo.VersaoCatalogo;

/** Formulários dos catálogos: texto da tela ↔ records do núcleo, com as mensagens em português. */
class FormulariosCatalogoTest {

    @Test
    void cadaTipoVoltaIgualDepoisDeEscreverELer() {
        ExemplosCatalogo.umDeCada().forEach(FormulariosCatalogoTest::idaEVolta);
        idaEVolta(ExemplosCatalogo.bicoFixo());
        for (Combustivel padrao : CombustiveisPadrao.todos()) {
            idaEVolta(new Exemplo<>(TipoCatalogo.COMBUSTIVEL, padrao,
                    new InfoCatalogo(padrao.nome(), padrao.fonte(), null, false, null, "")));
        }
    }

    private static <T> void idaEVolta(Exemplo<T> exemplo) {
        FormularioCatalogo<T> formulario = FormasCatalogo.de(exemplo.tipo());
        Leitura<T> lido = formulario.ler(formulario.escrever(exemplo.dados(), exemplo.info()));
        assertEquals(List.of(), lido.erros(), exemplo.info().nome());
        assertEquals(exemplo.dados(), lido.dados(), exemplo.info().nome());
        assertEquals(exemplo.info(), lido.info(), exemplo.info().nome());
    }

    @Test
    void todosOsTiposTemFormularioComChavesUnicasEColunasDoResumo() {
        assertEquals(TipoCatalogo.todos().size(), FormasCatalogo.todos().size());
        for (Exemplo<?> exemplo : ExemplosCatalogo.umDeCada()) {
            conferirFormulario(exemplo);
        }
    }

    private static <T> void conferirFormulario(Exemplo<T> exemplo) {
        FormularioCatalogo<T> formulario = FormasCatalogo.de(exemplo.tipo());
        Set<String> chaves = new HashSet<>();
        for (CampoForm campo : formulario.campos()) {
            assertTrue(chaves.add(campo.chave()), "chave repetida: " + campo.chave());
            if (campo.tipo() == CampoForm.Tipo.ESCOLHA) {
                assertFalse(campo.opcoes().isEmpty(), campo.chave());
            }
            if (campo.mostrarSe() != null) {
                CampoForm escolha = formulario.campos().stream()
                        .filter(c -> c.chave().equals(campo.mostrarSe().chave())).findFirst().orElseThrow();
                List<String> opcoes = escolha.opcoes().stream().map(CampoForm.Opcao::valor).toList();
                assertTrue(opcoes.containsAll(campo.mostrarSe().valores()), campo.chave());
            }
        }
        assertEquals(formulario.colunas().size(), formulario.resumo(exemplo.dados()).size(), exemplo.tipo().codigo());
    }

    @Test
    void bicoVazioMostraOQueFalta() {
        Leitura<Bico> lido = FormasCatalogo.de(TipoCatalogo.BICO).ler(new ValoresForm());
        assertNull(lido.dados());
        assertTrue(lido.erros().contains("Preencha \"Nome\"."), lido.erros()::toString);
        assertTrue(lido.erros().contains("Preencha \"Vazão nominal (cm³/min)\"."), lido.erros()::toString);
        assertTrue(lido.erros().contains("Preencha \"Fonte\"."), lido.erros()::toString);
        assertTrue(lido.erros().contains("Informe o tempo morto: uma linha só com o tempo ou a tabela do fabricante."),
                lido.erros()::toString);
    }

    @Test
    void numeroComPontoDecimalERecusado() {
        ValoresForm valores = valoresDoBico().texto(FormaBico.VAZAO, "3.5");
        Leitura<Bico> lido = FormasCatalogo.de(TipoCatalogo.BICO).ler(valores);
        assertEquals(List.of("\"Vazão nominal (cm³/min)\": \"3.5\" não é um número válido: use vírgula para decimais "
                + "(ex.: 3,5)."), lido.erros());
    }

    @Test
    void tempoMortoComUmaLinhaSoDoTempoEValorUnico() {
        ValoresForm valores = valoresDoBico().tabela(FormaBico.TEMPO_MORTO, List.of(List.of("", "", "1,05")));
        Bico bico = FormasCatalogo.de(TipoCatalogo.BICO).ler(valores).dados();
        assertEquals(TempoMorto.fixo(1.05), bico.tempoMorto());
    }

    @Test
    void tempoMortoPulaLinhaVaziaEApontaALinhaIncompleta() {
        FormularioCatalogo<Bico> formulario = FormasCatalogo.de(TipoCatalogo.BICO);
        ValoresForm comVazia = valoresDoBico().tabela(FormaBico.TEMPO_MORTO,
                List.of(List.of("12", "3", "1,1"), List.of("", "", ""), List.of("14", "3", "0,9")));
        assertEquals(2, formulario.ler(comVazia).dados().tempoMorto().pontos().size());

        ValoresForm incompleta = valoresDoBico().tabela(FormaBico.TEMPO_MORTO,
                List.of(List.of("12", "3", "1,1"), List.of("", "", ""), List.of("14", "", "0,9")));
        assertEquals(List.of("Tempo morto, linha 3: preencha tensão, pressão e tempo (ou deixe uma linha só, com o "
                + "tempo)."), formulario.ler(incompleta).erros());

        ValoresForm repetida = valoresDoBico().tabela(FormaBico.TEMPO_MORTO,
                List.of(List.of("12", "3", "1,1"), List.of("12", "3", "1,0")));
        assertEquals(List.of("Tempo morto: o ponto 12 V e 3 bar aparece duas vezes."), formulario.ler(repetida).erros());
    }

    @Test
    void injecaoTiraONomeDaMarcaEDoModelo() {
        FormularioCatalogo<Injecao> formulario = FormasCatalogo.de(TipoCatalogo.INJECAO);
        assertTrue(formulario.campos().stream().noneMatch(c -> c.chave().equals(FormularioCatalogo.NOME)));
        Exemplo<Injecao> exemplo = ExemplosCatalogo.injecao();
        ValoresForm valores = formulario.escrever(exemplo.dados(), exemplo.info()).texto(FormaInjecao.MODELO, "ECU 12");
        assertEquals("Marca Teste ECU 12", formulario.ler(valores).info().nome());

        List<String> erros = formulario.ler(valores.texto(FormaInjecao.MARCA, "")).erros();
        assertEquals(List.of("Preencha \"Marca\"."), erros);
    }

    @Test
    void combustivelConfereCodigoEComposicaoEmPorcentagem() {
        FormularioCatalogo<Combustivel> formulario = FormasCatalogo.de(TipoCatalogo.COMBUSTIVEL);
        Exemplo<Combustivel> exemplo = ExemplosCatalogo.combustivel();
        ValoresForm valores = formulario.escrever(exemplo.dados(), exemplo.info());
        assertEquals("30", valores.texto(FormaCombustivel.ETANOL_VOLUME));

        assertEquals("E27", formulario.ler(valores.texto(FormaCombustivel.CODIGO, "e27")).dados().codigo());
        assertEquals(List.of("O código do combustível usa só letras, números, - e _ (até 15), sem espaço (ex.: E32)."),
                formulario.ler(valores.texto(FormaCombustivel.CODIGO, "E 27")).erros());
        valores.texto(FormaCombustivel.CODIGO, "E27");
        assertEquals(List.of("O etanol na gasolina deve ficar entre 0 e 100% (32 = E32)."),
                formulario.ler(valores.texto(FormaCombustivel.ETANOL_VOLUME, "120")).erros());
        assertEquals(List.of("Preencha \"AFR estequiométrico\"."),
                formulario.ler(valores.texto(FormaCombustivel.COMPOSICAO, "AFR_FIXO")).erros());
        assertEquals(List.of("Escolha \"Composição\"."),
                formulario.ler(valores.texto(FormaCombustivel.COMPOSICAO, "")).erros());
    }

    @Test
    void campoDaComposicaoSoApareceNaEscolhaCerta() {
        CampoForm afr = FormasCatalogo.de(TipoCatalogo.COMBUSTIVEL).campos().stream()
                .filter(c -> c.chave().equals(FormaCombustivel.AFR)).findFirst().orElseThrow();
        assertTrue(afr.mostrarSe().atende(new ValoresForm().texto(FormaCombustivel.COMPOSICAO, "AFR_FIXO")));
        assertFalse(afr.mostrarSe().atende(new ValoresForm().texto(FormaCombustivel.COMPOSICAO, "ETANOL_HIDRATADO")));
        CampoForm codigo = FormasCatalogo.de(TipoCatalogo.COMBUSTIVEL).campos().stream()
                .filter(c -> c.chave().equals(FormaCombustivel.CODIGO)).findFirst().orElseThrow();
        assertTrue(codigo.fixoNaEdicao());
    }

    @Test
    void precoEDataInvalidosViramMensagem() {
        FormularioCatalogo<Bico> formulario = FormasCatalogo.de(TipoCatalogo.BICO);
        assertEquals(List.of("\"Preço de referência (R$)\": \"12,345\" não é um valor em reais: use vírgula para os "
                + "centavos (ex.: 1.234,56)."), formulario.ler(valoresDoBico().texto(FormularioCatalogo.PRECO, "12,345"))
                        .erros());
        assertEquals(List.of("O preço de referência não pode ser negativo."),
                formulario.ler(valoresDoBico().texto(FormularioCatalogo.PRECO, "-5,00")).erros());
        assertEquals(List.of("\"Data da informação\": use a data no formato dd/mm/aaaa."),
                formulario.ler(valoresDoBico().texto(FormularioCatalogo.DATA, "31/02/2026")).erros());
        assertEquals(45_000, formulario.ler(valoresDoBico().texto(FormularioCatalogo.PRECO, "R$ 450"))
                .info().precoReferencia().centavos());
    }

    @Test
    void regrasDaPecaVemDoNucleo() {
        FormularioCatalogo<Sensor> formulario = FormasCatalogo.de(TipoCatalogo.SENSOR);
        Exemplo<Sensor> exemplo = ExemplosCatalogo.sensor();
        ValoresForm valores = formulario.escrever(exemplo.dados(), exemplo.info()).texto(FormaSensor.FAIXA_MINIMA, "")
                .texto(FormaSensor.FAIXA_MAXIMA, "0");
        assertEquals(List.of("A faixa máxima do sensor MAP deve ser maior que zero (kPa absolutos)."),
                formulario.ler(valores).erros());

        FormularioCatalogo<ModeloMotor> motores = FormasCatalogo.de(TipoCatalogo.MOTOR);
        Exemplo<ModeloMotor> motor = ExemplosCatalogo.motor();
        ValoresForm treze = motores.escrever(motor.dados(), motor.info()).texto(FormaMotor.CILINDROS, "13");
        assertEquals(List.of(ValidadorMotor.MENSAGEM_CILINDROS), motores.ler(treze).erros());
    }

    @Test
    void duplicarMarcaCopiaNoNomeELimpaOCodigo() {
        Exemplo<Bico> bico = ExemplosCatalogo.bicoComTabela();
        assertEquals("Bico teste 440 (cópia)", FormasCatalogo.de(TipoCatalogo.BICO).duplicar(bico.dados(), bico.info())
                .texto(FormularioCatalogo.NOME));
        Exemplo<Combustivel> combustivel = ExemplosCatalogo.combustivel();
        ValoresForm copia = FormasCatalogo.de(TipoCatalogo.COMBUSTIVEL).duplicar(combustivel.dados(), combustivel.info());
        assertEquals("", copia.texto(FormaCombustivel.CODIGO));
        assertEquals("Gasolina teste E30 (cópia)", copia.texto(FormularioCatalogo.NOME));
    }

    @Test
    void resumoDaLista() {
        FormularioCatalogo<Bico> bicos = FormasCatalogo.de(TipoCatalogo.BICO);
        assertEquals(List.of("440 cm³/min", "3 bar", "tabela (4 pontos)"),
                bicos.resumo(ExemplosCatalogo.bicoComTabela().dados()));
        assertEquals(List.of("630 cm³/min", "3 bar", "1,05 ms"), bicos.resumo(ExemplosCatalogo.bicoFixo().dados()));
        assertEquals(List.of("12 e 13,5 V", "3 a 5 bar", "160 a 230 L/h"),
                FormasCatalogo.de(TipoCatalogo.BOMBA).resumo(ExemplosCatalogo.bomba().dados()));
        assertEquals(List.of("Fábrica", "4", "82,5 × 92,8 mm", "1.984 cm³ (ficha 1.984)", "1,55"),
                FormasCatalogo.de(TipoCatalogo.MOTOR).resumo(ExemplosCatalogo.motor().dados()));
        assertEquals(List.of("E32", "12,8", "0,756 kg/L", "0,8 / 0,87"),
                FormasCatalogo.de(TipoCatalogo.COMBUSTIVEL).resumo(CombustiveisPadrao.E32));
    }

    @Test
    void bicoNovoComecaComOFluidoDeTestePadrao() {
        assertEquals("0,7", FormasCatalogo.de(TipoCatalogo.BICO).novo().texto(FormaBico.DENSIDADE));
    }

    /**
     * Serviço (bloco 3.3): sem preço, as horas são obrigatórias; o novo já vem com a fonte da oficina e exigindo o
     * veículo, e a categoria é obrigatória (Fase 3B).
     */
    @Test
    void servicoPrecisaDasHorasOuDoPreco() {
        FormularioCatalogo<ServicoPadrao> servicos = FormasCatalogo.de(TipoCatalogo.SERVICO);
        assertEquals("Tabela da oficina", servicos.novo().texto(FormularioCatalogo.FONTE));
        assertEquals("true", servicos.novo().texto(FormaServico.EXIGE_VEICULO), "o serviço novo exige o veículo");
        assertEquals("", servicos.novo().texto(FormaServico.CATEGORIA), "a categoria se escolhe");
        ValoresForm semNada = novoServico(servicos);
        assertEquals(List.of("Informe as horas ou o preço de referência do serviço."), servicos.ler(semNada).erros());

        Leitura<ServicoPadrao> soPreco = servicos.ler(novoServico(servicos).texto(FormularioCatalogo.PRECO, "120,00"));
        assertEquals(List.of(), soPreco.erros());
        assertNull(soPreco.dados().horas());
        assertEquals(12_000, soPreco.info().precoReferencia().centavos());

        assertEquals(List.of("As horas do serviço devem ser maiores que zero."), servicos.ler(novoServico(servicos)
                .texto(FormaServico.HORAS, "0")).erros());
        assertEquals(1, servicos.ler(novoServico(servicos).texto(FormaServico.HORAS, "1.5")).erros().size(),
                "erro de digitação: só a mensagem do campo");
        assertEquals(List.of("Escolha \"Categoria\"."), servicos.ler(servicos.novo()
                .texto(FormularioCatalogo.NOME, "Alinhamento teste").texto(FormaServico.HORAS, "1")).erros());
    }

    /** Os campos da Fase 3B (SPEC 6.6.1): categoria, descrição no orçamento, garantia própria e o veículo. */
    @Test
    void servicoComCategoriaDescricaoGarantiaEVeiculo() {
        FormularioCatalogo<ServicoPadrao> servicos = FormasCatalogo.de(TipoCatalogo.SERVICO);
        Leitura<ServicoPadrao> chicote = servicos.ler(novoServico(servicos)
                .texto(FormularioCatalogo.NOME, "Confecção de chicote teste")
                .texto(FormaServico.CATEGORIA, "INJECAO_PROGRAMAVEL")
                .texto(FormaServico.DESCRICAO, "  Chicote novo para a injeção do cliente  ")
                .texto(FormaServico.HORAS, "12")
                .texto(FormaServico.GARANTIA, "365")
                .texto(FormaServico.EXIGE_VEICULO, "false"));
        assertEquals(List.of(), chicote.erros());
        assertEquals(new ServicoPadrao("Confecção de chicote teste", 12.0, "Tabela da oficina", null,
                CategoriaServico.INJECAO_PROGRAMAVEL, "Chicote novo para a injeção do cliente", false, 365, null),
                chicote.dados());

        Leitura<ServicoPadrao> semGarantia = servicos.ler(novoServico(servicos).texto(FormaServico.HORAS, "1")
                .texto(FormaServico.GARANTIA, ""));
        assertNull(semGarantia.dados().garantiaDias(), "em branco: a garantia da OS");
        assertEquals(0, servicos.ler(novoServico(servicos).texto(FormaServico.HORAS, "1")
                .texto(FormaServico.GARANTIA, "0")).dados().garantiaDias(), "0 = sem garantia");
        assertEquals(List.of("A garantia do serviço vai de 0 (sem garantia) a 3650 dias; em branco, vale a garantia da "
                + "OS."), servicos.ler(novoServico(servicos).texto(FormaServico.HORAS, "1")
                        .texto(FormaServico.GARANTIA, "3651")).erros());
        assertEquals(List.of("\"Garantia própria (dias)\": use um número inteiro."), servicos.ler(novoServico(servicos)
                .texto(FormaServico.HORAS, "1").texto(FormaServico.GARANTIA, "90,5")).erros());

        assertEquals(List.of("Categoria", "Horas", "Garantia"), servicos.colunas());
        assertEquals(List.of("Preparação", "1,5 h", "180 dias"), servicos.resumo(ExemplosCatalogo.servicoPorHora()
                .dados()));
        assertEquals(List.of("Diagnóstico elétrico", "—", "da OS"), servicos.resumo(ExemplosCatalogo.servicoComPreco()
                .dados()));
        assertEquals(List.of("Injeção programável", "12 h", "365 dias"), servicos.resumo(chicote.dados()));
        assertEquals("sem garantia", FormaServico.garantia(0));
        assertEquals("1 dia", FormaServico.garantia(1));
    }

    /** Item de material (Fase 3B): a unidade é obrigatória; o novo já vem com "un" e com a fonte da oficina. */
    @Test
    void itemPrecisaDaUnidade() {
        FormularioCatalogo<ItemMaterial> itens = FormasCatalogo.de(TipoCatalogo.ITEM);
        assertEquals("un", itens.novo().texto(FormaItem.UNIDADE));
        assertEquals("Tabela da oficina", itens.novo().texto(FormularioCatalogo.FONTE));
        Leitura<ItemMaterial> cabo = itens.ler(itens.novo().texto(FormularioCatalogo.NOME, "Cabo 6 mm²")
                .texto(FormaItem.UNIDADE, " m ").texto(FormularioCatalogo.PRECO, "18,00"));
        assertEquals(List.of(), cabo.erros());
        assertEquals(new ItemMaterial("Cabo 6 mm²", "m", "Tabela da oficina", null), cabo.dados());
        assertEquals(1_800, cabo.info().precoReferencia().centavos());
        assertEquals(List.of("Preencha \"Unidade\"."), itens.ler(itens.novo().texto(FormularioCatalogo.NOME, "Cabo")
                .texto(FormaItem.UNIDADE, "")).erros());
        assertEquals(List.of("Unidade"), itens.colunas());
        assertEquals(List.of("un"), itens.resumo(ExemplosCatalogo.item().dados()));
    }

    /**
     * Kit (Fase 3B): as linhas apontam para o catálogo (tipo, id, nome e quantidade); o kit não tem preço próprio, e
     * as versões mostram cada linha como "2 × nome (item)".
     */
    @Test
    void kitComLinhasDoCatalogo() {
        FormularioCatalogo<Kit> kits = FormasCatalogo.de(TipoCatalogo.KIT);
        assertFalse(kits.temPreco());
        assertTrue(kits.campos().stream().noneMatch(campo -> campo.chave().equals(FormularioCatalogo.PRECO)),
                "o kit não tem o campo do preço");
        Exemplo<Kit> exemplo = ExemplosCatalogo.kit(31, 21);
        idaEVolta(exemplo);
        conferirFormulario(exemplo);
        assertEquals(List.of("1 item, 1 serviço", "sim"), kits.resumo(exemplo.dados()));
        assertEquals(List.of("2 itens", "—"), kits.resumo(new Kit("Só itens", List.of(
                new Kit.Linha(Kit.TipoLinha.ITEM, 31, "Relé", BigDecimal.ONE),
                new Kit.Linha(Kit.TipoLinha.ITEM, 32, "Fusível", BigDecimal.ONE)), "Tabela da oficina", null)));

        ValoresForm errado = kits.novo().texto(FormularioCatalogo.NOME, "Kit errado").tabela(FormaKit.LINHAS, List.of(
                FormaKit.linha(Kit.TipoLinha.ITEM, 31, "Relé", "dois"),
                FormaKit.linha(Kit.TipoLinha.SERVICO, 21, "Instalação", ""),
                List.of("", "", "", "1")));
        assertEquals(List.of("Linha 1 (Relé): quantidade: \"dois\" não é um número válido: use vírgula para decimais "
                + "(ex.: 3,5).", "Linha 2 (Instalação): informe a quantidade.",
                "Linha 3: escolha o item ou o serviço no catálogo."), kits.ler(errado).erros());
        assertEquals(List.of("O kit precisa de pelo menos uma linha (um item ou um serviço do catálogo)."),
                kits.ler(kits.novo().texto(FormularioCatalogo.NOME, "Kit vazio")).erros());

        LinhaHistorico linhas = kits.historico(List.of(new VersaoCatalogo<>(1, 1, exemplo.dados(), exemplo.info(),
                LocalDateTime.of(2026, 10, 19, 10, 0)))).stream().filter(l -> l.campo().equals("Linhas do kit"))
                .findFirst().orElseThrow();
        assertEquals(List.of("2 × Relé teste 12 V 40 A (item)\n1 × Limpeza de bicos teste (serviço)"),
                linhas.valores());
    }

    /**
     * Checklist (Fase 3B, SPEC 6.6.2): os itens na ordem, cada um com "obrigatório" e "a verificar"; o item sem texto
     * fica de fora, o repetido é recusado, e as versões mostram cada item numerado, com as marcas.
     */
    @Test
    void checklistComItensNaOrdem() {
        FormularioCatalogo<ChecklistModelo> checklists = FormasCatalogo.de(TipoCatalogo.CHECKLIST);
        assertFalse(checklists.temPreco());
        Exemplo<ChecklistModelo> exemplo = ExemplosCatalogo.checklist();
        assertEquals(List.of("Itens", "Obrigatórios"), checklists.colunas());
        assertEquals(List.of("3", "2"), checklists.resumo(exemplo.dados()));
        assertEquals("Tabela da oficina", checklists.novo().texto(FormularioCatalogo.FONTE));

        ValoresForm comVazio = checklists.novo().texto(FormularioCatalogo.NOME, "Vidro").tabela(FormaChecklist.ITENS,
                List.of(FormaChecklist.linha(" Testar o motor ", true, false), FormaChecklist.linha("  ", true, false),
                        FormaChecklist.linha("Teste final com o cliente", false, true)));
        Leitura<ChecklistModelo> lido = checklists.ler(comVazio);
        assertEquals(List.of(), lido.erros());
        assertEquals(List.of(new ChecklistModelo.Item("Testar o motor", true, false),
                new ChecklistModelo.Item("Teste final com o cliente", false, true)), lido.dados().itens(),
                "o item sem texto fica de fora, e o texto fica sem espaços nas pontas");

        assertEquals(List.of("O checklist precisa de pelo menos um item."),
                checklists.ler(checklists.novo().texto(FormularioCatalogo.NOME, "Vazio")).erros());
        assertEquals(List.of("Item 2 (testar o motor): já está no checklist."), checklists.ler(checklists.novo()
                .texto(FormularioCatalogo.NOME, "Repetido").tabela(FormaChecklist.ITENS, List.of(
                        FormaChecklist.linha("Testar o motor", true, false),
                        FormaChecklist.linha("testar o motor", true, false)))).erros());

        LinhaHistorico itens = checklists.historico(List.of(new VersaoCatalogo<>(1, 1, exemplo.dados(),
                exemplo.info(), LocalDateTime.of(2026, 10, 19, 10, 0)))).stream()
                .filter(l -> l.campo().equals("Itens")).findFirst().orElseThrow();
        assertEquals(List.of("1. Medir a tensão da bateria (obrigatório, a verificar)\n"
                + "2. Teste de ruído com o motor ligado (obrigatório)\n3. Fotos de antes e depois"), itens.valores());
    }

    /** Um serviço novo com o nome e a categoria preenchidos (o resto, cada teste preenche). */
    private static ValoresForm novoServico(FormularioCatalogo<ServicoPadrao> servicos) {
        return servicos.novo().texto(FormularioCatalogo.NOME, "Alinhamento teste")
                .texto(FormaServico.CATEGORIA, "OUTRO");
    }

    @Test
    void tabelaColadaDoExcelOuComPontoEVirgula() {
        assertEquals(List.of(List.of("12", "3", "1,10"), List.of("14", "3", "0,90")),
                TabelaColada.ler("12\t3\t1,10\r\n\r\n14; 3; 0,90\n", 3));
        assertEquals(List.of(List.of("12", "", "")), TabelaColada.ler("12", 3), "o que falta fica vazio");
        assertEquals(List.of(List.of("1", "2")), TabelaColada.ler("1\t2\t3\t4", 2), "o que sobra é ignorado");
        assertTrue(TabelaColada.ler(null, 3).isEmpty());
    }

    @Test
    void historicoMostraOQueMudouEntreVersoes() {
        Exemplo<Bico> v1 = ExemplosCatalogo.bicoComTabela();
        Bico b = v1.dados();
        Bico maior = new Bico(b.nome(), 450.0, b.pressaoTesteKpa(), b.densidadeFluidoTesteKgL(), b.tempoMorto(),
                b.impedanciaOhm(), b.fonte(), b.data());
        LocalDateTime agora = LocalDateTime.of(2026, 10, 12, 10, 0);
        List<LinhaHistorico> linhas = FormasCatalogo.de(TipoCatalogo.BICO).historico(List.of(
                new VersaoCatalogo<>(1L, 2, maior, v1.info(), agora),
                new VersaoCatalogo<>(1L, 1, b, v1.info(), agora.minusDays(1))));
        LinhaHistorico vazao = linha(linhas, "Vazão nominal (cm³/min)");
        assertEquals(List.of("450", "440"), vazao.valores());
        assertEquals(List.of(true, false), vazao.mudou());
        LinhaHistorico tempoMorto = linha(linhas, "Tempo morto");
        assertEquals("12 · 3 · 1,1\n14 · 3 · 0,9\n12 · 4 · 1,2\n14 · 4 · 1", tempoMorto.valores().getFirst());
        assertEquals(List.of(false, false), tempoMorto.mudou());
        assertEquals(List.of("não", "não"), linha(linhas, "A verificar (dado ainda não confirmado)").valores());

        Exemplo<Combustivel> gasolina = ExemplosCatalogo.combustivel();
        List<String> campos = FormasCatalogo.de(TipoCatalogo.COMBUSTIVEL).historico(List.of(
                new VersaoCatalogo<>(2L, 1, gasolina.dados(), gasolina.info(), agora))).stream()
                .map(LinhaHistorico::campo).toList();
        assertTrue(campos.contains("Etanol na gasolina (% em volume)"));
        assertFalse(campos.contains("AFR estequiométrico"), "campo que não se aplica fica de fora");
        assertFalse(campos.contains("Aviso sobre a lambda"), "campo vazio em todas as versões fica de fora");
    }

    @Test
    void turboComecaSemMapaEOHistoricoMostraOMapaNovo() {
        FormularioCatalogo<Turbo> turbos = FormasCatalogo.de(TipoCatalogo.TURBO);
        ValoresForm novo = turbos.novo();
        novo.texto(FormularioCatalogo.NOME, "Turbo sem mapa").texto(FormularioCatalogo.FONTE, "Ficha de teste");
        Leitura<Turbo> lido = turbos.ler(novo);
        assertEquals(List.of(), lido.erros());
        assertNull(lido.dados().mapa(), "o mapa se desenha depois, no digitalizador");
        assertEquals(List.of("—", "sem mapa", "—", "—"), turbos.resumo(lido.dados()));

        Exemplo<Turbo> comMapa = ExemplosCatalogo.turbo();
        assertEquals(List.of("0,63", "digitalizado", "74%", "150.000 rpm"), turbos.resumo(comMapa.dados()));
        Turbo antes = comMapa.dados().comMapa(null, null, null);
        LocalDateTime agora = LocalDateTime.of(2026, 10, 13, 10, 0);
        List<LinhaHistorico> linhas = turbos.historico(List.of(
                new VersaoCatalogo<>(3L, 2, comMapa.dados(), comMapa.info(), agora),
                new VersaoCatalogo<>(3L, 1, antes, comMapa.info(), agora.minusDays(1))));
        LinhaHistorico mapa = linha(linhas, "Mapa do compressor");
        assertEquals(List.of("3 pontos de surge, 3 de choke, 1 ilhas, 1 linhas de rotação; máxima 150.000 rpm", "—"),
                mapa.valores());
        assertEquals(List.of(true, false), mapa.mudou());
        assertEquals(List.of("calibrada", "—"), linha(linhas, "Calibração da imagem").valores());
    }

    private static LinhaHistorico linha(List<LinhaHistorico> linhas, String campo) {
        return linhas.stream().filter(l -> l.campo().equals(campo)).findFirst().orElseThrow(() -> new AssertionError(
                "sem a linha " + campo));
    }

    private static ValoresForm valoresDoBico() {
        Exemplo<Bico> bico = ExemplosCatalogo.bicoComTabela();
        return FormasCatalogo.de(TipoCatalogo.BICO).escrever(bico.dados(), bico.info());
    }
}
