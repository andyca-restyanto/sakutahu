// filepath: /frontend/src/shared/api/errors.ts
import { isAxiosError } from 'axios'

/** Harus sama dengan enum ErrorCode di backend (/backend/.../shared/exception/ErrorCode.java). */
export type ApiErrorCode =
  | 'VALIDATION_ERROR'
  | 'EMAIL_ALREADY_EXISTS'
  | 'INVALID_CREDENTIALS'
  | 'EMAIL_NOT_VERIFIED'
  | 'ACCOUNT_LOCKED'
  | 'TOO_MANY_REQUESTS'
  | 'TOKEN_EXPIRED'
  | 'VERIFICATION_TOKEN_INVALID'
  | 'VERIFICATION_TOKEN_EXPIRED'
  | 'RESET_TOKEN_INVALID'
  | 'RESET_TOKEN_EXPIRED'
  | 'CATEGORY_TYPE_MISMATCH'
  | 'TRANSACTION_NOT_FOUND'
  | 'ACCOUNT_BLOCKED'
  | 'ACCOUNT_DISABLED'
  | 'PASSWORD_CHANGE_REQUIRED'
  | 'CURRENT_PASSWORD_INVALID'
  | 'ADMIN_EMAIL_EXISTS'
  | 'ADMIN_OWNER_PROTECTED'
  | 'INVITATION_INVALID'
  | 'INVITATION_EXPIRED'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'INTERNAL_ERROR'
  | 'NETWORK_ERROR'

export interface ApiFieldError {
  field: string
  message: string
}

export interface ApiErrorResponse {
  status: number
  code: ApiErrorCode
  message: string
  errors: ApiFieldError[]
  timestamp: string
}

/** Mengubah error apa pun dari axios menjadi format error standar backend. */
export function toApiError(error: unknown): ApiErrorResponse {
  if (isAxiosError(error)) {
    const data = error.response?.data as Partial<ApiErrorResponse> | undefined
    if (data && typeof data.code === 'string') {
      return {
        status: data.status ?? error.response?.status ?? 0,
        code: data.code,
        message: data.message ?? 'Terjadi kesalahan',
        errors: data.errors ?? [],
        timestamp: data.timestamp ?? new Date().toISOString(),
      }
    }
    if (!error.response) {
      return networkError()
    }
  }
  return {
    status: 0,
    code: 'INTERNAL_ERROR',
    message: 'Terjadi kesalahan, coba lagi beberapa saat.',
    errors: [],
    timestamp: new Date().toISOString(),
  }
}

/** Ubah daftar error per field menjadi objek { field: pesan } untuk vee-validate setErrors(). */
export function fieldErrorsToRecord<K extends string = string>(errors: ApiFieldError[]): Partial<Record<K, string>> {
  const result: Partial<Record<K, string>> = {}
  for (const e of errors) {
    const key = e.field as K
    if (result[key] === undefined) result[key] = e.message
  }
  return result
}

function networkError(): ApiErrorResponse {
  return {
    status: 0,
    code: 'NETWORK_ERROR',
    message: 'Tidak dapat terhubung ke server. Periksa koneksi internet kamu.',
    errors: [],
    timestamp: new Date().toISOString(),
  }
}
