/**
 * 用户相关 API 接口模块
 * 封装了用户管理相关的所有 HTTP 请求，包括：
 * - 用户注册（创建新用户）
 * - 查询用户列表
 * - 查询单个用户信息
 * - 用户登录认证
 * - 用户退出登录
 * - 获取当前登录用户信息（用于会话恢复）
 */
import api from './index'

/** 创建新用户（注册） */
export function createUser(data) {
  return api.post('/users', data)
}

/** 获取所有用户列表 */
export function getUsers() {
  return api.get('/users')
}

/** 根据 ID 获取单个用户信息 */
export function getUser(id) {
  return api.get(`/users/${id}`)
}

/** 用户登录（账号密码认证） */
export function login(data) {
  return api.post('/auth/login', data)
}

/** 用户退出登录 */
export function logout() {
  return api.post('/auth/logout')
}

/** 获取当前登录用户信息（用于页面刷新后恢复会话） */
export function getCurrentUser() {
  return api.get('/auth/current')
}
