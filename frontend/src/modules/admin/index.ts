// filepath: /frontend/src/modules/admin/index.ts
import type { RouteLocationNormalized, RouteLocationRaw, RouteRecordRaw } from 'vue-router'
import { useAdminAuthStore } from './stores/useAdminAuthStore'

export { useAdminAuthStore }
export type * from './types/admin.types'

/**
 * Panel admin di /admin. Semua halaman di-lazy-load sehingga kode admin tidak ikut terunduh oleh user biasa.
 * meta.admin = 'guest' (belum login), 'auth' (wajib login admin), 'owner' (wajib OWNER).
 */
export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('./views/AdminLoginView.vue'),
    meta: { admin: 'guest', title: 'Admin · Masuk' },
  },
  {
    path: '/admin/accept-invite',
    name: 'admin-accept-invite',
    component: () => import('./views/AdminAcceptInviteView.vue'),
    meta: { admin: 'public', title: 'Admin · Terima undangan' },
  },
  {
    path: '/admin/change-password',
    name: 'admin-change-password',
    component: () => import('./views/AdminChangePasswordView.vue'),
    meta: { admin: 'auth', title: 'Admin · Ganti password' },
  },
  {
    path: '/admin',
    component: () => import('./components/AdminLayout.vue'),
    meta: { admin: 'auth' },
    redirect: { name: 'admin-users' },
    children: [
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('./views/AdminUsersView.vue'),
        meta: { admin: 'auth', title: 'Admin · User' },
      },
      {
        path: 'users/:id',
        name: 'admin-user-detail',
        component: () => import('./views/AdminUserDetailView.vue'),
        meta: { admin: 'auth', title: 'Admin · Detail user' },
      },
      {
        path: 'settings/receipt-scan',
        name: 'admin-receipt-settings',
        component: () => import('./views/AdminReceiptSettingsView.vue'),
        meta: { admin: 'auth', title: 'Admin · Kuota struk' },
      },
      {
        path: 'admins',
        name: 'admin-accounts',
        component: () => import('./views/AdminAccountsView.vue'),
        meta: { admin: 'owner', title: 'Admin · Kelola admin' },
      },
    ],
  },
  { path: '/admin/:pathMatch(.*)*', redirect: { name: 'admin-users' } },
]

/** Guard khusus /admin/**. Dipanggil dari router utama; tidak menyentuh sesi user. */
export async function adminGuard(to: RouteLocationNormalized): Promise<true | RouteLocationRaw> {
  const store = useAdminAuthStore()
  const mode = to.meta.admin
  if (mode === 'public') return true

  if (!store.initialized) {
    await store.restoreSession()
  }
  if (mode === 'guest') {
    if (!store.isAuthenticated) return true
    return store.mustChangePassword ? { name: 'admin-change-password' } : { name: 'admin-users' }
  }
  if (!store.isAuthenticated) {
    return { name: 'admin-login', query: { redirect: to.fullPath } }
  }
  // Password awal wajib diganti dulu
  if (store.mustChangePassword && to.name !== 'admin-change-password') {
    return { name: 'admin-change-password' }
  }
  if (mode === 'owner' && !store.isOwner) {
    return { name: 'admin-users' }
  }
  return true
}
