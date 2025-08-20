import { type FormEvent, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useRegister } from "./api";

export default function RegisterPage() {
  const nav = useNavigate();
  const reg = useRegister();
  const [email, setEmail] = useState("");
  const [password, setPw] = useState("");
  const [confirm, setConfirm] = useState("");

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (password !== confirm) throw new Error("Passwords do not match");
    await reg.mutateAsync({ email, password });
    nav("/login");
  };

  return (
    <div className="mx-auto max-w-sm mt-10">
      <h1 className="text-2xl font-semibold mb-4">Create account</h1>
      <form onSubmit={onSubmit} className="space-y-3">
        <div>
          <label className="block text-sm">Email</label>
          <input type="email" value={email} onChange={e => setEmail(e.target.value)}
            className="mt-1 w-full rounded-lg border p-2" required />
        </div>
        <div>
          <label className="block text-sm">Password</label>
          <input type="password" value={password} onChange={e => setPw(e.target.value)}
            className="mt-1 w-full rounded-lg border p-2" required minLength={6} />
        </div>
        <div>
          <label className="block text-sm">Confirm password</label>
          <input type="password" value={confirm} onChange={e => setConfirm(e.target.value)}
            className="mt-1 w-full rounded-lg border p-2" required minLength={6} />
        </div>
        {reg.isError && <div className="text-red-600 text-sm">{
          (reg.error as Error).message || "Registration failed"
        }</div>}
        <button type="submit" disabled={reg.isPending}
          className="w-full rounded-lg bg-black px-4 py-2 text-white disabled:opacity-60">
          {reg.isPending ? "Creating…" : "Create account"}
        </button>
      </form>
      <p className="mt-3 text-sm">
        Already have an account? <Link to="/login" className="underline">Sign in</Link>
      </p>
    </div>
  );
}
