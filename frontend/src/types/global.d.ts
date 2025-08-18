declare global {
  interface Window {
    devLogin?: () => Promise<string>;
    devLogout?: () => void;
    devWhoAmI?: () => unknown;
  }
}
export {};
