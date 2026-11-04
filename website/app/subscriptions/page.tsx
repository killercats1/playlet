import { redirect } from "next/navigation";
import { LoadError, Shelves } from "@/components/VideoGrid";
import { signedInToken, subscriptionShelves } from "@/lib/youtube";

export const dynamic = "force-dynamic";

export default async function Subscriptions() {
  let token: string | null = null;
  try {
    token = await signedInToken();
  } catch (error) {
    return <LoadError error={error} />;
  }
  if (!token) redirect("/signin");
  try {
    return <Shelves shelves={await subscriptionShelves(token)} />;
  } catch (error) {
    return <LoadError error={error} />;
  }
}
