/**
 * 用户状态管理 Store（Pinia）
 * 管理当前登录用户的状态信息，提供以下功能：
 * - 从 localStorage 恢复用户登录状态（页面刷新后保持登录）
 * - 用户登录（调用登录接口并保存用户信息到本地）
 * - 用户退出登录（清除本地和内存中的用户信息）
 * - 恢复会话（通过后端接口验证当前 session 是否有效）
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '../api/user'

export const useUserStore = defineStore('user', () => {
  // 当前登录用户信息，从 localStorage 初始化（页面刷新后恢复登录状态）
  const currentUser = ref(JSON.parse(localStorage.getItem('currentUser') || 'null'))

  /**
   * 用户登录
   * 调用后端登录接口，成功后将用户信息保存到响应式状态和 localStorage
   * @param {string} username - 用户名
   * @param {string} password - 密码
   */
  async function login(username, password) {
    const res = await loginApi({ username, password })
    currentUser.value = res.data.user
    // 持久化用户信息到 localStorage，实现页面刷新后保持登录
    localStorage.setItem('currentUser', JSON.stringify(res.data.user))
  }

  /**
   * 用户退出登录
   * 无论后端退出接口是否成功，都会清除本地用户信息
   * 使用 finally 确保即使后端请求失败也能正常清除本地状态
   */
  async function logout() {
    try {
      await logoutApi()
    } finally {
      // 清除内存中的用户状态
      currentUser.value = null
      // 清除 localStorage 中的持久化数据
      localStorage.removeItem('currentUser')
    }
  }

  /**
   * 恢复会话
   * 通过后端接口验证当前 session 是否有效，更新用户信息
   * 如果 session 无效，清除本地存储的用户信息
   * 用于页面刷新或重新打开时验证登录状态
   */
  async function restoreSession() {
    try {
      const res = await getCurrentUser()
      // 后端返回成功且有用户数据，更新本地状态
      if (res.code === 200 && res.data) {
        currentUser.value = res.data
        localStorage.setItem('currentUser', JSON.stringify(res.data))
      } else {
        // 后端返回异常，清除本地状态
        currentUser.value = null
        localStorage.removeItem('currentUser')
      }
    } catch {
      // 请求失败（如网络错误），清除本地状态
      currentUser.value = null
      localStorage.removeItem('currentUser')
    }
  }

  // 导出状态和方法供组件使用
  return { currentUser, login, logout, restoreSession }
})
