# Playlet website

Playlet in the browser: your YouTube home and subscriptions (signed in the same way as Playlet on the TV, with
a code at yt.be/activate), search, and an ad-free player that streams through public Invidious servers (with
YouTube's own player as the fallback).

## Deploy on Vercel

1. vercel.com → Add New → Project → import `killercats1/playlet`.
2. Production branch = `website` (Settings → Environments → Production → Branch Tracking; older layouts: Settings → Git). This branch holds only the site.
3. Root Directory: leave it empty. Framework: Next.js (`vercel.json` says so).
4. **Environment Variables**: `SESSION_SECRET` = a long random string (it encrypts the sign-in cookie).
5. Deploy.

## Notes

- YouTube sometimes refuses requests from cloud servers (Vercel's included) for a while; pages then say so.
- Video links from YouTube only work for the server that asked for them, so playback goes through Invidious
  servers instead; when they're all down, YouTube's embedded player is used (which can show ads).
- Run locally: `npm install`, `SESSION_SECRET=something-long npm run dev`.
