// What each page loads.
import { innertube, TV } from "./innertube";
import { collectShelves, collectVideos, parseWatch, type Shelf, type Video } from "./parse";
import { getAccessToken, getSession } from "./session";

export async function signedInToken(): Promise<string | null> {
  const session = await getSession();
  if (!session) return null;
  return getAccessToken(session);
}

// Signed in: the account's TV home (Recommended and YouTube's other shelves). Signed out: YouTube's web home.
export async function homeShelves(accessToken: string | null): Promise<Shelf[]> {
  if (accessToken) {
    const data = await innertube("browse", { browseId: "FEwhat_to_watch" }, { client: TV, accessToken });
    const shelves = collectShelves(data);
    if (shelves.length) return shelves;
    return [{ title: "Recommended", videos: collectVideos(data) }];
  }
  const data = await innertube("browse", { browseId: "FEwhat_to_watch" });
  return [{ title: "Popular on YouTube", videos: collectVideos(data) }];
}

export async function subscriptionShelves(accessToken: string): Promise<Shelf[]> {
  const data = await innertube("browse", { browseId: "FEsubscriptions" }, { client: TV, accessToken });
  const shelves = collectShelves(data);
  if (shelves.length) return shelves;
  return [{ title: "Subscriptions", videos: collectVideos(data) }];
}

export async function search(query: string): Promise<Video[]> {
  const data = await innertube("search", { query });
  return collectVideos(data);
}

export async function watch(videoId: string) {
  return parseWatch(await innertube("next", { videoId }));
}
