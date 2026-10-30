// filepath: /frontend/src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import { authRoutes, useAuthStore } from '@/modules/auth'
import { telegramRoutes } from '@/modules/telegram'
import { dashboardRoutes } from '@/modules/dashboard'
import { transactionRoutes } from '@/modules/transaction'
import { adminGuard, adminRoutes } from '@/modules/admin'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...authRoutes,
    ...telegramRoutes,
    // Panel admin: sesi & guard terpisah dari user
    ...adminRoutes,
    // Halaman utama setelah login memakai layout dengan navigasi & tombol "+ Catat"
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      redirect: { name: 'dashboard' },
      children: [...dashboardRoutes, ...transactionRoutes],
    },
    { path: '/:pathMatch(.*)*', redirect: { name: 'dashboard' } },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  // Panel admin tidak memakai (dan tidak memulihkan) sesi user
  if (to.meta.admin) {
    return adminGuard(to)
  }

  const auth = useAuthStore()
  // Pulihkan sesi dari cookie refresh_token sekali saat aplikasi pertama dibuka
  if (!auth.initialized) {
    await auth.restoreSession()
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAuth && auth.user && !auth.user.emailVerified) {
    return { name: 'check-email', query: { email: auth.user.email } }
  }
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'dashboard' }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · SakuTahu` : 'SakuTahu'
  // Panel admin tidak boleh muncul di mesin pencari
  let robots = document.querySelector<HTMLMetaElement>('meta[name="robots"]')
  if (to.meta.admin) {
    if (!robots) {
      robots = document.createElement('meta')
      robots.name = 'robots'
      document.head.appendChild(robots)
    }
    robots.content = 'noindex, nofollow'
  } else {
    robots?.remove()
  }
})
