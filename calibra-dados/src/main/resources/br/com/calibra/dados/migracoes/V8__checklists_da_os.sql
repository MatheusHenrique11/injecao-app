-- V8 (Fase 3B, bloco 3B.3): os checklists da OS (SPEC 6.5 e 6.6.2). Ao entrar em execução, cada serviço do orçamento
-- aprovado que tem checklist ganha uma cópia (instantâneo) do modelo do catálogo, com a versão de onde ela veio: mudar
-- o modelo depois não muda a cópia. Cada item da cópia guarda a marcação (situação, observação e justificativa), quem
-- marcou e quando.
-- O banco recusa mudar o texto da cópia, apagar a cópia e mexer no checklist da OS entregue ou cancelada.

-- Uma cópia por serviço do catálogo na OS. O nome do serviço e o do modelo ficam como eram na hora da cópia.
CREATE TABLE checklist_os (
    id              INTEGER PRIMARY KEY,
    os_id           INTEGER NOT NULL REFERENCES ordem_servico (id),
    servico_item_id INTEGER NOT NULL REFERENCES catalogo_item (id),
    servico         TEXT    NOT NULL,
    modelo_item_id  INTEGER NOT NULL REFERENCES catalogo_item (id),
    modelo_versao   INTEGER NOT NULL,
    modelo_nome     TEXT    NOT NULL,
    criado_em       TEXT    NOT NULL,
    UNIQUE (os_id, servico_item_id)
) STRICT;

-- Os itens da cópia, na ordem do modelo. "Não se aplica" exige a justificativa (teste nº 4 da Fase 3B).
CREATE TABLE checklist_os_item (
    id            INTEGER PRIMARY KEY,
    checklist_id  INTEGER NOT NULL REFERENCES checklist_os (id),
    ordem         INTEGER NOT NULL,
    texto         TEXT    NOT NULL CHECK (texto <> ''),
    obrigatorio   INTEGER NOT NULL CHECK (obrigatorio IN (0, 1)),
    a_verificar   INTEGER NOT NULL CHECK (a_verificar IN (0, 1)),
    situacao      TEXT    NOT NULL DEFAULT 'PENDENTE' CHECK (situacao IN ('PENDENTE', 'FEITO', 'NAO_SE_APLICA')),
    observacao    TEXT    NOT NULL DEFAULT '',
    justificativa TEXT    NOT NULL DEFAULT '',
    mecanico_id   INTEGER REFERENCES mecanico (id),
    marcado_em    TEXT,
    UNIQUE (checklist_id, ordem),
    CHECK (situacao <> 'NAO_SE_APLICA' OR justificativa <> '')
) STRICT;

CREATE TRIGGER checklist_os_so_em_andamento BEFORE INSERT ON checklist_os
    WHEN (SELECT status FROM ordem_servico WHERE id = NEW.os_id) IN ('ENTREGUE', 'CANCELADA')
BEGIN
    SELECT RAISE(ABORT, 'OS entregue ou cancelada não ganha checklist.');
END;

CREATE TRIGGER checklist_os_nao_muda BEFORE UPDATE ON checklist_os
BEGIN
    SELECT RAISE(ABORT, 'A cópia do checklist na OS não muda.');
END;

CREATE TRIGGER checklist_os_nao_apaga BEFORE DELETE ON checklist_os
BEGIN
    SELECT RAISE(ABORT, 'A cópia do checklist na OS não é apagada.');
END;

CREATE TRIGGER checklist_os_item_so_marcacao BEFORE UPDATE ON checklist_os_item
    WHEN NEW.checklist_id IS NOT OLD.checklist_id OR NEW.ordem IS NOT OLD.ordem OR NEW.texto IS NOT OLD.texto
        OR NEW.obrigatorio IS NOT OLD.obrigatorio OR NEW.a_verificar IS NOT OLD.a_verificar
BEGIN
    SELECT RAISE(ABORT, 'O texto do checklist da OS não muda: só a marcação.');
END;

CREATE TRIGGER checklist_os_item_os_finalizada BEFORE UPDATE ON checklist_os_item
    WHEN (SELECT os.status FROM ordem_servico os JOIN checklist_os c ON c.os_id = os.id
        WHERE c.id = OLD.checklist_id) IN ('ENTREGUE', 'CANCELADA')
BEGIN
    SELECT RAISE(ABORT, 'OS entregue ou cancelada: o checklist não muda.');
END;

CREATE TRIGGER checklist_os_item_nao_apaga BEFORE DELETE ON checklist_os_item
BEGIN
    SELECT RAISE(ABORT, 'Item do checklist da OS não é apagado.');
END;
