import { useQuery } from "@tanstack/react-query";
import { api } from "../../api/axios";
import { API } from "../../api/config";

export type OrderSummary = {
  id: number;
  status: string;
  total?: number;
  createdAt?: string;
};

function userIdFromJwt(): number | null {
  const t = localStorage.getItem("token");
  if (!t) return null;
  try {
    const [, payload] = t.split(".");
    const claims = JSON.parse(atob(payload));
    const n = Number(claims.userId);
    return Number.isFinite(n) ? n : null;
  } catch { return null; }
}

export function useOrders() {
  const userId = userIdFromJwt();
  return useQuery({
    enabled: !!userId,
    queryKey: ["orders", userId],
    queryFn: async (): Promise<OrderSummary[]> => {
      const { data } = await api.get(`${API.order}`, { params: { userId } });
      return data;
    },
  });
}
