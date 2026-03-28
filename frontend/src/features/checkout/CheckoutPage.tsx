import { Link, useNavigate } from "react-router-dom";
import { useCart, useClearCart } from "../../features/cart/api";
import { useProducts, type Product } from "../../features/catalog/useProducts";
import { useCreateOrder, type OrderItemReq } from "./api";
import { formatCurrency } from "../../lib/format";

export default function CheckoutPage() {
  const nav = useNavigate();
  const { data: cart, isLoading: cartLoading, error: cartError } = useCart();
  const { data: products, isLoading: productsLoading, error: productsError } = useProducts();
  const createOrder = useCreateOrder();
  const clearCart = useClearCart();

  const byId = new Map<number, Product>((products ?? []).map((p) => [Number(p.id), p]));
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
  const itemCount = rows.reduce((sum, row) => sum + row.quantity, 0);

  const placeOrder = async () => {
    const items: OrderItemReq[] = rows.map(({ productId, quantity }) => ({ productId, quantity }));
    const res = await createOrder.mutateAsync(items);
    try {
      await clearCart.mutateAsync();
    } catch (error) {
      console.warn("Order was placed, but clearing the cart failed.", error);
    }
    nav(`/order/${res.orderId}?traceId=${encodeURIComponent(res.traceId)}`, { state: { traceId: res.traceId } });
  };

  if (cartLoading || productsLoading) {
    return (
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.5fr)_minmax(18rem,0.95fr)]">
        <div className="surface h-[28rem] animate-pulse bg-slate-100" />
        <div className="space-y-4">
          <div className="surface h-56 animate-pulse bg-slate-100" />
          <div className="surface h-40 animate-pulse bg-slate-100" />
        </div>
      </div>
    );
  }

  if (cartError || productsError) {
    return (
      <section className="surface-muted p-6 sm:p-8">
        <p className="eyebrow">Checkout</p>
        <h1 className="mt-3 page-heading">Checkout is temporarily unavailable.</h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
          We couldn't load the latest cart or product details needed to place your order.
        </p>
      </section>
    );
  }

  if (!rows.length) {
    return (
      <section className="surface-muted p-8 text-center sm:p-10">
        <p className="eyebrow">Checkout</p>
        <h1 className="mt-3 page-heading">Your cart is empty</h1>
        <p className="mt-3 text-sm leading-6 text-slate-600 sm:text-base">
          Add something to your cart before placing an order.
        </p>
        <Link to="/" className="button-primary mt-6">
          Return to catalog
        </Link>
      </section>
    );
  }

  return (
    <div className="space-y-8">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-2">
          <p className="eyebrow">Checkout</p>
          <h1 className="page-heading">Review and place your order</h1>
          <p className="text-sm leading-6 text-slate-600 sm:text-base">
            Confirm the items in your cart and place your order when you're ready.
          </p>
        </div>
        <Link to="/cart" className="button-secondary self-start">
          Back to cart
        </Link>
      </header>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.5fr)_minmax(18rem,0.95fr)]">
        <section className="surface overflow-hidden">
          <div className="border-b border-slate-200 px-5 py-4 sm:px-6">
            <h2 className="section-heading">Order details</h2>
            <p className="mt-1 text-sm text-slate-600">{itemCount} item{itemCount === 1 ? "" : "s"} ready to submit.</p>
          </div>

          <div className="divide-y divide-slate-200">
            {rows.map((r) => (
              <div key={r.productId} className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6">
                <div>
                  <div className="font-medium text-slate-950">{r.name ?? `Product #${r.productId}`}</div>
                  <div className="mt-1 text-sm text-slate-600">
                    {formatCurrency(r.price)} x {r.quantity}
                  </div>
                </div>
                <div className="text-sm font-semibold text-slate-950">{formatCurrency(r.price * r.quantity)}</div>
              </div>
            ))}
          </div>
        </section>

        <aside className="space-y-4">
          <div className="surface p-6 lg:sticky lg:top-24">
            <h2 className="section-heading">Summary</h2>
            <div className="mt-5 space-y-4 text-sm text-slate-600">
              <div className="flex items-center justify-between">
                <span>Items</span>
                <span className="font-medium text-slate-950">{itemCount}</span>
              </div>
              <div className="flex items-center justify-between">
                <span>Subtotal</span>
                <span className="font-medium text-slate-950">{formatCurrency(total)}</span>
              </div>
              <div className="border-t border-slate-200 pt-4">
                <div className="flex items-center justify-between text-base font-semibold text-slate-950">
                  <span>Total</span>
                  <span>{formatCurrency(total)}</span>
                </div>
              </div>
            </div>

            <button onClick={placeOrder} disabled={createOrder.isPending} className="button-primary mt-6 w-full">
              {createOrder.isPending ? "Placing order..." : "Place order"}
            </button>

            {createOrder.isError && (
              <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                Failed to place order. Check item quantities and your session, then try again.
              </div>
            )}
          </div>

          <div className="surface-muted p-6">
            <h2 className="section-heading text-base">Before you place it</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              Review quantities and totals before submitting your order.
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
}
