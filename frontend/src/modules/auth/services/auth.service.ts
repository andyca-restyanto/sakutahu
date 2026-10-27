// filepath: /frontend/src/modules/auth/services/auth.service.ts
import { http, rawHttp } from '@/shared/api/http'
import type {
  AuthResponse,
  LoginRequest,
  MessageResponse,
  RegisterRequest,
  RegisterResponse,
  ResendVerificationRequest,
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

  async me(): Promise<UserResponse> {
    const { data } = await http.get<UserResponse>('/auth/me')
    return data
  },
}
