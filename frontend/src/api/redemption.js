/**
 * 兑换记录相关 API 接口模块
 * 封装了积分兑换相关的操作，包括：
 * - 提交积分兑换请求（使用积分兑换奖励）
 * - 查询用户的兑换历史记录
 */
import api from './index'

/** 提交积分兑换（包含用户ID和奖励商品ID） */
export function redeemReward(data) {
  return api.post('/redemptions', data)
}

/** 查询兑换历史记录列表，不传userId时管理员返回全部，普通用户返回自己的 */
export function getRedemptionHistory(userId) {
  const params = userId != null ? { userId } : {}
  return api.get('/redemptions', { params })
}

/** 撤销兑换（仅管理员），退还积分到用户账户 */
export function cancelRedemption(id) {
  return api.put(`/redemptions/${id}/cancel`)
}

/** 使用兑换券（核销），提交使用描述和凭证图片 */
export function useRedemption(id, data) {
  return api.put(`/redemptions/${id}/use`, data)
}
