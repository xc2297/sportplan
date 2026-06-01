<script setup>
/**
 * 底部导航栏组件
 * 固定在页面底部，提供四个主要页面的导航入口：
 * - 首页（仪表盘）：查看积分概览和排行榜
 * - 记录：提交运动记录
 * - 兑换：积分兑换奖励商品
 * - 我的：个人信息和历史记录
 * 当前激活的导航项会高亮显示（根据路由路径匹配）
 */
import { useRoute } from 'vue-router'

// 当前路由信息，用于判断哪个导航项处于激活状态
const route = useRoute()

// 导航栏标签页配置：路径、图标、标签文字
const tabs = [
  { path: '/', icon: '🏠', label: '首页' },
  { path: '/record', icon: '📝', label: '记录' },
  { path: '/rewards', icon: '🎁', label: '兑换' },
  { path: '/team', icon: '👥', label: '小组' },
  { path: '/profile', icon: '👤', label: '我的' },
]
</script>

<template>
  <nav class="navbar">
    <router-link
      v-for="tab in tabs"
      :key="tab.path"
      :to="tab.path"
      class="nav-item"
      :class="{ active: route.path === tab.path }"
    >
      <span class="nav-icon">{{ tab.icon }}</span>
      <span class="nav-label">{{ tab.label }}</span>
    </router-link>
  </nav>
</template>

<style scoped>
.navbar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  display: flex;
  background: white;
  border-top: 1px solid var(--border);
  z-index: 50;
  padding: 6px 0 env(safe-area-inset-bottom);
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 0;
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 11px;
  transition: color 0.2s;
}

.nav-item.active {
  color: var(--primary);
}

.nav-icon {
  font-size: 22px;
  margin-bottom: 2px;
}

.nav-label {
  font-weight: 500;
}
</style>
