/**
 * Axios 实例配置文件
 * 创建并导出一个统一的 Axios 实例，用于所有 API 请求。
 * 配置包括：
 * - 基础路径（与后端 context-path 匹配）
 * - 请求超时时间
 * - 默认请求头（JSON 格式）
 * - 响应拦截器：自动解包响应数据，统一处理错误和 401 未授权状态
 */
import axios from 'axios'
import router from '../router'

// 创建 Axios 实例，配置基础路径和超时时间
const api = axios.create({
  baseURL: '/sportplan-0.0.1-SNAPSHOT', // 后端应用上下文路径
  timeout: 10000,                        // 请求超时时间：10秒
  headers: { 'Content-Type': 'application/json' }, // 默认请求头
})

// 响应拦截器：统一处理响应数据和错误
api.interceptors.response.use(
  // 成功响应：直接返回 response.data（后端 ApiResponse 的 JSON 内容）
  (response) => response.data,
  // 错误响应：处理 HTTP 错误状态码
  (error) => {
    // 401 未授权：清除本地用户信息并跳转到登录页
    if (error.response?.status === 401) {
      localStorage.removeItem('currentUser')
      router.replace('/login')
    }
    // 提取后端返回的错误信息，默认提示"网络错误"
    const msg = error.response?.data?.message || '网络错误'
    return Promise.reject(new Error(msg))
  },
)

export default api
