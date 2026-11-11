<template>
  <div class="chat card">
    <div class="chat-header">
      <span>💬 宠物健康智能问答</span>
      <button class="btn-ghost btn small" @click="clearChat">新建会话</button>
    </div>

    <div class="messages" ref="msgBox">
      <div v-if="!messages.length" class="empty">
        <p>🐶 你好,我是宠物智能健康管家!</p>
        <p class="muted">上传知识库文档后,可以问我:犬猫疫苗接种、常见疾病症状、喂养与护理等问题。</p>
      </div>
      <div v-for="(m, i) in messages" :key="i" :class="['msg-row', m.role]">
        <div class="bubble">
          <div class="content">{{ m.content }}</div>
          <div v-if="m.references && m.references.length" class="refs">
            <div class="refs-title">📎 参考来源</div>
            <div v-for="(r, j) in m.references" :key="j" class="ref">
              <span class="tag">{{ r.category || '未分类' }}</span>
              <span class="ref-file">《{{ r.fileName || '未知文档' }}》</span>
              <span class="score">相似度 {{ (r.score * 100).toFixed(1) }}%</span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="loading" class="msg-row assistant">
        <div class="bubble"><div class="content thinking">思考中...</div></div>
      </div>
    </div>

    <div class="composer">
      <select v-model="category" class="cat-select">
        <option value="">全部分类</option>
        <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
      </select>
      <textarea v-model="input" rows="1" placeholder="输入你的问题,Enter 发送,Shift+Enter 换行"
                @keydown.enter.exact.prevent="send"></textarea>
      <button class="btn" :disabled="loading || !input.trim()" @click="send">发送</button>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { chatApi, kbApi } from '../api'

const messages = ref([])
const input = ref('')
const loading = ref(false)
const category = ref('')
const categories = ref([])
const sessionId = ref('sess-' + Date.now())
const msgBox = ref(null)

onMounted(async () => {
  try { categories.value = await kbApi.listCategories() } catch (e) { /* ignore */ }
  const history = await chatApi.history(sessionId.value)
  messages.value = (history || []).map((h) => ({ role: h.role, content: h.content }))
  scrollToBottom()
})

async function send() {
  const q = input.value.trim()
  if (!q || loading.value) return
  messages.value.push({ role: 'user', content: q })
  input.value = ''
  loading.value = true
  scrollToBottom()
  try {
    const data = await chatApi.ask({ sessionId: sessionId.value, question: q, category: category.value || undefined })
    messages.value.push({ role: 'assistant', content: data.answer, references: data.references })
    if (data.sessionId) sessionId.value = data.sessionId
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '出错了:' + e.message })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = []
  sessionId.value = 'sess-' + Date.now()
}

function scrollToBottom() {
  nextTick(() => {
    if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
  })
}
</script>

<style scoped>
.chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 108px);
  padding: 0;
  overflow: hidden;
}
.chat-header {
  padding: 14px 18px;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: var(--bg);
}
.empty {
  text-align: center;
  margin-top: 60px;
}
.msg-row {
  display: flex;
  margin-bottom: 16px;
}
.msg-row.user { justify-content: flex-end; }
.msg-row.assistant { justify-content: flex-start; }
.bubble {
  max-width: 72%;
  padding: 12px 15px;
  border-radius: 14px;
  line-height: 1.6;
}
.user .bubble {
  background: var(--primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.assistant .bubble {
  background: #fff;
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
}
.content {
  white-space: pre-wrap;
  word-break: break-word;
}
.thinking { color: var(--muted); }
.refs {
  margin-top: 10px;
  border-top: 1px dashed var(--border);
  padding-top: 8px;
}
.refs-title {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}
.ref {
  display: flex;
  gap: 10px;
  align-items: center;
  font-size: 12px;
  padding: 3px 0;
}
.ref-file { color: var(--text); }
.score { color: var(--muted); margin-left: auto; }
.composer {
  display: flex;
  gap: 10px;
  padding: 14px 18px;
  border-top: 1px solid var(--border);
  background: #fff;
  align-items: flex-end;
}
.cat-select { width: 130px; }
.composer textarea {
  flex: 1;
  resize: none;
  max-height: 120px;
}
.btn.small { padding: 5px 12px; font-size: 13px; }
</style>
