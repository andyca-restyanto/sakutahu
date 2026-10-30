// filepath: /frontend/src/modules/admin/services/adminAuth.service.ts
import { adminHttp, adminRawHttp } from './adminHttp'
import type {
  AcceptInvitationRequest,
  AdminAuthResponse,
  AdminLoginRequest,
  AdminResponse,
  ChangePasswordRequest,
  InvitationInfoResponse,
} from '../types/admin.types'

export const adminAuthService = {
  async login(payload: AdminLoginRequest): Promise<AdminAuthResponse> {
    const { data } = await adminRawHttp.post<AdminAuthResponse>('/auth/login', payload)
    return data
  },

  async logout(): Promise<void> {
    await adminRawHttp.post('/auth/logout')
  },

  async me(): Promise<AdminResponse> {
    const { data } = await adminHttp.get<AdminResponse>('/auth/me')
    return data
  },

  async changePassword(payload: ChangePasswordRequest): Promise<AdminAuthResponse> {
    const { data } = await adminHttp.post<AdminAuthResponse>('/auth/change-password', payload)
    return data
  },

  async inspectInvitation(token: string): Promise<InvitationInfoResponse> {
    const { data } = await adminRawHttp.post<InvitationInfoResponse>('/invitations/inspect', { token })
    return data
  },

  async acceptInvitation(payload: AcceptInvitationRequest): Promise<AdminAuthResponse> {
    const { data } = await adminRawHttp.post<AdminAuthResponse>('/invitations/accept', payload)
    return data
  },
}
