// AI 智能助手浮窗组件：支持多轮对话和运动记录意图识别确认
<script setup>
import { ref, nextTick } from 'vue'
import { sendMessage } from '../api/chat'
import { submitRecord } from '../api/exercise'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const open = ref(false)
const input = ref('')
const messages = ref([])
const loading = ref(false)

// 微信风格语音输入状态
const voiceMode = ref(false)
const pressing = ref(false)
const willCancel = ref(false)
let touchStartY = 0

// 语音识别（浏览器原生 Web Speech API）
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
let recognition = null

if (SpeechRecognition) {
  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.continuous = false
  recognition.interimResults = true
  recognition.onresult = (e) => {
    let text = ''
    for (let i = e.resultIndex; i < e.results.length; i++) {
      text += e.results[i][0].transcript
    }
    input.value = text
  }
  recognition.onend = () => { pressing.value = false }
  recognition.onerror = () => { pressing.value = false }
}

// 切换语音模式，不支持时提示
function switchVoiceMode() {
  if (!voiceMode.value && !SpeechRecognition) {
    alert('当前浏览器不支持语音输入')
    return
  }
  voiceMode.value = !voiceMode.value
}

// 按住开始录音
function onPressStart(e) {
  if (!recognition || loading.value) return
  willCancel.value = false
  pressing.value = true
  input.value = ''
  // 记录触摸/鼠标起始Y坐标
  if (e.touches) {
    touchStartY = e.touches[0].clientY
  } else {
    touchStartY = e.clientY
  }
  recognition.start()
}

// 滑动时检测上滑取消
function onPressMove(e) {
  if (!pressing.value) return
  const currentY = e.touches ? e.touches[0].clientY : e.clientY
  // 上滑超过50px则标记为取消
  willCancel.value = (touchStartY - currentY > 50)
}

// 松开结束录音，根据是否取消决定发送
function onPressEnd() {
  if (!pressing.value) return
  pressing.value = false
  recognition.stop()
  if (willCancel.value) {
    input.value = ''
    willCancel.value = false
  } else {
    // 延迟一帧等识别结果填充input后再发送
    setTimeout(() => {
      if (input.value.trim()) {
        handleSend()
      }
    }, 300)
  }
}

// 将文本中的 URL 转为可点击链接（排除尾部中文/英文标点）
function linkify(text) {
  if (!text) return ''
  return text.replace(
    /(https?:\/\/[^\s<>)(）。，,；;！!？?]+)/g,
    '<a href="$1" target="_blank" rel="noopener" style="color:#409eff;text-decoration:underline">$1</a>'
  )
}

function toggle() {
  open.value = !open.value
  if (open.value && messages.value.length === 0) {
    messages.value.push({ role: 'assistant', content: '你好！我是运动积分系统小助手，有什么可以帮你的吗？' })
  }
  nextTick(scrollToBottom)
}

function scrollToBottom() {
  const el = document.querySelector('.chat-messages')
  if (el) el.scrollTop = el.scrollHeight
}

// 发送消息：过滤掉含确认卡片的历史（避免AI重复识别），解析后端返回的意图
async function handleSend() {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  await nextTick(scrollToBottom)

  try {
    // 过滤历史：排除当前消息（最后一条）、含确认卡片的AI消息及其前面的用户消息
    const sliced = messages.value.slice(0, -1).slice(-20)
    const skipIndices = new Set()
    for (let i = 0; i < sliced.length; i++) {
      if ((sliced[i].recordIntents || sliced[i].recordIntent)) {
        skipIndices.add(i)
        // 跳过该AI回复前面的用户消息
        if (i > 0 && sliced[i - 1].role === 'user') skipIndices.add(i - 1)
      }
    }
    const history = sliced.filter((_, idx) => !skipIndices.has(idx) && sliced[idx].role !== 'system').map(m => ({
      role: m.role, content: m.content
    }))
    const res = await sendMessage(text, history)
    const data = res.data
    const msg = { role: 'assistant', content: data.reply }
    // 兼容旧的单条 recordIntent 和新的多条 recordIntents
    if (data.recordIntents && data.recordIntents.length > 0) {
      msg.recordIntents = data.recordIntents.map(intent => ({ ...intent, status: 'pending' }))
    } else if (data.recordIntent) {
      msg.recordIntents = [{ ...data.recordIntent, status: 'pending' }]
    }
    messages.value.push(msg)
  } catch {
    messages.value.push({ role: 'assistant', content: '网络异常，请稍后再试。' })
  } finally {
    loading.value = false
    await nextTick(scrollToBottom)
  }
}

// 确认单条记录录入
async function confirmOne(msg, intent) {
  if (intent.status !== 'pending') return
  intent.status = 'confirming'

  try {
    const today = new Date().toISOString().slice(0, 10)
    await submitRecord({
      userId: userStore.currentUser.id,
      recordDate: today,
      exerciseTypeId: intent.exerciseTypeId,
      amount: intent.amount,
      imageUrl: null
    })
    intent.status = 'done'
  } catch (e) {
    intent.status = 'error'
    intent.errorMsg = e.message || '请稍后再试'
  }
  await nextTick(scrollToBottom)
}

// 一键确认全部
async function confirmAll(msg) {
  for (const intent of msg.recordIntents) {
    if (intent.status === 'pending') {
      await confirmOne(msg, intent)
    }
  }
}

// 取消全部未处理的记录
function cancelAll(msg) {
  for (const intent of msg.recordIntents) {
    if (intent.status === 'pending') intent.status = 'cancelled'
  }
}
</script>

<template>
  <div class="chat-assistant">
    <!-- 聊天面板 -->
    <div v-if="open" class="chat-panel">
      <div class="chat-header">
        <span>智能助手</span>
        <button class="close-btn" @click="open = false">✕</button>
      </div>
      <div class="chat-messages">
        <div v-for="(msg, i) in messages" :key="i"
             :class="['chat-bubble', msg.role === 'user' ? 'bubble-user' : 'bubble-ai']">
          <template v-if="msg.role === 'user'">{{ msg.content }}</template>
          <div v-else v-html="linkify(msg.content)"></div>
          <!-- 多条运动记录确认卡片 -->
          <div v-if="msg.recordIntents && msg.recordIntents.some(i => i.status !== 'cancelled')" class="record-cards">
            <div v-for="(intent, j) in msg.recordIntents" :key="j" class="record-card">
              <div class="record-info">
                {{ intent.exerciseName }} {{ intent.amount }}{{ intent.unit }}
                <span v-if="intent.status === 'done'" class="record-status done">✅</span>
                <span v-if="intent.status === 'error'" class="record-status error">❌</span>
              </div>
              <div v-if="intent.status === 'pending'" class="record-actions">
                <button class="confirm-btn" @click="confirmOne(msg, intent)">确认</button>
                <button class="cancel-btn" @click="intent.status = 'cancelled'">跳过</button>
              </div>
              <div v-if="intent.status === 'error'" class="record-error">{{ intent.errorMsg }}</div>
            </div>
            <!-- 多条时才显示一键确认/取消按钮 -->
            <div v-if="msg.recordIntents.length > 1 && msg.recordIntents.some(i => i.status === 'pending')" class="record-actions batch">
              <button class="confirm-btn" @click="confirmAll(msg)">全部确认</button>
              <button class="cancel-btn" @click="cancelAll(msg)">全部取消</button>
            </div>
          </div>
        </div>
        <div v-if="loading" class="chat-bubble bubble-ai">思考中...</div>
      </div>
      <div class="chat-input">
        <!-- 左侧切换按钮：键盘 ↔ 麦克风 -->
        <button v-if="SpeechRecognition" class="mode-toggle" @click="switchVoiceMode" :title="voiceMode ? '键盘输入' : '语音输入'">
          <!-- 麦克风图标 -->
          <svg v-if="!voiceMode" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
            <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
            <line x1="12" y1="19" x2="12" y2="23"/>
            <line x1="8" y1="23" x2="16" y2="23"/>
          </svg>
          <!-- 键盘图标 -->
          <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="4" width="20" height="16" rx="2"/>
            <line x1="6" y1="8" x2="6" y2="8"/>
            <line x1="10" y1="8" x2="10" y2="8"/>
            <line x1="14" y1="8" x2="14" y2="8"/>
            <line x1="18" y1="8" x2="18" y2="8"/>
            <line x1="6" y1="12" x2="6" y2="12"/>
            <line x1="10" y1="12" x2="10" y2="12"/>
            <line x1="14" y1="12" x2="14" y2="12"/>
            <line x1="18" y1="12" x2="18" y2="12"/>
            <line x1="8" y1="16" x2="16" y2="16"/>
          </svg>
        </button>
        <!-- 文本模式：输入框 + 发送按钮 -->
        <template v-if="!voiceMode">
          <input v-model="input" placeholder="输入问题..." @keyup.enter="handleSend" :disabled="loading" />
          <button class="send-btn" @click="handleSend" :disabled="loading || !input.trim()">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13"/>
              <polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
          </button>
        </template>
        <!-- 语音模式：按住说话按钮 -->
        <template v-else>
          <div class="voice-area">
            <button class="hold-talk-btn"
                    :class="{ pressing: pressing, cancel: willCancel }"
                    @touchstart.prevent="onPressStart"
                    @touchmove.prevent="onPressMove"
                    @touchend.prevent="onPressEnd"
                    @mousedown="onPressStart"
                    @mousemove="onPressMove"
                    @mouseup="onPressEnd"
                    @mouseleave="onPressEnd">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align: middle; margin-right: 4px;">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                <line x1="12" y1="19" x2="12" y2="23"/>
                <line x1="8" y1="23" x2="16" y2="23"/>
              </svg>
              {{ willCancel ? '松开 取消' : (pressing ? '松开 结束' : '按住 说话') }}
            </button>
            <!-- 录音中遮罩提示 -->
            <div v-if="pressing" class="voice-overlay">
              <div class="voice-indicator" :class="{ cancel: willCancel }">
                <div v-if="willCancel" class="voice-indicator-icon cancel-ring">
                  <svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="#fff" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                  </svg>
                </div>
                <template v-else>
                  <div class="voice-indicator-icon recording-ring">
                    <svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                      <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                      <line x1="12" y1="19" x2="12" y2="23"/>
                    </svg>
                  </div>
                  <div class="voice-hint">手指上滑，取消发送</div>
                </template>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 浮动按钮 -->
    <button class="fab-btn" @click="toggle">
      <svg v-if="open" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
        <line x1="18" y1="6" x2="6" y2="18"/>
        <line x1="6" y1="6" x2="18" y2="18"/>
      </svg>
      <svg v-else viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
        <circle cx="8.5" cy="11.5" r="0.8" fill="currentColor" stroke="none"/>
        <circle cx="12" cy="11.5" r="0.8" fill="currentColor" stroke="none"/>
        <circle cx="15.5" cy="11.5" r="0.8" fill="currentColor" stroke="none"/>
      </svg>
    </button>
  </div>
</template>

<style scoped>
.chat-assistant {
  position: fixed;
  bottom: 80px;
  right: 16px;
  z-index: 200;
}

.fab-btn {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: white;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(79,70,229,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: auto;
  transition: transform 0.2s, box-shadow 0.2s;
}

.fab-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 20px rgba(79,70,229,0.5);
}

.fab-btn:active {
  transform: scale(0.95);
}

.chat-panel {
  width: 320px;
  height: 420px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  margin-bottom: 12px;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--primary);
  color: white;
  font-weight: 600;
  font-size: 15px;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 18px;
  cursor: pointer;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-bubble {
  max-width: 80%;
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-user {
  align-self: flex-end;
  background: var(--primary);
  color: white;
  border-bottom-right-radius: 4px;
}

.bubble-ai {
  align-self: flex-start;
  background: #f1f3f5;
  color: #333;
  border-bottom-left-radius: 4px;
}

.record-cards {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.record-card {
  padding: 8px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.record-info {
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  margin-bottom: 6px;
}

.record-status.done { color: #52c41a; }
.record-status.error { color: #ff4d4f; }

.record-error {
  font-size: 12px;
  color: #ff4d4f;
  margin-top: 4px;
}

.record-actions {
  display: flex;
  gap: 6px;
}

.record-actions.batch {
  margin-top: 4px;
  padding-top: 6px;
  border-top: 1px solid #eee;
}

.confirm-btn {
  flex: 1;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: var(--primary);
  color: white;
  font-size: 12px;
  cursor: pointer;
}

.confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.cancel-btn {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: white;
  color: #666;
  font-size: 12px;
  cursor: pointer;
}

.cancel-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.chat-input {
  display: flex;
  align-items: center;
  border-top: 1px solid #eee;
  padding: 8px 10px;
  gap: 6px;
  background: #fafafa;
}

.chat-input input {
  flex: 1;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  padding: 8px 14px;
  font-size: 13px;
  outline: none;
  background: #fff;
  transition: border-color 0.2s;
}

.chat-input input:focus {
  border-color: var(--primary);
}

.send-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: var(--primary);
  color: white;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s, transform 0.15s;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.08);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

@keyframes recording-pulse {
  0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(79,70,229,0.4); }
  50% { transform: scale(1.08); box-shadow: 0 0 0 10px rgba(79,70,229,0); }
  100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(79,70,229,0); }
}

.mode-toggle {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #888;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s, background 0.2s;
}

.mode-toggle:hover {
  color: var(--primary);
  background: rgba(79,70,229,0.08);
}

.voice-area {
  flex: 1;
  position: relative;
}

.hold-talk-btn {
  width: 100%;
  padding: 9px 12px;
  border: none;
  border-radius: 20px;
  background: #fff;
  color: #555;
  font-size: 13px;
  letter-spacing: 1px;
  cursor: pointer;
  user-select: none;
  -webkit-user-select: none;
  touch-action: none;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.hold-talk-btn:active,
.hold-talk-btn.pressing {
  background: var(--primary);
  color: #fff;
  box-shadow: 0 2px 8px rgba(79,70,229,0.3);
}

.hold-talk-btn.cancel {
  background: #ff4d4f;
  color: #fff;
  box-shadow: 0 2px 8px rgba(255,77,79,0.3);
}

.voice-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 300;
  pointer-events: none;
}

.voice-indicator {
  background: rgba(0, 0, 0, 0.78);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 20px 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  min-width: 130px;
  animation: fade-in 0.15s ease-out;
}

@keyframes fade-in {
  from { opacity: 0; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1); }
}

.voice-indicator.cancel {
  background: rgba(255, 77, 79, 0.85);
}

.voice-indicator-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.voice-indicator-icon.recording-ring {
  background: rgba(255,255,255,0.15);
  animation: recording-pulse 1.5s ease-in-out infinite;
}

.voice-indicator-icon.cancel-ring {
  background: rgba(255,255,255,0.2);
}

.voice-hint {
  color: rgba(255, 255, 255, 0.85);
  font-size: 12px;
}

@media (max-width: 380px) {
  .chat-panel {
    width: calc(100vw - 32px);
    right: 0;
  }
}
</style>
