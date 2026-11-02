CREATE TABLE public.usuario (
    id            bigint GENERATED ALWAYS AS IDENTITY,
    username      varchar(255) NOT NULL,
    email         varchar(255) NOT NULL,
    senha_hash    varchar(100) NOT NULL,
    bio           text NULL,
    image         text NULL,
    criado_em     timestamptz  NOT NULL DEFAULT now(),
    atualizado_em timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uq_usuario_username UNIQUE (username),
    CONSTRAINT ck_usuario_username_nao_vazio CHECK (btrim(username) <> ''),
    CONSTRAINT ck_usuario_email_nao_vazio CHECK (btrim(email) <> '')
);

CREATE UNIQUE INDEX uq_usuario_email_lower ON public.usuario (lower(email));
