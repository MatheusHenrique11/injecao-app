-- V7 (fase 3, bloco 3.2): anexos da OS e do veículo (fotos da entrada e do serviço, documentos, nota fiscal, laudos).
-- O arquivo fica na pasta de anexos (o backup leva junto); aqui ficam o caminho relativo, o nome original, o tamanho
-- e o SHA-256 para conferir. Sem OS, o anexo é do veículo (documentos, para a ficha do bloco 3.3).
-- Sem gatilho contra apagar: a anonimização (LGPD) apaga documento pessoal mesmo de OS entregue; quando se pode
-- remover fica no caso de uso.

CREATE TABLE anexo (
    id            INTEGER PRIMARY KEY,
    veiculo_id    INTEGER NOT NULL REFERENCES veiculo (id),
    os_id         INTEGER REFERENCES ordem_servico (id),
    tipo          TEXT    NOT NULL,
    descricao     TEXT    NOT NULL DEFAULT '',
    arquivo       TEXT    NOT NULL,
    nome_original TEXT    NOT NULL,
    tamanho_bytes INTEGER NOT NULL CHECK (tamanho_bytes >= 0),
    sha256        TEXT    NOT NULL,
    validade      TEXT,
    pessoal       INTEGER NOT NULL DEFAULT 0 CHECK (pessoal IN (0, 1)),
    criado_em     TEXT    NOT NULL
) STRICT;

CREATE INDEX anexo_por_os ON anexo (os_id, id);
CREATE INDEX anexo_por_veiculo ON anexo (veiculo_id, id);
