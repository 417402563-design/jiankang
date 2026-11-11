<template>
  <div class="login-wrap">
    <div class="card login-card">
      <h2>🐾 宠物智能健康管家</h2>
      <p class="muted subtitle">知识库 · AI 问答 · 智能搜索</p>

      <div class="tabs">
        <button :class="['tab', mode === 'login' ? 'active' : '']" @click="mode = 'login'">登录</button>
        <button :class="['tab', mode === 'register' ? 'active' : '']" @click="mode = 'register'">注册</button>
      </div>

      <div class="field">
        <label>用户名</label>
        <input v-model.trim="form.username" placeholder="请输入用户名" @keyup.enter="submit" />
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="请输入密码" @keyup.enter="submit" />
      </div>

      <template v-if="mode === 'register'">
        <div class="field">
          <label>昵称</label>
          <input v-model.trim="form.nickname" placeholder="选填" />
        </div>
        <div class="field">
          <label>邮箱</label>
          <input v-model.trim="form.email" placeholder="选填" />
        </div>
      </template>

      <div v-if="error" class="error">{{ error }}</div>

      <button class="btn submit" :disabled="loading" @click="submit">
        {{ loading ? '处理中...' : (mode === 'login' ? '登录' : '注册') }}
      </button>

      <p class="hint muted">默认账号:admin / admin123 · editor / editor123 · user / user123</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()

const mode = ref('login')
const loading = ref(false)
const error = ref('')
const form = reactive({ username: '', password: '', nickname: '', email: '' })

async function submit() {
  error.value = ''
  if (!form.username || !form.password) {
    error.value = '用户名和密码不能为空'
    return
  }
  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login(form.username, form.password)
    } else {
      await auth.register({
        username: form.username,
        password: form.password,
        nickname: form.nickname || undefined,
        email: form.email || undefined
      })
    }
    router.push('/chat')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2f855a 0%, #2b6cb0 100%);
}
.login-card {
  width: 380px;
  padding: 32px;
}
h2 {
  margin: 0;
  text-align: center;
}
.subtitle {
  text-align: center;
  margin: 6px 0 20px;
}
.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
}
.tab {
  flex: 1;
  padding: 8px;
  border: 1px solid var(--border);
  background: #fff;
  border-radius: 8px;
}
.tab.active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}
.field {
  margin-bottom: 14px;
}
.field label {
  display: block;
  font-size: 13px;
  margin-bottom: 4px;
  color: var(--muted);
}
.field input {
  width: 100%;
}
.submit {
  width: 100%;
  margin-top: 8px;
}
.error {
  color: #e53e3e;
  font-size: 13px;
  margin-bottom: 10px;
}
.hint {
  font-size: 12px;
  text-align: center;
  margin-top: 16px;
}
</style>
