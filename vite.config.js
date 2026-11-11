import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发服务器端口 5173,并将 /api 代理到 Spring Boot 后端 8080
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
