// filepath: /frontend/src/modules/admin/services/adminUser.service.ts
import type { PageResponse } from '@/shared/api/page'
import { adminHttp } from './adminHttp'
import type {
  AdminUserDetailResponse,
  AdminUserListQuery,
  AdminUserSummaryResponse,
  BlockUserRequest,
  ChangePlanRequest,
} from '../types/admin.types'

export const adminUserService = {
  async list(query: AdminUserListQuery): Promise<PageResponse<AdminUserSummaryResponse>> {
    const { data } = await adminHttp.get<PageResponse<AdminUserSummaryResponse>>('/users', { params: query })
    return data
  },

  async get(id: string): Promise<AdminUserDetailResponse> {
    const { data } = await adminHttp.get<AdminUserDetailResponse>(`/users/${id}`)
    return data
  },

  async changePlan(id: string, payload: ChangePlanRequest): Promise<AdminUserDetailResponse> {
    const { data } = await adminHttp.put<AdminUserDetailResponse>(`/users/${id}/plan`, payload)
    return data
  },

  async block(id: string, payload: BlockUserRequest): Promise<AdminUserDetailResponse> {
    const { data } = await adminHttp.post<AdminUserDetailResponse>(`/users/${id}/block`, payload)
    return data
  },

  async unblock(id: string): Promise<AdminUserDetailResponse> {
    const { data } = await adminHttp.post<AdminUserDetailResponse>(`/users/${id}/unblock`)
    return data
  },
}
