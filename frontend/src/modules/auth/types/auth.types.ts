// filepath: /frontend/src/modules/auth/types/auth.types.ts
// Harus match 100% dengan /backend/.../modules/auth/dto/*.java
// UUID & Instant (Java) → string (ISO-8601); Long → number; boolean → boolean.

/** RegisterRequest.java */
export interface RegisterRequest {
  fullName: string
  email: string
  password: string
  currency?: string | null
  timezone?: string | null
}

/** RegisterResponse.java */
export interface RegisterResponse {
  email: string
  verificationRequired: boolean
  message: string
}

/** LoginRequest.java */
export interface LoginRequest {
  email: string
  password: string
}

/** VerifyEmailRequest.java */
export interface VerifyEmailRequest {
  token: string
}

/** ResendVerificationRequest.java */
export interface ResendVerificationRequest {
  email: string
}

/** MessageResponse.java */
export interface MessageResponse {
  message: string
}

/** UserPlan.java (modules/subscription/entity) */
export type UserPlan = 'FREE' | 'VIP'

/** UserResponse.java */
export interface UserResponse {
  id: string
  fullName: string
  email: string
  emailVerified: boolean
  currency: string
  timezone: string
  telegramLinked: boolean
  /** Plan efektif: VIP yang sudah kedaluwarsa dikirim sebagai FREE */
  plan: UserPlan
  createdAt: string
}

/** ForgotPasswordRequest.java */
export interface ForgotPasswordRequest {
  email: string
}

/** ResetPasswordInspectRequest.java */
export interface ResetPasswordInspectRequest {
  token: string
}

/** ResetPasswordInfoResponse.java */
export interface ResetPasswordInfoResponse {
  maskedEmail: string
  expiresAt: string
}

/** ResetPasswordRequest.java */
export interface ResetPasswordRequest {
  token: string
  newPassword: string
}

/** UpdatePasswordRequest.java (ganti password saat login) */
export interface UpdatePasswordRequest {
  currentPassword: string
  newPassword: string
}

/** AuthResponse.java */
export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: UserResponse
}
