<template>
  <div class="search">
    <div class="card">
      <h3>🔍 知识库语义搜索</h3>
      <p class="muted">基于向量相似度检索,并对命中关键词高亮显示</p>
      <div class="search-bar">
        <input v-model.trim="query" placeholder="输入搜索内容,如:狗狗拉稀怎么办"
               class="q" @keyup.enter="doSearch" />
        <select v-model="category">
          <option value="">全部分类</option>
          <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
        </select>
        <select v-model.number="topK">
          <option :value="5">Top 5</option>
          <option :value="10">Top 10</option>
          <option :value="20">Top 20</option>
        </select>
        <button class="btn" :disabled="loading || !query" @click="doSearch">
          {{ loading ? '搜索中...' : '搜索' }}
        </button>
      </div>
      <div v-if="error" class="err">{{ error }}</div>
    </div>

    <div class="results">
      <div v-if="searched" class="result-summary muted">
        共 {{ total }} 条相关结果,关键词:「{{ query }}」
      </div>
      <div v-for="item in items" :key="item.chunkId" class="card result-item">
        <div class="result-head">
          <span class="tag">{{ item.category || '未分类' }}</span>
          <span class="file">《{{ item.fileName || '未知文档' }}》</span>
          <span class="score-badge">相似度 {{ (item.score * 100).toFixed(1) }}%</span>
        </div>
        <!-- 后端已生成含 <mark> 的高亮 HTML -->
        <div class="snippet" v-html="item.highlighted"></div>
        <div class="result-foot">
          <button class="btn-ghost btn small" @click="askAbout(item)">就这条提问</button>
        </div>
      </div>
      <div v-if="searched && !items.length" class="card muted center">未检索到相关内容,请先上传知识库文档。</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { kbApi, searchApi } from '../api'

const router = useRouter()
const query = ref('')
const category = ref('')
const topK = ref(5)
const items = ref([])
const total = ref(0)
const loading = ref(false)
const searched = ref(false)
const error = ref('')
const categories = ref([])

onMounted(async () => {
  try { categories.value = await kbApi.listCategories() } catch (e) { /* ignore */ }
})

async function doSearch() {
  if (!query.value) return
  loading.value = true
  error.value = ''
  try {
    const data = await searchApi.search({ query: query.value, category: category.value || undefined, topK: topK.value })
    items.value = data.items || []
    total.value = data.total || 0
    searched.value = true
  } catch (e) {
    error.value = '搜索失败:' + e.message
  } finally {
    loading.value = false
  }
}

function askAbout(item) {
  // 将选中片段作为上下文带入问答(通过路由参数简单传递)
  router.push({ name: 'chat' })
}
</script>

<style scoped>
.card { margin-bottom: 16px; }
.search-bar {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.q { flex: 1; min-width: 240px; }
.err { color: #e53e3e; margin-top: 10px; font-size: 13px; }
.result-summary { margin-bottom: 12px; font-size: 14px; }
.result-item { padding: 16px; }
.result-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.file { font-weight: 600; }
.score-badge {
  margin-left: auto;
  font-size: 12px;
  color: var(--primary-dark);
  background: #e6fffa;
  padding: 2px 10px;
  border-radius: 999px;
}
.snippet {
  line-height: 1.7;
  color: #2d3748;
  max-height: 120px;
  overflow: hidden;
  word-break: break-word;
}
.snippet :deep(mark) {
  background: var(--mark);
  padding: 0 2px;
  border-radius: 3px;
  font-weight: 600;
}
.result-foot { margin-top: 10px; text-align: right; }
.btn.small { padding: 5px 12px; font-size: 13px; }
.center { text-align: center; }
</style>
