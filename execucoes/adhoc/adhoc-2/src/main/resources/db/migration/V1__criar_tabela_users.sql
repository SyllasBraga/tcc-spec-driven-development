CREATE TABLE public.users (
    id            bigint GENERATED ALWAYS AS IDENTITY,
    email         varchar(255) NOT NULL,
    username      varchar(255) NOT NULL,
    password_hash varchar(100) NOT NULL,
    bio           text,
    image         text,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_email_minusculo CHECK (email = lower(email)),
    CONSTRAINT ck_users_email_preenchido CHECK (btrim(email) <> ''),
    CONSTRAINT ck_users_username_preenchido CHECK (btrim(username) <> '')
);
