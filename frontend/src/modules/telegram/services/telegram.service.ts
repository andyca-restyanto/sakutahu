// filepath: /frontend/src/modules/telegram/services/telegram.service.ts
import { http } from '@/shared/api/http'
import type { TelegramLinkResponse, TelegramStatusResponse } from '../types/telegram.types'

export const telegramService = {
  async createLinkToken(): Promise<TelegramLinkResponse> {
    const { data } = await http.post<TelegramLinkResponse>('/telegram/link-token')
    return data
  },

  async getStatus(): Promise<TelegramStatusResponse> {
    const { data } = await http.get<TelegramStatusResponse>('/telegram/status')
    return data
  },

  async unlink(): Promise<void> {
    await http.delete('/telegram/link')
  },
}
