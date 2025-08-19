import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../api/axios";
import { API } from "../../api/config";

export type CartItem = {
  productId: number;
  name: string;
  price: number;
  quantity: number;
  imageUrl?: string;
};

function getUserIdFromToken(): number | null {
  const t = localStorage.getItem("token");
  if (!t) return null;
  try {
    const [, payload] = t.split(".");
    const claims = JSON.parse(atob(payload));
    const candidate = claims.userId;
    const n = Number(candidate);
    return Number.isFinite(n) ? n : null;
  } catch {
    return null;
  }
}

export function useCart() {
  return useQuery({
    queryKey: ["cart"],
    queryFn: async (): Promise<CartItem[]> => {
      const { data } = await api.get(`${API.cart}`);
      return data;
    },
  });
}

export function useAddToCart() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: { productId: number; quantity?: number }) => {
      const userId = getUserIdFromToken();
      if (!userId) throw new Error("No userId found in token; please login first.");
      return api.post(`${API.cart}/add`, {
        userId,
        productId: Number(payload.productId),
        quantity: payload.quantity ?? 1,
      });
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["cart"] }),
  });
}

export function useRemoveFromCart() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (productId: number) =>
      api.delete(`${API.cart}/remove`, { params: { productId } }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["cart"] }),
  });
}
