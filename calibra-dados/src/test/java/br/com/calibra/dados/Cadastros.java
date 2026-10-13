package br.com.calibra.dados;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Consentimento;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.cadastro.Tracao;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.dados.cadastro.ClienteRepositorio;
import br.com.calibra.dados.cadastro.VeiculoRepositorio;
import br.com.calibra.dados.projeto.MotorRepositorio;
import br.com.calibra.dados.projeto.ProjetoRepositorio;

/** Cadastros fictícios para os testes do banco (nenhum é de pessoa ou veículo real). */
public final class Cadastros {

    public static final LocalDate HOJE = LocalDate.of(2026, 10, 11);

    private Cadastros() {
    }

    public static Cliente cliente(String nome) {
        return Cliente.novo(nome, "(11) 98765-4321", "cliente@exemplo.com.br", "529.982.247-25",
                new Consentimento(HOJE, Consentimento.FINALIDADE_PADRAO), "Prefere contato à tarde");
    }

    public static Veiculo carro(String placa) {
        return new Veiculo(TipoVeiculo.CARRO, "VW", "Gol", "GTi", 1994, 1994, placa, "9BWZZZ377VT004251", "", "", "",
                250_000.0, TipoCambio.MANUAL, false, Tracao.DIANTEIRA, 960.0, "Branco", "");
    }

    public static Veiculo lancha(String nome) {
        return new Veiculo(TipoVeiculo.LANCHA, "Focker", "240", "", 2019, 2020, "", "", nome, "123456789", "CASCO-1",
                350.0, TipoCambio.NAUTICO_RABETA_OU_REVERSOR, false, null, null, "", "");
    }

    /** Repositórios com o relógio da base de teste. */
    public record Repositorios(ClienteRepositorio clientes, VeiculoRepositorio veiculos, ProjetoRepositorio projetos,
            MotorRepositorio motores) {

        public static Repositorios de(BaseDeTeste base) {
            return new Repositorios(new ClienteRepositorio(base.relogio), new VeiculoRepositorio(base.relogio),
                    new ProjetoRepositorio(base.relogio), new MotorRepositorio(base.relogio));
        }

        /** Cliente, veículo, projeto e motor v1 do caso B, numa conexão. Devolve o id do projeto. */
        public long projetoCasoB(Connection conexao, String nomeCliente, String placa) throws SQLException {
            long cliente = clientes.inserir(conexao, cliente(nomeCliente));
            long veiculo = veiculos.inserir(conexao, cliente, carro(placa), null);
            long projeto = projetos.inserir(conexao, veiculo, CasoBDados.entrada().meta(), "", null);
            motores.salvarVersao(conexao, projeto, "AP 2.0 turbo", CasoBDados.entrada().motor());
            return projeto;
        }
    }
}
