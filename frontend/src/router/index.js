/**
 * 路由配置文件
 * 定义了应用的所有页面路由，包括：
 * - 登录页和注册页（无需登录即可访问）
 * - 仪表盘首页、运动记录、运动类型管理、兑换中心、奖励管理、个人中心（需要登录才能访问）
 *
 * 路由守卫：
 * - 未登录用户访问需要认证的页面时，自动跳转到登录页
 * - 已登录用户访问登录页或注册页时，自动跳转到首页
 *
 * 所有页面组件采用懒加载（动态 import），提升首屏加载速度
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

// 路由配置列表
const routes = [
  // 登录页（无需登录）
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
  // 注册页（无需登录）
  { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
  // 仪表盘首页（需登录）
  { path: '/', name: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { auth: true } },
  // 运动记录页（需登录）
  { path: '/record', name: 'record', component: () => import('../views/RecordView.vue'), meta: { auth: true } },
  // 运动类型管理页（需登录，管理员功能）
  { path: '/exercise-admin', name: 'exercise-admin', component: () => import('../views/ExerciseTypeAdminView.vue'), meta: { auth: true } },
  // 兑换中心页（需登录）
  { path: '/rewards', name: 'rewards', component: () => import('../views/RewardView.vue'), meta: { auth: true } },
  // 奖励管理页（需登录，管理员功能）
  { path: '/reward-admin', name: 'reward-admin', component: () => import('../views/RewardAdminView.vue'), meta: { auth: true } },
  // 个人中心页（需登录）
  { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { auth: true } },
  // 小组页（需登录）
  { path: '/team', name: 'team', component: () => import('../views/TeamView.vue'), meta: { auth: true } },
]

// 创建路由实例，使用 HTML5 History 模式，基础路径与后端 context-path 匹配
const router = createRouter({
  history: createWebHistory('/sportplan-0.0.1-SNAPSHOT/'),
  routes,
})

// 标记是否已完成首次会话恢复，避免每次导航都请求后端
let sessionRestored = false

/**
 * 全局前置路由守卫
 * 首次访问时先通过后端接口验证 session 是否有效，再进行路由访问控制：
 * 1. 首次导航：调用 restoreSession() 恢复登录状态
 * 2. 需要登录的页面（meta.auth = true）：未登录用户重定向到登录页
 * 3. 登录/注册页：已登录用户重定向到首页（避免重复登录）
 * 4. 其他情况：正常放行
 */
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  // 首次访问时恢复会话，确保 localStorage 中的数据与服务端 session 一致
  if (!sessionRestored) {
    await userStore.restoreSession()
    sessionRestored = true
  }

  if (to.meta.auth && !userStore.currentUser) {
    // 需要登录但未登录，跳转到登录页
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && userStore.currentUser) {
    // 已登录用户访问登录/注册页，跳转到首页
    next('/')
  } else {
    // 正常放行
    next()
  }
})

export default router
