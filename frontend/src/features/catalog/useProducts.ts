import { useQuery } from "@tanstack/react-query";
import { api } from "../../api/axios";
import { API } from "../../api/config";

export type Product = {
  id: string;
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
};

export function useProducts() {
  return useQuery({
    queryKey: ["products"],
    queryFn: async (): Promise<Product[]> => {
      const { data } = await api.get(API.product); // GET http://localhost:8082/api/products
      return data;
    },
    staleTime: 60_000,
    retry: 1,
  });
}
