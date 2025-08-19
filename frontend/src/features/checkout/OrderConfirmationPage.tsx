import { useLocation, useParams, Link } from "react-router-dom";

export default function OrderConfirmationPage() {
  const { id } = useParams();
  const { state } = useLocation() as { state?: { traceId?: string } };

  return (
    <div className="max-w-xl space-y-4">
      <h1 className="text-2xl font-semibold">Order placed 🎉</h1>
      <p>
        Your order ID is <span className="font-mono font-semibold">{id}</span>.
        {state?.traceId && (
          <> Trace: <span className="font-mono">{state.traceId}</span></>
        )}
      </p>
      <div className="space-x-3">
        <Link to="/" className="underline">Continue shopping</Link>
        <Link to="/cart" className="underline">View cart</Link>
      </div>
    </div>
  );
}
