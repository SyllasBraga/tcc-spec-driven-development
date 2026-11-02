CREATE TABLE public.seguimento (
    seguidor_id bigint      NOT NULL,
    seguido_id  bigint      NOT NULL,
    criado_em   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_seguimento PRIMARY KEY (seguidor_id, seguido_id),
    CONSTRAINT fk_seguimento_seguidor FOREIGN KEY (seguidor_id) REFERENCES public.usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_seguimento_seguido FOREIGN KEY (seguido_id) REFERENCES public.usuario (id) ON DELETE CASCADE,
    CONSTRAINT ck_seguimento_nao_si_mesmo CHECK (seguidor_id <> seguido_id)
);

CREATE INDEX ix_seguimento_seguido_id ON public.seguimento (seguido_id);
