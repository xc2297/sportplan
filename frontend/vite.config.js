/**
 * Vite 构建工具配置文件
 * 配置前端开发服务器和构建行为，包括：
 * - Vue 3 插件支持
 * - 基础路径配置（与后端 context-path 匹配）
 * - 开发服务器端口和 API 代理配置
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置：https://vite.dev/config/
export default defineConfig({
  // 插件列表：启用 Vue 3 单文件组件支持
  plugins: [vue()],
  // 部署基础路径，需与后端 Spring Boot 的 context-path 保持一致
  base: '/sportplan-0.0.1-SNAPSHOT/',
  // 开发服务器配置
  server: {
    port: 5173,  // 开发服务器端口号
    // API 代理配置：将前端的 /api 请求代理到后端服务器，解决跨域问题
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 后端 Spring Boot 服务地址
        changeOrigin: true,               // 修改请求头中的 Origin 字段，使后端认为是直接请求
      },
    },
  },
})
