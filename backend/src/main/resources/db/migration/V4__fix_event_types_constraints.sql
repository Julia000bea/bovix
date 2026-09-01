-- ===================================================================
-- Bovix - Fix CHECK constraints and update event types
-- ===================================================================

-- Remove the old constraint on tipo
ALTER TABLE eventos_reproducao DROP CONSTRAINT eventos_reproducao_tipo_check;

-- Add new constraint with all required event types
ALTER TABLE eventos_reproducao ADD CONSTRAINT eventos_reproducao_tipo_check 
  CHECK (tipo IN ('cio_detectado','cio_confirmado','inseminacao','reproducao','diagnostico_prenhez','prenhez_confirmada','secagem','pre_parto','parto'));

-- Make resultado column allow NULL properly (it already does, but let's be explicit)
-- The constraint already allows NULL, this is just for clarity
-- No action needed - resultado can be NULL
