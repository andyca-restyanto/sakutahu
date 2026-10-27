<!-- filepath: /frontend/src/modules/subscription/components/ReceiptScanInfo.vue -->
<script setup lang="ts">
/** Status fitur "foto struk" untuk panduan Telegram: tersedia (sisa kuota) atau terkunci (VIP). */
import { computed, onMounted } from 'vue'
import { useFeatureStore } from '../stores/useFeatureStore'

const store = useFeatureStore()
onMounted(() => store.refresh())

const access = computed(() => store.features?.receiptScan ?? null)
const locked = computed(() => access.value !== null && !access.value.available)
const remaining = computed(() => {
  const a = access.value
  if (!a || a.monthlyQuota === null) return null
  return Math.max(0, a.monthlyQuota - a.usedThisMonth)
})
</script>

<template>
  <div class="mt-5 rounded-2xl border border-krem-200 p-4" :class="locked ? 'bg-krem-100' : 'bg-white'">
    <div class="flex flex-wrap items-center justify-between gap-2">
      <h3 class="text-sm font-bold text-arang-900">📸 Catat dari foto struk</h3>
      <span
        v-if="access?.reason === 'VIP_ONLY'"
        class="rounded-full bg-kunyit-100 px-2.5 py-1 text-xs font-bold text-kunyit-600"
      >
        Fitur khusus VIP ⭐
      </span>
      <span
        v-else-if="access && access.monthlyQuota === null"
        class="rounded-full bg-daun-50 px-2.5 py-1 text-xs font-semibold text-daun-700"
      >
        Tanpa batas ⭐
      </span>
      <span
        v-else-if="access && remaining !== null"
        class="rounded-full px-2.5 py-1 text-xs font-semibold"
        :class="remaining > 0 ? 'bg-daun-50 text-daun-700' : 'bg-red-50 text-red-700'"
      >
        Sisa {{ remaining }} dari {{ access.monthlyQuota }} bulan ini
      </span>
    </div>
    <p class="mt-1 text-sm text-arang-600">
      Kirim foto struk ke bot. Total belanja dibaca otomatis dan dicatat ke 🛍️ Belanja Pribadi untuk hari ini —
      kategori bisa diubah lewat tombol di pesan bot. Tambahkan caption untuk catatan.
    </p>
    <p v-if="access?.reason === 'VIP_ONLY'" class="mt-2 text-xs font-medium text-arang-600">
      Akunmu masih Free. Tetap bisa mencatat dengan mengetik, mis. <code class="font-mono">belanja 125rb</code>.
    </p>
    <p v-else-if="access?.reason === 'QUOTA_EXCEEDED'" class="mt-2 text-xs font-medium text-red-700">
      Kuota bulan ini habis. Kuota terisi lagi tanggal 1.
    </p>
  </div>
</template>
