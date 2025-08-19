import { Link } from "react-router-dom";
import { useOrders } from "./api.ts";

export default function OrdersPage() {
  const { data, isLoading, error } = useOrders();

  if (isLoading) return <div>Loading orders…</div>;
  if (error) return <div className="text-red-600">Failed to load orders.</div>;
  if (!data?.length) return <div>No orders yet.</div>;

  return (
    <div className="max-w-2xl space-y-4">
      <h1 className="text-2xl font-semibold">Your Orders</h1>
      <div className="rounded-xl border bg-white divide-y">
        {data.map((o) => (
          <div key={o.id} className="flex items-center justify-between p-4">
            <div>
              <div className="font-medium">Order #{o.id}</div>
              <div className="text-sm text-gray-600">
                {o.status}{o.createdAt ? ` • ${new Date(o.createdAt).toLocaleString()}` : ""}
              </div>
            </div>
            <div className="flex items-center gap-4">
              {o.total != null && <div className="font-medium">${Number(o.total).toFixed(2)}</div>}
              <Link to={`/order/${o.id}`} className="underline">View</Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
