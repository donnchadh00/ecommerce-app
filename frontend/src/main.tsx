import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';

import './index.css'
import App from './App.tsx'

if (import.meta.env.DEV) {
  import("./dev/devLogin").then((m) => {
    window.devLogin = m.devLogin;
    window.devLogout = m.devLogout;
    window.devWhoAmI = m.devWhoAmI;
    console.info('[dev] Helpers available: devLogin(), devLogout(), devWhoAmI()');
  });
}

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 60_000, // 1 minute "fresh" window
    },
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <App />
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </BrowserRouter>
  </StrictMode>,
)
