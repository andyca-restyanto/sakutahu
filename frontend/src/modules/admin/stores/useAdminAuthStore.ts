// filepath: /frontend/src/modules/admin/stores/useAdminAuthStore.ts
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { adminSession, refreshAdminSession } from '../services/adminHttp'
import { adminAuthService } from '../services/adminAuth.service'
import type {
  AcceptInvitationRequest,
  AdminAuthResponse,
  AdminLoginRequest,
  AdminResponse,
  ChangePasswordRequest,
} from '../types/admin.types'

/** Sesi admin, sepenuhnya terpisah dari useAuthStore (sesi user). */
export const useAdminAuthStore = defineStore('adminAuth', () => {
  const admin = ref<AdminResponse | null>(null)
  const initialized = ref(false)
  /** true jika sesi berakhir sendiri (refresh gagal), untuk pesan di halaman login. */
  const expired = ref(false)

  // admin & token selalu di-set bersamaan lewat adminSession.apply()
  const isAuthenticated = computed(() => !!admin.value)
  const isOwner = computed(() => admin.value?.role === 'OWNER')
  const mustChangePassword = computed(() => !!admin.value?.mustChangePassword)

  // Interceptor adminHttp memberi tahu store setiap kali sesi berubah
  adminSession.onChange((auth) => {
    if (auth) {
      admin.value = auth.admin
    } else if (admin.value) {
      admin.value = null
      expired.value = true
    }
  })

  function apply(auth: AdminAuthResponse): void {
    expired.value = false
    adminSession.apply(auth)
  }

  async function login(payload: AdminLoginRequest): Promise<void> {
    apply(await adminAuthService.login(payload))
  }

  async function acceptInvitation(payload: AcceptInvitationRequest): Promise<void> {
    apply(await adminAuthService.acceptInvitation(payload))
  }

  async function changePassword(payload: ChangePasswordRequest): Promise<void> {
    apply(await adminAuthService.changePassword(payload))
  }

  /** Sekali saat panel admin dibuka: pulihkan sesi dari cookie refresh admin. */
  async function restoreSession(): Promise<void> {
    if (initialized.value) return
    try {
      await refreshAdminSession()
      expired.value = false
    } finally {
      initialized.value = true
    }
  }

  async function logout(): Promise<void> {
    try {
      await adminAuthService.logout()
    } finally {
      admin.value = null
      adminSession.apply(null)
      expired.value = false
    }
  }

  return { admin, initialized, expired, isAuthenticated, isOwner, mustChangePassword, login, acceptInvitation,
    changePassword, restoreSession, logout }
})
