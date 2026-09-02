-- O fluxo atual de diagnostico de prenhez usa "prenhe" e "vazia".
-- Convertemos os valores legados antes de tornar a regra explicita.
UPDATE eventos_reproducao
SET resultado = CASE resultado
    WHEN 'positivo' THEN 'prenhe'
    WHEN 'negativo' THEN 'vazia'
    ELSE resultado
END
WHERE resultado IN ('positivo', 'negativo');

ALTER TABLE eventos_reproducao
    DROP CONSTRAINT IF EXISTS eventos_reproducao_resultado_check;

ALTER TABLE eventos_reproducao
    ADD CONSTRAINT eventos_reproducao_resultado_check
    CHECK (resultado IS NULL OR resultado IN ('prenhe', 'vazia'));
