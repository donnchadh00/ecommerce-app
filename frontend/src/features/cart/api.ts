import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../api/axios";
import { API } from "../../api/config";
import { getStoredUserId } from "../auth/jwt";

export type CartItem = {
  productId: number;
  name: string;
  price: number;
  quantity: number;
  imageUrl?: string;
};

export function useCart() {
  const userId = getStoredUserId();

  return useQuery({
    enabled: !!userId,
    queryKey: ["cart", userId],
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
      const userId = getStoredUserId();
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

export function useClearCart() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () =>
      await api.delete(`${API.cart}/clear`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["cart"] }),
  });
}
