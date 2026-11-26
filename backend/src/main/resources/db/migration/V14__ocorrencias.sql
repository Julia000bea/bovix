-- Ocorrências do dia que podem explicar variações na produção (falta de água, quebra da ordenhadeira...).
CREATE TABLE ocorrencias (
    id          BIGSERIAL PRIMARY KEY,
    fazenda_id  BIGINT NOT NULL REFERENCES fazendas(id) ON DELETE CASCADE,
    data        DATE NOT NULL,
    tipo        VARCHAR(20) NOT NULL
                    CHECK (tipo IN ('alimentacao','agua','equipamento','clima','sanidade','manejo','outro')),
    descricao   VARCHAR(500) NOT NULL,
    criado_em   TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_ocorrencias_fazenda_data ON ocorrencias(fazenda_id, data);
