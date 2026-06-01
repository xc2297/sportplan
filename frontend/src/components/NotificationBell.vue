// 通知铃铛组件：固定在页面右上角，显示未读数量红点，点击展开通知列表
// 支持定时轮询检查新通知，并可选浏览器 Notification 弹窗提醒
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getUnreadNotifications, markNotificationRead, markAllNotificationsRead } from '../api/notification'

const showPanel = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
let pollTimer = null

// 轮询间隔（毫秒）
const POLL_INTERVAL = 30000

// 拉取未读通知
async function fetchNotifications() {
  try {
    const res = await getUnreadNotifications()
    console.log('通知API返回:', res)
    const list = res.data || []
    const oldCount = unreadCount.value
    notifications.value = list
    unreadCount.value = list.length

    // 有新通知时，用浏览器 Notification 弹窗提醒
    if (list.length > oldCount && oldCount >= 0) {
      const newOnes = list.slice(0, list.length - oldCount)
      for (const n of newOnes) {
        showBrowserNotification(n)
      }
    }
  } catch (e) {
    console.error('获取通知失败:', e)
  }
}

// 浏览器原生通知弹窗
function showBrowserNotification(n) {
  if (!('Notification' in window)) return
  if (Notification.permission === 'granted') {
    new Notification(n.title, { body: n.content, tag: 'notification-' + n.id })
  } else if (Notification.permission === 'default') {
    Notification.requestPermission()
  }
}

// 标记单条已读
async function markRead(n) {
  if (n.read) return
  try {
    await markNotificationRead(n.id)
    n.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    // 从列表中移除已读项
    notifications.value = notifications.value.filter(item => item.id !== n.id)
  } catch {
    // 静默忽略
  }
}

// 全部标记已读
async function markAllRead() {
  if (notifications.value.length === 0) return
  try {
    await markAllNotificationsRead()
    notifications.value = []
    unreadCount.value = 0
  } catch {
    // 静默忽略
  }
}

// 点击铃铛切换面板
function togglePanel() {
  showPanel.value = !showPanel.value
  if (showPanel.value) fetchNotifications()
}

// 点击外部关闭面板
function onClickOutside(e) {
  if (showPanel.value && !e.target.closest('.notification-bell')) {
    showPanel.value = false
  }
}

// 启动轮询
function startPolling() {
  fetchNotifications()
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = setInterval(fetchNotifications, POLL_INTERVAL)
}

// 停止轮询
function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 格式化时间显示
function formatTime(createdAt) {
  if (!createdAt) return ''
  const date = new Date(createdAt)
  const now = new Date()
  const diffMs = now - date
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return diffMin + '分钟前'
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return diffHour + '小时前'
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return diffDay + '天前'
  return date.toLocaleDateString()
}

onMounted(() => {
  document.addEventListener('click', onClickOutside)
  // 组件挂载时自动启动轮询（通过 v-if 控制挂载，登录后自动开始）
  startPolling()
})

onUnmounted(() => {
  document.removeEventListener('click', onClickOutside)
  stopPolling()
})

// 暴露方法供 App.vue 调用
defineExpose({ startPolling, stopPolling })
</script>

<template>
  <div class="notification-bell">
    <button class="bell-btn" @click.stop="togglePanel">
      <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
        <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
      </svg>
      <!-- 未读数量红点 -->
      <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </button>

    <!-- 通知列表面板 -->
    <div v-if="showPanel" class="notification-panel">
      <div class="panel-header">
        <span>通知</span>
        <button v-if="notifications.length > 0" class="read-all-btn" @click="markAllRead">全部已读</button>
      </div>
      <div class="panel-body">
        <div v-if="notifications.length === 0" class="empty-tip">暂无通知</div>
        <div v-for="n in notifications" :key="n.id" class="notification-item" @click="markRead(n)">
          <div class="notification-title">{{ n.title }}</div>
          <div class="notification-content">{{ n.content }}</div>
          <div class="notification-time">{{ formatTime(n.createdAt) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notification-bell {
  position: fixed;
  top: 12px;
  right: 16px;
  z-index: 100;
}

.bell-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  color: #555;
  position: relative;
  transition: transform 0.15s;
}

.bell-btn:hover {
  transform: scale(1.08);
  color: #333;
}

.bell-btn:active {
  transform: scale(0.95);
}

.badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background: #ef4444;
  color: white;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  line-height: 1;
}

.notification-panel {
  position: absolute;
  top: 48px;
  right: 0;
  width: 300px;
  max-height: 400px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.15);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
  font-weight: 600;
  font-size: 15px;
  color: #333;
}

.read-all-btn {
  background: none;
  border: none;
  color: var(--primary);
  font-size: 13px;
  cursor: pointer;
}

.read-all-btn:hover {
  text-decoration: underline;
}

.panel-body {
  overflow-y: auto;
  max-height: 340px;
}

.empty-tip {
  padding: 32px 16px;
  text-align: center;
  color: #aaa;
  font-size: 14px;
}

.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.15s;
}

.notification-item:hover {
  background: #f9f9f9;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-title {
  font-weight: 600;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.notification-content {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
}

.notification-time {
  font-size: 12px;
  color: #aaa;
  margin-top: 4px;
}

@media (max-width: 380px) {
  .notification-panel {
    width: calc(100vw - 32px);
    right: -8px;
  }
}
</style>
