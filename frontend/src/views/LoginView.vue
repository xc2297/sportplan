<script setup>
/**
 * 登录页面组件
 * 提供用户登录表单，包括账号和密码输入框。
 * 登录成功后自动跳转到首页（仪表盘）。
 * 如果用户未注册，可点击链接跳转到注册页面。
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

// 路由实例，用于页面跳转
const router = useRouter()
// 用户状态管理 store，用于执行登录操作和存储用户信息
const userStore = useUserStore()

// 表单数据：用户名
const username = ref('')
// 表单数据：密码
const password = ref('')
// 是否正在请求登录接口（控制按钮禁用状态和加载提示）
const loading = ref(false)
// 错误提示信息
const error = ref('')

/**
 * 处理登录操作
 * 1. 校验账号和密码是否为空
 * 2. 调用 store 的 login 方法进行登录
 * 3. 登录成功后跳转到首页，失败则显示错误信息
 */
async function handleLogin() {
  // 表单校验：账号和密码不能为空
  if (!username.value.trim() || !password.value.trim()) {
    error.value = '请输入账号和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    // 调用 store 中的登录方法，内部会请求后端接口并保存用户信息
    await userStore.login(username.value, password.value)
    // 登录成功，跳转到首页（使用 replace 避免回退到登录页）
    router.replace('/')
  } catch (e) {
    // 登录失败，显示错误信息
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-header">
      <div class="login-logo">🏃</div>
      <h1 class="login-title">运动积分系统</h1>
      <p class="login-subtitle">登录你的账号</p>
    </div>

    <div class="card login-form">
      <div class="form-group">
        <label>账号</label>
        <input
          v-model="username"
          type="text"
          placeholder="请输入账号"
          @keyup.enter="handleLogin"
        />
      </div>
      <div class="form-group">
        <label>密码</label>
        <input
          v-model="password"
          type="password"
          placeholder="请输入密码"
          @keyup.enter="handleLogin"
        />
      </div>
      <div v-if="error" class="login-error">{{ error }}</div>
      <button
        class="btn btn-primary btn-block"
        :disabled="loading"
        @click="handleLogin"
      >
        {{ loading ? '登录中...' : '登 录' }}
      </button>
      <div class="login-link">
        没有账号？<router-link to="/register">去注册</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 24px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  font-size: 48px;
  margin-bottom: 12px;
}

.login-title {
  font-size: 24px;
  font-weight: 700;
}

.login-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.login-form .btn {
  margin-top: 8px;
}

.login-error {
  color: var(--danger);
  font-size: 13px;
  margin-bottom: 8px;
  text-align: center;
}

.login-link {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: var(--text-secondary);
}

.login-link a {
  color: var(--primary);
  font-weight: 600;
}
</style>
