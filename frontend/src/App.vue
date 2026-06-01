<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from './stores/user'
import NavBar from './components/NavBar.vue'
import ChatAssistant from './components/ChatAssistant.vue'
import NotificationBell from './components/NotificationBell.vue'

const route = useRoute()
const userStore = useUserStore()
const showNav = computed(() => route.path !== '/login' && route.path !== '/register' && userStore.currentUser)
</script>

<template>
  <div class="app">
    <main class="main-content" :class="{ 'with-nav': showNav }">
      <router-view />
    </main>
    <!-- 通知铃铛：登录后显示，组件内部自动管理轮询 -->
    <NotificationBell v-if="showNav" />
    <NavBar v-if="showNav" />
    <ChatAssistant v-if="showNav" />
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.main-content {
  flex: 1;
  padding: 16px;
}
.with-nav {
  padding-bottom: 70px;
}
</style>
