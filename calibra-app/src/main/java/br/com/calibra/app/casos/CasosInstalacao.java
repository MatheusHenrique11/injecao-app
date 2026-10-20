package br.com.calibra.app.casos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.Instalacao;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.cadastro.EventoAuditoria;
import br.com.calibra.dados.cadastro.InstalacaoRepositorio;
import br.com.calibra.dados.cadastro.InstalacaoSalva;
import br.com.calibra.dados.cadastro.VeiculoRepositorio;
import br.com.calibra.dados.oficina.OrdemServicoRepositorio;
import br.com.calibra.dados.oficina.OrdemServicoSalva;
import br.com.calibra.dados.oficina.ResumoOs;

/**
 * As instalações do veículo (Fase 3B, SPEC 6.2): o que foi instalado ou mexido no carro, pela oficina ou informado pelo
 * cliente, com a OS de origem (opcional), a retirada e o histórico de cada mudança. A instalação é do veículo: pela
 * ficha, ela se muda sempre; pela OS, só se registra com a OS em andamento (depois, a tela da OS só mostra). Não é dado
 * pessoal: fica igual quando o cliente é anonimizado (teste nº 7 da fase). Sem JavaFX.
 */
public final class CasosInstalacao {

    /**
     * A instalação nova, já preenchida.
     *
     * @param osId a OS de origem; null pela ficha
     */
    public record Rascunho(long veiculoId, Long osId, Instalacao instalacao) {

        public Rascunho {
            Objects.requireNonNull(instalacao, "instalacao");
        }
    }

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BancoDados banco;
    private final Clock relogio;
    private final InstalacaoRepositorio instalacoes;
    private final OrdemServicoRepositorio ordens;
    private final VeiculoRepositorio veiculos;

    public CasosInstalacao(BancoDados banco, Clock relogio) {
        this.banco = Objects.requireNonNull(banco, "banco");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
        this.instalacoes = new InstalacaoRepositorio(relogio);
        this.ordens = new OrdemServicoRepositorio(relogio);
        this.veiculos = new VeiculoRepositorio(relogio);
    }

    // ===================== Consultas =====================

    /** As instalações do veículo, também as retiradas (as que estão nele primeiro). */
    public List<InstalacaoSalva> doVeiculo(long veiculoId) {
        return banco.ler(c -> instalacoes.doVeiculo(c, veiculoId));
    }

    /** As instalações ligadas à OS. */
    public List<InstalacaoSalva> daOs(long osId) {
        return banco.ler(c -> instalacoes.daOs(c, osId));
    }

    public Optional<InstalacaoSalva> buscar(long id) {
        return banco.ler(c -> instalacoes.buscar(c, id));
    }

    /** O histórico da instalação, do mais novo para o mais antigo. */
    public List<EventoAuditoria> historico(long id) {
        return banco.ler(c -> instalacoes.historico(c, id));
    }

    /** As OS do veículo, da mais nova para a mais antiga: a escolha da OS de origem. */
    public List<ResumoOs> osDoVeiculo(long veiculoId) {
        return banco.ler(c -> ordens.resumosDoVeiculo(c, veiculoId));
    }

    /** A instalação nova pela ficha do veículo: vazia e sem OS. */
    public Rascunho nova(long veiculoId) {
        banco.ler(c -> veiculos.buscar(c, veiculoId).orElseThrow(
                () -> new OperacaoRecusadaException("Veículo #" + veiculoId + " não encontrado.")));
        return new Rascunho(veiculoId, null, vazia(null));
    }

    /**
     * A instalação nova pela OS: com a OS de origem e a data de hoje. Com a OS entregue ou cancelada, a tela da OS só
     * mostra; a esquecida se registra pela ficha do veículo.
     */
    public Rascunho novaDaOs(long osId) {
        OrdemServicoSalva os = banco.ler(c -> exigirOs(c, osId));
        if (os.status().finalizada()) {
            throw new OperacaoRecusadaException("A OS " + os.numero().texto() + " está \"" + os.status().rotulo()
                    + "\": registre a instalação pela ficha do veículo, escolhendo esta OS como a de origem.");
        }
        return new Rascunho(os.veiculoId(), osId, vazia(hoje()));
    }

    // ===================== Gravação =====================

    /**
     * Instalação nova no veículo.
     *
     * @param osId a OS de origem, do mesmo veículo; null = sem OS
     * @return o id da instalação
     * @throws EntradaInvalidaException com tudo o que falta ou está errado (nada é gravado)
     */
    public long criar(long veiculoId, Long osId, Instalacao instalacao) {
        exigirValida(instalacao, null);
        return banco.transacao(c -> {
            veiculos.buscar(c, veiculoId).orElseThrow(
                    () -> new OperacaoRecusadaException("Veículo #" + veiculoId + " não encontrado."));
            exigirOsDoVeiculo(c, osId, veiculoId);
            return instalacoes.inserir(c, veiculoId, osId, instalacao);
        });
    }

    /** Grava a instalação como está agora; cada campo mudado vai para o histórico dela. */
    public void salvar(long id, Long osId, Instalacao instalacao) {
        banco.transacao(c -> {
            InstalacaoSalva atual = exigir(c, id);
            exigirValida(instalacao, atual.retiradaEm());
            exigirOsDoVeiculo(c, osId, atual.veiculoId());
            instalacoes.salvar(c, id, osId, instalacao);
            return null;
        });
    }

    /** O item saiu do veículo nesta data: ele continua na lista, marcado, e a retirada vai para o histórico. */
    public void retirar(long id, LocalDate data) {
        banco.transacao(c -> {
            InstalacaoSalva atual = exigir(c, id);
            if (atual.retirada()) {
                throw new OperacaoRecusadaException("Esta instalação já está retirada (" + atual.retiradaEm()
                        .format(DIA) + "). Para mudar a data, desfaça a retirada e retire de novo.");
            }
            List<String> erros = atual.instalacao().validarRetirada(data, hoje());
            if (!erros.isEmpty()) {
                throw new EntradaInvalidaException(erros);
            }
            instalacoes.retirar(c, id, data);
            return null;
        });
    }

    /** O item volta a estar no veículo (a retirada foi marcada por engano); o desfazer também vai para o histórico. */
    public void desfazerRetirada(long id) {
        banco.transacao(c -> {
            exigir(c, id);
            instalacoes.retirar(c, id, null);
            return null;
        });
    }

    /** Apaga a instalação cadastrada por engano, com o histórico dela. O item que saiu do carro se retira. */
    public void apagar(long id) {
        banco.transacao(c -> {
            instalacoes.apagar(c, id);
            return null;
        });
    }

    // ===================== Regras =====================

    private void exigirValida(Instalacao instalacao, LocalDate retiradaEm) {
        List<String> erros = instalacao == null ? List.of("Preencha a instalação.")
                : instalacao.validar(hoje(), retiradaEm);
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
    }

    /** A OS de origem precisa ser do mesmo veículo (o banco também confere). */
    private void exigirOsDoVeiculo(Connection c, Long osId, long veiculoId) throws SQLException {
        if (osId == null) {
            return;
        }
        OrdemServicoSalva os = exigirOs(c, osId);
        if (os.veiculoId() != veiculoId) {
            throw new OperacaoRecusadaException("A OS " + os.numero().texto() + " é de outro veículo: a OS de origem "
                    + "precisa ser do mesmo veículo.");
        }
    }

    private OrdemServicoSalva exigirOs(Connection c, long osId) throws SQLException {
        return ordens.buscar(c, osId).orElseThrow(
                () -> new OperacaoRecusadaException("OS #" + osId + " não encontrada."));
    }

    private InstalacaoSalva exigir(Connection c, long id) throws SQLException {
        return instalacoes.buscar(c, id).orElseThrow(
                () -> new OperacaoRecusadaException("Instalação #" + id + " não encontrada."));
    }

    private static Instalacao vazia(LocalDate data) {
        return new Instalacao(null, "", "", "", "", data, "");
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }
}
