import { Link } from "react-router-dom";
import { useOrders } from "./api.ts";
import { formatCurrency, formatDateTime } from "../../lib/format";

function getStatusTone(status: string) {
  const value = status.toLowerCase();

  if (value.includes("paid") || value.includes("complete") || value.includes("success")) {
    return "status-badge bg-emerald-100 text-emerald-700";
  }

  if (value.includes("pending") || value.includes("processing")) {
    return "status-badge bg-amber-100 text-amber-700";
  }

  if (value.includes("fail") || value.includes("cancel")) {
    return "status-badge bg-rose-100 text-rose-700";
  }

  return "status-badge bg-slate-100 text-slate-700";
}

export default function OrdersPage() {
  const { data, isLoading, error } = useOrders();

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="surface h-40 animate-pulse bg-slate-100" />
        <div className="surface h-24 animate-pulse bg-slate-100" />
        <div className="surface h-24 animate-pulse bg-slate-100" />
      </div>
    );
  }

  if (error) {
    return (
      <section className="surface-muted p-6 sm:p-8">
        <p className="eyebrow">Orders</p>
        <h1 className="mt-3 page-heading">We couldn't load your orders.</h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
          The order history could not be loaded right now.
        </p>
      </section>
    );
  }

  if (!data?.length) {
    return (
      <section className="surface-muted p-8 text-center sm:p-10">
        <p className="eyebrow">Orders</p>
        <h1 className="mt-3 page-heading">No orders yet</h1>
        <p className="mt-3 text-sm leading-6 text-slate-600 sm:text-base">
          Orders will appear here with status, totals, and trace access when available.
        </p>
        <Link to="/" className="button-primary mt-6">
          Start shopping
        </Link>
      </section>
    );
  }

  return (
    <div className="space-y-6">
      <section className="surface p-6 sm:p-8">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div className="space-y-2">
            <p className="eyebrow">Orders</p>
            <h1 className="page-heading">Your order history</h1>
            <p className="text-sm leading-6 text-slate-600 sm:text-base">
              Review totals, statuses, and trace details for your recent orders.
            </p>
          </div>
          <span className="chip self-start sm:self-auto">
            {data.length} order{data.length === 1 ? "" : "s"}
          </span>
        </div>
      </section>

      <div className="space-y-4">
        {data.map((o) => {
          const placedAt = formatDateTime(o.createdAt);

          return (
            <article key={o.id} className="surface p-5">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                <div className="space-y-3">
                  <div className="flex flex-wrap items-center gap-3">
                    <h2 className="text-lg font-semibold tracking-tight text-slate-950">Order #{o.id}</h2>
                    <span className={getStatusTone(o.status)}>{o.status}</span>
                    {o.traceId && <span className="chip border-slate-200 bg-slate-50 text-slate-600 shadow-none">Trace ready</span>}
                  </div>
                  <p className="text-sm text-slate-600">{placedAt ? `Placed ${placedAt}` : "Recently created order"}</p>
                </div>

                <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                  {o.total != null && (
                    <div className="text-base font-semibold text-slate-950">{formatCurrency(o.total)}</div>
                  )}
                  <Link
                    to={o.traceId ? `/order/${o.id}?traceId=${encodeURIComponent(o.traceId)}` : `/order/${o.id}`}
                    className="button-secondary"
                  >
                    View details
                  </Link>
                </div>
              </div>
            </article>
          );
        })}
      </div>
    </div>
  );
}
