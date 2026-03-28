import { useCart, useRemoveFromCart } from "./api";
import { useProducts } from "../../features/catalog/useProducts";
import type { Product } from "../../features/catalog/useProducts";
import { Link } from "react-router-dom";
import { formatCurrency } from "../../lib/format";

export default function CartPage() {
  const { data: cart, isLoading: cartLoading, error: cartError } = useCart();
  const { data: products, isLoading: prodLoading, error: prodError } = useProducts();
  const remove = useRemoveFromCart();

  if (cartLoading || prodLoading) {
    return (
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.8fr)_minmax(18rem,0.95fr)]">
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, index) => (
            <div key={index} className="surface h-28 animate-pulse bg-slate-100" />
          ))}
        </div>
        <div className="surface h-64 animate-pulse bg-slate-100" />
      </div>
    );
  }

  if (cartError || prodError) {
    return (
      <section className="surface-muted p-6 sm:p-8">
        <p className="eyebrow">Cart</p>
        <h1 className="mt-3 page-heading">We couldn't load your cart.</h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
          Your session is intact, but the cart or product service is currently unavailable.
        </p>
      </section>
    );
  }

  if (!cart?.length) {
    return (
      <section className="surface-muted p-8 text-center sm:p-10">
        <p className="eyebrow">Cart</p>
        <h1 className="mt-3 page-heading">Your cart is empty</h1>
        <p className="mt-3 text-sm leading-6 text-slate-600 sm:text-base">
          Add a few products from the catalog, then come back here to review the checkout flow.
        </p>
        <Link to="/" className="button-primary mt-6">
          Browse catalog
        </Link>
      </section>
    );
  }

  const byId = new Map<number, Product>((products ?? []).map((p) => [Number(p.id), p]));
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
  const itemCount = rows.reduce((sum, r) => sum + r.quantity, 0);

  return (
    <div className="space-y-8">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-2">
          <p className="eyebrow">Cart</p>
          <h1 className="page-heading">Review your selections</h1>
          <p className="text-sm leading-6 text-slate-600 sm:text-base">
            {itemCount} item{itemCount === 1 ? "" : "s"} ready for checkout.
          </p>
        </div>
        <Link to="/" className="button-secondary self-start">
          Continue shopping
        </Link>
      </header>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.8fr)_minmax(18rem,0.95fr)]">
        <section className="space-y-4">
          {rows.map((r) => (
            <article key={r.productId} className="surface p-4 sm:p-5">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-start gap-4">
                  {r.imageUrl ? (
                    <img src={r.imageUrl} alt={r.name} className="h-20 w-20 rounded-2xl object-cover" />
                  ) : (
                    <div className="flex h-20 w-20 items-center justify-center rounded-2xl bg-slate-100 text-xs font-semibold text-slate-400">
                      No image
                    </div>
                  )}

                  <div className="space-y-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="text-lg font-semibold tracking-tight text-slate-950">{r.name}</h2>
                      <span className="chip border-slate-200 bg-slate-50 text-slate-600 shadow-none">
                        Qty {r.quantity}
                      </span>
                    </div>
                    <p className="text-sm text-slate-600">{formatCurrency(r.price)} each</p>
                    <p className="text-sm font-medium text-slate-950">{formatCurrency(r.price * r.quantity)} line total</p>
                  </div>
                </div>

                <button
                  onClick={() => remove.mutate(r.productId)}
                  className="button-secondary sm:self-center"
                  disabled={remove.isPending}
                >
                  Remove
                </button>
              </div>
            </article>
          ))}
        </section>

        <aside className="surface h-fit p-6 lg:sticky lg:top-24">
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

          <Link to="/checkout" className="button-primary mt-6 w-full">
            Go to checkout
          </Link>

          <p className="mt-4 text-sm leading-6 text-slate-500">
            Review your items before continuing to checkout.
          </p>
        </aside>
      </div>
    </div>
  );
}
