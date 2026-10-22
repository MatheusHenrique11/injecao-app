-- V16 (fase 4, bloco 4.4): o mapa base do projeto, em versões (SPEC 7.5; decisão 296). Como o cálculo salvo, cada
-- mapa guarda a entrada, o resultado (as tabelas, as travas, os alertas e a memória), a versão do núcleo, o cálculo e
-- a versão do motor que usou e as peças do catálogo (injeção, modelos de VE e de ignição) com a versão de cada uma.
-- Nunca muda nem é apagado: gerar de novo cria a versão seguinte.

CREATE TABLE mapa_base (
    id              INTEGER PRIMARY KEY,
    projeto_id      INTEGER NOT NULL REFERENCES projeto (id),
    numero          INTEGER NOT NULL,
    calculo_id      INTEGER NOT NULL REFERENCES calculo (id),
    motor_versao_id INTEGER NOT NULL REFERENCES motor_versao (id),
    entrada_json    TEXT    NOT NULL,
    resultado_json  TEXT    NOT NULL,
    versao_nucleo   TEXT    NOT NULL,
    versao_app      TEXT    NOT NULL,
    vermelhos       INTEGER NOT NULL,
    amarelos        INTEGER NOT NULL,
    criado_em       TEXT    NOT NULL,
    UNIQUE (projeto_id, numero)
) STRICT;

CREATE INDEX mapa_base_por_projeto ON mapa_base (projeto_id, id);

CREATE TRIGGER mapa_base_nao_muda BEFORE UPDATE ON mapa_base
BEGIN
    SELECT RAISE(ABORT, 'Mapa base salvo não muda: gere de novo e salve outra versão.');
END;

CREATE TRIGGER mapa_base_nao_apaga BEFORE DELETE ON mapa_base
BEGIN
    SELECT RAISE(ABORT, 'Mapa base salvo não é apagado.');
END;

-- As peças do catálogo que o mapa usou, na versão daquele momento (regra 2.5).
CREATE TABLE mapa_base_peca (
    mapa_base_id INTEGER NOT NULL REFERENCES mapa_base (id),
    papel        TEXT    NOT NULL,
    item_id      INTEGER NOT NULL REFERENCES catalogo_item (id),
    versao       INTEGER NOT NULL,
    PRIMARY KEY (mapa_base_id, papel)
) STRICT;

CREATE TRIGGER mapa_base_peca_nao_muda BEFORE UPDATE ON mapa_base_peca
BEGIN
    SELECT RAISE(ABORT, 'As peças de um mapa base salvo não mudam.');
END;

CREATE TRIGGER mapa_base_peca_nao_apaga BEFORE DELETE ON mapa_base_peca
BEGIN
    SELECT RAISE(ABORT, 'As peças de um mapa base salvo não são apagadas.');
END;

-- Regra 2.4: o alerta vermelho do mapa só passa com justificativa escrita, gravada com data e hora.
CREATE TABLE mapa_base_justificativa (
    id           INTEGER PRIMARY KEY,
    mapa_base_id INTEGER NOT NULL REFERENCES mapa_base (id),
    alerta       TEXT    NOT NULL,
    texto        TEXT    NOT NULL,
    data_hora    TEXT    NOT NULL,
    UNIQUE (mapa_base_id, alerta)
) STRICT;

CREATE TRIGGER mapa_base_justificativa_nao_muda BEFORE UPDATE ON mapa_base_justificativa
BEGIN
    SELECT RAISE(ABORT, 'Justificativa gravada não muda.');
END;

CREATE TRIGGER mapa_base_justificativa_nao_apaga BEFORE DELETE ON mapa_base_justificativa
BEGIN
    SELECT RAISE(ABORT, 'Justificativa gravada não é apagada.');
END;
