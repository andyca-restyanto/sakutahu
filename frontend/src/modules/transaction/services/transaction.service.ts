// filepath: /frontend/src/modules/transaction/services/transaction.service.ts
import { http } from '@/shared/api/http'
import type { PageResponse } from '@/shared/api/page'
import type { TransactionListQuery, TransactionRequest, TransactionResponse } from '../types/transaction.types'

export const transactionService = {
  async list(query: TransactionListQuery): Promise<PageResponse<TransactionResponse>> {
    const { data } = await http.get<PageResponse<TransactionResponse>>('/transactions', { params: query })
    return data
  },

  async get(id: string): Promise<TransactionResponse> {
    const { data } = await http.get<TransactionResponse>(`/transactions/${encodeURIComponent(id)}`)
    return data
  },

  async create(payload: TransactionRequest): Promise<TransactionResponse> {
    const { data } = await http.post<TransactionResponse>('/transactions', payload)
    return data
  },

  async update(id: string, payload: TransactionRequest): Promise<TransactionResponse> {
    const { data } = await http.put<TransactionResponse>(`/transactions/${encodeURIComponent(id)}`, payload)
    return data
  },

  async remove(id: string): Promise<void> {
    await http.delete(`/transactions/${encodeURIComponent(id)}`)
  },
}
