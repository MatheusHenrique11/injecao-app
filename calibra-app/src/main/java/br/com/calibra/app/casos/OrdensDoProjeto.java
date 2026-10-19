package br.com.calibra.app.casos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.cadastro.ClienteRepositorio;
import br.com.calibra.dados.cadastro.VeiculoRepositorio;
import br.com.calibra.dados.oficina.DadosAbertura;
import br.com.calibra.dados.oficina.OrdemServicoRepositorio;
import br.com.calibra.dados.oficina.OrdemServicoSalva;
import br.com.calibra.dados.projeto.ProjetoRepositorio;
import br.com.calibra.dados.projeto.ProjetoSalvo;

/**
 * A OS atual de um projeto de preparação: a mais nova que não foi entregue nem cancelada. O orçamento feito pela aba
 * do projeto vai para ela; sem nenhuma em andamento, uma OS nova é aberta para o projeto (regra 7 do bloco 3.1).
 */
final class OrdensDoProjeto {

    /** O serviço pedido da OS aberta para um projeto (o mesmo das OS criadas na atualização para a 0.3.0). */
    static final String SERVICO_DE_PREPARACAO = "Preparação";

    private final Clock relogio;
    private final OrdemServicoRepositorio ordens;
    private final ProjetoRepositorio projetos;
    private final VeiculoRepositorio veiculos;
    private final ClienteRepositorio clientes;

    OrdensDoProjeto(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
        this.ordens = new OrdemServicoRepositorio(relogio);
        this.projetos = new ProjetoRepositorio(relogio);
        this.veiculos = new VeiculoRepositorio(relogio);
        this.clientes = new ClienteRepositorio(relogio);
    }

    /**
     * O id da OS atual do projeto; sem nenhuma em andamento, abre uma (ABERTA, sem a entrada do veículo).
     *
     * @throws OperacaoRecusadaException se precisar abrir uma OS e o cliente tiver sido anonimizado (LGPD)
     */
    long atual(Connection conexao, long projetoId) throws SQLException {
        Optional<OrdemServicoSalva> atual = ordens.atualDoProjeto(conexao, projetoId);
        if (atual.isPresent()) {
            return atual.get().id();
        }
        ProjetoSalvo projeto = projetos.buscar(conexao, projetoId).orElseThrow(
                () -> new OperacaoRecusadaException("Projeto #" + projetoId + " não encontrado."));
        long clienteId = veiculos.buscar(conexao, projeto.veiculoId()).orElseThrow().clienteId();
        if (clientes.buscar(conexao, clienteId).orElseThrow().cliente().anonimizado()) {
            throw new OperacaoRecusadaException("O cliente deste projeto foi anonimizado (LGPD) e não recebe OS nova. "
                    + "O projeto e os cálculos ficam para consulta.");
        }
        int ano = LocalDate.now(relogio).getYear();
        return ordens.inserir(conexao, new NumeroOs(ano, ordens.proximoSequencial(conexao, ano)),
                new DadosAbertura(clienteId, projeto.veiculoId(), projetoId, SERVICO_DE_PREPARACAO, null, null, ""));
    }
}
