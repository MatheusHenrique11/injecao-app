-- V13 (Fase 3B, bloco 3B.6): as fichas de chicote para injeção programável (SPEC 6.6.5), uma por chicote, da OS. O
-- conteúdo (a injeção, o motor, a versão, a fonte do pinout, os pontos, os materiais, as observações e a situação)
-- fica em JSON, como o do laudo. A versão nova aponta para a anterior, que fica guardada, só para ver (teste nº 13).
-- Com a OS entregue ou cancelada, o banco recusa criar, mudar e apagar ficha.

CREATE TABLE ficha_chicote (
    id                 INTEGER PRIMARY KEY,
    os_id              INTEGER NOT NULL REFERENCES ordem_servico (id),
    versao_anterior_id INTEGER UNIQUE REFERENCES ficha_chicote (id),
    conteudo_json      TEXT    NOT NULL,
    criado_em          TEXT    NOT NULL,
    atualizado_em      TEXT    NOT NULL
) STRICT;

CREATE INDEX ficha_chicote_por_os ON ficha_chicote (os_id, id);

CREATE TRIGGER ficha_chicote_so_em_andamento BEFORE INSERT ON ficha_chicote
    WHEN (SELECT status FROM ordem_servico WHERE id = NEW.os_id) IN ('ENTREGUE', 'CANCELADA')
BEGIN
    SELECT RAISE(ABORT, 'OS entregue ou cancelada não ganha ficha do chicote.');
END;

CREATE TRIGGER ficha_chicote_versao_da_mesma_os BEFORE INSERT ON ficha_chicote
    WHEN NEW.versao_anterior_id IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM ficha_chicote WHERE id = NEW.versao_anterior_id AND os_id = NEW.os_id)
BEGIN
    SELECT RAISE(ABORT, 'A versão nova do chicote é da mesma OS que a anterior.');
END;

CREATE TRIGGER ficha_chicote_nao_muda_de_lugar BEFORE UPDATE ON ficha_chicote
    WHEN NEW.os_id IS NOT OLD.os_id OR NEW.versao_anterior_id IS NOT OLD.versao_anterior_id
BEGIN
    SELECT RAISE(ABORT, 'A ficha do chicote não muda de OS nem de versão anterior.');
END;

CREATE TRIGGER ficha_chicote_os_finalizada_nao_muda BEFORE UPDATE ON ficha_chicote
    WHEN (SELECT status FROM ordem_servico WHERE id = OLD.os_id) IN ('ENTREGUE', 'CANCELADA')
BEGIN
    SELECT RAISE(ABORT, 'OS entregue ou cancelada: a ficha do chicote não muda.');
END;

CREATE TRIGGER ficha_chicote_os_finalizada_nao_apaga BEFORE DELETE ON ficha_chicote
    WHEN (SELECT status FROM ordem_servico WHERE id = OLD.os_id) IN ('ENTREGUE', 'CANCELADA')
BEGIN
    SELECT RAISE(ABORT, 'OS entregue ou cancelada: a ficha do chicote não é apagada.');
END;

CREATE TRIGGER ficha_chicote_anterior_nao_muda BEFORE UPDATE ON ficha_chicote
    WHEN EXISTS (SELECT 1 FROM ficha_chicote WHERE versao_anterior_id = OLD.id)
BEGIN
    SELECT RAISE(ABORT, 'Esta versão do chicote já tem uma versão nova: ela fica só para ver.');
END;

CREATE TRIGGER ficha_chicote_anterior_nao_apaga BEFORE DELETE ON ficha_chicote
    WHEN EXISTS (SELECT 1 FROM ficha_chicote WHERE versao_anterior_id = OLD.id)
BEGIN
    SELECT RAISE(ABORT, 'Esta versão do chicote já tem uma versão nova: ela não é apagada.');
END;
