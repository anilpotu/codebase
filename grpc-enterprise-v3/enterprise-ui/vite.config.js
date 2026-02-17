import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/users': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/api/accounts': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/transactions': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/health-records': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/vitals': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/profiles': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
      '/api/posts': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
      '/api/connections': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
    },
  },
});
