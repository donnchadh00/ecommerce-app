import { type FormEvent, useState } from "react";
import { useNavigate, Link, Navigate } from "react-router-dom";
import { useRegister } from "./api";
import { useAuth } from "./useAuth";

export default function RegisterPage() {
  const nav = useNavigate();
  const { user } = useAuth();
  const reg = useRegister();
  const [email, setEmail] = useState("");
  const [password, setPw] = useState("");
  const [confirm, setConfirm] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);

  if (user) {
    return <Navigate to="/account" replace />;
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setValidationError(null);

    if (password !== confirm) {
      setValidationError("Passwords do not match.");
      return;
    }

    await reg.mutateAsync({ email, password });
    nav("/login");
  };

  const errorMessage = validationError || (reg.isError ? (reg.error as Error).message || "Registration failed" : null);

  return (
    <div className="mx-auto grid max-w-5xl gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
      <section className="surface p-6 sm:p-8">
        <p className="eyebrow">Create account</p>
        <h1 className="mt-3 page-heading">Set up a new login</h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
          Create an account to save cart items, complete checkout, and track your orders.
        </p>

        <form onSubmit={onSubmit} className="mt-8 space-y-4">
          <div>
            <label htmlFor="register-email" className="label">
              Email
            </label>
            <input
              id="register-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="input-field mt-2"
              required
              autoComplete="email"
              placeholder="you@example.com"
            />
          </div>

          <div>
            <label htmlFor="register-password" className="label">
              Password
            </label>
            <input
              id="register-password"
              type="password"
              value={password}
              onChange={(e) => {
                setPw(e.target.value);
                setValidationError(null);
              }}
              className="input-field mt-2"
              required
              minLength={6}
              autoComplete="new-password"
              placeholder="Minimum 6 characters"
            />
          </div>

          <div>
            <label htmlFor="register-confirm" className="label">
              Confirm password
            </label>
            <input
              id="register-confirm"
              type="password"
              value={confirm}
              onChange={(e) => {
                setConfirm(e.target.value);
                setValidationError(null);
              }}
              className="input-field mt-2"
              required
              minLength={6}
              autoComplete="new-password"
              placeholder="Repeat your password"
            />
          </div>

          {errorMessage && (
            <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {errorMessage}
            </div>
          )}

          <button type="submit" disabled={reg.isPending} className="button-primary w-full">
            {reg.isPending ? "Creating account..." : "Create account"}
          </button>
        </form>

        <p className="mt-6 text-sm text-slate-600">
          Already have an account?{" "}
          <Link to="/login" className="font-semibold text-teal-700 transition hover:text-teal-800">
            Sign in
          </Link>
        </p>
      </section>

      <aside className="space-y-4">
        <div className="surface p-6">
          <h2 className="section-heading text-base">Account access</h2>
          <p className="mt-3 text-sm leading-6 text-slate-600">
            Create an account to save cart items, complete checkout, and view your orders.
          </p>
        </div>
      </aside>
    </div>
  );
}
