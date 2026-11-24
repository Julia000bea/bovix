-- Fotos dos animais passam a ficar no banco, não no disco do servidor.
-- O disco do Render (e da maioria das hospedagens de container) é apagado a cada deploy/reinício.
-- Uma foto por animal; "chave" é aleatória e muda a cada envio (vira a URL pública da foto).
CREATE TABLE animal_fotos (
    animal_id       BIGINT PRIMARY KEY REFERENCES animais(id) ON DELETE CASCADE,
    chave           VARCHAR(64) NOT NULL UNIQUE,
    content_type    VARCHAR(30) NOT NULL,
    conteudo        BYTEA NOT NULL,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT now()
);
