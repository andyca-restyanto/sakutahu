// filepath: /frontend/src/modules/subscription/services/subscription.service.ts
import { http } from '@/shared/api/http'
import type { FeatureAccessResponse } from '../types/subscription.types'

export const subscriptionService = {
  async getFeatures(): Promise<FeatureAccessResponse> {
    const { data } = await http.get<FeatureAccessResponse>('/me/features')
    return data
  },
}
