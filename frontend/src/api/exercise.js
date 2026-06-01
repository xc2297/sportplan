/**
 * 运动记录相关 API 接口模块
 * 封装了运动记录的增删查操作，包括：
 * - 提交运动记录（新增或更新）
 * - 查询指定用户指定日期的运动记录
 * - 查询指定用户指定日期范围内的运动记录
 * - 删除运动记录
 */
import api from './index'

/** 提交运动记录（包含用户ID、日期、运动类型ID、数量） */
export function submitRecord(data) {
  return api.post('/exercise-records', data)
}

/** 查询指定用户在指定日期的运动记录列表 */
export function getRecord(userId, date) {
  return api.get('/exercise-records', { params: { userId, date } })
}

/** 查询指定用户在指定日期范围内的运动记录列表 */
export function getRecordsByRange(userId, startDate, endDate) {
  return api.get('/exercise-records/range', { params: { userId, startDate, endDate } })
}

/** 根据 ID 删除运动记录 */
export function deleteRecord(id) {
  return api.delete(`/exercise-records/${id}`)
}

/** 获取日历视图数据，返回指定月份每天各用户积分汇总（yearMonth 格式：yyyy-MM） */
export function getCalendarData(yearMonth) {
  return api.get('/exercise-records/calendar', { params: { yearMonth } })
}

/** 获取某天的运动详情（date 格式：yyyy-MM-dd） */
export function getDayDetail(date) {
  return api.get('/exercise-records/day-detail', { params: { date } })
}

/** 上传图片文件，返回图片访问 URL */
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
