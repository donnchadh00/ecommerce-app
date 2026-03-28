import { useCart } from "./api";

export default function CartBadge() {
  const { data } = useCart();
  const count = data?.reduce((sum, i) => sum + i.quantity, 0) ?? 0;
  return (
    <span className="inline-flex min-w-6 items-center justify-center rounded-full bg-teal-500/12 px-2 py-0.5 text-xs font-semibold text-teal-800">
      {count}
    </span>
  );
}
