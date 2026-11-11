<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">🐾 健康管家</div>
      <nav>
        <router-link to="/chat" class="nav-item">💬 智能问答</router-link>
        <router-link to="/kb" class="nav-item">📚 知识库管理</router-link>
        <router-link to="/search" class="nav-item">🔍 知识搜索</router-link>
        <router-link v-if="auth.isAdmin" to="/users" class="nav-item">👥 用户权限</router-link>
      </nav>
    </aside>

    <div class="main">
      <header class="topbar">
        <span class="title">{{ pageTitle }}</span>
        <div class="user">
          <span class="tag">{{ roleLabel }}</span>
          <span>{{ auth.user?.nickname || auth.user?.username }}</span>
          <button class="btn-ghost btn" @click="logout">退出</button>
        </div>
      </header>
      <section class="content">
        <router-view />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const titles = { chat: '智能问答', kb: '知识库管理', search: '知识搜索', users: '用户权限管理' }
const pageTitle = computed(() => titles[route.name] || '宠物智能健康管家服务平台')

const roleLabel = computed(() => {
  const r = auth.roles
  if (r.includes('ROLE_ADMIN')) return '管理员'
  if (r.includes('ROLE_EDITOR')) return '编辑'
  return '普通用户'
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
}
.sidebar {
  width: 220px;
  background: #1a202c;
  color: #cbd5e0;
  padding: 18px 12px;
  display: flex;
  flex-direction: column;
}
.brand {
  font-size: 20px;
  font-weight: bold;
  color: #fff;
  padding: 10px 12px 22px;
}
nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.nav-item {
  padding: 11px 14px;
  border-radius: 8px;
  color: #cbd5e0;
  transition: background 0.2s;
}
.nav-item:hover {
  background: #2d3748;
}
.nav-item.router-link-active {
  background: var(--primary);
  color: #fff;
}
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.topbar {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}
.title {
  font-size: 18px;
  font-weight: 600;
}
.user {
  display: flex;
  align-items: center;
  gap: 12px;
}
.content {
  flex: 1;
  padding: 24px;
  overflow: auto;
}
</style>
