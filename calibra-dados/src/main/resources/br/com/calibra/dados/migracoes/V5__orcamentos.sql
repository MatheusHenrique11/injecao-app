-- V5 (fase 2, bloco 2.5): orçamentos com revisões e o histórico de status.
-- Cada linha é uma revisão (ORC-AAAA-NNNN rev. N). O conteúdo (itens, desconto, prazo, validade, condições) fica
-- em JSON; o total, em coluna para listar. A revisão aponta para o cálculo e para a versão do motor que usou.

CREATE TABLE orcamento (
    id                  INTEGER PRIMARY KEY,
    projeto_id          INTEGER NOT NULL REFERENCES projeto (id),
    calculo_id          INTEGER NOT NULL REFERENCES calculo (id),
    motor_versao_id     INTEGER NOT NULL REFERENCES motor_versao (id),
    ano                 INTEGER NOT NULL,
    sequencial          INTEGER NOT NULL,
    revisao             INTEGER NOT NULL,
    status              TEXT    NOT NULL,
    conteudo_json       TEXT    NOT NULL,
    total_centavos      INTEGER NOT NULL,
    enviado_em          TEXT,
    respondido_em       TEXT,
    meio_resposta       TEXT,
    observacao_resposta TEXT    NOT NULL DEFAULT '',
    criado_em           TEXT    NOT NULL,
    atualizado_em       TEXT    NOT NULL,
    UNIQUE (ano, sequencial, revisao)
) STRICT;

CREATE INDEX orcamento_por_projeto ON orcamento (projeto_id, id);

-- Só o rascunho muda de conteúdo ou de cálculo: depois de enviado (e principalmente de aprovado), mudar exige uma
-- revisão nova.
CREATE TRIGGER orcamento_so_rascunho_muda BEFORE UPDATE OF conteudo_json, total_centavos, calculo_id, motor_versao_id
    ON orcamento WHEN OLD.status <> 'RASCUNHO'
BEGIN
    SELECT RAISE(ABORT, 'Orçamento enviado não muda: abra uma revisão nova.');
END;

CREATE TRIGGER orcamento_nao_apaga BEFORE DELETE ON orcamento
BEGIN
    SELECT RAISE(ABORT, 'Orçamento não é apagado.');
END;

-- Histórico do status de cada revisão, com data e hora.
CREATE TABLE orcamento_status (
    id           INTEGER PRIMARY KEY,
    orcamento_id INTEGER NOT NULL REFERENCES orcamento (id),
    status       TEXT    NOT NULL,
    data_hora    TEXT    NOT NULL
) STRICT;

CREATE INDEX orcamento_status_por_orcamento ON orcamento_status (orcamento_id, id);
