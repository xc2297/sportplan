<script setup>
/**
 * 注册页面组件
 * 提供新用户注册表单，包括账号、密码、确认密码、姓名和性别。
 * 注册成功后自动跳转到登录页面。
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createUser } from '../api/user'

// 路由实例，用于页面跳转
const router = useRouter()

// 注册表单数据对象
const form = ref({
  username: '',        // 账号
  password: '',        // 密码
  confirmPassword: '', // 确认密码
  name: '',            // 姓名
  gender: 'female'     // 性别，默认为女性
})
// 是否正在请求注册接口（控制按钮禁用状态）
const loading = ref(false)
// 错误提示信息
const error = ref('')

/**
 * 处理注册操作
 * 1. 校验必填项是否已填写
 * 2. 校验两次密码是否一致
 * 3. 校验密码长度是否满足最低要求（4位）
 * 4. 调用注册接口，成功后跳转到登录页
 */
async function handleRegister() {
  const { username, password, confirmPassword, name, gender } = form.value
  // 校验必填项：账号、密码、姓名不能为空
  if (!username.trim() || !password.trim() || !name.trim()) {
    error.value = '请填写所有必填项'
    return
  }
  // 校验两次密码是否一致
  if (password !== confirmPassword) {
    error.value = '两次密码不一致'
    return
  }
  // 校验密码最低长度
  if (password.length < 4) {
    error.value = '密码至少4位'
    return
  }
  loading.value = true
  error.value = ''
  try {
    // 调用用户注册接口
    await createUser({ username, password, name, gender })
    // 注册成功后跳转到登录页
    router.replace('/login')
  } catch (e) {
    error.value = e.message || '注册失败'
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
      <p class="login-subtitle">注册新账号</p>
    </div>

    <div class="card login-form">
      <div class="form-group">
        <label>账号</label>
        <input v-model="form.username" type="text" placeholder="请输入账号" />
      </div>
      <div class="form-group">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="请输入密码（至少4位）" />
      </div>
      <div class="form-group">
        <label>确认密码</label>
        <input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" @keyup.enter="handleRegister" />
      </div>
      <div class="form-group">
        <label>姓名</label>
        <input v-model="form.name" type="text" placeholder="请输入姓名" />
      </div>
      <div class="form-group">
        <label>性别</label>
        <select v-model="form.gender">
          <option value="female">女</option>
          <option value="male">男</option>
        </select>
      </div>
      <div v-if="error" class="login-error">{{ error }}</div>
      <button class="btn btn-primary btn-block" :disabled="loading" @click="handleRegister">
        {{ loading ? '注册中...' : '注 册' }}
      </button>
      <div class="login-link">
        已有账号？<router-link to="/login">去登录</router-link>
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
