import { useCart } from "./api";

export default function CartBadge() {
  const { data } = useCart();
  const count = data?.reduce((sum, i) => sum + i.quantity, 0) ?? 0;
  return (
    <span className="ml-1 inline-flex items-center rounded-full border px-2 text-xs">
      {count}
    </span>
  );
}
