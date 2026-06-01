/**
 * 运动类型相关 API 接口模块
 * 封装了运动类型（如跑步、走路、俯卧撑等）的 CRUD 操作，包括：
 * - 查询运动类型列表（支持仅查询已启用的类型）
 * - 新增运动类型
 * - 更新运动类型信息
 * - 删除运动类型
 */
import api from './index'

/**
 * 获取运动类型列表
 * @param {boolean} activeOnly - 是否仅查询已启用的运动类型（true=仅启用，false/undefined=全部）
 */
export function getExerciseTypes(activeOnly) {
  const params = activeOnly ? { activeOnly: true } : {}
  return api.get('/exercise-types', { params })
}

/** 新增运动类型 */
export function createExerciseType(data) {
  return api.post('/exercise-types', data)
}

/** 根据 ID 更新运动类型信息 */
export function updateExerciseType(id, data) {
  return api.put(`/exercise-types/${id}`, data)
}

/** 根据 ID 删除运动类型 */
export function deleteExerciseType(id) {
  return api.delete(`/exercise-types/${id}`)
}
