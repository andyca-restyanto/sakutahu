// filepath: /frontend/src/modules/admin/services/adminAccount.service.ts
import type { MessageResponse } from '@/modules/auth'
import { adminHttp } from './adminHttp'
import type {
  AdminInvitationResponse,
  AdminResponse,
  InviteAdminRequest,
  UpdateAdminStatusRequest,
} from '../types/admin.types'

/** Hanya untuk OWNER. */
export const adminAccountService = {
  async list(): Promise<AdminResponse[]> {
    const { data } = await adminHttp.get<AdminResponse[]>('/admins')
    return data
  },

  async changeStatus(id: string, payload: UpdateAdminStatusRequest): Promise<AdminResponse> {
    const { data } = await adminHttp.patch<AdminResponse>(`/admins/${id}/status`, payload)
    return data
  },

  /** Link reset dikirim ke email admin tersebut (bukan ke OWNER). */
  async sendPasswordReset(id: string): Promise<MessageResponse> {
    const { data } = await adminHttp.post<MessageResponse>(`/admins/${id}/password-reset`)
    return data
  },

  async listInvitations(): Promise<AdminInvitationResponse[]> {
    const { data } = await adminHttp.get<AdminInvitationResponse[]>('/admins/invitations')
    return data
  },

  async invite(payload: InviteAdminRequest): Promise<AdminInvitationResponse> {
    const { data } = await adminHttp.post<AdminInvitationResponse>('/admins/invitations', payload)
    return data
  },

  async resendInvitation(id: string): Promise<AdminInvitationResponse> {
    const { data } = await adminHttp.post<AdminInvitationResponse>(`/admins/invitations/${id}/resend`)
    return data
  },

  async revokeInvitation(id: string): Promise<void> {
    await adminHttp.delete(`/admins/invitations/${id}`)
  },
}
