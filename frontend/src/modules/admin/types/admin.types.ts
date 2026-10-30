// filepath: /frontend/src/modules/admin/types/admin.types.ts
// Harus match 100% dengan /backend/.../modules/admin/{dto,entity}/*.java
// UUID & Instant (Java) → string (ISO-8601); Long/int → number.
import type { UserPlan } from '@/modules/auth'
import type { FeatureAccessMode } from '@/modules/subscription'

export type { UserPlan, FeatureAccessMode }

/** AdminRole.java */
export type AdminRole = 'OWNER' | 'ADMIN'

/** AdminStatus.java */
export type AdminStatus = 'ACTIVE' | 'DISABLED'

/** AdminUserStatusFilter.java (query param) */
export type AdminUserStatusFilter = 'ACTIVE' | 'BLOCKED'

/** AdminLoginRequest.java */
export interface AdminLoginRequest {
  email: string
  password: string
}

/** AdminResponse.java */
export interface AdminResponse {
  id: string
  fullName: string
  email: string
  role: AdminRole
  status: AdminStatus
  mustChangePassword: boolean
  lastLoginAt: string | null
  createdAt: string
}

/** AdminAuthResponse.java */
export interface AdminAuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  admin: AdminResponse
}

/** ChangePasswordRequest.java */
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

/** InviteAdminRequest.java */
export interface InviteAdminRequest {
  email: string
  fullName: string
}

/** AdminInvitationResponse.java */
export interface AdminInvitationResponse {
  id: string
  email: string
  fullName: string
  invitedByName: string | null
  expiresAt: string
  expired: boolean
  createdAt: string
}

/** InvitationTokenRequest.java */
export interface InvitationTokenRequest {
  token: string
}

/** InvitationInfoResponse.java */
export interface InvitationInfoResponse {
  email: string
  fullName: string
  expiresAt: string
}

/** AcceptInvitationRequest.java */
export interface AcceptInvitationRequest {
  token: string
  password: string
}

/** UpdateAdminStatusRequest.java */
export interface UpdateAdminStatusRequest {
  status: AdminStatus
}

/** AdminUserSummaryResponse.java — plan = plan efektif */
export interface AdminUserSummaryResponse {
  id: string
  fullName: string
  email: string
  plan: UserPlan
  planExpiresAt: string | null
  emailVerified: boolean
  telegramLinked: boolean
  blocked: boolean
  createdAt: string
}

/** AdminAuditEntryResponse.java */
export interface AdminAuditEntryResponse {
  id: number
  action: string
  adminName: string | null
  detail: Record<string, string | null> | null
  createdAt: string
}

/** AdminUserDetailResponse.java */
export interface AdminUserDetailResponse {
  id: string
  fullName: string
  email: string
  plan: UserPlan
  planExpiresAt: string | null
  emailVerified: boolean
  telegramLinked: boolean
  telegramUsername: string | null
  timezone: string
  transactionCount: number
  receiptScansThisMonth: number
  blocked: boolean
  blockedAt: string | null
  blockedReason: string | null
  createdAt: string
  history: AdminAuditEntryResponse[]
}

/** ChangePlanRequest.java — planExpiresAt null = VIP tanpa batas */
export interface ChangePlanRequest {
  plan: UserPlan
  planExpiresAt: string | null
}

/** BlockUserRequest.java */
export interface BlockUserRequest {
  reason: string
}

/** ReceiptScanUsage.java */
export interface ReceiptScanUsage {
  month: string
  totalScans: number
  successScans: number
  failedScans: number
  usersAtQuota: number
}

/** ReceiptScanSettingsResponse.java */
export interface ReceiptScanSettingsResponse {
  access: FeatureAccessMode
  freeMonthlyQuota: number
  updatedAt: string | null
  usage: ReceiptScanUsage
}

/** UpdateReceiptScanSettingsRequest.java */
export interface UpdateReceiptScanSettingsRequest {
  access: FeatureAccessMode
  freeMonthlyQuota: number
}

/** Query param GET /admin/users */
export interface AdminUserListQuery {
  q?: string
  plan?: UserPlan
  status?: AdminUserStatusFilter
  page?: number
  size?: number
}
