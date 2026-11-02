create table public.usuario (
    id            bigint generated always as identity,
    username      text        not null,
    email         text        not null,
    senha_hash    text        not null,
    bio           text        null,
    imagem        text        null,
    criado_em     timestamptz not null default now(),
    atualizado_em timestamptz not null default now(),
    constraint pk_usuario primary key (id),
    constraint uq_usuario_username unique (username),
    constraint ck_usuario_username_nao_vazio check (btrim(username) <> ''),
    constraint ck_usuario_email_nao_vazio check (btrim(email) <> '')
);

create unique index uq_usuario_email on public.usuario (lower(email));
