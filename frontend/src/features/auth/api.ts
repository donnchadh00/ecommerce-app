import { useMutation } from "@tanstack/react-query";
import { api } from "../../api/axios";
import { API } from "../../api/config";

export function useRegister() {
  return useMutation({
    mutationFn: async (payload: { email: string; password: string }) => {
      const { data, status } = await api.post(`${API.auth}/register`, payload, { validateStatus: () => true });
      if (status >= 400) throw new Error((data && (data.message || data.error)) || "Registration failed");
      return data;
    },
  });
}

export function useLogin() {
  return useMutation({
    mutationFn: async (payload: { emailOrUsername: string; password: string }) => {
      const body = { email: payload.emailOrUsername, password: payload.password }
      const { data, status } = await api.post(`${API.auth}/login`, body, { validateStatus: () => true });
      if (status >= 400) throw new Error((data && (data.message || data.error)) || "Login failed");
      const token = data?.token;
      if (!token) throw new Error("Login succeeded but no token returned");
      return token as string;
    },
  });
}
