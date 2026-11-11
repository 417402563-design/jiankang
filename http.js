import axios from 'axios'
import router from '../router'
import { useAuthStore } from '../store/auth'

// 统一 axios 实例,baseURL 走 vite 代理到后端
const http = axios.create({
  baseURL: '/api',
  timeout: 60000
})

// 请求拦截:自动携带 JWT
http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

// 响应拦截:统一解包 ApiResponse,401 跳登录
http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) {
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data
    }
    return body
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      const auth = useAuthStore()
      auth.logout()
      router.push({ name: 'login' })
    }
    const msg = error.response?.data?.message || error.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export default http
