/**
 * 奖励商品相关 API 接口模块
 * 封装了奖励商品的 CRUD 操作，包括：
 * - 查询所有奖励商品列表
 * - 新增奖励商品
 * - 更新奖励商品信息
 * - 删除奖励商品
 */
import api from './index'

/** 获取所有奖励商品列表 */
export function getRewardItems() {
  return api.get('/reward-items')
}

/** 新增奖励商品（包含名称、描述、所需积分、额度、排序等） */
export function createRewardItem(data) {
  return api.post('/reward-items', data)
}

/** 根据 ID 更新奖励商品信息 */
export function updateRewardItem(id, data) {
  return api.put(`/reward-items/${id}`, data)
}

/** 根据 ID 删除奖励商品 */
export function deleteRewardItem(id) {
  return api.delete(`/reward-items/${id}`)
}
