package br.com.calibra.dados.oficina;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import br.com.calibra.core.oficina.ChecklistModelo;
import br.com.calibra.core.oficina.ItemDoChecklist;
import br.com.calibra.core.oficina.Marcacao;
import br.com.calibra.core.oficina.SituacaoItem;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.Sql;

/**
 * Os checklists da OS (tabelas {@code checklist_os} e {@code checklist_os_item}, V8, Fase 3B): a cópia do modelo de
 * cada serviço, feita ao entrar em execução, e a marcação de cada item. O banco recusa mudar o texto da cópia,
 * apagá-la e mexer no checklist da OS entregue ou cancelada.
 */
public final class ChecklistRepositorio {

    private final Clock relogio;

    public ChecklistRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /**
     * Copia o modelo para um serviço da OS: os itens na ordem do modelo, todos pendentes. Cada serviço tem uma cópia
     * só na OS (o banco recusa a segunda).
     *
     * @param servico o nome do serviço no catálogo, como fica na cópia
     * @param modeloVersao a versão do modelo copiada
     * @return o id da cópia
     */
    public long copiar(Connection conexao, long osId, long servicoItemId, String servico, long modeloItemId,
            int modeloVersao, ChecklistModelo modelo) throws SQLException {
        try (PreparedStatement insercao = conexao.prepareStatement("INSERT INTO checklist_os (os_id, servico_item_id, "
                + "servico, modelo_item_id, modelo_versao, modelo_nome, criado_em) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            insercao.setLong(1, osId);
            insercao.setLong(2, servicoItemId);
            insercao.setString(3, servico);
            insercao.setLong(4, modeloItemId);
            insercao.setInt(5, modeloVersao);
            insercao.setString(6, modelo.nome());
            insercao.setString(7, Sql.texto(Sql.agora(relogio)));
            insercao.executeUpdate();
        }
        long id = Sql.ultimoId(conexao);
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO checklist_os_item (checklist_id, ordem, texto, obrigatorio, a_verificar) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            int ordem = 1;
            for (ChecklistModelo.Item item : modelo.itens()) {
                insercao.setLong(1, id);
                insercao.setInt(2, ordem++);
                insercao.setString(3, item.texto());
                insercao.setInt(4, item.obrigatorio() ? 1 : 0);
                insercao.setInt(5, item.aVerificar() ? 1 : 0);
                insercao.executeUpdate();
            }
        }
        return id;
    }

    /** As cópias da OS, na ordem em que foram feitas, cada uma com os itens na ordem do modelo. */
    public List<ChecklistSalvo> daOs(Connection conexao, long osId) throws SQLException {
        Map<Long, List<ItemChecklistSalvo>> itens = new HashMap<>();
        try (PreparedStatement consulta = conexao.prepareStatement("SELECT i.id, i.checklist_id, i.ordem, i.texto, "
                + "i.obrigatorio, i.a_verificar, i.situacao, i.observacao, i.justificativa, i.mecanico_id, "
                + "m.nome AS marcado_por, i.marcado_em FROM checklist_os_item i "
                + "JOIN checklist_os c ON c.id = i.checklist_id LEFT JOIN mecanico m ON m.id = i.mecanico_id "
                + "WHERE c.os_id = ? ORDER BY i.checklist_id, i.ordem")) {
            consulta.setLong(1, osId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    itens.computeIfAbsent(linha.getLong("checklist_id"), chave -> new ArrayList<>()).add(item(linha));
                }
            }
        }
        List<ChecklistSalvo> checklists = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement("SELECT id, os_id, servico_item_id, servico, "
                + "modelo_item_id, modelo_versao, modelo_nome, criado_em FROM checklist_os WHERE os_id = ? "
                + "ORDER BY id")) {
            consulta.setLong(1, osId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    long id = linha.getLong("id");
                    checklists.add(new ChecklistSalvo(id, linha.getLong("os_id"), linha.getLong("servico_item_id"),
                            linha.getString("servico"), linha.getLong("modelo_item_id"), linha.getInt("modelo_versao"),
                            linha.getString("modelo_nome"), Sql.dataHora(linha, "criado_em"),
                            itens.getOrDefault(id, List.of())));
                }
            }
        }
        return checklists;
    }

    /**
     * Grava a marcação do item, com quem marcou e agora. O "não se aplica" sem justificativa o banco também recusa.
     *
     * @param mecanicoId quem marcou; null se a oficina não tem mecânico cadastrado
     */
    public void marcar(Connection conexao, long itemId, Marcacao marcacao, Long mecanicoId) throws SQLException {
        Objects.requireNonNull(marcacao.situacao(), "situacao");
        try (PreparedStatement atualizacao = conexao.prepareStatement("UPDATE checklist_os_item SET situacao = ?, "
                + "observacao = ?, justificativa = ?, mecanico_id = ?, marcado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, marcacao.situacao().name());
            atualizacao.setString(2, marcacao.observacao());
            atualizacao.setString(3, marcacao.justificativa());
            Sql.longo(atualizacao, 4, mecanicoId);
            atualizacao.setString(5, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(6, itemId);
            if (atualizacao.executeUpdate() == 0) {
                throw new OperacaoRecusadaException("Item de checklist #" + itemId + " não encontrado.");
            }
        }
    }

    private static ItemChecklistSalvo item(ResultSet linha) throws SQLException {
        Marcacao marcacao = new Marcacao(Sql.constante(linha, "situacao", SituacaoItem.class),
                linha.getString("observacao"), linha.getString("justificativa"));
        ItemDoChecklist item = new ItemDoChecklist(linha.getString("texto"), linha.getInt("obrigatorio") == 1,
                linha.getInt("a_verificar") == 1, marcacao);
        return new ItemChecklistSalvo(linha.getLong("id"), linha.getInt("ordem"), item, Sql.longo(linha, "mecanico_id"),
                linha.getString("marcado_por"), Sql.dataHora(linha, "marcado_em"));
    }
}
