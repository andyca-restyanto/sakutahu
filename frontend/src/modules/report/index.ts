// filepath: /frontend/src/modules/report/index.ts
import type { RouteRecordRaw } from 'vue-router'

export { reportService } from './services/report.service'
export type * from './types/report.types'

/** Dipasang sebagai child dari layout utama (lihat /frontend/src/router/index.ts). */
export const reportRoutes: RouteRecordRaw[] = [
  {
    path: '/reports',
    name: 'reports',
    component: () => import('./views/JournalReportView.vue'),
    meta: { requiresAuth: true, title: 'Laporan' },
  },
]
