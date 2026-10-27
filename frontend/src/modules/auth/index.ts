// filepath: /frontend/src/modules/auth/index.ts
import type { RouteRecordRaw } from 'vue-router'

export { useAuthStore } from './stores/useAuthStore'
export { authService } from './services/auth.service'
export type * from './types/auth.types'

export const authRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('./views/LoginView.vue'),
    meta: { guestOnly: true, title: 'Masuk' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('./views/RegisterView.vue'),
    meta: { guestOnly: true, title: 'Daftar' },
  },
  {
    path: '/check-email',
    name: 'check-email',
    component: () => import('./views/CheckEmailView.vue'),
    meta: { title: 'Cek email' },
  },
  {
    path: '/verify-email',
    name: 'verify-email',
    component: () => import('./views/VerifyEmailView.vue'),
    meta: { title: 'Verifikasi email' },
  },
]
