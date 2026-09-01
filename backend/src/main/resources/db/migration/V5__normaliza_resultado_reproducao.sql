-- `resultado` is optional. Empty strings sent by the form must not remain
-- stored because the column accepts only diagnostic outcomes.
UPDATE eventos_reproducao
SET resultado = NULL
WHERE resultado IS NOT NULL
  AND btrim(resultado) = '';

ALTER TABLE eventos_reproducao
    DROP CONSTRAINT IF EXISTS eventos_reproducao_resultado_check;

ALTER TABLE eventos_reproducao
    ADD CONSTRAINT eventos_reproducao_resultado_check
    CHECK (resultado IS NULL OR resultado IN ('positivo', 'negativo'));
