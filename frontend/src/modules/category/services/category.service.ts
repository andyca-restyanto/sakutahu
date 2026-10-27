// filepath: /frontend/src/modules/category/services/category.service.ts
import { http } from '@/shared/api/http'
import type { CategoryResponse, TransactionType } from '../types/category.types'

export const categoryService = {
  async list(type?: TransactionType): Promise<CategoryResponse[]> {
    const { data } = await http.get<CategoryResponse[]>('/categories', { params: type ? { type } : undefined })
    return data
  },
}
