create table public.users (
    id            bigint generated always as identity,
    username      text        not null,
    email         text        not null,
    password_hash text        not null,
    bio           text,
    image         text,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    constraint pk_users primary key (id),
    constraint uq_users_username unique (username),
    constraint uq_users_email unique (email),
    constraint ck_users_username_not_blank check (btrim(username) <> ''),
    constraint ck_users_email_not_blank check (btrim(email) <> ''),
    constraint ck_users_email_lowercase check (email = lower(email))
);
