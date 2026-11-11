<template>
  <div class="users">
    <div class="card">
      <div class="head">
        <h3>👥 用户与角色权限管理</h3>
        <button class="btn-ghost btn small" @click="load">刷新</button>
      </div>
      <table class="table">
        <thead>
          <tr><th>ID</th><th>用户名</th><th>昵称</th><th>邮箱</th><th>当前角色</th><th>调整角色</th></tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.username }}</td>
            <td>{{ u.nickname }}</td>
            <td>{{ u.email || '-' }}</td>
            <td><span v-for="r in u.roles" :key="r" class="tag">{{ roleLabel(r) }}</span></td>
            <td>
              <div class="role-edit">
                <select v-model="u._selected">
                  <option value="ROLE_USER">普通用户</option>
                  <option value="ROLE_EDITOR">编辑</option>
                  <option value="ROLE_ADMIN">管理员</option>
                </select>
                <button class="btn small" @click="changeRole(u)">保存</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="msg" class="msg">{{ msg }}</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../api'

const users = ref([])
const msg = ref('')

onMounted(load)

async function load() {
  const list = await adminApi.listUsers()
  users.value = list.map((u) => ({ ...u, _selected: u.roles[0] || 'ROLE_USER' }))
}

const labels = { ROLE_ADMIN: '管理员', ROLE_EDITOR: '编辑', ROLE_USER: '普通用户' }
function roleLabel(r) {
  return labels[r] || r
}

async function changeRole(u) {
  msg.value = ''
  try {
    await adminApi.updateRole(u.id, u._selected)
    msg.value = `已更新 ${u.username} 的角色`
    await load()
  } catch (e) {
    msg.value = '更新失败:' + e.message
  }
}
</script>

<style scoped>
.head {
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
.table th { color: var(--muted); }
.tag { margin-right: 6px; }
.role-edit { display: flex; gap: 8px; }
.btn.small { padding: 5px 12px; font-size: 13px; }
.msg { margin-top: 12px; color: var(--primary-dark); font-size: 13px; }
</style>
