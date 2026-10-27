// filepath: /frontend/src/modules/subscription/stores/useFeatureStore.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { subscriptionService } from '../services/subscription.service'
import type { FeatureAccessResponse } from '../types/subscription.types'

/** Akses fitur & sisa kuota. Selalu ambil ulang saat dibutuhkan karena kuota berubah dari Telegram. */
export const useFeatureStore = defineStore('feature', () => {
  const features = ref<FeatureAccessResponse | null>(null)
  const loading = ref(false)

  async function refresh(): Promise<void> {
    loading.value = true
    try {
      features.value = await subscriptionService.getFeatures()
    } catch {
      // Panduan tetap tampil tanpa info kuota
      features.value = null
    } finally {
      loading.value = false
    }
  }

  function reset(): void {
    features.value = null
  }

  return { features, loading, refresh, reset }
})
