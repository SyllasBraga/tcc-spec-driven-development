CREATE TABLE public.users (
    id            bigint GENERATED ALWAYS AS IDENTITY,
    username      varchar(100)  NOT NULL,
    email         varchar(254)  NOT NULL,
    password_hash varchar(255)  NOT NULL,
    bio           text,
    image         varchar(2048),
    created_at    timestamptz   NOT NULL DEFAULT now(),
    updated_at    timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_username_not_blank CHECK (btrim(username) <> ''),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email))
);
