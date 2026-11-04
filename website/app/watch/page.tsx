import Player from "@/components/Player";
import { LoadError, VideoGrid } from "@/components/VideoGrid";
import { watch } from "@/lib/youtube";

export const dynamic = "force-dynamic";

export default async function WatchPage({ searchParams }: { searchParams: Promise<{ v?: string }> }) {
  const { v } = await searchParams;
  const videoId = (v ?? "").trim();
  if (!/^[A-Za-z0-9_-]{6,20}$/.test(videoId)) return <p className="empty">No video picked.</p>;

  let details: Awaited<ReturnType<typeof watch>> | null = null;
  let loadError: unknown = null;
  try {
    details = await watch(videoId);
  } catch (error) {
    loadError = error;
  }
  return (
    <div className="watch">
      <div className="watch-main">
        <Player videoId={videoId} />
        {details ? (
          <>
            <h1>{details.title}</h1>
            <div className="card-meta">
              {[details.author, details.views, details.date].filter(Boolean).join(" • ")}
            </div>
            {details.description ? <p className="description">{details.description}</p> : null}
          </>
        ) : (
          <LoadError error={loadError} />
        )}
      </div>
      {details?.related.length ? (
        <aside className="watch-related">
          <h2>Up next</h2>
          <VideoGrid videos={details.related} />
        </aside>
      ) : null}
    </div>
  );
}
