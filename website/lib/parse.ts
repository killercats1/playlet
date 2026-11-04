// Turns YouTube's responses into simple video lists. Loose on purpose: it looks for the video renderers YouTube
// uses (TV tiles, the web's videoRenderer / lockupViewModel, Shorts) anywhere in the response, since their
// nesting changes often.

export type Video = {
  id: string;
  title: string;
  author?: string;
  authorId?: string;
  meta?: string;
  duration?: string;
  short?: boolean;
  thumbnail: string;
};

export type Shelf = { title: string; videos: Video[] };

export function textOf(value: any): string {
  if (!value) return "";
  if (typeof value === "string") return value;
  if (typeof value.simpleText === "string") return value.simpleText;
  if (typeof value.content === "string") return value.content;
  if (Array.isArray(value.runs)) return value.runs.map((run: any) => run?.text ?? "").join("");
  return "";
}

function thumbnailFor(id: string): string {
  return `https://i.ytimg.com/vi/${id}/mqdefault.jpg`;
}

function channelIdOf(value: any): string | undefined {
  const runs = value?.runs;
  if (!Array.isArray(runs)) return undefined;
  for (const run of runs) {
    const id = run?.navigationEndpoint?.browseEndpoint?.browseId;
    if (typeof id === "string" && id.startsWith("UC")) return id;
  }
  return undefined;
}

function fromTile(tile: any): Video | null {
  const type = tile?.contentType;
  if (type !== "TILE_CONTENT_TYPE_VIDEO" && type !== "TILE_CONTENT_TYPE_SHORTS") return null;
  const id = tile.contentId;
  if (typeof id !== "string") return null;
  const lines: string[] = [];
  let authorId: string | undefined;
  for (const line of tile.metadata?.tileMetadataRenderer?.lines ?? []) {
    const parts: string[] = [];
    for (const item of line?.lineRenderer?.items ?? []) {
      const text = textOf(item?.lineItemRenderer?.text);
      if (text && text !== "•") parts.push(text);
      authorId = authorId ?? channelIdOf(item?.lineItemRenderer?.text);
    }
    if (parts.length) lines.push(parts.join(" "));
  }
  let duration: string | undefined;
  for (const overlay of tile.header?.tileHeaderRenderer?.thumbnailOverlays ?? []) {
    const text = textOf(overlay?.thumbnailOverlayTimeStatusRenderer?.text);
    if (text) duration = text;
  }
  return {
    id,
    title: textOf(tile.metadata?.tileMetadataRenderer?.title),
    author: lines[0],
    authorId,
    meta: lines.slice(1).join(" • ") || undefined,
    duration,
    short: type === "TILE_CONTENT_TYPE_SHORTS",
    thumbnail: thumbnailFor(id),
  };
}

function fromVideoRenderer(renderer: any): Video | null {
  const id = renderer?.videoId;
  if (typeof id !== "string") return null;
  const byline = renderer.ownerText ?? renderer.shortBylineText ?? renderer.longBylineText;
  const meta = [textOf(renderer.shortViewCountText) || textOf(renderer.viewCountText), textOf(renderer.publishedTimeText)]
    .filter(Boolean)
    .join(" • ");
  return {
    id,
    title: textOf(renderer.title) || textOf(renderer.headline),
    author: textOf(byline) || undefined,
    authorId: channelIdOf(byline),
    meta: meta || undefined,
    duration: textOf(renderer.lengthText) || undefined,
    thumbnail: thumbnailFor(id),
  };
}

function fromLockup(lockup: any): Video | null {
  if (lockup?.contentType !== "LOCKUP_CONTENT_TYPE_VIDEO" || typeof lockup.contentId !== "string") return null;
  const metadata = lockup.metadata?.lockupMetadataViewModel;
  const rows: string[] = [];
  for (const row of metadata?.metadata?.contentMetadataViewModel?.metadataRows ?? []) {
    const parts = (row?.metadataParts ?? []).map((part: any) => textOf(part?.text)).filter(Boolean);
    if (parts.length) rows.push(parts.join(" • "));
  }
  let duration: string | undefined;
  for (const overlay of lockup.contentImage?.thumbnailViewModel?.overlays ?? []) {
    for (const badge of overlay?.thumbnailOverlayBadgeViewModel?.thumbnailBadges ?? []) {
      const text = textOf(badge?.thumbnailBadgeViewModel?.text);
      if (text) duration = text;
    }
  }
  return {
    id: lockup.contentId,
    title: textOf(metadata?.title),
    author: rows[0],
    meta: rows.slice(1).join(" • ") || undefined,
    duration,
    thumbnail: thumbnailFor(lockup.contentId),
  };
}

function fromShort(renderer: any): Video | null {
  const id = renderer?.videoId ?? renderer?.onTap?.innertubeCommand?.reelWatchEndpoint?.videoId;
  if (typeof id !== "string") return null;
  return {
    id,
    title: textOf(renderer.headline) || textOf(renderer.overlayMetadata?.primaryText),
    meta: textOf(renderer.viewCountText) || textOf(renderer.overlayMetadata?.secondaryText) || undefined,
    short: true,
    thumbnail: `https://i.ytimg.com/vi/${id}/oar2.jpg`,
  };
}

const RENDERERS: Record<string, (value: any) => Video | null> = {
  tileRenderer: fromTile,
  videoRenderer: fromVideoRenderer,
  compactVideoRenderer: fromVideoRenderer,
  gridVideoRenderer: fromVideoRenderer,
  lockupViewModel: fromLockup,
  reelItemRenderer: fromShort,
  shortsLockupViewModel: fromShort,
};

export function collectVideos(data: any, videos: Video[] = [], seen = new Set<string>(), depth = 0): Video[] {
  if (depth > 40 || data === null || typeof data !== "object") return videos;
  if (Array.isArray(data)) {
    for (const child of data) collectVideos(child, videos, seen, depth + 1);
    return videos;
  }
  for (const [key, value] of Object.entries(data)) {
    const make = RENDERERS[key];
    if (make) {
      const video = make(value);
      if (video && video.title && !seen.has(video.id)) {
        seen.add(video.id);
        videos.push(video);
      }
      continue;
    }
    collectVideos(value, videos, seen, depth + 1);
  }
  return videos;
}

// The TV home / subscriptions page: one shelf per shelfRenderer, with YouTube's titles.
export function collectShelves(data: any): Shelf[] {
  const shelves: Shelf[] = [];
  const seen = new Set<string>();
  const walk = (node: any, depth: number) => {
    if (depth > 30 || node === null || typeof node !== "object") return;
    if (Array.isArray(node)) {
      node.forEach((child) => walk(child, depth + 1));
      return;
    }
    if (node.shelfRenderer) {
      const shelf = node.shelfRenderer;
      const header = shelf.headerRenderer?.shelfHeaderRenderer;
      const title = textOf(header?.title) || textOf(header?.avatarLockup?.avatarLockupRenderer?.title) || textOf(shelf.title);
      const videos = collectVideos(shelf.content, [], seen);
      if (videos.length) shelves.push({ title, videos });
      return;
    }
    Object.values(node).forEach((child) => walk(child, depth + 1));
  };
  walk(data, 0);
  return shelves;
}

export type WatchDetails = {
  title: string;
  author: string;
  authorId?: string;
  views: string;
  date: string;
  description: string;
  related: Video[];
};

export function parseWatch(data: any): WatchDetails {
  const contents = data?.contents?.twoColumnWatchNextResults?.results?.results?.contents ?? [];
  const details: WatchDetails = { title: "", author: "", views: "", date: "", description: "", related: [] };
  for (const item of contents) {
    const primary = item?.videoPrimaryInfoRenderer;
    if (primary) {
      details.title = textOf(primary.title);
      details.views = textOf(primary.viewCount?.videoViewCountRenderer?.viewCount);
      details.date = textOf(primary.dateText);
    }
    const secondary = item?.videoSecondaryInfoRenderer;
    if (secondary) {
      const owner = secondary.owner?.videoOwnerRenderer;
      details.author = textOf(owner?.title);
      details.authorId = owner?.navigationEndpoint?.browseEndpoint?.browseId;
      details.description = secondary.attributedDescription?.content ?? textOf(secondary.description);
    }
  }
  details.related = collectVideos(data?.contents?.twoColumnWatchNextResults?.secondaryResults).slice(0, 24);
  return details;
}
