-- V10 (Fase 3B, bloco 3B.4): os laudos de diagnóstico elétrico da OS (SPEC 6.6.3), vários por OS. O conteúdo (a data
-- e a hora, a queixa, as medições, os códigos de falha, os textos e o resultado) fica em JSON, como o do orçamento; o
-- mecânico fica numa coluna, para o nome vir do cadastro.
-- O laudo é dado técnico e fica quando o cliente é anonimizado. Com a OS entregue ou cancelada, o banco recusa criar,
-- mudar e apagar laudo.

CREATE TABLE laudo (
    id            INTEGER PRIMARY KEY,
    os_id         INTEGER NOT NULL REFERENCES ordem_servico (id),
    mecanico_id   INTEGER REFERENCES mecanico (id),
    conteudo_json TEXT    NOT NULL,
    criado_em     TEXT    NOT NULL,
    atualizado_em TEXT    NOT NULL
) STRICT;

CREATE INDEX laudo_por_os ON laudo (os_id, id);

CREATE TRIGGER laudo_so_em_andamento BEFORE INSERT ON laudo
    WHEN (SELECT status FROM ordem_servico WHERE id = NEW.os_id) IN ('ENTREGUE', 'CANCELADA')
BEGIN
    SELECT RAISE(ABORT, 'OS entregue ou cancelada não ganha laudo.');
END;

CREATE TRIGGER laudo_nao_muda_de_os BEFORE UPDATE ON laudo
    WHEN NEW.os_id IS NOT OLD.os_id
BEGIN
    SELECT RAISE(ABORT, 'O laudo não muda de OS.');
END;

CREATE TRIGGER laudo_os_finalizada_nao_muda BEFORE UPDATE ON laudo
    WHEN (SELECT status FROM ordem_servico WHERE id = OLD.os_id) IN ('ENTREGUE', 'CANCELADA')
BEGIN
    SELECT RAISE(ABORT, 'OS entregue ou cancelada: o laudo não muda.');
END;

CREATE TRIGGER laudo_os_finalizada_nao_apaga BEFORE DELETE ON laudo
    WHEN (SELECT status FROM ordem_servico WHERE id = OLD.os_id) IN ('ENTREGUE', 'CANCELADA')
BEGIN
    SELECT RAISE(ABORT, 'OS entregue ou cancelada: o laudo não é apagado.');
END;
