import { Link, useLocation, useParams, useSearchParams } from "react-router-dom";
import TraceTimeline from "./TraceTimeline";
import { useTraceTimeline } from "./traceApi";
import { useOrders } from "../orders/api";
import { formatCurrency, formatDateTime } from "../../lib/format";

export default function OrderConfirmationPage() {
  const { id } = useParams();
  const { state } = useLocation() as { state?: { traceId?: string } };
  const [searchParams] = useSearchParams();
  const { data: orders } = useOrders();
  const orderId = id ? Number(id) : null;
  const order = orderId != null ? orders?.find((candidate) => Number(candidate.id) === orderId) : undefined;
  const orderTraceId =
    orderId != null
      ? order?.traceId ?? null
      : null;
  const traceId = searchParams.get("traceId") ?? state?.traceId ?? orderTraceId ?? null;
  const traceTimeline = useTraceTimeline(traceId);
  const placedAt = formatDateTime(order?.createdAt);

  return (
    <div className="space-y-6">
      <section className="surface p-6 sm:p-8">
        <p className="eyebrow">Order placed</p>
        <div className="mt-3 flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
          <div className="max-w-2xl space-y-3">
            <h1 className="page-heading">Your order is moving through the system</h1>
            <p className="text-sm leading-6 text-slate-600 sm:text-base">
              Order <span className="font-mono font-semibold text-slate-950">#{id}</span>
              {placedAt ? ` was created ${placedAt}.` : " was created successfully."}
              {traceId
                ? " A trace is attached so you can inspect how the request moved across services."
                : " Trace details are only available when a trace ID is attached to the order."}
            </p>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <div className="rounded-2xl bg-slate-50 p-4">
              <div className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Order ID</div>
              <div className="mt-2 font-mono text-base font-semibold text-slate-950">#{id}</div>
            </div>
            <div className="rounded-2xl bg-slate-50 p-4">
              <div className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Status</div>
              <div className="mt-2 text-base font-semibold text-slate-950">{order?.status ?? "Placed"}</div>
            </div>
            {order?.total != null && (
              <div className="rounded-2xl bg-slate-50 p-4">
                <div className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Total</div>
                <div className="mt-2 text-base font-semibold text-slate-950">{formatCurrency(order.total)}</div>
              </div>
            )}
            {traceId && (
              <div className="rounded-2xl bg-slate-50 p-4">
                <div className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Trace ID</div>
                <div className="mt-2 break-all font-mono text-sm font-semibold text-slate-950">{traceId}</div>
              </div>
            )}
          </div>
        </div>
      </section>

      {traceTimeline.isLoading && traceId && (
        <div className="surface p-4 text-sm text-slate-600">
          Loading trace details...
        </div>
      )}

      {traceTimeline.data?.available && (
        <TraceTimeline timeline={traceTimeline.data} />
      )}

      {traceId && traceTimeline.data && !traceTimeline.data.available && (
        <div className="surface-muted p-4 text-sm text-slate-600">
          {traceTimeline.data.message ?? "Trace details are not available right now."}
        </div>
      )}

      {!traceId && (
        <div className="surface-muted p-4 text-sm text-slate-600">
          Trace details are only available when a trace ID is present for this order.
        </div>
      )}

      <div className="flex flex-wrap gap-3">
        <Link to="/" className="button-primary">
          Continue shopping
        </Link>
        <Link to="/orders" className="button-secondary">
          View orders
        </Link>
        <Link to="/cart" className="button-ghost">
          View cart
        </Link>
      </div>
    </div>
  );
}
