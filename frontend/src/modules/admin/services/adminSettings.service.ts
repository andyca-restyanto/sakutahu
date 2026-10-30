// filepath: /frontend/src/modules/admin/services/adminSettings.service.ts
import { adminHttp } from './adminHttp'
import type { ReceiptScanSettingsResponse, UpdateReceiptScanSettingsRequest } from '../types/admin.types'

export const adminSettingsService = {
  async getReceiptScan(): Promise<ReceiptScanSettingsResponse> {
    const { data } = await adminHttp.get<ReceiptScanSettingsResponse>('/settings/receipt-scan')
    return data
  },

  async updateReceiptScan(payload: UpdateReceiptScanSettingsRequest): Promise<ReceiptScanSettingsResponse> {
    const { data } = await adminHttp.put<ReceiptScanSettingsResponse>('/settings/receipt-scan', payload)
    return data
  },
}
