-- ===================================================================
-- Bovix - Gestão Inteligente do Rebanho
-- Schema inicial do banco de dados (PostgreSQL)
-- ===================================================================

CREATE TABLE usuarios (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    email           VARCHAR(180) NOT NULL UNIQUE,
    senha_hash      VARCHAR(255) NOT NULL,
    criado_em       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE fazendas (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nome            VARCHAR(150) NOT NULL,
    cidade          VARCHAR(120),
    estado          VARCHAR(2),
    criado_em       TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_fazendas_usuario ON fazendas(usuario_id);

CREATE TABLE configuracoes (
    id                  BIGSERIAL PRIMARY KEY,
    fazenda_id          BIGINT NOT NULL UNIQUE REFERENCES fazendas(id) ON DELETE CASCADE,
    gestacao_dias       INTEGER NOT NULL DEFAULT 283,
    cio_dias            INTEGER NOT NULL DEFAULT 21,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE animais (
    id              BIGSERIAL PRIMARY KEY,
    fazenda_id      BIGINT NOT NULL REFERENCES fazendas(id) ON DELETE CASCADE,
    brinco          VARCHAR(30) NOT NULL,
    nome            VARCHAR(120),
    raca            VARCHAR(80),
    nascimento      DATE,
    categoria       VARCHAR(30) NOT NULL DEFAULT 'Bezerra'
                        CHECK (categoria IN ('Bezerra','Novilha','Vaca lactante','Vaca seca')),
    status          VARCHAR(20) NOT NULL DEFAULT 'Ativa'
                        CHECK (status IN ('Ativa','Vendida','Morta')),
    notas           TEXT,
    foto_url        VARCHAR(500),
    criado_em       TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_animal_brinco_por_fazenda UNIQUE (fazenda_id, brinco)
);
CREATE INDEX idx_animais_fazenda ON animais(fazenda_id);
CREATE INDEX idx_animais_status ON animais(status);

CREATE TABLE eventos_reproducao (
    id                  BIGSERIAL PRIMARY KEY,
    animal_id           BIGINT NOT NULL REFERENCES animais(id) ON DELETE CASCADE,
    tipo                VARCHAR(20) NOT NULL
                            CHECK (tipo IN ('cio','inseminacao','diagnostico','parto','secagem')),
    data                DATE NOT NULL,
    detalhe             VARCHAR(255),
    resultado           VARCHAR(20) CHECK (resultado IN ('positivo','negativo')),
    criado_em           TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_repro_animal ON eventos_reproducao(animal_id);
CREATE INDEX idx_repro_data ON eventos_reproducao(data);

CREATE TABLE eventos_saude (
    id                          BIGSERIAL PRIMARY KEY,
    animal_id                   BIGINT NOT NULL REFERENCES animais(id) ON DELETE CASCADE,
    tipo                        VARCHAR(20) NOT NULL
                                    CHECK (tipo IN ('vacina','medicamento','vermifugo','outro')),
    produto                     VARCHAR(150) NOT NULL,
    data                        DATE NOT NULL,
    carencia_leite_dias         INTEGER,
    proxima_aplicacao_dias      INTEGER,
    observacoes                 TEXT,
    criado_em                   TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_saude_animal ON eventos_saude(animal_id);
CREATE INDEX idx_saude_data ON eventos_saude(data);

CREATE TABLE ordenhas (
    id              BIGSERIAL PRIMARY KEY,
    fazenda_id      BIGINT NOT NULL REFERENCES fazendas(id) ON DELETE CASCADE,
    animal_id       BIGINT REFERENCES animais(id) ON DELETE CASCADE,
    data            DATE NOT NULL,
    periodo         VARCHAR(20) NOT NULL DEFAULT 'Dia todo'
                        CHECK (periodo IN ('Manhã','Tarde','Dia todo')),
    litros          NUMERIC(8,2) NOT NULL CHECK (litros >= 0),
    criado_em       TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_ordenha_fazenda_data ON ordenhas(fazenda_id, data);
CREATE INDEX idx_ordenha_animal ON ordenhas(animal_id);

CREATE TABLE financeiro (
    id              BIGSERIAL PRIMARY KEY,
    fazenda_id      BIGINT NOT NULL REFERENCES fazendas(id) ON DELETE CASCADE,
    data            DATE NOT NULL,
    categoria       VARCHAR(30) NOT NULL
                        CHECK (categoria IN ('Ração','Insumos','Medicamentos','Mão de obra','Manutenção','Outros')),
    descricao       VARCHAR(255),
    valor           NUMERIC(10,2) NOT NULL CHECK (valor >= 0),
    criado_em       TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_financeiro_fazenda_data ON financeiro(fazenda_id, data);
CREATE INDEX idx_financeiro_categoria ON financeiro(categoria);
