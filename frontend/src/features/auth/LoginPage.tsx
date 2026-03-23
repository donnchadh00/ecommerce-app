import { type FormEvent, useState } from "react";
import { useLocation, useNavigate, Link } from "react-router-dom";
import { useLogin } from "./api.ts";
import { useAuth } from "./useAuth";

const GUEST_EMAIL = "guest@demo.local";
const GUEST_PASSWORD = "Admin123!";

export default function LoginPage() {
  const nav = useNavigate();
  const location = useLocation();
  const { login: saveToken } = useAuth();
  const login = useLogin();
  const [emailOrUsername, setId] = useState("");
  const [password, setPw] = useState("");
  const [showPw, setShowPw] = useState(false);
  const destination = location.state && typeof location.state === "object" && "from" in location.state
    ? String(location.state.from || "/")
    : "/";

  const completeLogin = async (nextEmailOrUsername: string, nextPassword: string) => {
    const token = await login.mutateAsync({ emailOrUsername: nextEmailOrUsername, password: nextPassword });
    saveToken(token);
    nav(destination, { replace: true });
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    await completeLogin(emailOrUsername, password);
  };

  const onGuestLogin = async () => {
    try {
      await completeLogin(GUEST_EMAIL, GUEST_PASSWORD);
    } catch {
      setId(GUEST_EMAIL);
      setPw(GUEST_PASSWORD);
    }
  };

  return (
    <div className="mx-auto max-w-sm mt-10">
      <h1 className="text-2xl font-semibold mb-4">Sign in</h1>
      <form onSubmit={onSubmit} className="space-y-3">
        <div>
          <label className="block text-sm">Email or username</label>
          <input value={emailOrUsername} onChange={e => setId(e.target.value)}
            className="mt-1 w-full rounded-lg border p-2" required />
        </div>
        <div>
          <label className="block text-sm">Password</label>
          <div className="mt-1 flex">
            <input type={showPw ? "text" : "password"} value={password} onChange={e => setPw(e.target.value)}
              className="w-full rounded-l-lg border p-2" required minLength={6} />
            <button type="button" onClick={() => setShowPw(s => !s)}
              className="rounded-r-lg border border-l-0 px-3">{showPw ? "Hide" : "Show"}</button>
          </div>
        </div>
        {login.isError && <div className="text-red-600 text-sm">{
          emailOrUsername === GUEST_EMAIL
            ? "Guest account is not available in this environment."
            : (login.error as Error).message || "Login failed"
        }</div>}
        <button type="submit" disabled={login.isPending}
          className="w-full rounded-lg bg-black px-4 py-2 text-white disabled:opacity-60">
          {login.isPending ? "Signing in…" : "Sign in"}
        </button>
      </form>
      <button
        type="button"
        onClick={onGuestLogin}
        disabled={login.isPending}
        className="mt-3 w-full rounded-lg border px-4 py-2 hover:bg-gray-50 disabled:opacity-60"
      >
        Continue as guest
      </button>
      <p className="mt-3 text-sm">
        No account? <Link to="/register" className="underline">Create one</Link>
      </p>
    </div>
  );
}
