// The signed-in session lives in one httpOnly cookie: the refresh token and the TV client identity it belongs
// to, encrypted with SESSION_SECRET (AES-256-GCM) so it can't be read or changed outside the server. Access
// tokens are kept in memory for the hour they last.
import { createCipheriv, createDecipheriv, createHash, randomBytes } from "crypto";
import { cookies } from "next/headers";
import { refreshAccessToken } from "./oauth";

export const SESSION_COOKIE = "playlet_session";

export type Session = { refreshToken: string; clientId: string; clientSecret: string };

function key(): Buffer {
  const secret = process.env.SESSION_SECRET;
  if (!secret || secret.length < 16) {
    throw new Error("SESSION_SECRET is not set (Vercel: Settings > Environment Variables, at least 16 characters)");
  }
  return createHash("sha256").update(secret).digest();
}

export function seal(value: unknown): string {
  const iv = randomBytes(12);
  const cipher = createCipheriv("aes-256-gcm", key(), iv);
  const data = Buffer.concat([cipher.update(JSON.stringify(value), "utf8"), cipher.final()]);
  return Buffer.concat([iv, cipher.getAuthTag(), data]).toString("base64url");
}

export function unseal<T>(sealed: string): T | null {
  try {
    const raw = Buffer.from(sealed, "base64url");
    const decipher = createDecipheriv("aes-256-gcm", key(), raw.subarray(0, 12));
    decipher.setAuthTag(raw.subarray(12, 28));
    const text = Buffer.concat([decipher.update(raw.subarray(28)), decipher.final()]).toString("utf8");
    return JSON.parse(text) as T;
  } catch {
    return null;
  }
}

export async function getSession(): Promise<Session | null> {
  const store = await cookies();
  const sealed = store.get(SESSION_COOKIE)?.value;
  if (!sealed) {
    return null;
  }
  return unseal<Session>(sealed);
}

export const sessionCookieOptions = {
  httpOnly: true,
  secure: true,
  sameSite: "lax" as const,
  path: "/",
  maxAge: 60 * 60 * 24 * 365,
};

const accessTokens = new Map<string, { token: string; expiresAt: number }>();

export async function getAccessToken(session: Session): Promise<string> {
  const cached = accessTokens.get(session.refreshToken);
  if (cached && cached.expiresAt - 60_000 > Date.now()) {
    return cached.token;
  }
  const fresh = await refreshAccessToken(
    { clientId: session.clientId, clientSecret: session.clientSecret },
    session.refreshToken,
  );
  accessTokens.set(session.refreshToken, { token: fresh.accessToken, expiresAt: Date.now() + fresh.expiresIn * 1000 });
  return fresh.accessToken;
}
