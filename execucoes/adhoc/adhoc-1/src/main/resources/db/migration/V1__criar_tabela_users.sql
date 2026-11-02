create table public.users (
    id            bigint generated always as identity,
    username      varchar(50)  not null,
    email         varchar(255) not null,
    password_hash varchar(255) not null,
    bio           text,
    image         text,
    constraint pk_users primary key (id),
    constraint uq_users_username unique (username),
    constraint uq_users_email unique (email),
    constraint ck_users_username_not_blank check (btrim(username) <> ''),
    constraint ck_users_email_lowercase check (email = lower(email))
);
