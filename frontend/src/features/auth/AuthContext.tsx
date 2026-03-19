import { useEffect, useMemo, useState } from "react";
import { AuthContext } from "./auth-context";
import { parseJwt } from "./jwt";

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

  return <AuthContext.Provider value={{ user, token, login, logout }}>{children}</AuthContext.Provider>;
}
