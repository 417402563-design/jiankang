import { defineStore } from 'pinia'
import http from '../api/http'

// 认证与权限状态管理
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('jk_token') || '',
    user: JSON.parse(localStorage.getItem('jk_user') || 'null')
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    roles: (s) => (s.user && s.user.roles) || [],
    // 是否可维护知识库(ADMIN 或 EDITOR)
    canManageKb: (s) => {
      const r = (s.user && s.user.roles) || []
      return r.includes('ROLE_ADMIN') || r.includes('ROLE_EDITOR')
    },
    isAdmin: (s) => ((s.user && s.user.roles) || []).includes('ROLE_ADMIN')
  },
  actions: {
    async login(username, password) {
      const data = await http.post('/auth/login', { username, password })
      this.setAuth(data)
      return data
    },
    async register(payload) {
      const data = await http.post('/auth/register', payload)
      this.setAuth(data)
      return data
    },
    async fetchMe() {
      const data = await http.get('/auth/me')
      this.user = data
      localStorage.setItem('jk_user', JSON.stringify(data))
    },
    setAuth(data) {
      this.token = data.token
      this.user = data.user
      localStorage.setItem('jk_token', data.token)
      localStorage.setItem('jk_user', JSON.stringify(data.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('jk_token')
      localStorage.removeItem('jk_user')
    }
  }
})
