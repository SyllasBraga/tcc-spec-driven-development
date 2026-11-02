CREATE TABLE public.user_follow (
    follower_id bigint      NOT NULL,
    followed_id bigint      NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_follow PRIMARY KEY (follower_id, followed_id),
    CONSTRAINT fk_user_follow_follower FOREIGN KEY (follower_id) REFERENCES public.app_user (id),
    CONSTRAINT fk_user_follow_followed FOREIGN KEY (followed_id) REFERENCES public.app_user (id),
    CONSTRAINT ck_user_follow_not_self CHECK (follower_id <> followed_id)
);

-- A PK já cobre buscas por follower_id; este índice cobre a FK e buscas por seguido.
CREATE INDEX ix_user_follow_followed_id ON public.user_follow (followed_id);
