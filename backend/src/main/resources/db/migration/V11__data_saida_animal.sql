-- Data em que o animal saiu do rebanho (venda ou morte), usada no histórico de vacas em lactação.
-- Animais que já estavam como Vendida/Morta não tinham essa data: usamos a data da última
-- alteração do cadastro (atualizado_em) como aproximação.
ALTER TABLE animais ADD COLUMN data_saida DATE;

UPDATE animais
SET data_saida = COALESCE(atualizado_em, criado_em)::date
WHERE status IN ('Vendida', 'Morta');
