import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    proxy: {
      '/api/auth': 'http://localhost:8081',
      '/api/products': 'http://localhost:8082',
      '/api/orders': 'http://localhost:8083',
      '/api/cart': 'http://localhost:8084',
      '/api/payments': 'http://localhost:8085',
      '/api/inventory': 'http://localhost:8086',
    }
  }
})
