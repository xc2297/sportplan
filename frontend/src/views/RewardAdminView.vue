<script setup>
/**
 * 奖励管理页面组件（管理员功能）
 * 管理员可在此页面管理所有奖励商品，包括：
 * - 查看所有奖励商品列表（显示名称、描述、所需积分、额度）
 * - 新增奖励商品（弹出表单弹窗）
 * - 编辑已有奖励商品（弹出预填表单弹窗）
 * - 删除奖励商品（带确认提示）
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRewardItems, createRewardItem, updateRewardItem, deleteRewardItem } from '../api/rewardItem'

const router = useRouter()
// 奖励商品列表
const items = ref([])
// 是否正在加载数据
const loading = ref(false)
// 是否显示新增/编辑表单弹窗
const showForm = ref(false)
// 当前正在编辑的奖励项（null 表示新增模式）
const editing = ref(null)
// 表单数据对象
const form = ref({ name: '', description: '', pointsCost: 5, maxAmount: 30, sortOrder: 0 })
// Toast 提示消息
const toast = ref(null)

/**
 * 加载所有奖励商品列表
 */
async function loadItems() {
  loading.value = true
  try {
    const res = await getRewardItems()
    items.value = res.data || []
  } catch (e) {
    showToast(e.message, 'error')
  }
  loading.value = false
}

/**
 * 打开新增奖励表单弹窗
 * 重置表单数据，排序号自动递增
 */
function openCreate() {
  editing.value = null
  form.value = { name: '', description: '', pointsCost: 5, maxAmount: 30, sortOrder: items.value.length + 1 }
  showForm.value = true
}

/**
 * 打开编辑奖励表单弹窗
 * 将选中的奖励数据填充到表单中
 * @param {Object} item - 要编辑的奖励对象
 */
function openEdit(item) {
  editing.value = item
  form.value = { name: item.name, description: item.description || '', pointsCost: item.pointsCost, maxAmount: Number(item.maxAmount), sortOrder: item.sortOrder }
  showForm.value = true
}

/**
 * 提交表单（新增或编辑）
 * 根据 editing 状态判断是新增还是编辑操作
 * 成功后关闭弹窗并刷新列表
 */
async function handleSubmit() {
  // 校验奖励名称不能为空
  if (!form.value.name.trim()) {
    showToast('请填写奖励名称', 'error')
    return
  }
  try {
    if (editing.value) {
      // 编辑模式：调用更新接口
      await updateRewardItem(editing.value.id, form.value)
      showToast('修改成功', 'success')
    } else {
      // 新增模式：调用创建接口
      await createRewardItem(form.value)
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
 * 删除奖励商品
 * 弹出确认框后调用删除接口，成功后刷新列表
 * @param {Object} item - 要删除的奖励对象
 */
async function handleDelete(item) {
  if (!confirm(`确定删除「${item.name}」吗？`)) return
  try {
    await deleteRewardItem(item.id)
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

// 组件挂载时加载奖励列表
onMounted(loadItems)
</script>

<template>
  <div>
    <div class="admin-header">
      <button class="btn btn-back" @click="router.back()">&larr; 返回</button>
      <h1 class="page-title" style="flex:1; margin:0">奖励管理</h1>
      <button class="btn btn-primary" @click="openCreate">新增</button>
    </div>

    <div v-if="loading" class="empty-state">加载中...</div>

    <div v-else>
      <div v-if="items.length === 0" class="empty-state card">暂无奖励项</div>
      <div v-for="item in items" :key="item.id" class="card reward-admin-item">
        <div class="item-info">
          <div class="item-name">{{ item.name }}</div>
          <div class="item-desc">{{ item.description || '无描述' }}</div>
          <div class="item-meta">
            <span class="item-cost">{{ item.pointsCost }} 积分</span>
            <span class="item-amount">≤ {{ item.maxAmount }} 元</span>
          </div>
        </div>
        <div class="item-actions">
          <button class="btn btn-sm btn-edit" @click="openEdit(item)">编辑</button>
          <button class="btn btn-sm btn-del" @click="handleDelete(item)">删除</button>
        </div>
      </div>
    </div>

    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal">
        <div class="modal-title">{{ editing ? '编辑奖励' : '新增奖励' }}</div>
        <div class="form-group">
          <label>名称</label>
          <input v-model="form.name" placeholder="如：即时畅饮券" />
        </div>
        <div class="form-group">
          <label>描述</label>
          <input v-model="form.description" placeholder="如：兑换一杯奶茶/咖啡" />
        </div>
        <div class="form-group">
          <label>所需积分</label>
          <input v-model.number="form.pointsCost" type="number" min="1" />
        </div>
        <div class="form-group">
          <label>额度（元）</label>
          <input v-model.number="form.maxAmount" type="number" min="1" />
        </div>
        <div class="form-group">
          <label>排序</label>
          <input v-model.number="form.sortOrder" type="number" min="0" />
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
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.btn-back {
  background: none;
  color: var(--primary);
  padding: 6px 10px;
  font-size: 14px;
}

.reward-admin-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-info {
  flex: 1;
}

.item-name {
  font-weight: 700;
  font-size: 16px;
}

.item-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.item-meta {
  margin-top: 4px;
  font-size: 13px;
}

.item-cost {
  color: var(--primary);
  font-weight: 600;
  margin-right: 12px;
}

.item-amount {
  color: var(--text-secondary);
}

.item-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.btn-edit {
  background: #eef2ff;
  color: var(--primary);
}

.btn-del {
  background: #fef2f2;
  color: var(--danger);
}
</style>
