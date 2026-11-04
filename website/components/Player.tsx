"use client";
// Ad-free playback through public Invidious servers (their proxied streams play in any browser), one after the
// other when one fails; YouTube's own embedded player as the last resort.
import { useState } from "react";

const INSTANCES = [
  "https://inv.nadeko.net",
  "https://invidious.nerdvpn.de",
  "https://yewtu.be",
  "https://invidious.jing.rocks",
  "https://invidious.privacyredirect.com",
];

export default function Player({ videoId }: { videoId: string }) {
  const [index, setIndex] = useState(0);
  const [useEmbed, setUseEmbed] = useState(false);

  if (useEmbed || index >= INSTANCES.length) {
    return (
      <div className="player">
        <iframe
          src={`https://www.youtube-nocookie.com/embed/${encodeURIComponent(videoId)}?autoplay=1`}
          title="YouTube player"
          allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
          allowFullScreen
        />
        {index >= INSTANCES.length ? (
          <p className="small">The ad-free servers couldn&apos;t play this one, so it&apos;s YouTube&apos;s own player.</p>
        ) : null}
      </div>
    );
  }

  const instance = INSTANCES[index];
  return (
    <div className="player">
      <video
        key={instance}
        src={`${instance}/latest_version?id=${encodeURIComponent(videoId)}&itag=18&local=true`}
        controls
        autoPlay
        playsInline
        onError={() => setIndex(index + 1)}
      />
      <p className="small">
        Playing through {new URL(instance).hostname} ·{" "}
        <button className="link" onClick={() => setIndex(index + 1)}>
          try another server
        </button>{" "}
        ·{" "}
        <button className="link" onClick={() => setUseEmbed(true)}>
          use YouTube&apos;s player
        </button>
      </p>
    </div>
  );
}
