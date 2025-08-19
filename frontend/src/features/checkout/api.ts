import { useMutation } from "@tanstack/react-query";
import { api } from "../../api/axios";
import { API } from "../../api/config";

export type OrderItemReq = { productId: number; quantity: number };
export type PlaceOrderResponse = { orderId: number; status: string; traceId: string };

function userIdFromJwt(): number | null {
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

export function useCreateOrder() {
  return useMutation({
    mutationFn: async (items: OrderItemReq[]) => {
      if (!items.length) throw new Error("No items to order.");
      const userId = userIdFromJwt();
      const payload = { userId: userId ?? undefined, items, status: "PENDING" }; // service will default to PENDING too
      const { data } = await api.post(`${API.order}`, payload);
      return data as PlaceOrderResponse;
    },
  });
}
