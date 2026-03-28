import { Link } from "react-router-dom";
import { useAuth } from "./useAuth";
import { userDisplayName } from "./displayName";
import { useState, useRef, useEffect } from "react";

export default function AuthMenu() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (!ref.current) return;
      if (!ref.current.contains(e.target as Node)) setOpen(false);
    }

    function onEscape(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }

    document.addEventListener("click", onDocClick);
    document.addEventListener("keydown", onEscape);

    return () => {
      document.removeEventListener("click", onDocClick);
      document.removeEventListener("keydown", onEscape);
    };
  }, []);

  if (!user) {
    return (
      <Link to="/login" className="button-secondary whitespace-nowrap">
        Sign in
      </Link>
    );
  }

  const name = userDisplayName(user);

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className="button-secondary whitespace-nowrap"
        aria-expanded={open}
        aria-haspopup="menu"
      >
        <span className="flex h-7 w-7 items-center justify-center rounded-full bg-slate-950 text-xs font-semibold text-white">
          {name.charAt(0).toUpperCase()}
        </span>
        <span>Hello, {name}</span>
      </button>

      {open && (
        <div
          className="absolute right-0 mt-2 w-60 rounded-2xl border border-slate-200 bg-white p-2 shadow-lg shadow-slate-950/10"
          role="menu"
        >
          <div className="rounded-xl bg-slate-50 px-3 py-3">
            <div className="text-sm font-semibold text-slate-950">{name}</div>
            <div className="mt-1 truncate text-sm text-slate-500">{user.email ?? "Signed-in session"}</div>
          </div>
          <Link
            to="/orders"
            className="mt-2 block rounded-xl px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            onClick={() => setOpen(false)}
          >
            Your Orders
          </Link>
          <Link
            to="/account"
            className="block rounded-xl px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            onClick={() => setOpen(false)}
          >
            Account
          </Link>
          <button
            onClick={() => {
              setOpen(false);
              logout();
            }}
            className="mt-1 w-full rounded-xl px-3 py-2 text-left text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          >
            Sign out
          </button>
        </div>
      )}
    </div>
  );
}
