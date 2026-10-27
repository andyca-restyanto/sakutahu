// filepath: /frontend/src/modules/dashboard/index.ts
import type { RouteRecordRaw } from 'vue-router'

export { dashboardService } from './services/dashboard.service'
export type * from './types/dashboard.types'

/** Dipasang sebagai child dari layout utama (lihat /frontend/src/router/index.ts). */
export const dashboardRoutes: RouteRecordRaw[] = [
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('./views/DashboardView.vue'),
    meta: { requiresAuth: true, title: 'Dashboard' },
  },
]
