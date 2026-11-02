create table public.seguimento (
    seguidor_id bigint      not null,
    seguido_id  bigint      not null,
    criado_em   timestamptz not null default now(),
    constraint pk_seguimento primary key (seguidor_id, seguido_id),
    constraint fk_seguimento_seguidor foreign key (seguidor_id) references public.usuario (id),
    constraint fk_seguimento_seguido foreign key (seguido_id) references public.usuario (id),
    constraint ck_seguimento_nao_segue_a_si_mesmo check (seguidor_id <> seguido_id)
);

create index ix_seguimento_seguido_id on public.seguimento (seguido_id);
