import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { parseJwt, type User } from "./jwt.ts";

type AuthCtx = {
  user: User | null;
  token: string | null;
  login: (token: string) => void;
  logout: () => void;
};

const Ctx = createContext<AuthCtx | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem("token"));
  const user = useMemo(() => parseJwt(token), [token]);

  const login = (t: string) => {
    localStorage.setItem("token", t);
    setToken(t);
  };
  const logout = () => {
    localStorage.removeItem("token");
    setToken(null);
  };

  // keep state in sync if another tab logs out
  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === "token") setToken(localStorage.getItem("token"));
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  return <Ctx.Provider value={{ user, token, login, logout }}>{children}</Ctx.Provider>;
}

export function useAuth() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
