import { useCart, useRemoveFromCart } from "./api";
import { useProducts } from "../../features/catalog/useProducts";

export default function CartPage() {
  const { data: cart, isLoading: cartLoading, error: cartError } = useCart();
  const { data: products, isLoading: prodLoading, error: prodError } = useProducts();
  const remove = useRemoveFromCart();

  if (cartLoading || prodLoading) return <div>Loading…</div>;
  if (cartError || prodError) return <div className="text-red-600">Failed to load cart/products</div>;
  if (!cart?.length) return <div>Your cart is empty.</div>;

  const byId = new Map(products?.map((p: any) => [Number(p.id), p]));
  const rows = cart.map((i) => {
    const p = byId.get(Number(i.productId));
    return {
      ...i,
      name: p?.name ?? `Product #${i.productId}`,
      price: p?.price ?? 0,
      imageUrl: p?.imageUrl,
    };
  });

  const total = rows.reduce((sum, r) => sum + Number(r.price) * r.quantity, 0);

  return (
    <div className="space-y-4">
      {rows.map((r) => (
        <div key={r.productId} className="flex items-center justify-between rounded-xl border bg-white p-4">
          <div className="flex items-center gap-4">
            {r.imageUrl && <img src={r.imageUrl} alt={r.name} className="h-16 w-16 rounded-lg object-cover" />}
            <div>
              <div className="font-medium">{r.name}</div>
              <div className="text-sm text-gray-600">${r.price} × {r.quantity}</div>
            </div>
          </div>
          <button
            onClick={() => remove.mutate(r.productId)}
            className="rounded-lg border px-3 py-2 hover:bg-gray-50"
            disabled={remove.isPending}
          >
            Remove
          </button>
        </div>
      ))}
      <div className="text-right text-lg font-semibold">Total: ${total.toFixed(2)}</div>
    </div>
  );
}
