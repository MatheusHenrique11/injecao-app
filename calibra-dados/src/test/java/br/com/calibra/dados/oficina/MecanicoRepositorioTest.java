package br.com.calibra.dados.oficina;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.dados.BaseDeTeste;

/** Mecânicos: só o nome, em ordem alfabética; quem sai fica inativo e some da lista de escolha. */
class MecanicoRepositorioTest {

    @TempDir
    Path pasta;

    @Test
    void cadastroInativoERenomear() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        MecanicoRepositorio mecanicos = new MecanicoRepositorio(base.relogio);
        long carlos = base.banco.transacao(c -> mecanicos.inserir(c, " Carlos "));
        long ana = base.banco.transacao(c -> mecanicos.inserir(c, "ana"));
        assertEquals(List.of(new MecanicoSalvo(ana, "ana", true), new MecanicoSalvo(carlos, "Carlos", true)),
                base.banco.ler(c -> mecanicos.listar(c, false)), "em ordem de nome, sem ligar para maiúsculas");

        base.banco.transacao(c -> {
            mecanicos.ativar(c, carlos, false);
            mecanicos.renomear(c, ana, "Ana Paula");
            return null;
        });
        assertEquals(List.of(new MecanicoSalvo(ana, "Ana Paula", true)), base.banco.ler(c -> mecanicos.listar(c, false)));
        assertEquals(2, base.banco.ler(c -> mecanicos.listar(c, true)).size(), "o inativo continua cadastrado");
        assertEquals(new MecanicoSalvo(carlos, "Carlos", false), base.banco.ler(c -> mecanicos.buscar(c, carlos))
                .orElseThrow());
    }
}
