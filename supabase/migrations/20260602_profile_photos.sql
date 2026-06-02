alter table public.usuarios
    add column if not exists foto_perfil_url text;

alter table public.oferta_conversaciones
    add column if not exists foto_comprador_url text,
    add column if not exists foto_vendedor_url text;
