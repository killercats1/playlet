import { NextResponse } from "next/server";
import { pollDeviceToken } from "@/lib/oauth";
import { seal, SESSION_COOKIE, sessionCookieOptions, unseal } from "@/lib/session";

export const dynamic = "force-dynamic";

type Pending = { deviceCode: string; clientId: string; clientSecret: string; expiresAt: number };

// Asks once whether the code was entered; on success the session cookie is set.
export async function POST(request: Request) {
  const { pending } = await request.json().catch(() => ({ pending: "" }));
  const state = typeof pending === "string" ? unseal<Pending>(pending) : null;
  if (!state) {
    return NextResponse.json({ status: "error", message: "Invalid sign-in state" }, { status: 400 });
  }
  if (Date.now() > state.expiresAt) {
    return NextResponse.json({ status: "expired" });
  }
  const result = await pollDeviceToken(state, state.deviceCode);
  if (result.status !== "ok") {
    return NextResponse.json(result);
  }
  const response = NextResponse.json({ status: "ok" });
  response.cookies.set(
    SESSION_COOKIE,
    seal({ refreshToken: result.refreshToken, clientId: state.clientId, clientSecret: state.clientSecret }),
    sessionCookieOptions,
  );
  return response;
}
