<script setup>
import { onMounted, ref } from 'vue'
import { useUserStore } from '../stores/user'
import { createUser } from '../api/user'

const userStore = useUserStore()
const showCreate = ref(false)
const newName = ref('')
const newGender = ref('female')

onMounted(() => {
  userStore.fetchUsers()
})

async function handleCreate() {
  if (!newName.value.trim()) return
  try {
    const res = await createUser({ name: newName.value, gender: newGender.value })
    await userStore.fetchUsers()
    userStore.selectUser(res.data)
    showCreate.value = false
    newName.value = ''
  } catch (e) {
    alert(e.message)
  }
}
</script>

<template>
  <div class="user-selector">
    <div v-if="!userStore.currentUser" class="card">
      <div class="section-title">选择用户</div>
      <div v-if="userStore.userList.length > 0" class="user-list">
        <div
          v-for="user in userStore.userList"
          :key="user.id"
          class="user-item"
          @click="userStore.selectUser(user)"
        >
          <span class="user-avatar">{{ user.gender === 'female' ? '👩' : '👨' }}</span>
          <span class="user-name">{{ user.name }}</span>
        </div>
      </div>
      <div v-else class="empty-state">暂无用户</div>
      <button class="btn btn-block" style="margin-top: 12px" @click="showCreate = true">
        创建新用户
      </button>
    </div>

    <div v-else class="current-user" @click="userStore.selectUser(null)">
      <span class="user-avatar">{{ userStore.currentUser.gender === 'female' ? '👩' : '👨' }}</span>
      <span>{{ userStore.currentUser.name }}</span>
    </div>

    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal">
        <div class="modal-title">创建新用户</div>
        <div class="form-group">
          <label>姓名</label>
          <input v-model="newName" placeholder="请输入姓名" @keyup.enter="handleCreate" />
        </div>
        <div class="form-group">
          <label>性别</label>
          <select v-model="newGender">
            <option value="female">女</option>
            <option value="male">男</option>
          </select>
        </div>
        <div class="modal-actions">
          <button class="btn btn-cancel" @click="showCreate = false">取消</button>
          <button class="btn btn-primary" @click="handleCreate">创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-item:hover {
  background: #f3f4f6;
}

.user-avatar {
  font-size: 24px;
  margin-right: 10px;
}

.user-name {
  font-weight: 600;
}

.current-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--card-bg);
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 16px;
}
</style>
