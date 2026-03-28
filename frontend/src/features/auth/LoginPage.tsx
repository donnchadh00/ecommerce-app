import { type FormEvent, useState } from "react";
import { useLocation, useNavigate, Link, Navigate } from "react-router-dom";
import { useLogin } from "./api.ts";
import { useAuth } from "./useAuth";

const GUEST_EMAIL = "guest@demo.local";
const GUEST_PASSWORD = "Admin123!";

export default function LoginPage() {
  const nav = useNavigate();
  const location = useLocation();
  const { user, login: saveToken } = useAuth();
  const login = useLogin();
  const [emailOrUsername, setId] = useState("");
  const [password, setPw] = useState("");
  const [showPw, setShowPw] = useState(false);
  const destination = location.state && typeof location.state === "object" && "from" in location.state
    ? String(location.state.from || "/")
    : "/";

  if (user) {
    return <Navigate to={destination} replace />;
  }

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

  const errorMessage = login.isError
    ? emailOrUsername === GUEST_EMAIL
      ? "Guest access is not available in this environment."
      : (login.error as Error).message || "Login failed"
    : null;

  return (
    <div className="mx-auto grid max-w-5xl gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
      <section className="surface p-6 sm:p-8">
        <p className="eyebrow">Welcome back</p>
        <h1 className="mt-3 page-heading">Sign in to continue</h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
          Sign in to manage your cart, complete checkout, and review order activity.
        </p>

        <form onSubmit={onSubmit} className="mt-8 space-y-4">
          <div>
            <label htmlFor="login-id" className="label">
              Email or username
            </label>
            <input
              id="login-id"
              value={emailOrUsername}
              onChange={(e) => setId(e.target.value)}
              className="input-field mt-2"
              required
              autoComplete="username"
              placeholder="you@example.com"
            />
          </div>

          <div>
            <label htmlFor="login-password" className="label">
              Password
            </label>
            <div className="mt-2 flex">
              <input
                id="login-password"
                type={showPw ? "text" : "password"}
                value={password}
                onChange={(e) => setPw(e.target.value)}
                className="input-field rounded-r-none"
                required
                minLength={6}
                autoComplete="current-password"
                placeholder="Enter your password"
              />
              <button
                type="button"
                onClick={() => setShowPw((value) => !value)}
                className="rounded-r-xl border border-l-0 border-slate-300 bg-white px-4 text-sm font-semibold text-slate-600 transition hover:bg-slate-50 hover:text-slate-950 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500"
              >
                {showPw ? "Hide" : "Show"}
              </button>
            </div>
          </div>

          {errorMessage && (
            <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {errorMessage}
            </div>
          )}

          <button type="submit" disabled={login.isPending} className="button-primary w-full">
            {login.isPending ? "Signing in..." : "Sign in"}
          </button>
        </form>

        <p className="mt-6 text-sm text-slate-600">
          No account yet?{" "}
          <Link to="/register" className="font-semibold text-teal-700 transition hover:text-teal-800">
            Create one
          </Link>
        </p>
      </section>

      <aside className="space-y-4">
        <div className="surface p-6">
          <h2 className="section-heading text-base">Guest access</h2>
          <p className="mt-3 text-sm leading-6 text-slate-600">
            Use the guest account for a quick storefront walkthrough.
          </p>
          <div className="mt-4 rounded-2xl bg-slate-50 p-4 text-sm text-slate-600">
            <div>
              <span className="font-semibold text-slate-950">Email:</span> {GUEST_EMAIL}
            </div>
            <div className="mt-2">
              <span className="font-semibold text-slate-950">Password:</span> {GUEST_PASSWORD}
            </div>
          </div>
          <button
            type="button"
            onClick={onGuestLogin}
            disabled={login.isPending}
            className="button-secondary mt-4 w-full"
          >
            Continue as guest
          </button>
        </div>

        <div className="surface-muted p-6 text-sm leading-6 text-slate-600">
          Sign in to access your cart, checkout, and order history.
        </div>
      </aside>
    </div>
  );
}
