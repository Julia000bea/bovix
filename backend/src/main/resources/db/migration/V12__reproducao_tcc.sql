-- Perda gestacional (aborto/reabsorção): depois dela a vaca volta a ser considerada vazia.
ALTER TABLE eventos_reproducao DROP CONSTRAINT IF EXISTS eventos_reproducao_tipo_check;
ALTER TABLE eventos_reproducao ADD CONSTRAINT eventos_reproducao_tipo_check
    CHECK (tipo IN ('cio_detectado','cio_confirmado','inseminacao','reproducao','diagnostico_prenhez',
                    'prenhez_confirmada','secagem','pre_parto','parto','perda_gestacional'));

-- dias_vazia_alerta: "vacas vazias há mais de X dias após o parto" (padrão 90).
-- periodo_seco_dias: dias antes do parto previsto em que a vaca é secada (padrão 60).
ALTER TABLE configuracoes
    ADD COLUMN dias_vazia_alerta INTEGER NOT NULL DEFAULT 90 CHECK (dias_vazia_alerta BETWEEN 30 AND 400),
    ADD COLUMN periodo_seco_dias INTEGER NOT NULL DEFAULT 60 CHECK (periodo_seco_dias BETWEEN 30 AND 120);
