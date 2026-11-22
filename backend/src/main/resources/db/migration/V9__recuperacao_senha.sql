-- "Esqueci minha senha": guardamos só o hash SHA-256 do token enviado por e-mail.
-- Datas em milissegundos desde 1970 (UTC), lidas e gravadas por JdbcTemplate.
CREATE TABLE tokens_redefinicao_senha (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expira_em   BIGINT NOT NULL,
    usado_em    BIGINT,
    criado_em   BIGINT NOT NULL
);
CREATE INDEX idx_tokens_redefinicao_usuario ON tokens_redefinicao_senha(usuario_id);

-- Quando a senha muda, tokens de login (JWT) emitidos antes deixam de valer.
ALTER TABLE usuarios ADD COLUMN senha_alterada_em BIGINT;
