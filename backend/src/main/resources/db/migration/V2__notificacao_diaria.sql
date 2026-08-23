-- Configuração do relatório diário por e-mail (Quadro do dia).
ALTER TABLE configuracoes
    ADD COLUMN notificacao_ativa    BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN notificacao_email    VARCHAR(180),
    ADD COLUMN notificacao_horario  TIME NOT NULL DEFAULT '07:00:00',
    ADD COLUMN notificacao_ultimo_envio DATE;
