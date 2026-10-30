// filepath: /frontend/src/modules/auth/stores/useAuthStore.ts
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { session } from '@/shared/api/session'
import { authService } from '../services/auth.service'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
  UpdatePasswordRequest,
  UserResponse,
} from '../types/auth.types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserResponse | null>(null)
  const accessToken = ref<string | null>(null)
  /** true setelah percobaan pemulihan sesi pertama selesai (berhasil atau tidak). */
  const initialized = ref(false)

  const isAuthenticated = computed(() => !!accessToken.value && !!user.value)

  function setSession(auth: AuthResponse): void {
    accessToken.value = auth.accessToken
    user.value = auth.user
    session.setToken(auth.accessToken)
  }

  function clear(): void {
    accessToken.value = null
    user.value = null
    session.setToken(null)
  }

  async function register(payload: RegisterRequest): Promise<RegisterResponse> {
    return authService.register(payload)
  }

  async function login(payload: LoginRequest): Promise<void> {
    setSession(await authService.login(payload))
  }

  /** Perangkat ini tetap login dengan sesi baru dari server. */
  async function changePassword(payload: UpdatePasswordRequest): Promise<void> {
    setSession(await authService.changePassword(payload))
  }

  async function verifyEmail(token: string): Promise<void> {
    setSession(await authService.verifyEmail({ token }))
  }

  /** Dipakai interceptor http saat access token kedaluwarsa. Mengembalikan token baru atau null. */
  async function refresh(): Promise<string | null> {
    try {
      const auth = await authService.refresh()
      setSession(auth)
      return auth.accessToken
    } catch {
      clear()
      return null
    }
  }

  /** Dipanggil sekali saat aplikasi dibuka: pulihkan sesi dari cookie refresh_token. */
  async function restoreSession(): Promise<void> {
    if (initialized.value) return
    try {
      await refresh()
    } finally {
      initialized.value = true
    }
  }

  async function logout(): Promise<void> {
    try {
      await authService.logout()
    } finally {
      clear()
    }
  }

  function patchUser(partial: Partial<UserResponse>): void {
    if (user.value) {
      user.value = { ...user.value, ...partial }
    }
  }

  return {
    user,
    accessToken,
    initialized,
    isAuthenticated,
    register,
    login,
    verifyEmail,
    changePassword,
    refresh,
    restoreSession,
    logout,
    clear,
    patchUser,
  }
})
