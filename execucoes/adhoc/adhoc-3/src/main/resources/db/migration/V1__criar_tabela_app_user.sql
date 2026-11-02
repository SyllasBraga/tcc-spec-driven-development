CREATE TABLE public.app_user (
    id            bigint GENERATED ALWAYS AS IDENTITY,
    username      varchar(64)  NOT NULL,
    email         varchar(254) NOT NULL,
    password_hash varchar(255) NOT NULL,
    bio           text,
    image         text,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT ck_app_user_username_not_blank CHECK (btrim(username) <> ''),
    CONSTRAINT ck_app_user_email_not_blank CHECK (btrim(email) <> '')
);

-- E-mail é único sem diferenciar maiúsculas de minúsculas.
CREATE UNIQUE INDEX uq_app_user_email ON public.app_user (lower(email));
