import { type User } from "./jwt";

export function userDisplayName(u: User | null): string {
  if (!u) return "";
  const name = u.email?.split("@")[0];
  return name || (u.id ? `User#${u.id}` : "User");
}
