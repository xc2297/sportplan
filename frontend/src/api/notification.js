import api from './index'

// 查询未读通知
export function getUnreadNotifications() {
  return api.get('/notifications')
}

// 标记单条已读
export function markNotificationRead(id) {
  return api.post(`/notifications/${id}/read`)
}

// 全部标记已读
export function markAllNotificationsRead() {
  return api.post('/notifications/read-all')
}
