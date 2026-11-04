import Link from "next/link";
import type { Shelf, Video } from "@/lib/parse";

export function VideoCard({ video }: { video: Video }) {
  return (
    <Link href={`/watch?v=${encodeURIComponent(video.id)}`} className={video.short ? "card short" : "card"}>
      <div className="thumb">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={video.thumbnail} alt="" loading="lazy" />
        {video.duration ? <span className="duration">{video.duration}</span> : null}
      </div>
      <div className="card-title">{video.title}</div>
      {video.author ? <div className="card-meta">{video.author}</div> : null}
      {video.meta ? <div className="card-meta">{video.meta}</div> : null}
    </Link>
  );
}

export function VideoGrid({ videos }: { videos: Video[] }) {
  if (!videos.length) return <p className="empty">Nothing here right now.</p>;
  return (
    <div className="grid">
      {videos.map((video) => (
        <VideoCard key={video.id} video={video} />
      ))}
    </div>
  );
}

export function Shelves({ shelves }: { shelves: Shelf[] }) {
  return (
    <>
      {shelves.map((shelf, index) => (
        <section key={`${shelf.title}-${index}`} className="shelf">
          {shelf.title ? <h2>{shelf.title}</h2> : null}
          <VideoGrid videos={shelf.videos} />
        </section>
      ))}
    </>
  );
}

export function LoadError({ error }: { error: unknown }) {
  return (
    <div className="notice">
      <strong>YouTube didn&apos;t answer this one.</strong>
      <p>It sometimes refuses requests from cloud servers for a while. Try again in a minute.</p>
      <p className="small">{(error as Error)?.message}</p>
    </div>
  );
}
