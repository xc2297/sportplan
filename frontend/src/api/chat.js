import api from './index'

// AI对话接口超时时间较长（智谱API流式响应需要时间）
export function sendMessage(message, history) {
  return api.post('/chat', { message, history }, { timeout: 180000 })
}
