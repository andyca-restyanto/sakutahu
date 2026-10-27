// filepath: /frontend/src/shared/api/http.ts
import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { session } from './session'
import type { ApiErrorResponse } from './errors'

const baseURL = `${import.meta.env.VITE_API_BASE_URL ?? ''}/api/v1`

/** Instance Axios utama untuk semua modul. */
export const http = axios.create({
  baseURL,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
})

/** Instance terpisah tanpa interceptor, khusus untuk /auth/refresh (mencegah loop). */
export const rawHttp = axios.create({
  baseURL,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
})

/** Endpoint auth publik: 401 di sini adalah jawaban bisnis, bukan tanda token kedaluwarsa. */
const NO_REFRESH_PATHS = [
  '/auth/login',
  '/auth/register',
  '/auth/refresh',
  '/auth/logout',
  '/auth/verify-email',
  '/auth/resend-verification',
]

http.interceptors.request.use((config) => {
  const token = session.getToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

/** Satu promise refresh dipakai bersama oleh semua request yang gagal bersamaan. */
let refreshInFlight: Promise<string | null> | null = null

function refreshOnce(): Promise<string | null> {
  refreshInFlight ??= session.refresh().finally(() => {
    refreshInFlight = null
  })
  return refreshInFlight
}

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean }

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const config = error.config as RetriableConfig | undefined
    const status = error.response?.status
    const isAuthPath = NO_REFRESH_PATHS.some((p) => config?.url?.startsWith(p))

    if (status !== 401 || !config || config._retry || isAuthPath) {
      throw error
    }

    config._retry = true
    const newToken = await refreshOnce()
    if (!newToken) {
      session.expire()
      throw error
    }
    config.headers.set('Authorization', `Bearer ${newToken}`)
    return http(config)
  },
)
