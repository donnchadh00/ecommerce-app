import { useNavigate } from "react-router-dom";
import { useCart } from "../../features/cart/api";
import { useProducts } from "../../features/catalog/useProducts";
import { useCreateOrder, type OrderItemReq } from "./api";

export default function CheckoutPage() {
  const nav = useNavigate();
  const { data: cart } = useCart();
  const { data: products } = useProducts();
  const createOrder = useCreateOrder();

  const byId = new Map((products ?? []).map((p: any) => [Number(p.id), p]));
  const rows = (cart ?? []).map((i) => {
    const p = byId.get(Number(i.productId));
    return {
      productId: Number(i.productId),
      quantity: i.quantity,
      name: p?.name,
      price: Number(p?.price ?? 0),
    };
  });

  const total = rows.reduce((s, r) => s + r.price * r.quantity, 0);

  const placeOrder = async () => {
    const items: OrderItemReq[] = rows.map(({ productId, quantity }) => ({ productId, quantity }));
    const res = await createOrder.mutateAsync(items);
    nav(`/order/${res.orderId}`, { state: { traceId: res.traceId } });
  };

  if (!rows.length) return <div>Your cart is empty.</div>;

  return (
    <div className="max-w-2xl space-y-4">
      <h1 className="text-2xl font-semibold">Checkout</h1>

      <div className="rounded-xl border bg-white">
        <div className="divide-y">
          {rows.map((r) => (
            <div key={r.productId} className="flex items-center justify-between p-4">
              <div>
                <div className="font-medium">{r.name ?? `Product #${r.productId}`}</div>
                <div className="text-sm text-gray-600">${r.price} × {r.quantity}</div>
              </div>
              <div className="font-medium">${(r.price * r.quantity).toFixed(2)}</div>
            </div>
          ))}
        </div>
        <div className="flex items-center justify-between p-4 border-t">
          <div className="text-lg font-semibold">Total</div>
          <div className="text-lg font-semibold">${total.toFixed(2)}</div>
        </div>
      </div>

      <button
        onClick={placeOrder}
        disabled={createOrder.isPending}
        className="rounded-lg bg-black px-4 py-2 text-white disabled:opacity-60"
      >
        {createOrder.isPending ? "Placing…" : "Place order"}
      </button>

      {createOrder.isError && (
        <div className="text-red-600">Failed to place order. Check item quantities and login.</div>
      )}
    </div>
  );
}
