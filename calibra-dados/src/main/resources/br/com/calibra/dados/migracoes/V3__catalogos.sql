-- V3 (fase 2, bloco 2.3): catálogos (SPEC 8.5) com versões e as peças usadas por cada cálculo salvo.
-- Os dados técnicos de cada item ficam em JSON (o record do núcleo de cada tipo); nome e busca, em colunas.

-- A versão atual de cada item. Editar cria uma versão nova; nada é apagado, só arquivado.
CREATE TABLE catalogo_item (
    id             INTEGER PRIMARY KEY,
    tipo           TEXT    NOT NULL,
    chave_semente  TEXT    UNIQUE,
    versao_atual   INTEGER NOT NULL,
    nome           TEXT    NOT NULL,
    fonte          TEXT    NOT NULL,
    data           TEXT,
    a_verificar    INTEGER NOT NULL CHECK (a_verificar IN (0, 1)),
    preco_centavos INTEGER,
    observacoes    TEXT    NOT NULL,
    dados_json     TEXT    NOT NULL,
    arquivado      INTEGER NOT NULL DEFAULT 0 CHECK (arquivado IN (0, 1)),
    busca          TEXT    NOT NULL,
    criado_em      TEXT    NOT NULL,
    atualizado_em  TEXT    NOT NULL
) STRICT;

CREATE INDEX catalogo_por_tipo ON catalogo_item (tipo, arquivado, nome);

-- Todas as versões de cada item, como foram gravadas. Versão não muda nem é apagada.
CREATE TABLE catalogo_versao (
    id             INTEGER PRIMARY KEY,
    item_id        INTEGER NOT NULL REFERENCES catalogo_item (id),
    versao         INTEGER NOT NULL,
    nome           TEXT    NOT NULL,
    fonte          TEXT    NOT NULL,
    data           TEXT,
    a_verificar    INTEGER NOT NULL CHECK (a_verificar IN (0, 1)),
    preco_centavos INTEGER,
    observacoes    TEXT    NOT NULL,
    dados_json     TEXT    NOT NULL,
    criada_em      TEXT    NOT NULL,
    UNIQUE (item_id, versao)
) STRICT;

CREATE TRIGGER catalogo_versao_nao_muda BEFORE UPDATE ON catalogo_versao
BEGIN
    SELECT RAISE(ABORT, 'Versão de catálogo não muda: salve uma versão nova.');
END;

CREATE TRIGGER catalogo_versao_nao_apaga BEFORE DELETE ON catalogo_versao
BEGIN
    SELECT RAISE(ABORT, 'Versão de catálogo não é apagada.');
END;

-- Regra 2.5: o cálculo salvo guarda a versão de cada peça do catálogo que usou.
CREATE TABLE calculo_peca (
    calculo_id INTEGER NOT NULL REFERENCES calculo (id),
    papel      TEXT    NOT NULL,
    item_id    INTEGER NOT NULL REFERENCES catalogo_item (id),
    versao     INTEGER NOT NULL,
    PRIMARY KEY (calculo_id, papel)
) STRICT;

CREATE TRIGGER calculo_peca_nao_muda BEFORE UPDATE ON calculo_peca
BEGIN
    SELECT RAISE(ABORT, 'Peça de cálculo salvo não muda.');
END;

CREATE TRIGGER calculo_peca_nao_apaga BEFORE DELETE ON calculo_peca
BEGIN
    SELECT RAISE(ABORT, 'Peça de cálculo salvo não é apagada.');
END;

-- SPEC 6.2 (alterado no bloco 2.3): câmbio do catálogo, opcional, no veículo.
ALTER TABLE veiculo ADD COLUMN cambio_item_id INTEGER REFERENCES catalogo_item (id);

-- SPEC 6.4: injeção escolhida do catálogo, opcional até a etapa de mapa base.
ALTER TABLE projeto ADD COLUMN injecao_item_id INTEGER REFERENCES catalogo_item (id);
