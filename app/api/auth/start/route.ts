import { NextResponse } from "next/server";
import { getClientIdentity, startDeviceFlow } from "@/lib/oauth";
import { seal } from "@/lib/session";

export const dynamic = "force-dynamic";

// Starts a sign-in: the code to enter at yt.be/activate, and a sealed handle the page polls with.
export async function POST() {
  try {
    const identity = await getClientIdentity();
    const flow = await startDeviceFlow(identity);
    const pending = seal({ deviceCode: flow.deviceCode, ...identity, expiresAt: Date.now() + flow.expiresIn * 1000 });
    return NextResponse.json({ userCode: flow.userCode, verificationUrl: flow.verificationUrl, interval: flow.interval, pending });
  } catch (error) {
    return NextResponse.json({ error: (error as Error).message }, { status: 502 });
  }
}
