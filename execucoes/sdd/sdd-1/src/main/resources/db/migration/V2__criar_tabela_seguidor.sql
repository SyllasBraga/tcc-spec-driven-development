CREATE TABLE public.seguidor (
    seguidor_id bigint NOT NULL,
    seguido_id bigint NOT NULL,
    criado_em timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_seguidor PRIMARY KEY (seguidor_id, seguido_id),
    CONSTRAINT fk_seguidor_usuario_seguidor FOREIGN KEY (seguidor_id)
        REFERENCES public.usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_seguidor_usuario_seguido FOREIGN KEY (seguido_id)
        REFERENCES public.usuario (id) ON DELETE CASCADE,
    CONSTRAINT ck_seguidor_distintos CHECK (seguidor_id <> seguido_id)
);

CREATE INDEX ix_seguidor_seguido_id ON public.seguidor (seguido_id);
