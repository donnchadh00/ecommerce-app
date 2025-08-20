import { Link } from "react-router-dom";
import { useAuth } from "./AuthContext";
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
    document.addEventListener("click", onDocClick);
    return () => document.removeEventListener("click", onDocClick);
  }, []);

  if (!user) {
    return (
      <Link to="/login" className="hover:underline whitespace-nowrap">
        Sign in
      </Link>
    );
  }

  const name = userDisplayName(user);

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className="px-3 py-2 rounded-lg border hover:bg-gray-50 whitespace-nowrap"
      >
        Hello, <span className="font-semibold">{name}</span>
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-48 rounded-xl border bg-white shadow">
          <Link
            to="/orders"
            className="block px-4 py-2 hover:bg-gray-50"
            onClick={() => setOpen(false)}
          >
            Your Orders
          </Link>
          <Link
            to="/account" // add later
            className="block px-4 py-2 hover:bg-gray-50"
            onClick={() => setOpen(false)}
          >
            Account
          </Link>
          <button
            onClick={() => {
              setOpen(false);
              logout();
            }}
            className="w-full text-left px-4 py-2 hover:bg-gray-50"
          >
            Sign out
          </button>
        </div>
      )}
    </div>
  );
}
