<script setup>
/**
 * 兑换中心页面组件
 * 用户可在此页面使用积分兑换奖励，包括：
 * - 显示当前可用积分余额
 * - 展示所有可兑换的奖励商品（以网格卡片形式）
 * - 点击奖励卡片进行积分兑换（积分不足时卡片置灰不可点击）
 * - 显示历史兑换记录列表（支持使用/撤销操作）
 * - 使用兑换券时需上传凭证图片并填写描述
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getDashboard } from '../api/dashboard'
import { redeemReward, getRedemptionHistory, cancelRedemption, useRedemption } from '../api/redemption'
import { getRewardItems } from '../api/rewardItem'
import { uploadImage } from '../api/exercise'

const router = useRouter()
const userStore = useUserStore()
const availablePoints = ref(0)
const rewards = ref([])
const history = ref([])
const showConfirm = ref(false)
const selectedReward = ref(null)
const toast = ref(null)

// 使用兑换券弹窗相关状态
const showUseModal = ref(false)
const useTarget = ref(null) // 正在使用的那条兑换记录
const useDescription = ref('')
const useImageFiles = ref([]) // 已选择的图片文件列表
const useImagePreviews = ref([]) // 本地预览URL列表
const useImageUrls = ref([]) // 上传后的COS URL列表
const useUploading = ref(false)
const useSubmitting = ref(false)

// 图片预览弹窗
const previewImage = ref(null)

const isAdmin = computed(() => userStore.currentUser?.isAdmin)

async function loadData() {
  if (!userStore.currentUser) return
  try {
    const [dashRes, histRes, rewardRes] = await Promise.all([
      getDashboard(userStore.currentUser.id),
      getRedemptionHistory(userStore.currentUser.id),
      getRewardItems(),
    ])
    availablePoints.value = Number(dashRes.data.availablePoints) || 0
    history.value = histRes.data || []
    rewards.value = (rewardRes.data || []).map(r => ({
      id: r.id,
      name: r.name,
      desc: r.description || '',
      amount: '≤ ' + r.maxAmount + '元',
      cost: r.pointsCost,
    }))
  } catch (e) {
    console.error(e)
  }
}

function tryRedeem(reward) {
  if (availablePoints.value < reward.cost) return
  selectedReward.value = reward
  showConfirm.value = true
}

async function confirmRedeem() {
  try {
    await redeemReward({ userId: userStore.currentUser.id, rewardItemId: selectedReward.value.id })
    showToast('兑换成功！', 'success')
    showConfirm.value = false
    loadData()
  } catch (e) {
    showToast(e.message, 'error')
  }
}

/** 打开使用兑换券弹窗 */
function openUseModal(item) {
  useTarget.value = item
  useDescription.value = ''
  useImageFiles.value = []
  useImagePreviews.value = []
  useImageUrls.value = []
  showUseModal.value = true
}

/** 选择多张图片 */
function handleUseImages(e) {
  const files = Array.from(e.target.files)
  for (const file of files) {
    if (!file.type.startsWith('image/')) {
      showToast('请选择图片文件', 'error'); continue
    }
    if (file.size > 5 * 1024 * 1024) {
      showToast(`${file.name} 超过5MB`, 'error'); continue
    }
    useImageFiles.value.push(file)
    const reader = new FileReader()
    reader.onload = (ev) => { useImagePreviews.value.push(ev.target.result) }
    reader.readAsDataURL(file)
  }
  // 清空 input 以便重复选择同一文件
  e.target.value = ''
}

/** 移除已选图片 */
function removeUseImage(idx) {
  useImageFiles.value.splice(idx, 1)
  useImagePreviews.value.splice(idx, 1)
  if (useImageUrls.value.length > idx) {
    useImageUrls.value.splice(idx, 1)
  }
}

/** 确认使用兑换券：先上传所有图片，再提交使用请求 */
async function confirmUse() {
  if (!useDescription.value.trim()) {
    showToast('请填写使用描述', 'error'); return
  }
  if (useImageFiles.value.length === 0 && useImageUrls.value.length === 0) {
    showToast('请至少上传1张凭证图片', 'error'); return
  }

  useSubmitting.value = true
  try {
    // 上传尚未上传的图片
    if (useImageFiles.value.length > useImageUrls.value.length) {
      useUploading.value = true
      for (let i = useImageUrls.value.length; i < useImageFiles.value.length; i++) {
        const res = await uploadImage(useImageFiles.value[i])
        useImageUrls.value.push(res.data)
      }
      useUploading.value = false
    }

    await useRedemption(useTarget.value.id, {
      description: useDescription.value.trim(),
      imageUrls: useImageUrls.value,
    })
    showToast('使用成功！', 'success')
    showUseModal.value = false
    loadData()
  } catch (e) {
    showToast(e.message, 'error')
  }
  useSubmitting.value = false
}

async function handleCancel(item) {
  if (!confirm(`确定撤销「${item.rewardName}」的兑换吗？积分将退回。`)) return
  try {
    await cancelRedemption(item.id)
    showToast('撤销成功，积分已退回', 'success')
    loadData()
  } catch (e) {
    showToast(e.message, 'error')
  }
}

function showToast(msg, type) {
  toast.value = { msg, type }
  setTimeout(() => { toast.value = null }, 2500)
}

/** 格式化时间显示（截取前16位：yyyy-MM-dd HH:mm） */
function formatTime(str) {
  return str ? str.slice(0, 16).replace('T', ' ') : ''
}

watch(() => userStore.currentUser, loadData)
onMounted(loadData)
</script>

<template>
  <div>
    <div class="reward-header">
      <h1 class="page-title" style="margin:0">兑换中心</h1>
      <router-link v-if="isAdmin" to="/reward-admin" class="admin-link">管理奖励</router-link>
    </div>

    <div>
      <div class="card" style="text-align: center; margin-bottom: 16px">
        <div class="label">可用积分</div>
        <div class="value" style="font-size: 36px; font-weight: 700; color: var(--primary)">
          {{ availablePoints }}
        </div>
      </div>

      <div class="rewards-grid">
        <div
          v-for="reward in rewards"
          :key="reward.id"
          class="reward-card"
          :class="{ disabled: availablePoints < reward.cost }"
          @click="tryRedeem(reward)"
        >
          <div class="reward-name">{{ reward.name }}</div>
          <div class="reward-desc">{{ reward.desc }}</div>
          <div class="reward-desc">{{ reward.amount }}</div>
          <div class="reward-cost">{{ reward.cost }} 积分</div>
        </div>
      </div>

      <div v-if="rewards.length === 0" class="empty-state card">暂无奖励</div>

      <!-- 兑换记录列表 -->
      <div v-if="history.length" style="margin-top: 20px">
        <div class="section-title">兑换记录</div>
        <div class="card">
          <div v-for="item in history" :key="item.id" class="history-item">
            <div style="flex:1; min-width:0">
              <div class="reward-name">
                {{ item.rewardName }}
                <span v-if="item.status === 'used'" class="status-used">已使用</span>
                <span v-if="item.status === 'cancelled'" class="status-cancelled">已撤销</span>
              </div>
              <div class="reward-desc">{{ formatTime(item.redeemedAt) }}</div>
              <!-- 已使用时显示描述和凭证图片 -->
              <div v-if="item.status === 'used' && item.usedDescription" class="used-desc">
                {{ item.usedDescription }}
              </div>
              <div v-if="item.status === 'used' && item.usedImages && item.usedImages.length" class="used-images">
                <img
                  v-for="(img, idx) in item.usedImages"
                  :key="idx"
                  :src="img"
                  class="used-thumb"
                  @click="previewImage = img"
                />
              </div>
            </div>
            <div style="display:flex; align-items:center; gap:8px; flex-shrink:0">
              <div :class="item.status === 'cancelled' ? 'cost-refund' : 'cost-text'">
                {{ item.status === 'cancelled' ? '+' : '-' }}{{ item.pointsCost }}分
              </div>
              <!-- active 状态：可点击"使用" -->
              <button
                v-if="item.status === 'active'"
                class="btn-use"
                @click="openUseModal(item)"
              >使用</button>
              <!-- active 状态：管理员可撤销 -->
              <button
                v-if="isAdmin && item.status === 'active'"
                class="btn-cancel-redemption"
                @click="handleCancel(item)"
              >撤销</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 兑换确认弹窗 -->
    <div v-if="showConfirm" class="modal-overlay" @click.self="showConfirm = false">
      <div class="modal">
        <div class="modal-title">确认兑换</div>
        <p>确定使用 <strong>{{ selectedReward.cost }} 积分</strong> 兑换「{{ selectedReward.name }}」吗？</p>
        <div class="modal-actions">
          <button class="btn btn-cancel" @click="showConfirm = false">取消</button>
          <button class="btn btn-primary" @click="confirmRedeem">确认</button>
        </div>
      </div>
    </div>

    <!-- 使用兑换券弹窗 -->
    <div v-if="showUseModal" class="modal-overlay" @click.self="showUseModal = false">
      <div class="modal" style="max-width: 400px">
        <div class="modal-title">使用「{{ useTarget?.rewardName }}」</div>

        <div class="use-form-group">
          <label>使用描述 <span style="color:var(--danger)">*</span></label>
          <textarea
            v-model="useDescription"
            placeholder="描述一下消费场景，如：和XX一起喝了奶茶"
            rows="3"
            class="use-textarea"
          ></textarea>
        </div>

        <div class="use-form-group">
          <label>凭证图片 <span style="color:var(--danger)">*</span></label>
          <div class="use-images-area">
            <img
              v-for="(preview, idx) in useImagePreviews"
              :key="idx"
              :src="preview"
              class="use-preview-img"
            />
            <div
              v-for="(_, idx) in useImagePreviews"
              :key="'del-'+idx"
              class="use-preview-del"
              :style="{ left: (idx % 4) * 76 + 68 + 'px', top: Math.floor(idx / 4) * 76 + 'px' }"
              @click="removeUseImage(idx)"
            >&times;</div>
            <div class="use-add-img" @click="$refs.useFileInput.click()">+</div>
          </div>
          <input
            ref="useFileInput"
            type="file"
            accept="image/*"
            multiple
            style="display:none"
            @change="handleUseImages"
          />
        </div>

        <div class="modal-actions">
          <button class="btn btn-cancel" @click="showUseModal = false">取消</button>
          <button
            class="btn btn-primary"
            :disabled="useSubmitting || useUploading"
            @click="confirmUse"
          >
            {{ useUploading ? '上传图片中...' : useSubmitting ? '提交中...' : '确认使用' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 图片预览弹窗 -->
    <div v-if="previewImage" class="preview-overlay" @click="previewImage = null">
      <img :src="previewImage" class="preview-img" />
    </div>

    <div v-if="toast" :class="['toast', 'toast-' + toast.type]">{{ toast.msg }}</div>
  </div>
</template>

<style scoped>
.reward-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.admin-link {
  font-size: 13px;
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
  background: #eef2ff;
  padding: 6px 12px;
  border-radius: 6px;
}

.admin-link:hover {
  background: #e0e7ff;
}

.rewards-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
  gap: 8px;
}

.history-item:last-child {
  border-bottom: none;
}

.cost-text {
  font-weight: 700;
  color: var(--danger);
}

.cost-refund {
  font-weight: 700;
  color: var(--success);
}

.status-cancelled {
  font-size: 12px;
  color: var(--text-secondary);
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 6px;
}

.status-used {
  font-size: 12px;
  color: white;
  background: var(--success);
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 6px;
}

.used-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
  line-height: 1.4;
}

.used-images {
  display: flex;
  gap: 6px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.used-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid var(--border);
}

.btn-use {
  background: #ecfdf5;
  color: var(--success);
  border: none;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  font-weight: 600;
}

.btn-use:hover {
  background: #d1fae5;
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

/* 使用弹窗表单 */
.use-form-group {
  margin-bottom: 16px;
}

.use-form-group label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
}

.use-textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
}

.use-textarea:focus {
  outline: none;
  border-color: var(--primary);
}

.use-images-area {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  position: relative;
}

.use-preview-img {
  width: 68px;
  height: 68px;
  object-fit: cover;
  border-radius: 6px;
}

.use-preview-del {
  position: absolute;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  color: white;
  border: none;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.use-preview-del:hover {
  background: rgba(0,0,0,0.7);
}

.use-add-img {
  width: 68px;
  height: 68px;
  border: 2px dashed #ddd;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: var(--text-secondary);
  cursor: pointer;
  flex-shrink: 0;
}

.use-add-img:hover {
  border-color: var(--primary);
  color: var(--primary);
}

/* 图片预览弹窗 */
.preview-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.85);
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
