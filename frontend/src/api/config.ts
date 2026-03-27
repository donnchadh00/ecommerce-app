const env = import.meta.env;

export const API = {
  auth: env.VITE_API_AUTH ?? "/api/auth",
  product: env.VITE_API_PRODUCT ?? "/api/products",
  order: env.VITE_API_ORDER ?? "/api/orders",
  cart: env.VITE_API_CART ?? "/api/cart",
  payment: env.VITE_API_PAYMENT ?? "/api/payments",
  inventory: env.VITE_API_INVENTORY ?? "/api/inventory",
};
