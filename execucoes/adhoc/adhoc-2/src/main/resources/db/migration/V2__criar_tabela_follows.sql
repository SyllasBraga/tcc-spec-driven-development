CREATE TABLE public.follows (
    follower_id bigint      NOT NULL,
    followed_id bigint      NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_follows PRIMARY KEY (follower_id, followed_id),
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES public.users (id),
    CONSTRAINT fk_follows_followed FOREIGN KEY (followed_id) REFERENCES public.users (id),
    CONSTRAINT ck_follows_nao_segue_a_si_mesmo CHECK (follower_id <> followed_id)
);

CREATE INDEX ix_follows_followed_id ON public.follows (followed_id);
