CREATE TABLE public.user_follows (
    follower_id bigint      NOT NULL,
    followed_id bigint      NOT NULL,
    created_at  timestamptz NOT NULL,
    CONSTRAINT pk_user_follows PRIMARY KEY (follower_id, followed_id),
    CONSTRAINT fk_user_follows_follower FOREIGN KEY (follower_id) REFERENCES public.users (id),
    CONSTRAINT fk_user_follows_followed FOREIGN KEY (followed_id) REFERENCES public.users (id),
    CONSTRAINT ck_user_follows_not_self CHECK (follower_id <> followed_id)
);

-- follower_id já é coberto pela chave primária; followed_id precisa de índice próprio.
CREATE INDEX ix_user_follows_followed_id ON public.user_follows (followed_id);
