import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

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

async function sha256(input: string) {
  const data = new TextEncoder().encode(input);
  const hashBuffer = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(hashBuffer))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return jsonResponse(405, { error: "Method not allowed" });
  }

  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) {
    return jsonResponse(500, { error: "Faltan variables de entorno en la funcion" });
  }

  try {
    const { email, code, newPassword } = await req.json();
    const normalizedEmail = String(email ?? "").trim().toLowerCase();
    const normalizedCode = String(code ?? "").trim();
    const password = String(newPassword ?? "");

    if (!normalizedEmail || !normalizedCode || !password) {
      return jsonResponse(400, { error: "Faltan datos obligatorios" });
    }

    if (password.length < 4) {
      return jsonResponse(400, { error: "La contrasena debe tener al menos 4 caracteres" });
    }

    const codeHash = await sha256(`${normalizedEmail}:${normalizedCode}`);

    const { data: verification, error: verificationError } = await supabase
      .from("email_verification_codes")
      .select("id, expires_at, used_at")
      .eq("email", normalizedEmail)
      .eq("purpose", "password_reset")
      .eq("code_hash", codeHash)
      .limit(1)
      .maybeSingle();

    if (verificationError) {
      return jsonResponse(500, { error: "No se pudo validar el codigo" });
    }

    if (!verification) {
      return jsonResponse(400, { error: "Codigo invalido" });
    }

    if (verification.used_at) {
      return jsonResponse(400, { error: "Este codigo ya fue utilizado" });
    }

    if (new Date(verification.expires_at).getTime() < Date.now()) {
      return jsonResponse(400, { error: "El codigo ha caducado" });
    }

    const { error: updatePasswordError } = await supabase
      .from("usuarios")
      .update({ contrasena: password })
      .eq("correo", normalizedEmail);

    if (updatePasswordError) {
      return jsonResponse(500, { error: "No se pudo actualizar la contrasena" });
    }

    const { error: updateCodeError } = await supabase
      .from("email_verification_codes")
      .update({ used_at: new Date().toISOString() })
      .eq("id", verification.id);

    if (updateCodeError) {
      return jsonResponse(500, { error: "La contrasena se cambio, pero no se pudo cerrar el codigo" });
    }

    return jsonResponse(200, { success: true });
  } catch (error) {
    return jsonResponse(500, { error: error instanceof Error ? error.message : "Error inesperado" });
  }
});
