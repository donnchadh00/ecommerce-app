import { useQuery } from "@tanstack/react-query";
import { api } from "../../api/axios";
import { API } from "../../api/config";
import { getStoredUserId } from "../auth/jwt";

export type OrderSummary = {
  id: number;
  status: string;
  traceId?: string | null;
  total?: number;
  createdAt?: string;
};

export function useOrders() {
  const userId = getStoredUserId();
  return useQuery({
    enabled: !!userId,
    queryKey: ["orders", userId],
    queryFn: async (): Promise<OrderSummary[]> => {
      const { data } = await api.get(`${API.order}`, { params: { userId } });
      return data;
    },
  });
}
