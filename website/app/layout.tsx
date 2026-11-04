import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";
import { getSession } from "@/lib/session";

export const metadata: Metadata = {
  title: "Playlet",
  description: "Your YouTube, ad-free, in the browser.",
};

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  let signedIn = false;
  try {
    signedIn = (await getSession()) !== null;
  } catch {
    signedIn = false;
  }
  return (
    <html lang="en">
      <body>
        <header className="topbar">
          <Link href="/" className="logo">
            Playlet
          </Link>
          <nav>
            <Link href="/">Home</Link>
            {signedIn ? <Link href="/subscriptions">Subscriptions</Link> : null}
          </nav>
          <form action="/search" className="search">
            <input name="q" placeholder="Search" aria-label="Search" />
          </form>
          {signedIn ? (
            <form action="/api/auth/signout" method="post">
              <button className="button secondary">Sign out</button>
            </form>
          ) : (
            <Link href="/signin" className="button">
              Sign in
            </Link>
          )}
        </header>
        <main>{children}</main>
      </body>
    </html>
  );
}
