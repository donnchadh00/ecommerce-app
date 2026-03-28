import { useQuery } from "@tanstack/react-query";
import { api } from "../../api/axios";
import { API } from "../../api/config";

const seededProductImages: Record<string, string> = {
  "Mechanical Keyboard": "/products/mechanical-keyboard.jpg",
  "Ultrawide Monitor": "/products/ultrawide-moniter.jpg",
  "USB-C Dock": "/products/usb-c-dock.jpg",
  "Noise-Cancelling Headphones": "/products/noise-cancelling-headphones.jpg",
  "Ergonomic Mouse": "/products/ergonomic-mouse.jpg",
  "Laptop Stand": "/products/laptop-stand.jpg",
};

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
      return data.map((product: Product) => ({
        ...product,
        imageUrl: product.imageUrl || seededProductImages[product.name],
      }));
    },
    staleTime: 60_000,
    retry: 1,
  });
}
