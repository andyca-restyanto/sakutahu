// filepath: /frontend/src/modules/auth/services/auth.service.ts
import { http, rawHttp } from '@/shared/api/http'
import type {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  MessageResponse,
  RegisterRequest,
  RegisterResponse,
  ResendVerificationRequest,
  ResetPasswordInfoResponse,
  ResetPasswordRequest,
  UpdatePasswordRequest,
  UserResponse,
  VerifyEmailRequest,
} from '../types/auth.types'

export const authService = {
  async register(payload: RegisterRequest): Promise<RegisterResponse> {
    const { data } = await http.post<RegisterResponse>('/auth/register', payload)
    return data
  },

  async verifyEmail(payload: VerifyEmailRequest): Promise<AuthResponse> {
    const { data } = await http.post<AuthResponse>('/auth/verify-email', payload)
    return data
  },

  async resendVerification(payload: ResendVerificationRequest): Promise<MessageResponse> {
    const { data } = await http.post<MessageResponse>('/auth/resend-verification', payload)
    return data
  },

  async login(payload: LoginRequest): Promise<AuthResponse> {
    const { data } = await http.post<AuthResponse>('/auth/login', payload)
    return data
  },

  /** Memakai rawHttp: refresh tidak boleh memicu interceptor refresh lagi. */
  async refresh(): Promise<AuthResponse> {
    const { data } = await rawHttp.post<AuthResponse>('/auth/refresh')
    return data
  },

  async logout(): Promise<void> {
    await rawHttp.post('/auth/logout')
  },

  /** Selalu berhasil dengan pesan yang sama, terdaftar atau tidak. */
  async forgotPassword(payload: ForgotPasswordRequest): Promise<MessageResponse> {
    const { data } = await http.post<MessageResponse>('/auth/forgot-password', payload)
    return data
  },

  async inspectReset(token: string): Promise<ResetPasswordInfoResponse> {
    const { data } = await http.post<ResetPasswordInfoResponse>('/auth/reset-password/inspect', { token })
    return data
  },

  async resetPassword(payload: ResetPasswordRequest): Promise<MessageResponse> {
    const { data } = await http.post<MessageResponse>('/auth/reset-password', payload)
    return data
  },

  /** Mengembalikan sesi baru; sesi di perangkat lain dicabut server. */
  async changePassword(payload: UpdatePasswordRequest): Promise<AuthResponse> {
    const { data } = await http.post<AuthResponse>('/auth/change-password', payload)
    return data
  },

  async me(): Promise<UserResponse> {
    const { data } = await http.get<UserResponse>('/auth/me')
    return data
  },
}
