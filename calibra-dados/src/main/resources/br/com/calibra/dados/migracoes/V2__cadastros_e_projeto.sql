-- V2 (fase 2, bloco 2.2): clientes, veículos, projetos, versões do motor e cálculos salvos.
-- Datas em texto ISO. A coluna "busca" guarda o texto sem acento, sem pontuação e em minúsculas, montado
-- pelo app (TextoBusca), para "joao" achar "João".

-- SPEC 6.1. Anonimizar (LGPD) troca os dados pessoais por "Cliente anonimizado #id".
CREATE TABLE cliente (
    id                       INTEGER PRIMARY KEY,
    nome                     TEXT    NOT NULL,
    telefone                 TEXT    NOT NULL,
    email                    TEXT    NOT NULL,
    documento                TEXT    NOT NULL,
    consentimento_data       TEXT,
    consentimento_finalidade TEXT    NOT NULL,
    observacoes              TEXT    NOT NULL,
    anonimizado              INTEGER NOT NULL DEFAULT 0 CHECK (anonimizado IN (0, 1)),
    busca                    TEXT    NOT NULL,
    criado_em                TEXT    NOT NULL,
    atualizado_em            TEXT    NOT NULL
) STRICT;

-- SPEC 6.2. Veículo com projeto não é apagado (a chave estrangeira do projeto impede), só arquivado.
CREATE TABLE veiculo (
    id                INTEGER PRIMARY KEY,
    cliente_id        INTEGER NOT NULL REFERENCES cliente (id),
    tipo              TEXT    NOT NULL,
    marca             TEXT    NOT NULL,
    modelo            TEXT    NOT NULL,
    versao            TEXT    NOT NULL,
    ano_fabricacao    INTEGER,
    ano_modelo        INTEGER,
    placa             TEXT    NOT NULL,
    chassi            TEXT    NOT NULL,
    nome_embarcacao   TEXT    NOT NULL,
    numero_inscricao  TEXT    NOT NULL,
    numero_casco      TEXT    NOT NULL,
    uso_acumulado     REAL,
    cambio            TEXT,
    cambio_controlado INTEGER NOT NULL CHECK (cambio_controlado IN (0, 1)),
    tracao            TEXT,
    peso_kg           REAL,
    cor               TEXT    NOT NULL,
    observacoes       TEXT    NOT NULL,
    arquivado         INTEGER NOT NULL DEFAULT 0 CHECK (arquivado IN (0, 1)),
    busca             TEXT    NOT NULL,
    criado_em         TEXT    NOT NULL,
    atualizado_em     TEXT    NOT NULL
) STRICT;

CREATE INDEX veiculo_por_cliente ON veiculo (cliente_id);

-- SPEC 6.4. A meta (potência, rotações e uso) fica em JSON; o status, em coluna para filtrar a lista.
CREATE TABLE projeto (
    id                       INTEGER PRIMARY KEY,
    veiculo_id               INTEGER NOT NULL REFERENCES veiculo (id),
    status                   TEXT    NOT NULL,
    status_antes_de_arquivar TEXT,
    meta_json                TEXT    NOT NULL,
    observacoes              TEXT    NOT NULL,
    criado_em                TEXT    NOT NULL,
    atualizado_em            TEXT    NOT NULL
) STRICT;

CREATE INDEX projeto_por_veiculo ON projeto (veiculo_id);

-- Histórico do status do projeto, com data e hora.
CREATE TABLE projeto_status (
    id         INTEGER PRIMARY KEY,
    projeto_id INTEGER NOT NULL REFERENCES projeto (id),
    status     TEXT    NOT NULL,
    data_hora  TEXT    NOT NULL
) STRICT;

CREATE INDEX projeto_status_por_projeto ON projeto_status (projeto_id, id);

-- SPEC 6.3: cada gravação do motor é uma versão nova (v1, v2...). Versão não muda nem é apagada.
CREATE TABLE motor_versao (
    id                INTEGER PRIMARY KEY,
    projeto_id        INTEGER NOT NULL REFERENCES projeto (id),
    numero            INTEGER NOT NULL,
    descricao         TEXT    NOT NULL,
    configuracao_json TEXT    NOT NULL,
    cilindros         INTEGER NOT NULL,
    cilindrada_cc     REAL,
    aspiracao         TEXT,
    combustivel       TEXT,
    busca             TEXT    NOT NULL,
    criada_em         TEXT    NOT NULL,
    UNIQUE (projeto_id, numero)
) STRICT;

CREATE TRIGGER motor_versao_nao_muda BEFORE UPDATE ON motor_versao
BEGIN
    SELECT RAISE(ABORT, 'Versão do motor não muda: salve uma versão nova.');
END;

CREATE TRIGGER motor_versao_nao_apaga BEFORE DELETE ON motor_versao
BEGIN
    SELECT RAISE(ABORT, 'Versão do motor não é apagada.');
END;

-- Regra 2.5: o cálculo salvo guarda a entrada, o resultado (com a memória e os avisos), a versão do núcleo
-- e a versão do motor usada. Nunca é recalculado nem alterado: recalcular cria outro.
CREATE TABLE calculo (
    id              INTEGER PRIMARY KEY,
    projeto_id      INTEGER NOT NULL REFERENCES projeto (id),
    motor_versao_id INTEGER NOT NULL REFERENCES motor_versao (id),
    entrada_json    TEXT    NOT NULL,
    resultado_json  TEXT    NOT NULL,
    versao_nucleo   TEXT    NOT NULL,
    versao_app      TEXT    NOT NULL,
    vermelhos       INTEGER NOT NULL,
    amarelos        INTEGER NOT NULL,
    criado_em       TEXT    NOT NULL
) STRICT;

CREATE INDEX calculo_por_projeto ON calculo (projeto_id, id);

CREATE TRIGGER calculo_nao_muda BEFORE UPDATE ON calculo
BEGIN
    SELECT RAISE(ABORT, 'Cálculo salvo não muda: calcule de novo e salve outro.');
END;

CREATE TRIGGER calculo_nao_apaga BEFORE DELETE ON calculo
BEGIN
    SELECT RAISE(ABORT, 'Cálculo salvo não é apagado.');
END;

-- Regra 2.4: resultado vermelho só avança com justificativa escrita, gravada com data e hora.
CREATE TABLE calculo_justificativa (
    id         INTEGER PRIMARY KEY,
    calculo_id INTEGER NOT NULL REFERENCES calculo (id),
    indicador  TEXT    NOT NULL,
    texto      TEXT    NOT NULL,
    data_hora  TEXT    NOT NULL,
    UNIQUE (calculo_id, indicador)
) STRICT;

CREATE TRIGGER justificativa_nao_muda BEFORE UPDATE ON calculo_justificativa
BEGIN
    SELECT RAISE(ABORT, 'Justificativa gravada não muda.');
END;

CREATE TRIGGER justificativa_nao_apaga BEFORE DELETE ON calculo_justificativa
BEGIN
    SELECT RAISE(ABORT, 'Justificativa gravada não é apagada.');
END;
