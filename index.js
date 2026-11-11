import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import KnowledgeBase from '../views/KnowledgeBase.vue'
import Chat from '../views/Chat.vue'
import Search from '../views/Search.vue'
import Users from '../views/Users.vue'

const routes = [
  { path: '/login', name: 'login', component: Login, meta: { public: true } },
  {
    path: '/',
    component: Layout,
    redirect: '/chat',
    children: [
      { path: 'kb', name: 'kb', component: KnowledgeBase },
      { path: 'chat', name: 'chat', component: Chat },
      { path: 'search', name: 'search', component: Search },
      { path: 'users', name: 'users', component: Users, meta: { adminOnly: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫:未登录跳转登录页,管理员路由鉴权
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    return { name: 'login' }
  }
  if (to.meta.adminOnly && !auth.isAdmin) {
    return { name: 'chat' }
  }
  return true
})

export default router
