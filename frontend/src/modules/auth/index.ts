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
    // Publik (bukan guestOnly) agar bisa dibuka juga dari halaman Akun saat lupa password saat ini
    path: '/forgot-password',
    name: 'forgot-password',
    component: () => import('./views/ForgotPasswordView.vue'),
    meta: { title: 'Lupa password' },
  },
  {
    path: '/reset-password',
    name: 'reset-password',
    component: () => import('./views/ResetPasswordView.vue'),
    meta: { title: 'Buat password baru' },
  },
  {
    path: '/verify-email',
    name: 'verify-email',
    component: () => import('./views/VerifyEmailView.vue'),
    meta: { title: 'Verifikasi email' },
  },
]

/** Halaman di dalam layout utama (wajib login). */
export const accountRoutes: RouteRecordRaw[] = [
  {
    path: 'account',
    name: 'account',
    component: () => import('./views/AccountView.vue'),
    meta: { requiresAuth: true, title: 'Akun' },
  },
]
