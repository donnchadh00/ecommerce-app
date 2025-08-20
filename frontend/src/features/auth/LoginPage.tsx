import { type FormEvent, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useLogin } from "./api.ts";
import { useAuth } from "./AuthContext";

export default function LoginPage() {
  const nav = useNavigate();
  const { login: saveToken } = useAuth();
  const login = useLogin();
  const [emailOrUsername, setId] = useState("");
  const [password, setPw] = useState("");
  const [showPw, setShowPw] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const token = await login.mutateAsync({ emailOrUsername, password });
    saveToken(token);
    nav("/");
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
          (login.error as Error).message || "Login failed"
        }</div>}
        <button type="submit" disabled={login.isPending}
          className="w-full rounded-lg bg-black px-4 py-2 text-white disabled:opacity-60">
          {login.isPending ? "Signing in…" : "Sign in"}
        </button>
      </form>
      <p className="mt-3 text-sm">
        No account? <Link to="/register" className="underline">Create one</Link>
      </p>
    </div>
  );
}
