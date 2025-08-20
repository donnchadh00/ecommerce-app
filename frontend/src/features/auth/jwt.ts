export type User = {
  id?: number;
  email?: string;
  roles?: string[];
  raw?: Record<string, any>;
};

export function parseJwt(token: string | null): User | null {
  if (!token) return null;
  try {
    const [, payload] = token.split(".");
    const claims = JSON.parse(atob(payload));
    const id = Number(claims.userId);
    const email = claims.sub;
    const roles = (claims.roles ?? claims.role)
      .toString()
      .split(/[ ,]/)
      .filter(Boolean);
    return { id: Number.isFinite(id) ? id : undefined, email, roles, raw: claims };
  } catch {
    return null;
  }
}
