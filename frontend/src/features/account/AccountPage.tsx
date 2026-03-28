import { Link } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { userDisplayName } from "../auth/displayName";

export default function AccountPage() {
  const { user } = useAuth();
  const roles = user?.roles?.filter(Boolean) ?? [];

  return (
    <div className="space-y-6">
      <section className="surface p-6 sm:p-8">
        <p className="eyebrow">Account</p>
        <div className="mt-3 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="space-y-2">
            <h1 className="page-heading">Signed in as {userDisplayName(user)}</h1>
            <p className="max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
              Review your profile details, recent activity entry points, and current session information.
            </p>
          </div>
          <div className="chip self-start">Session active</div>
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.35fr)_minmax(18rem,0.9fr)]">
        <section className="surface p-6">
          <h2 className="section-heading">Profile snapshot</h2>
          <dl className="mt-6 grid gap-4 sm:grid-cols-2">
            <div className="rounded-2xl bg-slate-50 p-4">
              <dt className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Display name</dt>
              <dd className="mt-2 text-base font-semibold text-slate-950">{userDisplayName(user)}</dd>
            </div>
            <div className="rounded-2xl bg-slate-50 p-4">
              <dt className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Email</dt>
              <dd className="mt-2 break-words text-base font-semibold text-slate-950">
                {user?.email ?? "Unavailable"}
              </dd>
            </div>
            <div className="rounded-2xl bg-slate-50 p-4">
              <dt className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">User ID</dt>
              <dd className="mt-2 text-base font-semibold text-slate-950">
                {user?.id != null ? `#${user.id}` : "Unavailable"}
              </dd>
            </div>
            <div className="rounded-2xl bg-slate-50 p-4">
              <dt className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Roles</dt>
              <dd className="mt-2 flex flex-wrap gap-2">
                {roles.length ? (
                  roles.map((role) => (
                    <span key={role} className="chip border-slate-200 bg-white text-slate-700 shadow-none">
                      {role}
                    </span>
                  ))
                ) : (
                  <span className="text-sm text-slate-600">No explicit roles attached to this token.</span>
                )}
              </dd>
            </div>
          </dl>
        </section>

        <aside className="space-y-4">
          <div className="surface p-6">
            <h2 className="section-heading">Quick actions</h2>
            <div className="mt-4 grid gap-3">
              <Link to="/orders" className="button-secondary w-full">
                View order history
              </Link>
              <Link to="/cart" className="button-secondary w-full">
                Review cart
              </Link>
              <Link to="/" className="button-ghost w-full justify-center">
                Back to catalog
              </Link>
            </div>
          </div>

          <div className="surface-muted p-6">
            <h2 className="section-heading text-base">Account overview</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              Manage your storefront navigation from one place with quick access to orders, cart contents, and catalog browsing.
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
}
