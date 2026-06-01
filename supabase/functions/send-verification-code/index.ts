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

  try {
    const { email, purpose: rawPurpose } = await req.json();
    const normalizedEmail = String(email ?? "").trim().toLowerCase();
    const purpose = String(rawPurpose ?? "password_reset").trim();

    if (!normalizedEmail || !normalizedEmail.includes("@")) {
      return jsonResponse(400, { error: "Correo no valido" });
    }

    const { data: user, error: userError } = await supabase
      .from("usuarios")
      .select("id_usuario, nombre, activo")
      .eq("correo", normalizedEmail)
      .limit(1)
      .maybeSingle();

    if (userError) {
      return jsonResponse(500, { error: "No se pudo comprobar el usuario" });
    }

    if (!user) {
      return jsonResponse(404, { error: "No existe ninguna cuenta con ese correo" });
    }

    // CÓDIGO DEMO FIJO
    const demoCode = "123456";
    const codeHash = await sha256(`${normalizedEmail}:${demoCode}`);
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString();

    await supabase
      .from("email_verification_codes")
      .delete()
      .eq("email", normalizedEmail)
      .eq("purpose", purpose);

    const { error: insertError } = await supabase
      .from("email_verification_codes")
      .insert({
        email: normalizedEmail,
        purpose,
        code_hash: codeHash,
        expires_at: expiresAt,
      });

    if (insertError) {
      return jsonResponse(500, { error: "No se pudo guardar el codigo" });
    }

    // Devuelve el código al usuario (solo en desarrollo)
    return jsonResponse(200, {
      success: true,
      demoCode: demoCode, // ← El usuario recibe el código aquí
      message: "Código de verificación: 123456"
    });
  } catch (error) {
    return jsonResponse(500, { error: error instanceof Error ? error.message : "Error inesperado" });
  }
});