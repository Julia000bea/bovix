-- Tratamentos de vários dias: a carência do leite conta a partir da ÚLTIMA aplicação.
-- Registros antigos ficam com 1 dia (dose única), que era o comportamento anterior.
ALTER TABLE eventos_saude
    ADD COLUMN duracao_tratamento_dias INTEGER NOT NULL DEFAULT 1
        CHECK (duracao_tratamento_dias BETWEEN 1 AND 60);
