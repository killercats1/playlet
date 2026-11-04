// YouTube's TV sign-in (OAuth device flow), like Playlet on the Roku: the site shows a code, the user enters
// it at yt.be/activate, and the server trades it for tokens. The TV client's id and secret are read from
// YouTube's own TV page (they change from time to time).
import { TV } from "./innertube";

export type ClientIdentity = { clientId: string; clientSecret: string };

let cachedIdentity: { identity: ClientIdentity; at: number } | null = null;

export async function getClientIdentity(): Promise<ClientIdentity> {
  if (cachedIdentity && Date.now() - cachedIdentity.at < 6 * 3600 * 1000) {
    return cachedIdentity.identity;
  }
  const headers = { "user-agent": TV.userAgent, referer: "https://www.youtube.com/tv", "accept-language": "en-US" };
  const page = await (await fetch("https://www.youtube.com/tv", { headers, cache: "no-store" })).text();
  const script = page.match(/<script\s+id="base-js"\s+src="([^"]+)"[^>]*><\/script>/);
  if (!script) {
    throw new Error("Couldn't find YouTube's TV script");
  }
  const baseJs = await (await fetch(`https://www.youtube.com${script[1]}`, { headers, cache: "no-store" })).text();
  const ids = baseJs.match(/clientId:"([^"]+)",[^"]*?:"([^"]+)"/);
  if (!ids) {
    throw new Error("Couldn't find the TV client id");
  }
  const identity = { clientId: ids[1], clientSecret: ids[2] };
  cachedIdentity = { identity, at: Date.now() };
  return identity;
}

export async function startDeviceFlow(identity: ClientIdentity) {
  const response = await fetch("https://www.youtube.com/o/oauth2/device/code", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      client_id: identity.clientId,
      scope: "http://gdata.youtube.com https://www.googleapis.com/auth/youtube-paid-content",
      device_id: crypto.randomUUID(),
      device_model: "ytlr::",
    }),
    cache: "no-store",
  });
  const data = await response.json();
  if (!response.ok || data.error_code || !data.device_code) {
    throw new Error(`Couldn't start sign-in: ${JSON.stringify(data)}`);
  }
  return {
    deviceCode: data.device_code as string,
    userCode: data.user_code as string,
    verificationUrl: (data.verification_url as string) || "https://www.youtube.com/activate",
    interval: Number(data.interval) || 5,
    expiresIn: Number(data.expires_in) || 1800,
  };
}

type TokenResult =
  | { status: "ok"; accessToken: string; refreshToken: string; expiresIn: number }
  | { status: "pending" | "slow_down" | "expired" | "denied" }
  | { status: "error"; message: string };

async function tokenRequest(payload: Record<string, string>): Promise<any> {
  const response = await fetch("https://www.youtube.com/o/oauth2/token", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(payload),
    cache: "no-store",
  });
  return response.json();
}

export async function pollDeviceToken(identity: ClientIdentity, deviceCode: string): Promise<TokenResult> {
  const data = await tokenRequest({
    client_id: identity.clientId,
    client_secret: identity.clientSecret,
    code: deviceCode,
    grant_type: "http://oauth.net/grant_type/device/1.0",
  });
  if (data.access_token && data.refresh_token) {
    return { status: "ok", accessToken: data.access_token, refreshToken: data.refresh_token, expiresIn: Number(data.expires_in) || 3600 };
  }
  switch (data.error) {
    case "authorization_pending":
      return { status: "pending" };
    case "slow_down":
      return { status: "slow_down" };
    case "expired_token":
      return { status: "expired" };
    case "access_denied":
      return { status: "denied" };
    default:
      return { status: "error", message: String(data.error ?? "unknown error") };
  }
}

export async function refreshAccessToken(identity: ClientIdentity, refreshToken: string) {
  const data = await tokenRequest({
    client_id: identity.clientId,
    client_secret: identity.clientSecret,
    refresh_token: refreshToken,
    grant_type: "refresh_token",
  });
  if (!data.access_token) {
    throw new Error(`Couldn't refresh the sign-in: ${data.error ?? "unknown error"}`);
  }
  return { accessToken: data.access_token as string, expiresIn: Number(data.expires_in) || 3600 };
}
