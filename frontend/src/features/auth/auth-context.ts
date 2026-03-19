import { createContext } from "react";
import type { User } from "./jwt";

export type AuthCtx = {
  user: User | null;
  token: string | null;
  login: (token: string) => void;
  logout: () => void;
};

export const AuthContext = createContext<AuthCtx | undefined>(undefined);
