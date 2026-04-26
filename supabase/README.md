# Supabase + Resend para codigos por email

## Funciones incluidas

- `send-verification-code`: genera un codigo de 6 digitos, lo guarda en `email_verification_codes` y envia el email con Resend.
- `reset-password-with-code`: valida el codigo y cambia la contrasena del usuario.

## Variables de entorno

Configuralas en Supabase antes de desplegar:

- `RESEND_API_KEY`
- `EMAIL_FROM`
- `APP_NAME`
- `EMAIL_LOGO_URL`

Ejemplo:

```bash
supabase secrets set RESEND_API_KEY=re_xxx
supabase secrets set EMAIL_FROM="FKAEH <no-reply@tudominio.com>"
supabase secrets set APP_NAME=FKAEH
supabase secrets set EMAIL_LOGO_URL="https://gomfmabmfytmrhvwmugh.supabase.co/storage/v1/object/public/branding/ghost-logo.png"
```

## Despliegue

```bash
supabase functions deploy send-verification-code
supabase functions deploy reset-password-with-code
```

## Nota sobre el icono de marca

La imagen de la cabecera del email si se controla desde el HTML del mensaje y esta funcion ya la usa con `EMAIL_LOGO_URL`.

El icono que algunos clientes muestran junto al remitente en la bandeja o cabecera no depende solo del HTML. Para eso normalmente necesitas:

- dominio propio verificado en Resend
- SPF y DKIM bien configurados
- BIMI
- en algunos casos un logo SVG Tiny PS y certificado VMC

Sin eso, el email puede mostrar tu logo dentro del mensaje, pero Gmail u Outlook pueden seguir enseñando un avatar generico junto al remitente.
