<!-- filepath: /frontend/src/modules/report/components/JournalSummaryCards.vue -->
<script setup lang="ts">
import { formatRupiah } from '@/shared/utils/currency'
import type { JournalSummaryResponse } from '../types/report.types'

defineProps<{ summary: JournalSummaryResponse; filtered: boolean }>()
</script>

<template>
  <div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
    <div v-if="!filtered" class="rounded-2xl bg-white p-4 shadow-sm">
      <p class="text-sm text-arang-600">Saldo awal</p>
      <p class="mt-1 text-lg font-extrabold tabular-nums sm:text-xl">{{ formatRupiah(summary.openingBalance) }}</p>
    </div>
    <div class="rounded-2xl bg-white p-4 shadow-sm">
      <p class="text-sm text-arang-600">Total masuk</p>
      <p class="mt-1 text-lg font-extrabold text-daun-700 tabular-nums sm:text-xl">{{ formatRupiah(summary.totalIncome) }}</p>
    </div>
    <div class="rounded-2xl bg-white p-4 shadow-sm">
      <p class="text-sm text-arang-600">Total keluar</p>
      <p class="mt-1 text-lg font-extrabold text-kunyit-600 tabular-nums sm:text-xl">
        {{ formatRupiah(summary.totalExpense + summary.totalSaving) }}
      </p>
      <p v-if="summary.totalSaving > 0" class="mt-0.5 text-xs text-arang-600">
        termasuk tabungan {{ formatRupiah(summary.totalSaving) }}
      </p>
    </div>
    <div v-if="!filtered" class="rounded-2xl bg-daun-600 p-4 text-white shadow-sm">
      <p class="text-sm text-daun-100">Saldo akhir</p>
      <p class="mt-1 text-lg font-extrabold tabular-nums sm:text-xl">{{ formatRupiah(summary.closingBalance) }}</p>
    </div>
    <div v-else class="rounded-2xl bg-white p-4 shadow-sm">
      <p class="text-sm text-arang-600">Jumlah transaksi</p>
      <p class="mt-1 text-lg font-extrabold tabular-nums sm:text-xl">{{ summary.entryCount.toLocaleString('id-ID') }}</p>
    </div>
  </div>
</template>
