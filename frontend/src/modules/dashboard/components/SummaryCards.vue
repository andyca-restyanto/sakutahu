<!-- filepath: /frontend/src/modules/dashboard/components/SummaryCards.vue -->
<script setup lang="ts">
import { computed } from 'vue'
import { formatRupiah } from '@/shared/utils/currency'
import type { DashboardComparison, DashboardSummary } from '../types/dashboard.types'
import { SERIES } from './chartTheme'

const props = defineProps<{ summary: DashboardSummary; comparison: DashboardComparison }>()

type Trend = { text: string; tone: 'good' | 'bad' | 'neutral'; arrow: '↑' | '↓' | '' }

/** Naik itu baik untuk pemasukan & tabungan, buruk untuk pengeluaran. Selalu ada teks, tidak hanya warna. */
function trend(change: number | null, upIsGood: boolean): Trend {
  if (change === null) return { text: 'Belum ada pembanding', tone: 'neutral', arrow: '' }
  if (change === 0) return { text: 'Sama dengan periode lalu', tone: 'neutral', arrow: '' }
  const up = change > 0
  const pct = Math.abs(change).toLocaleString('id-ID', { maximumFractionDigits: 1 })
  return {
    text: `${up ? 'Naik' : 'Turun'} ${pct}% dari periode lalu`,
    tone: up === upIsGood ? 'good' : 'bad',
    arrow: up ? '↑' : '↓',
  }
}

const cards = computed(() => [
  {
    label: 'Pemasukan',
    value: props.summary.income,
    dot: SERIES.income,
    trend: trend(props.comparison.income, true),
  },
  {
    label: 'Pengeluaran',
    value: props.summary.spending,
    dot: SERIES.spending,
    trend: trend(props.comparison.spending, false),
  },
  {
    label: 'Ditabung & investasi',
    value: props.summary.saving,
    dot: SERIES.saving,
    trend: trend(props.comparison.saving, true),
  },
])

const balanceNote = computed(() =>
  props.summary.savingRate === null
    ? 'Belum ada pemasukan di periode ini'
    : `${props.summary.savingRate.toLocaleString('id-ID', { maximumFractionDigits: 1 })}% pemasukan ditabung`,
)

const toneClass = { good: 'text-daun-700', bad: 'text-red-700', neutral: 'text-arang-600' } as const
</script>

<template>
  <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
    <article v-for="c in cards" :key="c.label" class="rounded-3xl bg-white p-5 shadow-sm">
      <h3 class="flex items-center gap-2 text-sm font-semibold text-arang-600">
        <span class="h-2.5 w-2.5 rounded-full" :style="{ backgroundColor: c.dot }" aria-hidden="true" />
        {{ c.label }}
      </h3>
      <p class="mt-2 text-2xl font-extrabold tracking-tight tabular-nums text-arang-900">{{ formatRupiah(c.value) }}</p>
      <p class="mt-1 text-xs font-medium" :class="toneClass[c.trend.tone]">
        <span v-if="c.trend.arrow" aria-hidden="true">{{ c.trend.arrow }} </span>{{ c.trend.text }}
      </p>
    </article>

    <article class="rounded-3xl bg-daun-600 p-5 text-white shadow-sm">
      <h3 class="text-sm font-semibold text-daun-100">Sisa uang</h3>
      <p class="mt-2 text-2xl font-extrabold tracking-tight tabular-nums">
        {{ summary.balance < 0 ? '−' : '' }}{{ formatRupiah(Math.abs(summary.balance)) }}
      </p>
      <p class="mt-1 text-xs font-medium text-daun-100">{{ balanceNote }}</p>
    </article>
  </div>
</template>
