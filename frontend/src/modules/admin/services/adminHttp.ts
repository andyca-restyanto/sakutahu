// filepath: /frontend/src/modules/admin/services/adminHttp.ts
/**
 * Axios & sesi KHUSUS admin, terpisah dari shared/api/http (sesi user).
 * Token admin hanya di memory; refresh lewat cookie admin_refresh_token (path /api/v1/admin/auth).
 */
import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiErrorResponse } from '@/shared/api/errors'
import type { AdminAuthResponse } from '../types/admin.types'

const baseURL = `${import.meta.env.VITE_API_BASE_URL ?? ''}/api/v1/admin`

const config = {
  baseURL,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
}

/** Untuk endpoint yang butuh login admin. */
export const adminHttp = axios.create(config)
/** Tanpa interceptor: login, refresh, logout, undangan (mencegah loop refresh). */
export const adminRawHttp = axios.create(config)

type SessionListener = (auth: AdminAuthResponse | null) => void

let accessToken: string | null = null
let listener: SessionListener | null = null

export const adminSession = {
  getToken: (): string | null => accessToken,
  /** Dipanggil store setiap kali sesi berubah (login, refresh, logout). */
  apply(auth: AdminAuthResponse | null): void {
    accessToken = auth?.accessToken ?? null
    listener?.(auth)
  },
  onChange(fn: SessionListener): void {
    listener = fn
  },
}

adminHttp.interceptors.request.use((req) => {
  if (accessToken) req.headers.set('Authorization', `Bearer ${accessToken}`)
  return req
})

let refreshInFlight: Promise<string | null> | null = null

/** Satu refresh dipakai bersama oleh semua request yang gagal bersamaan. */
export function refreshAdminSession(): Promise<string | null> {
  refreshInFlight ??= adminRawHttp
    .post<AdminAuthResponse>('/auth/refresh')
    .then(({ data }) => {
      adminSession.apply(data)
      return data.accessToken
    })
    .catch(() => {
      adminSession.apply(null)
      return null
    })
    .finally(() => {
      refreshInFlight = null
    })
  return refreshInFlight
}

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean }

adminHttp.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const req = error.config as RetriableConfig | undefined
    if (error.response?.status !== 401 || !req || req._retry) throw error
    req._retry = true
    const token = await refreshAdminSession()
    if (!token) throw error
    req.headers.set('Authorization', `Bearer ${token}`)
    return adminHttp(req)
  },
)
