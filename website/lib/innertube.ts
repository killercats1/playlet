// Talking to YouTube's internal API (Innertube) from the server: the TV client for the signed-in account's
// feeds (the same client Playlet uses on the Roku), the web client for search and watch pages.

export const TV = {
  name: "TVHTML5",
  nameId: "2",
  version: "7.20260611.00.00",
  userAgent:
    "Mozilla/5.0(SMART-TV; Linux; Tizen 4.0.0.2) AppleWebkit/605.1.15 (KHTML, like Gecko) SamsungBrowser/9.2 TV Safari/605.1.15",
};

export const WEB = {
  name: "WEB",
  nameId: "1",
  version: "2.20260612.01.00",
  userAgent:
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
};

type Client = typeof TV;

function context(client: Client) {
  const base = { hl: "en", gl: "US", clientName: client.name, clientVersion: client.version, userAgent: client.userAgent };
  if (client === TV) {
    return {
      client: {
        ...base,
        deviceMake: "Samsung",
        deviceModel: "SmartTV",
        osName: "Tizen",
        osVersion: "4.0.0.2",
        platform: "TV",
        clientFormFactor: "UNKNOWN_FORM_FACTOR",
        originalUrl: "https://www.youtube.com/tv",
        tvAppInfo: { appQuality: "TV_APP_QUALITY_FULL_ANIMATION" },
      },
    };
  }
  return { client: { ...base, platform: "DESKTOP", originalUrl: "https://www.youtube.com" } };
}

export class YouTubeError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

export async function innertube(
  path: string,
  body: Record<string, unknown>,
  options: { client?: Client; accessToken?: string } = {},
): Promise<any> {
  const client = options.client ?? WEB;
  const headers: Record<string, string> = {
    "content-type": "application/json",
    "user-agent": client.userAgent,
    "x-youtube-client-name": client.nameId,
    "x-youtube-client-version": client.version,
    origin: "https://www.youtube.com",
  };
  if (options.accessToken) {
    headers.authorization = `Bearer ${options.accessToken}`;
  }
  const response = await fetch(`https://www.youtube.com/youtubei/v1/${path}?prettyPrint=false`, {
    method: "POST",
    headers,
    body: JSON.stringify({ context: context(client), ...body }),
    cache: "no-store",
  });
  if (!response.ok) {
    throw new YouTubeError(`YouTube answered ${response.status} for ${path}`, response.status);
  }
  return response.json();
}
