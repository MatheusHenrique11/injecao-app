-- V14 (versão 0.3.2, decisão 279): a anonimização do cliente (LGPD) troca o "quem retirou" das OS dele por
-- "(apagado na anonimização)", também na OS entregue. O gatilho da V6 recusava qualquer mudança na OS entregue ou
-- cancelada. Ele volta igual, com uma exceção só: trocar o "quem retirou" por esse texto, com todas as outras colunas
-- iguais (conferidas uma a uma). Nenhum dado muda nesta migração.

DROP TRIGGER ordem_servico_finalizada_nao_muda;

CREATE TRIGGER ordem_servico_finalizada_nao_muda BEFORE UPDATE ON ordem_servico
    WHEN OLD.status IN ('ENTREGUE', 'CANCELADA')
     AND NOT (NEW.retirado_por = '(apagado na anonimização)'
              AND NEW.id IS OLD.id AND NEW.ano IS OLD.ano AND NEW.sequencial IS OLD.sequencial
              AND NEW.cliente_id IS OLD.cliente_id AND NEW.veiculo_id IS OLD.veiculo_id
              AND NEW.projeto_id IS OLD.projeto_id AND NEW.status IS OLD.status
              AND NEW.servico_pedido IS OLD.servico_pedido AND NEW.mecanico_id IS OLD.mecanico_id
              AND NEW.previsao_entrega IS OLD.previsao_entrega AND NEW.entrada_em IS OLD.entrada_em
              AND NEW.uso_entrada IS OLD.uso_entrada AND NEW.combustivel_entrada IS OLD.combustivel_entrada
              AND NEW.vistoria_json IS OLD.vistoria_json AND NEW.pronta_em IS OLD.pronta_em
              AND NEW.entregue_em IS OLD.entregue_em AND NEW.uso_saida IS OLD.uso_saida
              AND NEW.garantia_dias IS OLD.garantia_dias AND NEW.garantia_ate IS OLD.garantia_ate
              AND NEW.motivo_cancelamento IS OLD.motivo_cancelamento AND NEW.observacoes IS OLD.observacoes
              AND NEW.criado_em IS OLD.criado_em AND NEW.atualizado_em IS OLD.atualizado_em)
BEGIN
    SELECT RAISE(ABORT, 'OS entregue ou cancelada não muda.');
END;
