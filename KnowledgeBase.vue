<template>
  <div class="kb">
    <!-- 上传区 -->
    <div class="card upload-card" v-if="auth.canManageKb">
      <h3>上传知识文档</h3>
      <p class="muted">支持 PDF / Word(doc, docx) / Excel(xls, xlsx) / TXT,自动解析、切片并向量化入库</p>
      <div class="upload-row">
        <input type="file" ref="fileInput" @change="onFileChange"
               accept=".pdf,.doc,.docx,.xls,.xlsx,.txt,.md" />
        <select v-model="category" placeholder="分类">
          <option value="">选择/输入分类</option>
          <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
        </select>
        <input v-model="newCategory" placeholder="或新分类名" class="cat-input" />
        <button class="btn" :disabled="!selectedFile || uploading" @click="doUpload">
          {{ uploading ? `上传中 ${progress}%` : '上传并入库' }}
        </button>
      </div>
      <div v-if="uploadMsg" :class="['msg', uploadOk ? 'ok' : 'err']">{{ uploadMsg }}</div>
    </div>
    <div v-else class="card muted">当前角色无知识库维护权限,仅可浏览文档列表(需管理员或编辑)。</div>

    <!-- 文档列表 -->
    <div class="card list-card">
      <div class="list-head">
        <h3>知识库文档</h3>
        <button class="btn-ghost btn" @click="loadDocuments">刷新</button>
      </div>
      <table class="table">
        <thead>
          <tr>
            <th>文件名</th><th>分类</th><th>类型</th><th>大小</th>
            <th>切片数</th><th>状态</th><th>上传者</th>
            <th v-if="auth.canManageKb">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in documents" :key="d.id">
            <td>{{ d.fileName }}</td>
            <td><span class="tag">{{ d.category || '未分类' }}</span></td>
            <td>{{ d.fileType }}</td>
            <td>{{ formatSize(d.fileSize) }}</td>
            <td>{{ d.chunkCount }}</td>
            <td><span :class="['status', 's-' + d.status]">{{ d.status }}</span></td>
            <td>{{ d.owner }}</td>
            <td v-if="auth.canManageKb">
              <button class="btn-danger btn small" @click="remove(d)">删除</button>
            </td>
          </tr>
          <tr v-if="!documents.length">
            <td :colspan="auth.canManageKb ? 8 : 7" class="muted center">暂无文档</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { kbApi } from '../api'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const documents = ref([])
const categories = ref([])
const selectedFile = ref(null)
const category = ref('')
const newCategory = ref('')
const uploading = ref(false)
const progress = ref(0)
const uploadMsg = ref('')
const uploadOk = ref(true)
const fileInput = ref(null)

onMounted(() => {
  loadDocuments()
  loadCategories()
})

function onFileChange(e) {
  selectedFile.value = e.target.files[0] || null
}

async function loadDocuments() {
  documents.value = await kbApi.listDocuments()
}

async function loadCategories() {
  try { categories.value = await kbApi.listCategories() } catch (e) { /* ignore */ }
}

async function doUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  progress.value = 0
  uploadMsg.value = ''
  const cat = newCategory.value || category.value
  try {
    const doc = await kbApi.upload(selectedFile.value, cat, (p) => (progress.value = p))
    uploadOk.value = true
    uploadMsg.value = `《${doc.fileName}》入库成功,共 ${doc.chunkCount} 个切片`
    selectedFile.value = null
    newCategory.value = ''
    if (fileInput.value) fileInput.value.value = ''
    await loadDocuments()
    await loadCategories()
  } catch (e) {
    uploadOk.value = false
    uploadMsg.value = '上传失败:' + e.message
  } finally {
    uploading.value = false
  }
}

async function remove(d) {
  if (!confirm(`确定删除《${d.fileName}》及其向量数据?`)) return
  await kbApi.deleteDocument(d.id)
  await loadDocuments()
}

function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}
</script>

<style scoped>
.upload-card {
  margin-bottom: 20px;
}
.upload-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 12px;
}
.cat-input {
  width: 150px;
}
.msg {
  margin-top: 10px;
  font-size: 13px;
}
.msg.ok { color: var(--primary-dark); }
.msg.err { color: #e53e3e; }
.list-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 12px;
  font-size: 14px;
}
.table th, .table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border);
  text-align: left;
}
.table th {
  color: var(--muted);
  font-weight: 600;
}
.center { text-align: center; }
.btn.small { padding: 5px 12px; font-size: 13px; }
.status { font-size: 12px; font-weight: 600; }
.s-DONE { color: var(--primary-dark); }
.s-PROCESSING { color: #dd6b20; }
.s-FAILED { color: #e53e3e; }
.s-PENDING { color: var(--muted); }
</style>
