import axios from "axios";

export const api = axios.create();

api.interceptors.request.use((cfg) => {
  const token = localStorage.getItem("token"); // dev-only
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

api.interceptors.response.use(
  (r) => r,
  (e) => {
    const msg = e?.response?.data?.message || e?.message || "Request failed";
    return Promise.reject(new Error(msg));
  }
);
