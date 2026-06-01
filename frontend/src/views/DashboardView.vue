<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useUserStore } from '../stores/user'
import { getDashboard, getLeaderboard, getAdminWeeklyScores } from '../api/dashboard'

const COLORS = ['#4f46e5','#10b981','#f59e0b','#ef4444','#8b5cf6','#ec4899','#14b8a6','#f97316','#6366f1','#84cc16']

const userStore = useUserStore()
const dashboard = ref(null)
const leaderboard = ref([])
const loading = ref(false)
const chartCanvas = ref(null)
const adminWeeklyData = ref(null)

const isAdmin = computed(() => userStore.currentUser?.isAdmin)

// 存储绘制后的节点坐标，用于点击命中检测
const chartPoints = ref([])  // [{ x, y, name, date, score, color }]

// tooltip 状态
const tooltip = ref(null)  // { x, y, name, date, score, color }

async function loadData() {
  if (!userStore.currentUser) return
  loading.value = true
  tooltip.value = null
  try {
    const [dashRes, lbRes] = await Promise.all([
      getDashboard(userStore.currentUser.id),
      getLeaderboard(),
    ])
    dashboard.value = dashRes.data
    leaderboard.value = lbRes.data || []

    if (isAdmin.value) {
      try {
        const adminRes = await getAdminWeeklyScores()
        adminWeeklyData.value = adminRes.data
      } catch {
        adminWeeklyData.value = null
      }
    }

    await nextTick()
    requestAnimationFrame(drawChart)
  } catch (e) {
    console.error(e)
  }
  loading.value = false
}

function onChartClick(e) {
  const canvas = chartCanvas.value
  if (!canvas || !chartPoints.value.length) return

  const rect = canvas.getBoundingClientRect()
  const mx = e.clientX - rect.left
  const my = e.clientY - rect.top

  const HIT_RADIUS = 16
  let closest = null
  let closestDist = Infinity
  for (const p of chartPoints.value) {
    const dist = Math.sqrt((p.x - mx) ** 2 + (p.y - my) ** 2)
    if (dist < HIT_RADIUS && dist < closestDist) {
      closestDist = dist
      closest = p
    }
  }

  if (closest) {
    tooltip.value = { ...closest }
  } else {
    tooltip.value = null
  }
}

function hideTooltip() {
  tooltip.value = null
}

function drawChart() {
  const canvas = chartCanvas.value
  if (!canvas) return

  const adminData = adminWeeklyData.value
  if (isAdmin.value && adminData?.users?.length) {
    drawMultiUserChart(canvas, adminData)
  } else if (dashboard.value?.weekDailyScores?.length) {
    drawSingleChart(canvas, dashboard.value.weekDailyScores)
  }
}

function drawMultiUserChart(canvas, adminData) {
  const W = canvas.offsetWidth || 300
  const H = canvas.offsetHeight || 200
  const dpr = window.devicePixelRatio || 1
  canvas.width = W * dpr
  canvas.height = H * dpr
  const ctx = canvas.getContext('2d')
  ctx.scale(dpr, dpr)

  const padL = 28, padR = 8, padT = 20, padB = 28
  const chartW = W - padL - padR
  const chartH = H - padT - padB

  const users = adminData.users
  const dayCount = users[0]?.scores?.length || 1
  let maxDataScore = 0
  users.forEach(u => u.scores.forEach(s => {
    const v = Number(s.score) || 0
    if (v > maxDataScore) maxDataScore = v
  }))
  const maxScore = Math.ceil(maxDataScore / 10) * 10 || 10

  drawGrid(ctx, padL, padT, chartW, chartH, maxScore)
  drawXLabels(ctx, padL, padT, chartW, chartH, users[0].scores)

  const allPoints = []
  users.forEach((user, ui) => {
    const color = COLORS[ui % COLORS.length]
    const scores = user.scores.map(s => ({ ...s, score: Number(s.score) || 0 }))
    const points = scores.map((d, i) => ({
      x: padL + (chartW / Math.max(dayCount - 1, 1)) * i,
      y: padT + chartH - (d.score / maxScore) * chartH,
      score: d.score,
      date: d.date,
      name: user.name,
      color,
    }))

    ctx.beginPath()
    ctx.strokeStyle = color
    ctx.lineWidth = 2
    ctx.lineJoin = 'round'
    ctx.lineCap = 'round'
    points.forEach((p, i) => i === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y))
    ctx.stroke()

    points.forEach(p => {
      ctx.beginPath()
      ctx.arc(p.x, p.y, 3, 0, Math.PI * 2)
      ctx.fillStyle = '#fff'
      ctx.fill()
      ctx.strokeStyle = color
      ctx.lineWidth = 2
      ctx.stroke()
    })

    allPoints.push(...points)
  })
  chartPoints.value = allPoints
}

function drawSingleChart(canvas, rawScores) {
  const W = canvas.offsetWidth || 300
  const H = canvas.offsetHeight || 200
  const dpr = window.devicePixelRatio || 1
  canvas.width = W * dpr
  canvas.height = H * dpr
  const ctx = canvas.getContext('2d')
  ctx.scale(dpr, dpr)

  const scores = rawScores.map(d => ({ ...d, score: Number(d.score) || 0 }))
  const maxDataScore = Math.max(...scores.map(d => d.score), 0)
  const maxScore = Math.ceil(maxDataScore / 10) * 10 || 10

  const padL = 28, padR = 8, padT = 20, padB = 28
  const chartW = W - padL - padR
  const chartH = H - padT - padB

  drawGrid(ctx, padL, padT, chartW, chartH, maxScore)
  drawXLabels(ctx, padL, padT, chartW, chartH, scores)

  const points = scores.map((d, i) => ({
    x: padL + (chartW / Math.max(scores.length - 1, 1)) * i,
    y: padT + chartH - (d.score / maxScore) * chartH,
    score: d.score,
    date: d.date,
    name: null,
    color: '#4f46e5',
  }))

  const grad = ctx.createLinearGradient(0, padT, 0, padT + chartH)
  grad.addColorStop(0, 'rgba(79,70,229,0.25)')
  grad.addColorStop(1, 'rgba(79,70,229,0.02)')
  ctx.beginPath()
  ctx.moveTo(points[0].x, padT + chartH)
  points.forEach(p => ctx.lineTo(p.x, p.y))
  ctx.lineTo(points[points.length - 1].x, padT + chartH)
  ctx.closePath()
  ctx.fillStyle = grad
  ctx.fill()

  ctx.beginPath()
  ctx.strokeStyle = '#4f46e5'
  ctx.lineWidth = 2.5
  ctx.lineJoin = 'round'
  ctx.lineCap = 'round'
  points.forEach((p, i) => i === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y))
  ctx.stroke()

  points.forEach(p => {
    ctx.beginPath()
    ctx.arc(p.x, p.y, 4, 0, Math.PI * 2)
    ctx.fillStyle = '#fff'
    ctx.fill()
    ctx.strokeStyle = '#4f46e5'
    ctx.lineWidth = 2
    ctx.stroke()
    ctx.fillStyle = '#374151'
    ctx.font = 'bold 11px -apple-system, sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'bottom'
    ctx.fillText(p.score, p.x, p.y - 8)
  })

  chartPoints.value = points
}

function drawGrid(ctx, padL, padT, chartW, chartH, maxScore) {
  ctx.strokeStyle = '#f0f0f0'
  ctx.lineWidth = 1
  const gridLines = 5
  for (let i = 0; i <= gridLines; i++) {
    const val = (maxScore / gridLines) * i
    const y = padT + chartH - (val / maxScore) * chartH
    ctx.beginPath()
    ctx.moveTo(padL, y)
    ctx.lineTo(padL + chartW, y)
    ctx.stroke()
    ctx.fillStyle = '#9ca3af'
    ctx.font = '11px -apple-system, sans-serif'
    ctx.textAlign = 'right'
    ctx.textBaseline = 'middle'
    ctx.fillText(val, padL - 6, y)
  }
}

function drawXLabels(ctx, padL, padT, chartW, chartH, scores) {
  const dayCount = scores.length
  ctx.fillStyle = '#9ca3af'
  ctx.font = '11px -apple-system, sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'top'
  scores.forEach((d, i) => {
    const x = padL + (chartW / Math.max(dayCount - 1, 1)) * i
    ctx.fillText(d.date.slice(5), x, padT + chartH + 8)
  })
}

watch(() => userStore.currentUser, loadData)
onMounted(loadData)
</script>

<template>
  <div>
    <h1 class="page-title">运动积分</h1>

    <div v-if="loading" class="empty-state">加载中...</div>

    <div v-else-if="dashboard">
      <div class="stats-grid card">
        <div class="stat-card">
          <div class="value">{{ dashboard.todayScore }}</div>
          <div class="label">今日积分</div>
        </div>
        <div class="stat-card">
          <div class="value">{{ dashboard.weekScore }}</div>
          <div class="label">近7天积分</div>
        </div>
        <div class="stat-card">
          <div class="value">{{ dashboard.totalEarned }}</div>
          <div class="label">累计积分</div>
        </div>
        <div class="stat-card">
          <div class="value">{{ dashboard.availablePoints }}</div>
          <div class="label">可用积分</div>
        </div>
      </div>

      <div v-if="isAdmin && adminWeeklyData?.users?.length || dashboard.weekDailyScores?.length" class="card" style="margin-top: 12px">
        <div class="section-title">近7天每日积分</div>
        <div class="chart-wrapper">
          <canvas ref="chartCanvas" class="line-chart" @click="onChartClick"></canvas>
          <div
            v-if="tooltip"
            class="chart-tooltip"
            :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }"
            @click.stop
          >
            <div v-if="tooltip.name" class="tooltip-name">
              <span class="tooltip-dot" :style="{ background: tooltip.color }"></span>
              {{ tooltip.name }}
            </div>
            <div class="tooltip-date">{{ tooltip.date }}</div>
            <div class="tooltip-score">{{ tooltip.score }} 分</div>
          </div>
        </div>
        <div v-if="isAdmin && adminWeeklyData?.users?.length" class="chart-legend">
          <span
            v-for="(user, i) in adminWeeklyData.users"
            :key="user.userId"
            class="legend-item"
          >
            <span class="legend-dot" :style="{ background: COLORS[i % COLORS.length] }"></span>
            {{ user.name }}
          </span>
        </div>
      </div>

      <div v-if="leaderboard.length" class="card" style="margin-top: 12px">
        <div class="section-title">积分排行榜</div>
        <div class="rank-item" v-for="(entry, index) in leaderboard" :key="entry.userId">
          <div
            class="rank-number"
            :class="{ gold: index === 0, silver: index === 1, bronze: index === 2 }"
          >
            {{ index + 1 }}
          </div>
          <div class="rank-info">
            <div class="rank-name">
              {{ entry.gender === 'female' ? '👩' : '👨' }} {{ entry.name }}
            </div>
            <div class="rank-score">可用 {{ entry.availablePoints }} 分</div>
          </div>
          <div class="rank-points">{{ entry.totalEarned }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chart-wrapper {
  position: relative;
}

.line-chart {
  width: 100%;
  height: 200px;
  display: block;
  cursor: pointer;
}

.chart-tooltip {
  position: absolute;
  background: #fff;
  border-radius: 8px;
  padding: 8px 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  transform: translate(-50%, -100%);
  pointer-events: auto;
  z-index: 10;
  white-space: nowrap;
  margin-top: -12px;
}

.tooltip-name {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.tooltip-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.tooltip-date {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.tooltip-score {
  font-size: 15px;
  font-weight: 700;
  color: var(--primary);
  margin-top: 2px;
}

.chart-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
