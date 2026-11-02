CREATE TABLE public.user_account (
    id            bigint GENERATED ALWAYS AS IDENTITY,
    username      varchar(100) NOT NULL,
    email         varchar(255) NOT NULL,
    password_hash varchar(255) NOT NULL,
    bio           text,
    image         text,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_account PRIMARY KEY (id),
    CONSTRAINT uq_user_account_username UNIQUE (username),
    CONSTRAINT uq_user_account_email UNIQUE (email),
    CONSTRAINT ck_user_account_username_not_blank CHECK (btrim(username) <> ''),
    CONSTRAINT ck_user_account_email_lowercase CHECK (email = lower(email))
);
