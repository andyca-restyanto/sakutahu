// filepath: /frontend/src/shared/api/page.ts

/** Match: /backend/.../shared/web/PageResponse.java */
export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}
