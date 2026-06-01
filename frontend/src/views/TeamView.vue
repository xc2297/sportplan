// 小组管理页面：三种状态 - 未加入(浏览/申请)、普通成员(查看/退出)、管理员(管理/解散)
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import {
  getGroups, createGroup, requestJoinGroup, leaveGroup, dissolveGroup,
  getGroupMembers, setMemberAdmin, getPendingRequests, getMyRequests,
  approveRequest, rejectRequest
} from '../api/group'

const userStore = useUserStore()

const groups = ref([])
const members = ref([])
const pendingRequests = ref([])
const myRequests = ref([])
const loading = ref(false)
const showCreateModal = ref(false)
const showConfirmModal = ref(false)
const confirmAction = ref(null)
const confirmMessage = ref('')
const newGroupName = ref('')
const newGroupDesc = ref('')
const error = ref('')

const currentUser = computed(() => userStore.currentUser)
const hasGroup = computed(() => !!currentUser.value?.groupId)
const isGroupAdmin = computed(() => !!currentUser.value?.isAdmin)

const myGroup = computed(() => {
  if (!hasGroup.value) return null
  return groups.value.find(g => g.isMember)
})

// 已申请的小组ID集合
const appliedGroupIds = computed(() => {
  return new Set(myRequests.value.map(r => r.groupId))
})

async function loadGroups() {
  loading.value = true
  error.value = ''
  try {
    const res = await getGroups()
    groups.value = res.data || []

    const mg = groups.value.find(g => g.isMember)
    if (mg) {
      const mRes = await getGroupMembers(mg.id)
      members.value = mRes.data || []

      // 管理员加载待审批申请
      if (isGroupAdmin.value) {
        const rRes = await getPendingRequests(mg.id)
        pendingRequests.value = rRes.data || []
      }
    }

    // 未加入小组时加载自己的申请
    if (!hasGroup.value) {
      const mrRes = await getMyRequests()
      myRequests.value = mrRes.data || []
    }
  } catch (e) {
    error.value = e.response?.data?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function handleCreateGroup() {
  if (!newGroupName.value.trim()) {
    error.value = '请输入小组名称'
    return
  }
  try {
    await createGroup({ name: newGroupName.value.trim(), description: newGroupDesc.value.trim() })
    showCreateModal.value = false
    newGroupName.value = ''
    newGroupDesc.value = ''
    await userStore.restoreSession()
    await loadGroups()
  } catch (e) {
    error.value = e.response?.data?.message || '创建失败'
  }
}

async function handleRequestJoin(groupId) {
  try {
    await requestJoinGroup(groupId)
    await loadGroups()
  } catch (e) {
    error.value = e.response?.data?.message || '申请失败'
  }
}

function confirmLeave() {
  confirmMessage.value = '确定要退出当前小组吗？'
  confirmAction.value = async () => {
    try {
      await leaveGroup()
      await userStore.restoreSession()
      await loadGroups()
    } catch (e) {
      error.value = e.response?.data?.message || '退出失败'
    }
  }
  showConfirmModal.value = true
}

async function handleToggleAdmin(member) {
  if (!myGroup.value) return
  const action = member.isAdmin ? '取消管理员' : '设为管理员'
  confirmMessage.value = `确定要${action}「${member.name}」吗？`
  confirmAction.value = async () => {
    try {
      await setMemberAdmin(myGroup.value.id, member.userId)
      await loadGroups()
    } catch (e) {
      error.value = e.response?.data?.message || '操作失败'
    }
  }
  showConfirmModal.value = true
}

async function handleApprove(req) {
  try {
    await approveRequest(req.id)
    await loadGroups()
  } catch (e) {
    error.value = e.response?.data?.message || '审批失败'
  }
}

async function handleReject(req) {
  try {
    await rejectRequest(req.id)
    await loadGroups()
  } catch (e) {
    error.value = e.response?.data?.message || '审批失败'
  }
}

function confirmDissolve() {
  if (!myGroup.value) return
  confirmMessage.value = '确定要解散小组吗？所有成员将被移出，此操作不可撤销。'
  confirmAction.value = async () => {
    try {
      await dissolveGroup(myGroup.value.id)
      await userStore.restoreSession()
      await loadGroups()
    } catch (e) {
      error.value = e.response?.data?.message || '解散失败'
    }
  }
  showConfirmModal.value = true
}

async function executeConfirm() {
  if (confirmAction.value) {
    await confirmAction.value()
  }
  showConfirmModal.value = false
}

onMounted(loadGroups)
</script>

<template>
  <div class="team-page">
    <div class="page-header">
      <h2>小组</h2>
    </div>

    <div v-if="error" class="error-msg">{{ error }}</div>
    <div v-if="loading" class="loading">加载中...</div>

    <!-- 已在小组中 -->
    <template v-if="hasGroup && myGroup && !loading">
      <div class="group-card my-group">
        <div class="group-header">
          <h3>{{ myGroup.name }}</h3>
          <span v-if="isGroupAdmin" class="admin-badge">管理员</span>
        </div>
        <p v-if="myGroup.description" class="group-desc">{{ myGroup.description }}</p>
        <div class="group-meta">
          <span>成员 {{ myGroup.memberCount }} 人</span>
          <span>创建者 {{ myGroup.creatorName }}</span>
        </div>

        <div v-if="isGroupAdmin" class="admin-actions">
          <router-link to="/exercise-admin" class="action-btn">管理运动类型</router-link>
          <router-link to="/reward-admin" class="action-btn">管理奖励</router-link>
        </div>
      </div>

      <!-- 管理员：待审批申请 -->
      <div v-if="isGroupAdmin && pendingRequests.length > 0" class="section">
        <h4>待审批申请</h4>
        <div class="request-list">
          <div v-for="req in pendingRequests" :key="req.id" class="request-item">
            <span class="request-name">{{ req.userName }}</span>
            <div class="request-actions">
              <button class="btn btn-primary btn-xs" @click="handleApprove(req)">同意</button>
              <button class="btn btn-secondary btn-xs" @click="handleReject(req)">拒绝</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 成员列表 -->
      <div class="section">
        <h4>小组成员</h4>
        <div class="member-list">
          <div v-for="m in members" :key="m.userId" class="member-item">
            <span class="member-name">{{ m.name }}</span>
            <span class="member-gender">{{ m.gender === 'male' ? '♂' : '♀' }}</span>
            <span v-if="m.isAdmin" class="admin-tag">管理员</span>
            <button v-if="isGroupAdmin && m.userId !== currentUser.id"
                class="btn-toggle-admin"
                @click="handleToggleAdmin(m)">
              {{ m.isAdmin ? '取消管理' : '设为管理' }}
            </button>
          </div>
        </div>
      </div>

      <div class="bottom-actions">
        <button v-if="isGroupAdmin" class="btn btn-danger" @click="confirmDissolve">解散小组</button>
        <button v-else class="btn btn-warning" @click="confirmLeave">退出小组</button>
      </div>
    </template>

    <!-- 未加入小组 -->
    <template v-if="!hasGroup && !loading">
      <button class="btn btn-primary create-btn" @click="showCreateModal = true">创建小组</button>

      <!-- 我的申请 -->
      <div v-if="myRequests.length > 0" class="section">
        <h4>我的申请</h4>
        <div class="request-list">
          <div v-for="req in myRequests" :key="req.id" class="request-item">
            <span>{{ req.groupName }}</span>
            <span class="pending-tag">等待审批</span>
          </div>
        </div>
      </div>

      <div class="section">
        <h4>所有小组</h4>
        <div v-if="groups.length === 0" class="empty">暂无小组，快来创建一个吧</div>
        <div v-for="g in groups" :key="g.id" class="group-card">
          <div class="group-header">
            <h3>{{ g.name }}</h3>
          </div>
          <p v-if="g.description" class="group-desc">{{ g.description }}</p>
          <div class="group-meta">
            <span>成员 {{ g.memberCount }} 人</span>
            <span>创建者 {{ g.creatorName }}</span>
          </div>
          <button v-if="appliedGroupIds.has(g.id)" class="btn btn-secondary btn-sm" disabled>已申请</button>
          <button v-else class="btn btn-primary btn-sm" @click="handleRequestJoin(g.id)">申请加入</button>
        </div>
      </div>
    </template>

    <!-- 创建小组弹窗 -->
    <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="modal">
        <h3>创建小组</h3>
        <div class="form-group">
          <label>小组名称 *</label>
          <input v-model="newGroupName" placeholder="请输入小组名称" maxlength="100" />
        </div>
        <div class="form-group">
          <label>小组描述</label>
          <textarea v-model="newGroupDesc" placeholder="选填" maxlength="500" rows="3"></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="showCreateModal = false">取消</button>
          <button class="btn btn-primary" @click="handleCreateGroup">创建</button>
        </div>
      </div>
    </div>

    <!-- 确认弹窗 -->
    <div v-if="showConfirmModal" class="modal-overlay" @click.self="showConfirmModal = false">
      <div class="modal">
        <p>{{ confirmMessage }}</p>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="showConfirmModal = false">取消</button>
          <button class="btn btn-danger" @click="executeConfirm">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.team-page {
  padding: 16px;
  padding-bottom: 80px;
  max-width: 480px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
}

.error-msg {
  background: #fee;
  color: #c33;
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 13px;
}

.loading {
  text-align: center;
  color: var(--text-secondary);
  padding: 40px 0;
}

.group-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid var(--border);
}

.group-card.my-group {
  border-color: var(--primary);
  border-width: 2px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.group-header h3 {
  font-size: 16px;
  font-weight: 600;
}

.admin-badge {
  background: var(--primary);
  color: white;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
}

.group-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.group-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.admin-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.action-btn {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 6px;
  background: var(--bg);
  color: var(--primary);
  font-size: 13px;
  text-decoration: none;
  font-weight: 500;
}

.section {
  margin-top: 20px;
}

.section h4 {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}

.request-list {
  background: white;
  border-radius: 12px;
  border: 1px solid var(--border);
  overflow: hidden;
}

.request-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
}

.request-item:last-child {
  border-bottom: none;
}

.request-name {
  font-weight: 500;
}

.request-actions {
  display: flex;
  gap: 6px;
}

.pending-tag {
  font-size: 11px;
  background: #f59e0b;
  color: white;
  padding: 2px 8px;
  border-radius: 8px;
}

.member-list {
  background: white;
  border-radius: 12px;
  border: 1px solid var(--border);
  overflow: hidden;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}

.member-item:last-child {
  border-bottom: none;
}

.member-name {
  font-size: 14px;
  flex: 1;
}

.member-gender {
  font-size: 14px;
  margin-right: 8px;
}

.admin-tag {
  font-size: 11px;
  background: var(--primary);
  color: white;
  padding: 2px 6px;
  border-radius: 8px;
}

.btn-toggle-admin {
  margin-left: 8px;
  padding: 3px 10px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: white;
  font-size: 11px;
  color: var(--primary);
  cursor: pointer;
}

.empty {
  text-align: center;
  color: var(--text-secondary);
  padding: 40px 0;
  font-size: 14px;
}

.create-btn {
  width: 100%;
  margin-bottom: 16px;
}

.bottom-actions {
  margin-top: 24px;
}

.btn {
  display: inline-block;
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  text-align: center;
  transition: opacity 0.2s;
}

.btn:active { opacity: 0.7; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: var(--primary); color: white; }
.btn-secondary { background: var(--bg); color: var(--text-primary); }
.btn-warning { background: #f59e0b; color: white; }
.btn-danger { background: #ef4444; color: white; }
.btn-sm { padding: 6px 16px; font-size: 13px; }
.btn-xs { padding: 4px 12px; font-size: 12px; }

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 16px;
}

.modal {
  background: white;
  border-radius: 16px;
  padding: 20px;
  width: 100%;
  max-width: 360px;
}

.modal h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.form-group {
  margin-bottom: 14px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 6px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
  font-family: inherit;
}

.modal-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
