-- V11 (Fase 3B, bloco 3B.5): as instalações do veículo (SPEC 6.2), o que foi instalado ou mexido no carro, pela
-- oficina ou informado pelo cliente. A OS de origem é opcional e precisa ser do mesmo veículo; a retirada é a data em
-- que o item saiu do carro (ele continua na lista). Cada mudança vai para o log de auditoria (entidade "instalacao"),
-- como as do veículo.
-- A instalação não é dado pessoal: fica quando o cliente é anonimizado e quando o veículo é arquivado.

CREATE TABLE instalacao (
    id                        INTEGER PRIMARY KEY,
    veiculo_id                INTEGER NOT NULL REFERENCES veiculo (id),
    os_id                     INTEGER REFERENCES ordem_servico (id),
    tipo                      TEXT    NOT NULL,
    descricao                 TEXT    NOT NULL,
    marca                     TEXT    NOT NULL,
    modelo                    TEXT    NOT NULL,
    potencia_ou_especificacao TEXT    NOT NULL,
    data_instalacao           TEXT,
    observacoes               TEXT    NOT NULL,
    retirada_em               TEXT,
    criado_em                 TEXT    NOT NULL,
    atualizado_em             TEXT    NOT NULL
) STRICT;

CREATE INDEX instalacao_por_veiculo ON instalacao (veiculo_id);
CREATE INDEX instalacao_por_os ON instalacao (os_id);

CREATE TRIGGER instalacao_os_do_veiculo BEFORE INSERT ON instalacao
    WHEN NEW.os_id IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM ordem_servico WHERE id = NEW.os_id AND veiculo_id = NEW.veiculo_id)
BEGIN
    SELECT RAISE(ABORT, 'A OS de origem precisa ser do mesmo veículo.');
END;

CREATE TRIGGER instalacao_os_do_veiculo_ao_mudar BEFORE UPDATE ON instalacao
    WHEN NEW.os_id IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM ordem_servico WHERE id = NEW.os_id AND veiculo_id = NEW.veiculo_id)
BEGIN
    SELECT RAISE(ABORT, 'A OS de origem precisa ser do mesmo veículo.');
END;

CREATE TRIGGER instalacao_nao_muda_de_veiculo BEFORE UPDATE ON instalacao
    WHEN NEW.veiculo_id IS NOT OLD.veiculo_id
BEGIN
    SELECT RAISE(ABORT, 'A instalação não muda de veículo.');
END;
