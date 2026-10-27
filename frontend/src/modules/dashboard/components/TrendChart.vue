<!-- filepath: /frontend/src/modules/dashboard/components/TrendChart.vue -->
<script setup lang="ts">
import { computed, ref } from 'vue'
import { Bar, Line } from 'vue-chartjs'
import type { ChartData, ChartOptions, TooltipItem } from 'chart.js'
import { formatRupiah, formatRupiahCompact } from '@/shared/utils/currency'
import { formatMonthYear, formatShortDate, formatShortMonth } from '@/shared/utils/date'
import type { Granularity, TrendPoint } from '../types/dashboard.types'
import { GRID, SERIES, tooltipStyle } from './chartTheme'

const props = defineProps<{ trend: TrendPoint[]; granularity: Granularity }>()
const showTable = ref(false)

const isDaily = computed(() => props.granularity === 'DAY')
const labels = computed(() =>
  props.trend.map((p) => (isDaily.value ? formatShortDate(p.bucket) : formatShortMonth(p.bucket))),
)
const fullLabel = (bucket: string) => (isDaily.value ? formatShortDate(bucket) : formatMonthYear(bucket))

const yAxis = {
  beginAtZero: true,
  border: { display: false },
  grid: { color: GRID },
  ticks: { callback: (v: string | number) => formatRupiahCompact(Number(v)), maxTicksLimit: 5 },
}
const xAxis = { grid: { display: false }, border: { color: GRID }, ticks: { maxRotation: 0, autoSkipPadding: 12 } }

/** Harian: satu seri (pengeluaran) sebagai bar; judul sudah menamai seri, jadi tanpa legenda. */
const barData = computed<ChartData<'bar'>>(() => ({
  labels: labels.value,
  datasets: [
    {
      label: 'Pengeluaran',
      data: props.trend.map((p) => p.spending),
      backgroundColor: SERIES.spending,
      borderRadius: { topLeft: 4, topRight: 4 },
      borderSkipped: 'bottom',
      maxBarThickness: 18,
      categoryPercentage: 0.8,
      barPercentage: 0.9,
    },
  ],
}))

const barOptions: ChartOptions<'bar'> = {
  responsive: true,
  maintainAspectRatio: false,
  interaction: { mode: 'index', intersect: false },
  scales: { y: yAxis, x: xAxis },
  plugins: {
    legend: { display: false },
    tooltip: {
      ...tooltipStyle,
      callbacks: {
        title: (items: TooltipItem<'bar'>[]) => fullLabel(props.trend[items[0].dataIndex].bucket),
        label: (ctx: TooltipItem<'bar'>) => ` ${formatRupiah(Number(ctx.raw))}`,
      },
    },
  },
}

/** Bulanan: tiga seri garis pada satu sumbu (semuanya Rupiah), legenda selalu tampil. */
const lineData = computed<ChartData<'line'>>(() => ({
  labels: labels.value,
  datasets: (
    [
      ['Pemasukan', 'income', SERIES.income],
      ['Pengeluaran', 'spending', SERIES.spending],
      ['Ditabung', 'saving', SERIES.saving],
    ] as const
  ).map(([label, key, color]) => ({
    label,
    data: props.trend.map((p) => p[key]),
    borderColor: color,
    backgroundColor: color,
    borderWidth: 2,
    pointRadius: 4,
    pointHoverRadius: 6,
    pointBorderColor: '#ffffff',
    pointBorderWidth: 2,
    tension: 0.25,
  })),
}))

const lineOptions: ChartOptions<'line'> = {
  responsive: true,
  maintainAspectRatio: false,
  interaction: { mode: 'index', intersect: false },
  scales: { y: yAxis, x: xAxis },
  plugins: {
    legend: {
      position: 'top',
      align: 'end',
      labels: { usePointStyle: true, pointStyle: 'circle', boxWidth: 8, boxHeight: 8, color: '#2b2a26' },
    },
    tooltip: {
      ...tooltipStyle,
      callbacks: {
        title: (items: TooltipItem<'line'>[]) => fullLabel(props.trend[items[0].dataIndex].bucket),
        label: (ctx: TooltipItem<'line'>) => ` ${ctx.dataset.label}: ${formatRupiah(Number(ctx.raw))}`,
      },
    },
  },
}
</script>

<template>
  <section class="min-w-0 rounded-3xl bg-white p-5 shadow-sm sm:p-6" aria-labelledby="trend-title">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 id="trend-title" class="text-lg font-bold">{{ isDaily ? 'Pengeluaran harian' : 'Tren bulanan' }}</h2>
        <p class="text-sm text-arang-600">
          {{ isDaily ? 'Tidak termasuk tabungan & investasi' : 'Pemasukan, pengeluaran, dan tabungan per bulan' }}
        </p>
      </div>
      <button
        type="button"
        class="rounded-lg px-3 py-1.5 text-sm font-semibold text-daun-700 hover:bg-daun-50"
        :aria-pressed="showTable"
        @click="showTable = !showTable"
      >
        {{ showTable ? 'Lihat grafik' : 'Lihat tabel' }}
      </button>
    </div>

    <div v-if="!showTable" class="mt-4 h-72">
      <Bar v-if="isDaily" :data="barData" :options="barOptions" role="img" aria-label="Grafik batang pengeluaran harian" />
      <Line v-else :data="lineData" :options="lineOptions" role="img" aria-label="Grafik garis tren bulanan" />
    </div>

    <div v-else class="mt-4 max-h-72 overflow-auto">
      <table class="w-full text-sm">
        <thead class="sticky top-0 bg-white text-left text-xs text-arang-600">
          <tr>
            <th class="py-2 font-semibold">{{ isDaily ? 'Tanggal' : 'Bulan' }}</th>
            <th class="py-2 text-right font-semibold">Pemasukan</th>
            <th class="py-2 text-right font-semibold">Pengeluaran</th>
            <th class="py-2 text-right font-semibold">Ditabung</th>
          </tr>
        </thead>
        <tbody class="tabular-nums">
          <tr v-for="p in trend" :key="p.bucket" class="border-t border-krem-100">
            <td class="py-1.5">{{ fullLabel(p.bucket) }}</td>
            <td class="py-1.5 text-right">{{ formatRupiah(p.income) }}</td>
            <td class="py-1.5 text-right">{{ formatRupiah(p.spending) }}</td>
            <td class="py-1.5 text-right">{{ formatRupiah(p.saving) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
