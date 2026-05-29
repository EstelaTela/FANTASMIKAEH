alter table public.direcciones
    add column if not exists alias text,
    add column if not exists nombre_completo text,
    add column if not exists telefono text,
    add column if not exists provincia text;

update public.direcciones
set
    alias = coalesce(alias, 'Direccion'),
    provincia = coalesce(provincia, pais)
where alias is null
   or provincia is null;
