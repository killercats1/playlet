import { LoadError, VideoGrid } from "@/components/VideoGrid";
import { search } from "@/lib/youtube";

export const dynamic = "force-dynamic";

export default async function SearchPage({ searchParams }: { searchParams: Promise<{ q?: string }> }) {
  const { q } = await searchParams;
  const query = (q ?? "").trim();
  if (!query) return <p className="empty">Type something in the search box.</p>;
  try {
    const videos = await search(query);
    return (
      <section className="shelf">
        <h2>Results for “{query}”</h2>
        <VideoGrid videos={videos} />
      </section>
    );
  } catch (error) {
    return <LoadError error={error} />;
  }
}
