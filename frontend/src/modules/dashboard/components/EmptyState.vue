<!-- filepath: /frontend/src/modules/dashboard/components/EmptyState.vue -->
<script setup lang="ts">
import BaseButton from '@/shared/components/BaseButton.vue'

defineProps<{ periodLabel: string }>()
const emit = defineEmits<{ add: [] }>()
</script>

<template>
  <div class="grid place-items-center rounded-3xl border-2 border-dashed border-krem-300 bg-krem-50 px-6 py-16 text-center">
    <div class="grid h-14 w-14 place-items-center rounded-2xl bg-daun-50 text-2xl" aria-hidden="true">📊</div>
    <p class="mt-4 font-bold text-arang-900">Belum ada catatan untuk {{ periodLabel }}</p>
    <p class="mt-1 max-w-sm text-sm text-arang-600">
      Catat pemasukan atau pengeluaran pertamamu, grafik akan terisi otomatis.
    </p>
    <BaseButton class="mt-5" @click="emit('add')">+ Catat transaksi pertama</BaseButton>
  </div>
</template>
