// filepath: /frontend/src/modules/dashboard/services/dashboard.service.ts
import { http } from '@/shared/api/http'
import type { DashboardResponse } from '../types/dashboard.types'

export const dashboardService = {
  /** Tanpa from/to: backend memakai bulan berjalan menurut zona waktu user. */
  async get(from?: string, to?: string): Promise<DashboardResponse> {
    const { data } = await http.get<DashboardResponse>('/dashboard', { params: { from, to } })
    return data
  },
}
