-- Localização da fazenda e estação meteorológica do INMET usada para o ITU.
ALTER TABLE fazendas
    ADD COLUMN latitude      NUMERIC(9,6) CHECK (latitude BETWEEN -90 AND 90),
    ADD COLUMN longitude     NUMERIC(9,6) CHECK (longitude BETWEEN -180 AND 180),
    ADD COLUMN estacao_inmet VARCHAR(10);

-- Clima diário por estação (compartilhado entre fazendas da mesma estação).
-- itu_* calculados por hora e agregados no dia; horas_itu_acima_72 = horas com ITU > 72.
-- Dados só diários (CSV sem horas) não têm horas_itu_acima_72.
CREATE TABLE clima_diario (
    id                  BIGSERIAL PRIMARY KEY,
    estacao             VARCHAR(10) NOT NULL,
    data                DATE NOT NULL,
    temp_media          NUMERIC(5,2),
    temp_max            NUMERIC(5,2),
    temp_min            NUMERIC(5,2),
    umidade_media       NUMERIC(5,2),
    itu_medio           NUMERIC(5,2),
    itu_max             NUMERIC(5,2),
    horas_itu_acima_72  INTEGER CHECK (horas_itu_acima_72 BETWEEN 0 AND 24),
    fonte               VARCHAR(20) NOT NULL CHECK (fonte IN ('inmet', 'csv')),
    criado_em           TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_clima_estacao_data UNIQUE (estacao, data)
);
