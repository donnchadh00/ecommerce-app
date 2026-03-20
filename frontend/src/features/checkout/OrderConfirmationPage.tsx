import { Link, useLocation, useParams, useSearchParams } from "react-router-dom";
import TraceTimeline from "./TraceTimeline";
import { useTraceTimeline } from "./traceApi";
import { useOrders } from "../orders/api";

export default function OrderConfirmationPage() {
  const { id } = useParams();
  const { state } = useLocation() as { state?: { traceId?: string } };
  const [searchParams] = useSearchParams();
  const { data: orders } = useOrders();
  const orderId = id ? Number(id) : null;
  const orderTraceId =
    orderId != null
      ? orders?.find((order) => Number(order.id) === orderId)?.traceId ?? null
      : null;
  const traceId = searchParams.get("traceId") ?? state?.traceId ?? orderTraceId ?? null;
  const traceTimeline = useTraceTimeline(traceId);

  return (
    <div className="max-w-4xl space-y-6">
      <h1 className="text-2xl font-semibold">Order placed 🎉</h1>
      <p>
        Your order ID is <span className="font-mono font-semibold">{id}</span>.
        {traceId && (
          <> Trace: <span className="font-mono">{traceId}</span></>
        )}
      </p>

      {traceTimeline.isLoading && traceId && (
        <div className="rounded-2xl border bg-white p-4 text-sm text-gray-600 shadow-sm">
          Loading trace details...
        </div>
      )}

      {traceTimeline.data?.available && (
        <TraceTimeline timeline={traceTimeline.data} />
      )}

      {traceId && traceTimeline.data && !traceTimeline.data.available && (
        <div className="rounded-2xl border border-dashed bg-white p-4 text-sm text-gray-600 shadow-sm">
          {traceTimeline.data.message ?? "Trace details are not available right now."}
        </div>
      )}

      {!traceId && (
        <div className="rounded-2xl border border-dashed bg-white p-4 text-sm text-gray-600 shadow-sm">
          Trace details are only available when a trace ID is present for this order.
        </div>
      )}

      <div className="space-x-3">
        <Link to="/" className="underline">Continue shopping</Link>
        <Link to="/cart" className="underline">View cart</Link>
      </div>
    </div>
  );
}
