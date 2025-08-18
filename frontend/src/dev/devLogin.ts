import { api } from "../api/axios";
import { API } from "../api/config";

// Dev-only helper: logs in and stores the JWT in localStorage.
// Call from the browser console:  await window.devLogin()
export async function devLogin() {
  const { data } = await api.post(`${API.auth}/login`, {
    email: "testAdmin@example.com",
    password: "mypassword",
  });

  const token = data.token;
  if (!token) {
    throw new Error("Login succeeded but no token field found on response.");
  }
  localStorage.setItem("token", token);
  return token;
}

// Clear token quickly
export function devLogout() {
  localStorage.removeItem("token");
}

// Check current token payload
export function devWhoAmI() {
  const t = localStorage.getItem("token");
  if (!t) return null;
  try {
    const [, payload] = t.split(".");
    return JSON.parse(atob(payload));
  } catch {
    return { token: t };
  }
}
