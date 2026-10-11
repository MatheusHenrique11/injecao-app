-- V1 (fase 2): configurações do app e log de auditoria.
-- Datas em texto ISO (2026-10-11T18:30:00), que ordena certo. Tabelas STRICT: o SQLite confere os tipos.

-- Cada configuração é um JSON (dados da oficina, padrões de cálculo e limiares, backup...).
CREATE TABLE configuracao (
    chave         TEXT PRIMARY KEY,
    valor_json    TEXT NOT NULL,
    atualizado_em TEXT NOT NULL
) STRICT;

-- SPEC 6.2: todo salvamento grava campo, valor antigo, valor novo, data e hora.
CREATE TABLE auditoria (
    id           INTEGER PRIMARY KEY,
    entidade     TEXT    NOT NULL,
    entidade_id  INTEGER NOT NULL,
    campo        TEXT    NOT NULL,
    valor_antigo TEXT,
    valor_novo   TEXT,
    data_hora    TEXT    NOT NULL
) STRICT;

CREATE INDEX auditoria_por_entidade ON auditoria (entidade, entidade_id, data_hora);
