<script setup>
/**
 * 运动类型管理页面组件（管理员功能）
 * 管理员可在此页面管理所有运动类型，包括：
 * - 查看运动类型列表（名称、单位、男女系数、每日积分上限）
 * - 新增运动类型
 * - 编辑已有运动类型（包括启用/停用状态切换）
 * - 删除运动类型
 * 积分系数说明：男性系数和女性系数分别用于计算不同性别的运动积分
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getExerciseTypes, createExerciseType, updateExerciseType, deleteExerciseType } from '../api/exerciseType'

const router = useRouter()
// 运动类型列表
const items = ref([])
// 是否正在加载数据
const loading = ref(false)
// 是否显示新增/编辑表单弹窗
const showForm = ref(false)
// 当前正在编辑的运动类型（null 表示新增模式）
const editing = ref(null)
// 表单数据对象
const form = ref({ name: '', unit: '', maleCoefficient: 0, femaleCoefficient: 0, dailyCap: 10, sortOrder: 0, active: true })
// Toast 提示消息
const toast = ref(null)

/**
 * 加载所有运动类型列表（包含已停用的类型）
 */
async function loadItems() {
  loading.value = true
  try {
    const res = await getExerciseTypes()
    items.value = res.data || []
  } catch (e) {
    showToast(e.message, 'error')
  }
  loading.value = false
}

/**
 * 打开新增运动类型表单弹窗
 * 重置表单数据，排序号自动递增，默认启用
 */
function openCreate() {
  editing.value = null
  form.value = { name: '', unit: '', maleCoefficient: 0, femaleCoefficient: 0, dailyCap: 10, sortOrder: items.value.length + 1, active: true }
  showForm.value = true
}

/**
 * 打开编辑运动类型表单弹窗
 * 将选中的运动类型数据填充到表单中
 * @param {Object} item - 要编辑的运动类型对象
 */
function openEdit(item) {
  editing.value = item
  form.value = {
    name: item.name, unit: item.unit,
    maleCoefficient: Number(item.maleCoefficient), femaleCoefficient: Number(item.femaleCoefficient),
    dailyCap: item.dailyCap, sortOrder: item.sortOrder, active: item.active
  }
  showForm.value = true
}

/**
 * 提交表单（新增或编辑）
 * 校验名称和单位是否为空，根据 editing 状态判断新增或编辑
 * 成功后关闭弹窗并刷新列表
 */
async function handleSubmit() {
  // 校验必填项：名称和单位不能为空
  if (!form.value.name.trim() || !form.value.unit.trim()) {
    showToast('请填写名称和单位', 'error'); return
  }
  try {
    if (editing.value) {
      // 编辑模式：调用更新接口
      await updateExerciseType(editing.value.id, form.value)
      showToast('修改成功', 'success')
    } else {
      // 新增模式：调用创建接口
      await createExerciseType(form.value)
      showToast('新增成功', 'success')
    }
    showForm.value = false
    // 刷新列表数据
    loadItems()
  } catch (e) {
    showToast(e.message, 'error')
  }
}

/**
 * 删除运动类型
 * 弹出确认框后调用删除接口，成功后刷新列表
 * @param {Object} item - 要删除的运动类型对象
 */
async function handleDelete(item) {
  if (!confirm(`确定删除「${item.name}」吗？`)) return
  try {
    await deleteExerciseType(item.id)
    showToast('删除成功', 'success')
    loadItems()
  } catch (e) {
    showToast(e.message, 'error')
  }
}

/**
 * 显示 Toast 提示消息
 * @param {string} msg - 提示文字
 * @param {string} type - 提示类型（success/error）
 */
function showToast(msg, type) {
  toast.value = { msg, type }
  setTimeout(() => { toast.value = null }, 2500)
}

// 组件挂载时加载运动类型列表
onMounted(loadItems)
</script>

<template>
  <div>
    <div class="admin-header">
      <button class="btn btn-back" @click="router.back()">&larr; 返回</button>
      <h1 class="page-title" style="flex:1; margin:0">运动类型管理</h1>
      <button class="btn btn-primary" @click="openCreate">新增</button>
    </div>

    <div v-if="loading" class="empty-state">加载中...</div>
    <div v-else>
      <div v-if="items.length === 0" class="empty-state card">暂无运动类型</div>
      <div v-for="item in items" :key="item.id" class="card" style="margin-bottom:8px">
        <div style="display:flex; justify-content:space-between; align-items:center">
          <div>
            <span style="font-weight:700; font-size:16px">{{ item.name }}</span>
            <span style="color:var(--text-secondary); font-size:13px; margin-left:6px">({{ item.unit }})</span>
            <span v-if="!item.active" style="color:var(--danger); font-size:12px; margin-left:6px">已停用</span>
          </div>
          <div style="display:flex; gap:8px">
            <button class="btn btn-sm btn-edit" @click="openEdit(item)">编辑</button>
            <button class="btn btn-sm btn-del" @click="handleDelete(item)">删除</button>
          </div>
        </div>
        <div style="font-size:13px; color:var(--text-secondary); margin-top:4px">
          男 {{ item.maleCoefficient }}/女 {{ item.femaleCoefficient }} | 上限 {{ item.dailyCap }}分
        </div>
      </div>
    </div>

    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal">
        <div class="modal-title">{{ editing ? '编辑运动类型' : '新增运动类型' }}</div>
        <div class="form-group">
          <label>名称</label>
          <input v-model="form.name" placeholder="如：跑步" />
        </div>
        <div class="form-group">
          <label>单位</label>
          <input v-model="form.unit" placeholder="如：公里、个" />
        </div>
        <div style="display:flex; gap:12px">
          <div class="form-group" style="flex:1">
            <label>男性系数</label>
            <input v-model.number="form.maleCoefficient" type="number" step="0.01" min="0" />
          </div>
          <div class="form-group" style="flex:1">
            <label>女性系数</label>
            <input v-model.number="form.femaleCoefficient" type="number" step="0.01" min="0" />
          </div>
        </div>
        <div style="display:flex; gap:12px">
          <div class="form-group" style="flex:1">
            <label>单项上限(分)</label>
            <input v-model.number="form.dailyCap" type="number" min="1" />
          </div>
          <div class="form-group" style="flex:1">
            <label>排序</label>
            <input v-model.number="form.sortOrder" type="number" min="0" />
          </div>
        </div>
        <div class="form-group" v-if="editing">
          <label><input type="checkbox" v-model="form.active" style="margin-right:6px" />启用</label>
        </div>
        <div class="modal-actions">
          <button class="btn btn-cancel" @click="showForm = false">取消</button>
          <button class="btn btn-primary" @click="handleSubmit">保存</button>
        </div>
      </div>
    </div>

    <div v-if="toast" :class="['toast', 'toast-' + toast.type]">{{ toast.msg }}</div>
  </div>
</template>

<style scoped>
.admin-header {
  display: flex; align-items: center; gap: 12px; margin-bottom: 16px;
}
.btn-back { background: none; color: var(--primary); padding: 6px 10px; font-size: 14px; }
.btn-sm { padding: 6px 12px; font-size: 13px; }
.btn-edit { background: #eef2ff; color: var(--primary); }
.btn-del { background: #fef2f2; color: var(--danger); }
</style>
