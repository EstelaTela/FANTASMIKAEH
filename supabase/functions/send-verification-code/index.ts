import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const RESEND_API_KEY = Deno.env.get("RESEND_API_KEY") ?? "";
const EMAIL_FROM = Deno.env.get("EMAIL_FROM") ?? "";
const APP_NAME = Deno.env.get("APP_NAME") ?? "FKAEH";
const EMAIL_LOGO_URL = Deno.env.get("EMAIL_LOGO_URL") ?? "";

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

function jsonResponse(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json",
    },
  });
}

function generateCode() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

async function sha256(input: string) {
  const data = new TextEncoder().encode(input);
  const hashBuffer = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(hashBuffer))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

function buildHtml(code: string) {
  const logoBlock = EMAIL_LOGO_URL
    ? `<img src="${EMAIL_LOGO_URL}" alt="${APP_NAME}" width="88" height="88" style="display:block;margin:0 auto 16px auto;border-radius:20px;background:#000;object-fit:contain;" />`
    : `<div style="width:88px;height:88px;margin:0 auto 16px auto;border-radius:20px;background:#000;color:#fff;font-size:28px;line-height:88px;text-align:center;font-weight:700;">FK</div>`;

  return `
    <div style="margin:0;background:#050505;padding:32px 16px;font-family:Arial,Helvetica,sans-serif;color:#f5f5f5;">
      <div style="max-width:520px;margin:0 auto;background:#111;border:1px solid #2a2a2a;border-radius:24px;padding:32px;">
        ${logoBlock}
        <div style="text-align:center;">
          <p style="margin:0 0 8px 0;font-size:12px;letter-spacing:1.6px;color:#a3a3a3;text-transform:uppercase;">${APP_NAME}</p>
          <h1 style="margin:0 0 12px 0;font-size:28px;line-height:1.2;color:#ffffff;">Tu codigo de verificacion</h1>
          <p style="margin:0 0 24px 0;font-size:15px;line-height:1.6;color:#d4d4d4;">
            Usa este codigo para continuar con el cambio de contrasena. Caduca en 10 minutos.
          </p>
          <div style="display:inline-block;background:#ffffff;color:#111111;padding:16px 24px;border-radius:18px;font-size:32px;font-weight:700;letter-spacing:8px;">
            ${code}
          </div>
          <p style="margin:24px 0 0 0;font-size:13px;line-height:1.6;color:#9ca3af;">
            Si no has solicitado este codigo, puedes ignorar este mensaje.
          </p>
        </div>
      </div>
    </div>
  `;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return jsonResponse(405, { error: "Method not allowed" });
  }

  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY || !RESEND_API_KEY || !EMAIL_FROM) {
    return jsonResponse(500, { error: "Faltan variables de entorno en la funcion" });
  }

  try {
    const { email } = await req.json();
    const normalizedEmail = String(email ?? "").trim().toLowerCase();

    if (!normalizedEmail || !normalizedEmail.includes("@")) {
      return jsonResponse(400, { error: "Correo no valido" });
    }

    const { data: user, error: userError } = await supabase
      .from("usuarios")
      .select("id_usuario, nombre")
      .eq("correo", normalizedEmail)
      .limit(1)
      .maybeSingle();

    if (userError) {
      return jsonResponse(500, { error: "No se pudo comprobar el usuario" });
    }

    if (!user) {
      return jsonResponse(404, { error: "No existe ninguna cuenta con ese correo" });
    }

    const code = generateCode();
    const codeHash = await sha256(`${normalizedEmail}:${code}`);
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString();

    await supabase
      .from("email_verification_codes")
      .delete()
      .eq("email", normalizedEmail)
      .eq("purpose", "password_reset");

    const { error: insertError } = await supabase
      .from("email_verification_codes")
      .insert({
        email: normalizedEmail,
        purpose: "password_reset",
        code_hash: codeHash,
        expires_at: expiresAt,
      });

    if (insertError) {
      return jsonResponse(500, { error: "No se pudo guardar el codigo" });
    }

    const resendResponse = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${RESEND_API_KEY}`,
        "Content-Type": "application/json",
        "User-Agent": "fkaeh-supabase-edge-function/1.0",
      },
      body: JSON.stringify({
        from: EMAIL_FROM,
        to: [normalizedEmail],
        subject: `${APP_NAME} | Codigo de verificacion`,
        html: buildHtml(code),
      }),
    });

    if (!resendResponse.ok) {
      const resendText = await resendResponse.text();
      return jsonResponse(502, {
        error: `Resend fallo al enviar el email: ${resendText}`,
      });
    }

    return jsonResponse(200, { success: true });
  } catch (error) {
    return jsonResponse(500, { error: error instanceof Error ? error.message : "Error inesperado" });
  }
});
