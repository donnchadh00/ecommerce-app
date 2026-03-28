import { useProducts } from "./useProducts";
import { useAddToCart } from "../../features/cart/api";
import { useAuth } from "../auth/useAuth";
import { useLocation, useNavigate } from "react-router-dom";
import { formatCurrency } from "../../lib/format";

export default function CatalogPage() {
  const { data, isLoading, error } = useProducts();
  const { user } = useAuth();
  const add = useAddToCart();
  const location = useLocation();
  const navigate = useNavigate();

  const handleAddToCart = (productId: number) => {
    if (!user) {
      navigate("/login", { state: { from: location.pathname } });
      return;
    }

    add.mutate({ productId, quantity: 1 });
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="surface h-44 animate-pulse bg-slate-100" />
        <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <div key={index} className="surface h-[22rem] animate-pulse bg-slate-100" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <section className="surface-muted p-6 sm:p-8">
        <p className="eyebrow">Catalog</p>
        <h1 className="mt-3 page-heading">We couldn't load the product catalog.</h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
          Refresh the page once the catalog service is available again.
        </p>
      </section>
    );
  }

  return (
    <div className="space-y-8">
      <section className="surface p-6 sm:p-8">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-2xl space-y-3">
            <p className="eyebrow">Storefront</p>
            <h1 className="page-heading">Browse the product catalog</h1>
            <p className="text-sm leading-6 text-slate-600 sm:text-base">
              Explore available products and add items to your cart when you're ready to check out.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <span className="chip">{data?.length ?? 0} products</span>
            <span className="chip">Cart ready</span>
            <span className="chip">Order tracking</span>
          </div>
        </div>
      </section>

      {!data?.length ? (
        <section className="surface-muted p-8 text-center">
          <h2 className="section-heading">No products available yet</h2>
          <p className="mt-3 text-sm text-slate-600">Products will appear here as soon as the catalog is populated.</p>
        </section>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
          {data.map((p) => (
            <article key={p.id} className="surface group flex h-full flex-col overflow-hidden">
              <div className="aspect-[4/3] overflow-hidden bg-slate-100">
                {p.imageUrl ? (
                  <img
                    src={p.imageUrl}
                    alt={p.name}
                    className="h-full w-full object-cover transition duration-500 group-hover:scale-[1.03]"
                  />
                ) : (
                  <div className="flex h-full items-center justify-center text-sm font-medium text-slate-400">
                    Product image unavailable
                  </div>
                )}
              </div>

              <div className="flex flex-1 flex-col gap-4 p-5">
                <div className="space-y-2">
                  <div className="flex items-start justify-between gap-3">
                    <h2 className="text-lg font-semibold tracking-tight text-slate-950">{p.name}</h2>
                    <span className="shrink-0 text-base font-semibold text-slate-950">{formatCurrency(p.price)}</span>
                  </div>
                  <p className="text-sm leading-6 text-slate-600">
                    {p.description?.trim() || "Available now in the storefront catalog."}
                  </p>
                </div>

                <button
                  onClick={() => handleAddToCart(Number(p.id))}
                  className="button-primary mt-auto w-full"
                  disabled={add.isPending}
                >
                  {add.isPending ? "Adding..." : user ? "Add to cart" : "Sign in to add"}
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
