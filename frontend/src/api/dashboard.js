/**
 * 仪表盘相关 API 接口模块
 * 封装了仪表盘数据获取的接口，包括：
 * - 获取指定用户的仪表盘数据（今日积分、本周积分、累计积分、可用积分、本周每日积分等）
 * - 获取全员积分排行榜数据
 */
import api from './index'

/** 获取指定用户的仪表盘统计数据 */
export function getDashboard(userId) {
  return api.get(`/dashboard/${userId}`)
}

/** 获取全员积分排行榜（按累计积分排序） */
export function getLeaderboard() {
  return api.get('/dashboard/leaderboard')
}

/** 管理员获取所有用户本周每日积分 */
export function getAdminWeeklyScores() {
  return api.get('/dashboard/admin/weekly-scores')
}
