<!-- filepath: /frontend/src/modules/report/components/JournalTable.vue -->
<script setup lang="ts">
import { computed } from 'vue'
import { formatRupiah } from '@/shared/utils/currency'
import { formatLongDate, formatShortDate } from '@/shared/utils/date'
import SourceBadge from './SourceBadge.vue'
import type { JournalEntryResponse, JournalSummaryResponse } from '../types/report.types'

const props = defineProps<{
  entries: JournalEntryResponse[]
  summary: JournalSummaryResponse
  filtered: boolean
  from: string
  /** Baris saldo awal hanya di halaman pertama; total hanya di halaman terakhir. */
  firstPage: boolean
  lastPage: boolean
}>()

/** Kelompokkan baris berurutan dengan tanggal yang sama (data sudah urut dari server). */
const groups = computed(() => {
  const out: { date: string; items: JournalEntryResponse[] }[] = []
  for (const e of props.entries) {
    const last = out[out.length - 1]
    if (last && last.date === e.occurredOn) last.items.push(e)
    else out.push({ date: e.occurredOn, items: [e] })
  }
  return out
})

function amount(v: number): string {
  return v ? formatRupiah(v) : ''
}
</script>

<template>
  <!-- Desktop -->
  <div class="hidden overflow-hidden rounded-2xl border border-krem-200 bg-white md:block">
    <table class="w-full text-left text-sm">
      <thead class="bg-krem-50 text-xs font-semibold tracking-wide text-arang-600 uppercase">
        <tr>
          <th scope="col" class="w-28 px-4 py-3">Tanggal</th>
          <th scope="col" class="px-4 py-3">Keterangan</th>
          <th scope="col" class="px-4 py-3">Kategori</th>
          <th scope="col" class="px-4 py-3 text-right">Masuk</th>
          <th scope="col" class="px-4 py-3 text-right">Keluar</th>
          <th v-if="!filtered" scope="col" class="px-4 py-3 text-right">Saldo</th>
        </tr>
      </thead>
      <tbody class="tabular-nums">
        <tr v-if="!filtered && firstPage" class="border-t border-krem-200 bg-krem-50/60 font-semibold">
          <td class="px-4 py-2.5 text-arang-600">{{ formatShortDate(from) }}</td>
          <td class="px-4 py-2.5" colspan="4">Saldo awal</td>
          <td class="px-4 py-2.5 text-right">{{ formatRupiah(summary.openingBalance) }}</td>
        </tr>
        <template v-for="g in groups" :key="g.date">
          <tr v-for="(e, i) in g.items" :key="e.id" class="border-t border-krem-200 align-top">
            <td class="px-4 py-2.5 whitespace-nowrap text-arang-600">
              <time v-if="i === 0" :datetime="g.date" :title="formatLongDate(g.date)">{{ formatShortDate(g.date) }}</time>
            </td>
            <td class="max-w-xs px-4 py-2.5">
              <span class="block truncate text-arang-900" :title="e.description">{{ e.description }}</span>
              <SourceBadge :entry="e" />
            </td>
            <td class="px-4 py-2.5 whitespace-nowrap">
              <span aria-hidden="true">{{ e.category.icon }}</span> {{ e.category.name }}
            </td>
            <td class="px-4 py-2.5 text-right whitespace-nowrap text-daun-700">{{ amount(e.income) }}</td>
            <td class="px-4 py-2.5 text-right whitespace-nowrap text-kunyit-600">{{ amount(e.expense) }}</td>
            <td v-if="!filtered" class="px-4 py-2.5 text-right whitespace-nowrap font-semibold">
              {{ formatRupiah(e.balance) }}
            </td>
          </tr>
        </template>
      </tbody>
      <tfoot v-if="lastPage" class="border-t-2 border-krem-300 font-bold tabular-nums">
        <tr>
          <td class="px-4 py-3" colspan="3">{{ filtered ? 'Total' : 'Total periode · saldo akhir' }}</td>
          <td class="px-4 py-3 text-right whitespace-nowrap text-daun-700">{{ formatRupiah(summary.totalIncome) }}</td>
          <td class="px-4 py-3 text-right whitespace-nowrap text-kunyit-600">
            {{ formatRupiah(summary.totalExpense + summary.totalSaving) }}
          </td>
          <td v-if="!filtered" class="px-4 py-3 text-right whitespace-nowrap">{{ formatRupiah(summary.closingBalance) }}</td>
        </tr>
      </tfoot>
    </table>
  </div>

  <!-- Mobile: kartu per tanggal -->
  <div class="flex flex-col gap-4 md:hidden">
    <div v-if="!filtered && firstPage" class="flex items-center justify-between rounded-2xl bg-krem-50 px-4 py-3 text-sm font-semibold">
      <span>Saldo awal</span>
      <span class="tabular-nums">{{ formatRupiah(summary.openingBalance) }}</span>
    </div>
    <section v-for="g in groups" :key="g.date">
      <h3 class="mb-1.5 px-1 text-xs font-bold tracking-wide text-arang-600 uppercase">{{ formatLongDate(g.date) }}</h3>
      <ul class="divide-y divide-krem-200 overflow-hidden rounded-2xl bg-white shadow-sm">
        <li v-for="e in g.items" :key="e.id" class="flex items-start justify-between gap-3 px-4 py-3">
          <div class="min-w-0">
            <p class="truncate font-semibold text-arang-900">{{ e.description }}</p>
            <p class="truncate text-xs text-arang-600">
              <span aria-hidden="true">{{ e.category.icon }}</span> {{ e.category.name }}
            </p>
            <SourceBadge :entry="e" />
          </div>
          <div class="shrink-0 text-right tabular-nums">
            <p class="font-bold" :class="e.income ? 'text-daun-700' : 'text-kunyit-600'">
              {{ e.income ? `+${formatRupiah(e.income)}` : `−${formatRupiah(e.expense)}` }}
            </p>
            <p v-if="!filtered" class="text-xs text-arang-600">Saldo {{ formatRupiah(e.balance) }}</p>
          </div>
        </li>
      </ul>
    </section>
    <div v-if="lastPage && !filtered" class="flex items-center justify-between rounded-2xl bg-daun-600 px-4 py-3 text-sm font-bold text-white">
      <span>Saldo akhir</span>
      <span class="tabular-nums">{{ formatRupiah(summary.closingBalance) }}</span>
    </div>
  </div>
</template>
