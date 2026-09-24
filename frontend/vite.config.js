import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Send every /api request to the Java backend
    proxy: {
      '/api': 'http://localhost:7070',
    },
  },
})