CREATE TABLE public.usuario (
    id            bigint GENERATED ALWAYS AS IDENTITY,
    email         varchar(255) NOT NULL,
    username      varchar(50)  NOT NULL,
    senha_hash    varchar(255) NOT NULL,
    bio           text         NULL,
    image         text         NULL,
    criado_em     timestamptz  NOT NULL DEFAULT now(),
    atualizado_em timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT uq_usuario_username UNIQUE (username),
    CONSTRAINT ck_usuario_email_nao_vazio CHECK (btrim(email) <> ''),
    CONSTRAINT ck_usuario_username_nao_vazio CHECK (btrim(username) <> '')
);
