import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    // Lets us write `import Button from '@/components/ui/Button'`
    // instead of '../../../components/ui/Button'.
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    port: 5173,
    // Every /api call is proxied to Spring Boot, so the browser only ever talks
    // to one origin in development and CORS never comes into play.
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
});
