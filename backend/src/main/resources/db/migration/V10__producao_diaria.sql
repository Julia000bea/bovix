-- Produção diária para o TCC.
-- estimado: o volume do dia não foi medido nesse dia (veio de uma coleta de N dias dividida igualmente).
-- dias_coleta: quantos dias a coleta original cobria (1 = medição do próprio dia).
-- coleta_id: agrupa os registros gerados por uma mesma coleta de N dias (removidos juntos).
ALTER TABLE ordenhas
    ADD COLUMN estimado    BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN dias_coleta INTEGER NOT NULL DEFAULT 1 CHECK (dias_coleta BETWEEN 1 AND 31),
    ADD COLUMN coleta_id   VARCHAR(36);

CREATE INDEX idx_ordenha_coleta ON ordenhas(coleta_id);
