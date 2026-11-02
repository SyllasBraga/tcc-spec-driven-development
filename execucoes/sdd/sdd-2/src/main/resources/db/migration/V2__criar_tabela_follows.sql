create table public.follows (
    follower_id bigint      not null,
    followed_id bigint      not null,
    created_at  timestamptz not null default now(),
    constraint pk_follows primary key (follower_id, followed_id),
    constraint fk_follows_follower foreign key (follower_id) references public.users (id),
    constraint fk_follows_followed foreign key (followed_id) references public.users (id),
    constraint ck_follows_not_self check (follower_id <> followed_id)
);

create index ix_follows_followed_id on public.follows (followed_id);
