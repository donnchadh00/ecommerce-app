import { useProducts } from "./useProducts";
import { useAddToCart } from "../../features/cart/api";
import { useAuth } from "../auth/useAuth";
import { useLocation, useNavigate } from "react-router-dom";

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

  if (isLoading) return <div>Loading products…</div>;
  if (error) return <div className="text-red-600">Failed to load products</div>;
  if (!data?.length) return <div>No products found.</div>;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {data.map((p) => (
        <article key={p.id} className="rounded-xl border bg-white p-4">
          {p.imageUrl && (
            <img
              src={p.imageUrl}
              alt={p.name}
              className="h-40 w-full object-cover rounded-lg mb-3"
            />
          )}
          <h3 className="font-semibold">{p.name}</h3>
          <p className="text-sm text-gray-600">${p.price}</p>
          <button
            onClick={() => handleAddToCart(Number(p.id))}
            className="mt-3 rounded-lg bg-black px-3 py-2 text-white disabled:opacity-60"
            disabled={add.isPending}
          >
            {add.isPending ? "Adding…" : "Add to cart"}
          </button>
        </article>
      ))}
    </div>
  );
}
