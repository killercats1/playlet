import Link from "next/link";
import { LoadError, Shelves } from "@/components/VideoGrid";
import { homeShelves, signedInToken } from "@/lib/youtube";

export const dynamic = "force-dynamic";

export default async function Home() {
  let token: string | null = null;
  try {
    token = await signedInToken();
  } catch (error) {
    return (
      <>
        <LoadError error={error} />
        <p>
          Your sign-in may have expired: <Link href="/signin">sign in again</Link>.
        </p>
      </>
    );
  }
  try {
    const shelves = await homeShelves(token);
    return (
      <>
        {!token ? (
          <div className="notice">
            <Link href="/signin">Sign in with your YouTube account</Link> to see your own recommendations and
            subscriptions.
          </div>
        ) : null}
        <Shelves shelves={shelves} />
      </>
    );
  } catch (error) {
    return <LoadError error={error} />;
  }
}
