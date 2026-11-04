"use client";
// The TV-style sign-in: show the code, the user enters it at yt.be/activate (on any device), we poll until done.
import { useEffect, useRef, useState } from "react";

type Start = { userCode: string; verificationUrl: string; interval: number; pending: string };

export default function SignIn() {
  const [start, setStart] = useState<Start | null>(null);
  const [message, setMessage] = useState("Getting a code…");
  const stopped = useRef(false);

  useEffect(() => {
    stopped.current = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    (async () => {
      const response = await fetch("/api/auth/start", { method: "POST" });
      const data = await response.json();
      if (!response.ok || data.error) {
        setMessage(`Couldn't start the sign-in: ${data.error ?? response.status}`);
        return;
      }
      setStart(data);
      setMessage("Waiting for you to enter the code…");
      let wait = Math.max(5, data.interval) * 1000;
      const poll = async () => {
        if (stopped.current) return;
        const result = await (await fetch("/api/auth/poll", {
          method: "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({ pending: data.pending }),
        })).json();
        if (result.status === "ok") {
          setMessage("Signed in! Taking you home…");
          window.location.href = "/";
          return;
        }
        if (result.status === "expired" || result.status === "denied" || result.status === "error") {
          setMessage(result.status === "denied" ? "Sign-in was cancelled." : "The code expired. Reload the page for a new one.");
          return;
        }
        if (result.status === "slow_down") wait += 3000;
        timer = setTimeout(poll, wait);
      };
      timer = setTimeout(poll, wait);
    })();
    return () => {
      stopped.current = true;
      if (timer) clearTimeout(timer);
    };
  }, []);

  return (
    <div className="signin">
      <h1>Sign in with YouTube</h1>
      {start ? (
        <>
          <p>
            On your phone or computer, go to{" "}
            <a href={start.verificationUrl} target="_blank" rel="noreferrer">
              {start.verificationUrl.replace(/^https?:\/\//, "")}
            </a>{" "}
            and enter:
          </p>
          <div className="code">{start.userCode}</div>
        </>
      ) : null}
      <p className="small">{message}</p>
      <p className="small">
        Same sign-in as Playlet on your TV. Your sign-in is kept in an encrypted cookie in this browser only.
      </p>
    </div>
  );
}
