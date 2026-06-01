<script setup>
/**
 * 运动记录页面组件
 * 用户在此页面提交每日运动数据，包括：
 * - 选择日期和运动类型（如跑步、走路等）
 * - 输入运动数量，实时预估可获得的积分
 * - 上传运动凭证图片（非必传）
 * - 查看当日已提交的运动记录列表
 * - 管理员可删除任意日期的记录
 * 积分计算逻辑：数量 * 性别系数，单项上限由运动类型的 dailyCap 决定
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { submitRecord, getRecord, deleteRecord, uploadImage } from '../api/exercise'
import { getExerciseTypes } from '../api/exerciseType'

const router = useRouter()
const userStore = useUserStore()
const recordDate = ref(new Date().toISOString().slice(0, 10))
const exerciseTypes = ref([])
const selectedTypeId = ref(null)
const amount = ref(0)
const todayRecords = ref([])
const submitting = ref(false)
const toast = ref(null)

// 图片上传相关状态
const imageFile = ref(null)
const imagePreview = ref(null)
const imageUrl = ref(null)
const uploading = ref(false)

const isAdmin = computed(() => userStore.currentUser?.isAdmin)

const selectedType = computed(() =>
  exerciseTypes.value.find(t => t.id === selectedTypeId.value)
)

/**
 * 计算属性：预估本次运动可获得的积分
 * 计算公式：运动数量 * 对应性别的积分系数，不超过单项每日上限
 */
const estimatedScore = computed(() => {
  if (!selectedType.value || !userStore.currentUser || amount.value <= 0) return '0.0'
  const gender = userStore.currentUser.gender
  const coeff = gender === 'female'
    ? Number(selectedType.value.femaleCoefficient)
    : Number(selectedType.value.maleCoefficient)
  const raw = amount.value * coeff
  const cap = selectedType.value.dailyCap || 10
  return Math.min(raw, cap).toFixed(1)
})

async function loadTypes() {
  try {
    const res = await getExerciseTypes(true)
    exerciseTypes.value = res.data || []
  } catch (e) { console.error(e) }
}

async function loadRecords() {
  if (!userStore.currentUser) return
  try {
    const res = await getRecord(userStore.currentUser.id, recordDate.value)
    todayRecords.value = res.data || []
  } catch {
    todayRecords.value = []
  }
}

/** 选择图片文件后，生成本地预览 */
function handleImageSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  // 校验文件类型
  if (!file.type.startsWith('image/')) {
    showToast('请选择图片文件', 'error'); return
  }
  // 校验文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    showToast('图片大小不能超过5MB', 'error'); return
  }
  imageFile.value = file
  // 生成本地预览
  const reader = new FileReader()
  reader.onload = (ev) => { imagePreview.value = ev.target.result }
  reader.readAsDataURL(file)
}

/** 清除已选择的图片 */
function clearImage() {
  imageFile.value = null
  imagePreview.value = null
  imageUrl.value = null
}

/**
 * 提交运动记录
 * 如果选择了图片，先上传图片获取 URL，再提交记录
 */
async function handleSubmit() {
  if (!userStore.currentUser || !selectedTypeId.value || amount.value <= 0) {
    showToast('请选择运动类型并填写数量', 'error'); return
  }
  submitting.value = true
  try {
    // 如果有图片，先上传
    let finalImageUrl = null
    if (imageFile.value) {
      uploading.value = true
      try {
        const uploadRes = await uploadImage(imageFile.value)
        finalImageUrl = uploadRes.data
      } catch (e) {
        showToast('图片上传失败：' + e.message, 'error')
        uploading.value = false
        submitting.value = false
        return
      }
      uploading.value = false
    }

    await submitRecord({
      userId: userStore.currentUser.id,
      recordDate: recordDate.value,
      exerciseTypeId: selectedTypeId.value,
      amount: amount.value,
      imageUrl: finalImageUrl,
    })
    showToast('记录成功！', 'success')
    // 重置表单
    amount.value = 0
    selectedTypeId.value = null
    clearImage()
    loadRecords()
  } catch (e) {
    showToast(e.message, 'error')
  }
  submitting.value = false
}

const isToday = computed(() => recordDate.value === new Date().toISOString().slice(0, 10))

async function handleDelete(r) {
  if (!confirm(`确定删除「${r.exerciseTypeName} ${r.amount}${r.unit}」吗？`)) return
  try {
    await deleteRecord(r.id)
    showToast('删除成功', 'success')
    loadRecords()
  } catch (e) {
    showToast(e.message, 'error')
  }
}

function showToast(msg, type) {
  toast.value = { msg, type }
  setTimeout(() => { toast.value = null }, 2500)
}

watch(() => userStore.currentUser, () => { loadRecords() })
watch(recordDate, loadRecords)
onMounted(() => { loadTypes(); loadRecords() })
</script>

<template>
  <div>
    <div class="record-header">
      <h1 class="page-title" style="margin:0">运动记录</h1>
      <router-link v-if="isAdmin" to="/exercise-admin" class="admin-link">管理运动类型</router-link>
    </div>

    <div class="card">
      <div class="form-group">
        <label>日期</label>
        <input type="date" v-model="recordDate" />
      </div>

      <div class="form-group">
        <label>运动类型</label>
        <select v-model="selectedTypeId">
          <option :value="null" disabled>请选择运动类型</option>
          <option v-for="t in exerciseTypes" :key="t.id" :value="t.id">
            {{ t.name }}（{{ t.unit }}）
          </option>
        </select>
      </div>

      <div v-if="selectedType" class="form-group">
        <label>{{ selectedType.name }}（{{ selectedType.unit }}）</label>
        <input type="number" v-model.number="amount" min="0" step="0.1" placeholder="0" />
        <div class="est-score">预计 +{{ estimatedScore }} 分</div>
      </div>

      <!-- 图片上传区域（非必传） -->
      <div v-if="selectedType" class="form-group">
        <label>运动凭证（选填）</label>
        <div v-if="imagePreview" class="image-preview-wrap">
          <img :src="imagePreview" class="image-preview" />
          <button class="btn-clear-img" @click="clearImage">&times;</button>
        </div>
        <div v-else class="image-upload-area" @click="$refs.fileInput.click()">
          <span>+ 上传图片</span>
        </div>
        <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleImageSelect" />
      </div>

      <button class="btn btn-primary btn-block" style="margin-top: 12px"
        :disabled="submitting || !selectedTypeId || amount <= 0" @click="handleSubmit">
        {{ uploading ? '上传图片中...' : submitting ? '提交中...' : '提交记录' }}
      </button>
    </div>

    <div v-if="todayRecords.length" class="card" style="margin-top: 12px">
      <div class="section-title">{{ isToday ? '今日' : recordDate.slice(5) }}已记录</div>
      <div v-for="r in todayRecords" :key="r.id" class="record-row">
        <div class="record-info">
          <img v-if="r.imageUrl" :src="r.imageUrl" class="record-thumb" @click="window.open(r.imageUrl)" />
          <div>
            <div>
              <span class="record-type">{{ r.exerciseTypeName }}</span>
              <span class="record-amount">{{ r.amount }} {{ r.unit }}</span>
            </div>
          </div>
        </div>
        <div style="display:flex; align-items:center; gap:10px">
          <div :class="Number(r.score) < 0 ? 'record-penalty' : 'record-score'">
            {{ Number(r.score) < 0 ? r.score : '+' + r.score }}分
          </div>
          <button v-if="r.exerciseTypeName === '未运动惩罚' ? isAdmin : (isToday || isAdmin)" class="btn-del-small" @click="handleDelete(r)">删除</button>
        </div>
      </div>
    </div>

    <div v-if="toast" :class="['toast', 'toast-' + toast.type]">{{ toast.msg }}</div>
  </div>
</template>

<style scoped>
.record-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
}
.admin-link {
  font-size: 13px; color: var(--primary); font-weight: 600; text-decoration: none;
  background: #eef2ff; padding: 6px 12px; border-radius: 6px;
}
.admin-link:hover { background: #e0e7ff; }
.est-score { font-size: 13px; color: var(--primary); margin-top: 4px; }
.record-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 0; border-bottom: 1px solid var(--border);
}
.record-row:last-child { border-bottom: none; }
.record-info { display: flex; gap: 8px; align-items: center; }
.record-type { font-weight: 600; font-size: 15px; }
.record-amount { font-size: 13px; color: var(--text-secondary); }
.record-score { font-weight: 700; color: var(--success); font-size: 15px; }
.record-penalty { font-weight: 700; color: var(--danger); font-size: 15px; }
.btn-del-small {
  background: #fef2f2; color: var(--danger); border: none;
  padding: 4px 10px; border-radius: 4px; font-size: 12px; cursor: pointer;
}
.btn-del-small:hover { background: #fee2e2; }

/* 图片上传相关样式 */
.image-upload-area {
  border: 2px dashed #ddd;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  color: var(--text-secondary);
  cursor: pointer;
  transition: border-color 0.2s;
}
.image-upload-area:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.image-preview-wrap {
  position: relative;
  display: inline-block;
}
.image-preview {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  display: block;
}
.btn-clear-img {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  color: white;
  border: none;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.btn-clear-img:hover {
  background: rgba(0,0,0,0.7);
}
.record-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
}
</style>
