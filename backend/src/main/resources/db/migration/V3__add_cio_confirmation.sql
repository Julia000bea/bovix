-- ===================================================================
-- Bovix - Adição de campos para fluxo de confirmação de cio
-- e vínculo entre reprodução e cio de origem
-- ===================================================================

-- Adicionar coluna data_confirmacao para registrar quando o cio foi confirmado
ALTER TABLE eventos_reproducao ADD COLUMN data_confirmacao DATE;

-- Adicionar coluna cio_vinculado_id para vincular reproduções ao cio de origem
ALTER TABLE eventos_reproducao ADD COLUMN cio_vinculado_id BIGINT REFERENCES eventos_reproducao(id) ON DELETE SET NULL;

-- Adicionar coluna previsao_parto para calcular data estimada do parto
ALTER TABLE eventos_reproducao ADD COLUMN previsao_parto DATE;

-- Criar índice para buscar rápido eventos vinculados a um cio
CREATE INDEX idx_repro_cio_vinculado ON eventos_reproducao(cio_vinculado_id);
