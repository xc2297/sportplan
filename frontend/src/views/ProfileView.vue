<script setup>
/**
 * 个人中心页面组件
 * 日历视图展示运动记录，功能包括：
 * - 日历格式展示每天运动积分（普通用户显示自己，管理员显示所有用户对比）
 * - 支持前后翻月查看历史数据
 * - 点击日期查看当天运动详情
 * - 兑换历史记录列表
 * - 退出登录功能
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getCalendarData, getDayDetail, deleteRecord } from '../api/exercise'
import { getRedemptionHistory, cancelRedemption } from '../api/redemption'

const router = useRouter()
const userStore = useUserStore()

// 当前激活的标签页（exercise=运动记录，redemption=兑换记录）
const activeTab = ref('exercise')

// 日历相关状态
const now = new Date()
// 当前查看的年份和月份
const viewYear = ref(now.getFullYear())
const viewMonth = ref(now.getMonth() + 1) // 1~12
// 日历数据：后端返回的有数据的日期列表
const calendarDays = ref([])
// 日历加载状态
const calendarLoading = ref(false)

// 详情弹窗相关状态
const showDetail = ref(false)
const detailLoading = ref(false)
const detailData = ref(null) // DayDetailResponse
const previewImage = ref(null) // 图片预览 URL

// 兑换历史记录列表
const redemptions = ref([])

// 当前用户是否为管理员
const isAdmin = computed(() => userStore.currentUser?.isAdmin)

// 今天的日期字符串
const todayStr = new Date().toISOString().slice(0, 10)

/**
 * 计算属性：当前查看月份的显示文本，如 "2026年5月"
 */
const monthLabel = computed(() => `${viewYear.value}年${viewMonth.value}月`)

/**
 * 计算属性：当前查看月份的 yearMonth 参数，如 "2026-05"
 */
const yearMonthParam = computed(() =>
  `${viewYear.value}-${String(viewMonth.value).padStart(2, '0')}`
)

/**
 * 计算属性：当前月份的天数
 */
const daysInMonth = computed(() => {
  return new Date(viewYear.value, viewMonth.value, 0).getDate()
})

/**
 * 计算属性：当前月份第一天是星期几（0=周日, 1=周一, ..., 6=周六）
 * 转换为周一起始（0=周一, ..., 6=周日）
 */
const firstDayOffset = computed(() => {
  const day = new Date(viewYear.value, viewMonth.value - 1, 1).getDay()
  // 将周日=0 转为 6，其余减1
  return day === 0 ? 6 : day - 1
})

/**
 * 计算属性：生成日历网格数据
 * 返回一个数组，每个元素为 null（空格）或日期数字
 */
const calendarCells = computed(() => {
  const cells = []
  // 前面补空格
  for (let i = 0; i < firstDayOffset.value; i++) {
    cells.push(null)
  }
  // 填充日期
  for (let d = 1; d <= daysInMonth.value; d++) {
    cells.push(d)
  }
  return cells
})

/**
 * 获取指定日期的日历数据（后端返回的积分数据）
 * @param {number} day - 日期数字（1~31）
 * @returns {Object|null} 该天的日历数据
 */
function getDayData(day) {
  const dateStr = `${viewYear.value}-${String(viewMonth.value).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  return calendarDays.value.find(d => d.date === dateStr) || null
}

/**
 * 判断指定日期是否为今天
 * @param {number} day - 日期数字
 * @returns {boolean}
 */
function isToday(day) {
  const t = new Date()
  return viewYear.value === t.getFullYear() &&
    viewMonth.value === t.getMonth() + 1 &&
    day === t.getDate()
}

/**
 * 格式化用户积分显示文本
 * 普通用户显示总分，管理员显示各用户分数列表
 * @param {Object} dayData - 日历日期数据
 * @returns {string} 显示文本
 */
function formatScoreText(dayData) {
  if (!dayData || !dayData.users || dayData.users.length === 0) return ''
  if (dayData.users.length === 1) {
    return dayData.users[0].totalScore + '分'
  }
  // 管理员模式：显示 "张三:20 李四:30"
  return dayData.users.map(u => `${u.userName}:${u.totalScore}`).join(' ')
}

/**
 * 切换到上一个月
 */
function prevMonth() {
  if (viewMonth.value === 1) {
    viewMonth.value = 12
    viewYear.value--
  } else {
    viewMonth.value--
  }
  loadCalendar()
}

/**
 * 切换到下一个月
 */
function nextMonth() {
  if (viewMonth.value === 12) {
    viewMonth.value = 1
    viewYear.value++
  } else {
    viewMonth.value++
  }
  loadCalendar()
}

/**
 * 点击日历日期，打开详情弹窗
 * @param {number} day - 日期数字
 */
async function openDetail(day) {
  const dayData = getDayData(day)
  if (!dayData) return // 无数据的日子不弹出

  const dateStr = dayData.date
  showDetail.value = true
  detailLoading.value = true
  detailData.value = null

  try {
    const res = await getDayDetail(dateStr)
    detailData.value = res.data
  } catch (e) {
    console.error(e)
    alert(e.message || '加载详情失败')
    showDetail.value = false
  }
  detailLoading.value = false
}

/**
 * 关闭详情弹窗
 */
function closeDetail() {
  showDetail.value = false
  detailData.value = null
}

/**
 * 删除运动记录（在详情弹窗中操作）
 * @param {Object} r - 要删除的记录对象
 */
async function handleDelete(r) {
  if (!confirm(`确定删除「${r.exerciseTypeName} ${r.amount}${r.unit}」吗？`)) return
  try {
    await deleteRecord(r.id)
    // 删除后刷新详情和日历数据
    if (detailData.value) {
      openDetail(parseInt(detailData.value.date.slice(8, 10)))
    }
    loadCalendar()
  } catch (e) {
    alert(e.message)
  }
}

/**
 * 加载日历数据
 * 调用后端日历接口获取当前月份的积分数据
 */
async function loadCalendar() {
  calendarLoading.value = true
  try {
    const res = await getCalendarData(yearMonthParam.value)
    calendarDays.value = res.data || []
  } catch (e) {
    console.error(e)
    calendarDays.value = []
  }
  calendarLoading.value = false
}

/**
 * 加载兑换历史记录
 */
async function loadRedemptions() {
  if (!userStore.currentUser) return
  try {
    // 管理员不传userId，后端返回全部用户的记录；普通用户只查自己的
    const res = await getRedemptionHistory(isAdmin.value ? null : userStore.currentUser.id)
    redemptions.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

/** 管理员撤销兑换，退还积分 */
async function handleCancelRedemption(item) {
  if (!confirm(`确定撤销「${item.rewardName}」的兑换吗？积分将退回。`)) return
  try {
    await cancelRedemption(item.id)
    loadRedemptions()
  } catch (e) {
    alert(e.message)
  }
}

/**
 * 处理退出登录
 */
async function handleLogout() {
  await userStore.logout()
  router.replace('/login')
}

// 组件挂载时加载数据
onMounted(() => {
  loadCalendar()
  loadRedemptions()
})
</script>

<template>
  <div>
    <h1 class="page-title">个人中心</h1>

    <!-- 用户信息卡片 -->
    <div class="card" style="margin-bottom: 16px">
      <div style="display: flex; align-items: center; gap: 12px">
        <span style="font-size: 40px">
          {{ userStore.currentUser?.gender === 'female' ? '👩' : '👨' }}
        </span>
        <div style="flex: 1">
          <div style="font-weight: 700; font-size: 18px">{{ userStore.currentUser?.name }}</div>
          <div style="font-size: 13px; color: var(--text-secondary)">
            {{ userStore.currentUser?.gender === 'female' ? '女' : '男' }}
          </div>
        </div>
        <button class="btn btn-logout" @click="handleLogout">退出登录</button>
      </div>
    </div>

    <!-- 标签栏切换 -->
    <div class="tab-bar">
      <div
        class="tab-item"
        :class="{ active: activeTab === 'exercise' }"
        @click="activeTab = 'exercise'"
      >
        运动记录
      </div>
      <div
        class="tab-item"
        :class="{ active: activeTab === 'redemption' }"
        @click="activeTab = 'redemption'"
      >
        兑换记录
      </div>
    </div>

    <!-- 运动记录 - 日历视图 -->
    <div v-if="activeTab === 'exercise'" class="card">
      <!-- 月份导航栏 -->
      <div class="month-nav">
        <button class="nav-btn" @click="prevMonth">&lt;</button>
        <span class="month-label">{{ monthLabel }}</span>
        <button class="nav-btn" @click="nextMonth">&gt;</button>
      </div>

      <!-- 日历网格 -->
      <div class="calendar-grid">
        <!-- 星期头部 -->
        <div class="weekday-header">一</div>
        <div class="weekday-header">二</div>
        <div class="weekday-header">三</div>
        <div class="weekday-header">四</div>
        <div class="weekday-header">五</div>
        <div class="weekday-header">六</div>
        <div class="weekday-header">日</div>

        <!-- 日期格子 -->
        <div
          v-for="(cell, idx) in calendarCells"
          :key="idx"
          class="day-cell"
          :class="{
            'empty': cell === null,
            'today': cell !== null && isToday(cell),
            'has-data': cell !== null && getDayData(cell),
            'clickable': cell !== null && getDayData(cell)
          }"
          @click="cell !== null && getDayData(cell) && openDetail(cell)"
        >
          <template v-if="cell !== null">
            <div class="day-number">{{ cell }}</div>
            <div v-if="getDayData(cell)" class="day-scores">
              <template v-if="getDayData(cell).users.length === 1">
                <span class="score-single">{{ getDayData(cell).users[0].totalScore }}分</span>
              </template>
              <template v-else>
                <div
                  v-for="u in getDayData(cell).users.slice(0, 3)"
                  :key="u.userId"
                  class="score-user"
                >
                  {{ u.userName }}:{{ u.totalScore }}
                </div>
                <div v-if="getDayData(cell).users.length > 3" class="score-more">
                  +{{ getDayData(cell).users.length - 3 }}人
                </div>
              </template>
            </div>
          </template>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="calendarLoading" class="empty-state" style="padding: 20px">加载中...</div>
    </div>

    <!-- 兑换记录 -->
    <div v-else class="card">
      <div class="section-title">兑换历史</div>
      <div v-if="redemptions.length === 0" class="empty-state">暂无兑换记录</div>
      <div v-for="item in redemptions" :key="item.id" class="record-item">
        <div style="flex:1; min-width:0">
          <div v-if="isAdmin && item.userName" class="redemption-user-name">{{ item.userName }}</div>
          <div class="reward-name">
            {{ item.rewardName }}
            <span v-if="item.status === 'used'" class="status-used-badge">已使用</span>
            <span v-if="item.status === 'cancelled'" class="status-cancelled">已撤销</span>
          </div>
          <div class="record-date">{{ item.redeemedAt ? item.redeemedAt.slice(0, 16).replace('T', ' ') : '' }}</div>
          <div v-if="item.status === 'used' && item.usedDescription" class="redemption-used-desc">{{ item.usedDescription }}</div>
          <div v-if="item.status === 'used' && item.usedImages && item.usedImages.length" class="redemption-used-images">
            <img v-for="(img, idx) in item.usedImages" :key="idx" :src="img" class="redemption-used-thumb" @click="previewImage = img" />
          </div>
        </div>
        <div style="display:flex; align-items:center; gap:8px; flex-shrink:0">
          <div :class="item.status === 'cancelled' ? 'record-refund' : 'record-cost'">
            {{ item.status === 'cancelled' ? '+' : '-' }}{{ item.pointsCost }}分
          </div>
          <button v-if="isAdmin && item.status === 'active'" class="btn-cancel-redemption" @click="handleCancelRedemption(item)">撤销</button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗（遮罩层） -->
    <div v-if="showDetail" class="modal-overlay" @click.self="closeDetail">
      <div class="modal-content">
        <div class="modal-header">
          <span class="modal-title">{{ detailData?.date }} 运动详情</span>
          <button class="modal-close" @click="closeDetail">&times;</button>
        </div>

        <!-- 加载中 -->
        <div v-if="detailLoading" class="empty-state" style="padding: 40px">加载中...</div>

        <!-- 详情内容 -->
        <template v-if="!detailLoading && detailData">
          <div v-if="detailData.users.length === 0" class="empty-state">暂无记录</div>

          <!-- 单用户模式（普通用户或管理员查看只有一人的情况） -->
          <div v-if="detailData.users.length === 1" class="detail-single">
            <div class="detail-user-header">
              <span class="detail-user-name">{{ detailData.users[0].userName }}</span>
              <span class="detail-total-score">总计 {{ detailData.users[0].totalScore }}分</span>
            </div>
            <div
              v-for="r in detailData.users[0].records"
              :key="r.id"
              class="record-item"
            >
              <div class="record-info-text">
                <div class="record-name">{{ r.exerciseTypeName }} {{ r.amount }}{{ r.unit }}</div>
                <img v-if="r.imageUrl" :src="r.imageUrl" class="detail-thumb" @click="previewImage = r.imageUrl" />
              </div>
              <div style="display: flex; align-items: center; gap: 8px">
                <span :class="Number(r.score) < 0 ? 'record-penalty' : 'record-score'">
                  {{ Number(r.score) < 0 ? r.score : '+' + r.score }}分
                </span>
                <button
                  v-if="r.exerciseTypeName === '未运动惩罚' ? isAdmin : (detailData.date === todayStr || isAdmin)"
                  class="btn-del-small"
                  @click="handleDelete(r)"
                >删除</button>
              </div>
            </div>
          </div>

          <!-- 多用户对比模式（管理员） -->
          <div v-if="detailData.users.length > 1" class="detail-compare">
            <div
              v-for="u in detailData.users"
              :key="u.userId"
              class="compare-card"
            >
              <div class="compare-header">
                <span class="compare-name">{{ u.userName }}</span>
                <span class="compare-score">{{ u.totalScore }}分</span>
              </div>
              <div
                v-for="r in u.records"
                :key="r.id"
                class="compare-record"
              >
                <div class="compare-record-left">
                  <img v-if="r.imageUrl" :src="r.imageUrl" class="compare-thumb" @click="previewImage = r.imageUrl" />
                  <span>{{ r.exerciseTypeName }} {{ r.amount }}{{ r.unit }}</span>
                </div>
                <span class="record-score">+{{ r.score }}</span>
              </div>
              <div v-if="u.records.length === 0" class="compare-empty">无记录</div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 图片预览弹窗 -->
    <div v-if="previewImage" class="preview-overlay" @click="previewImage = null">
      <img :src="previewImage" class="preview-img" />
    </div>
  </div>
</template>

<style scoped>
/* 标签栏 */
.tab-bar {
  display: flex;
  margin-bottom: 12px;
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-item.active {
  color: var(--primary);
  background: #f0f0ff;
}

/* 月份导航 */
.month-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.nav-btn {
  width: 32px;
  height: 32px;
  border: 1px solid var(--border);
  background: white;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}

.nav-btn:hover {
  background: #f5f5f5;
}

.month-label {
  font-size: 16px;
  font-weight: 700;
}

/* 日历网格 */
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.weekday-header {
  text-align: center;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
  padding: 4px 0;
}

.day-cell {
  min-height: 44px;
  padding: 3px 2px;
  border-radius: 4px;
  background: #fafafa;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.day-cell.empty {
  background: transparent;
}

.day-cell.today {
  border: 2px solid var(--primary);
}

.day-cell.has-data {
  background: #f0f5ff;
}

.day-cell.clickable {
  cursor: pointer;
}

.day-cell.clickable:hover {
  background: #e0eaff;
}

.day-number {
  font-size: 11px;
  color: var(--text-secondary);
  font-weight: 600;
  line-height: 1;
}

.day-scores {
  margin-top: 2px;
  text-align: center;
  width: 100%;
}

.score-single {
  font-size: 11px;
  font-weight: 700;
  color: var(--primary);
  line-height: 1.2;
}

.score-user {
  font-size: 9px;
  color: var(--text-secondary);
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.score-more {
  font-size: 9px;
  color: var(--text-secondary);
}

/* 记录列表 */
.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}

.record-item:last-child {
  border-bottom: none;
}

.record-date {
  font-size: 13px;
  color: var(--text-secondary);
}

.record-info-text {
  flex: 1;
  font-size: 13px;
  color: var(--text-secondary);
}

.record-score {
  font-weight: 700;
  color: var(--success);
  font-size: 14px;
}

.record-penalty {
  font-weight: 700;
  color: var(--danger);
  font-size: 14px;
}

.record-cost {
  font-weight: 700;
  color: var(--danger);
  font-size: 15px;
}

/* 退出登录按钮 */
.btn-logout {
  background: #fef2f2;
  color: var(--danger);
  border: 1px solid #fecaca;
  padding: 6px 14px;
  font-size: 13px;
}

.btn-logout:hover {
  background: #fee2e2;
}

/* 删除小按钮 */
.btn-del-small {
  background: #fef2f2;
  color: var(--danger);
  border: none;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.btn-del-small:hover {
  background: #fee2e2;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 16px;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 480px;
  max-height: 80vh;
  overflow-y: auto;
  padding: 20px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.modal-title {
  font-size: 16px;
  font-weight: 700;
}

.modal-close {
  width: 28px;
  height: 28px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}

.modal-close:hover {
  background: #eee;
}

/* 详情 - 单用户 */
.detail-user-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

.detail-user-name {
  font-weight: 700;
  font-size: 15px;
}

.detail-total-score {
  font-weight: 700;
  color: var(--primary);
  font-size: 15px;
}

/* 详情 - 多用户对比 */
.detail-compare {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.compare-card {
  background: #f8f9ff;
  border-radius: 8px;
  padding: 12px;
}

.compare-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.compare-name {
  font-weight: 700;
  font-size: 14px;
}

.compare-score {
  font-weight: 700;
  color: var(--primary);
  font-size: 14px;
}

.compare-record {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.compare-empty {
  font-size: 13px;
  color: var(--text-secondary);
  text-align: center;
  padding: 8px;
}

/* 兑换撤销相关样式 */
.redemption-user-name {
  font-size: 12px;
  color: var(--primary);
  font-weight: 600;
  margin-bottom: 2px;
}

.status-cancelled {
  font-size: 12px;
  color: var(--text-secondary);
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 6px;
}

.status-used-badge {
  font-size: 12px;
  color: white;
  background: var(--success);
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 6px;
}

.redemption-used-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
  line-height: 1.4;
}

.redemption-used-images {
  display: flex;
  gap: 6px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.redemption-used-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid var(--border);
}

.record-refund {
  font-weight: 700;
  color: var(--success);
  font-size: 15px;
}

.btn-cancel-redemption {
  background: #fef2f2;
  color: var(--danger);
  border: none;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.btn-cancel-redemption:hover {
  background: #fee2e2;
}

/* 详情弹窗缩略图样式 */
.record-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #333);
  margin-bottom: 4px;
}

.detail-thumb {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid var(--border);
}

.compare-record-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.compare-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid var(--border);
  flex-shrink: 0;
}

/* 图片预览弹窗 */
.preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 16px;
  cursor: pointer;
}

.preview-img {
  max-width: 100%;
  max-height: 90vh;
  border-radius: 8px;
  object-fit: contain;
}
</style>
