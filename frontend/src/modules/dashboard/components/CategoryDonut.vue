<!-- filepath: /frontend/src/modules/dashboard/components/CategoryDonut.vue -->
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Doughnut } from 'vue-chartjs'
import type { ActiveElement, Chart, ChartData, ChartEvent, ChartOptions } from 'chart.js'
import { formatRupiah } from '@/shared/utils/currency'
import type { CategoryBreakdownItem, DashboardByCategory } from '../types/dashboard.types'
import { OTHER_COLOR, SURFACE } from './chartTheme'

const props = defineProps<{ byCategory: DashboardByCategory; totalSpending: number; totalIncome: number }>()

const TOP_N = 5
const OTHER_CODE = 'OTHER_EXPENSE'

const mode = ref<'spending' | 'income'>('spending')
const items = computed(() => props.byCategory[mode.value])
const total = computed(() => (mode.value === 'spending' ? props.totalSpending : props.totalIncome))

interface Slice {
  label: string
  amount: number
  color: string
  categoryIds: number[]
}

/**
 * 5 kategori terbesar + satu potongan abu-abu untuk sisanya.
 * "Lain-lain" (OTHER_EXPENSE) selalu masuk potongan abu-abu, jadi tidak ada dua potongan abu-abu.
 */
const slices = computed<Slice[]>(() => {
  const named = items.value.filter((i) => i.code !== OTHER_CODE)
  const top = named.slice(0, TOP_N)
  const rest = items.value.filter((i) => !top.includes(i))
  const result: Slice[] = top.map((i) => ({ label: i.name, amount: i.amount, color: i.color, categoryIds: [i.categoryId] }))
  const restAmount = rest.reduce((sum, i) => sum + i.amount, 0)
  if (restAmount > 0) {
    result.push({
      label: mode.value === 'spending' ? 'Lain-lain' : 'Lainnya',
      amount: restAmount,
      color: OTHER_COLOR,
      categoryIds: rest.map((i) => i.categoryId),
    })
  }
  return result
})

/**
 * Detail potongan yang sedang disorot ditampilkan DI TENGAH donut (menggantikan "Total"),
 * bukan sebagai tooltip melayang, sehingga tidak pernah bertumpuk dengan label total.
 */
const activeIndex = ref<number | null>(null)
const active = computed(() => (activeIndex.value === null ? null : slices.value[activeIndex.value] ?? null))
const activePct = computed(() => {
  if (!active.value || !total.value) return null
  return `${((active.value.amount / total.value) * 100).toLocaleString('id-ID', { maximumFractionDigits: 1 })}%`
})

const chartRef = ref<{ chart?: Chart<'doughnut'> } | null>(null)

function highlight(index: number | null): void {
  activeIndex.value = index
  const chart = chartRef.value?.chart
  if (!chart) return
  chart.setActiveElements(index === null ? [] : [{ datasetIndex: 0, index }])
  chart.update('none')
}

function sliceIndexOf(item: CategoryBreakdownItem): number | null {
  const idx = slices.value.findIndex((s) => s.categoryIds.includes(item.categoryId))
  return idx < 0 ? null : idx
}

watch(mode, () => highlight(null))

const chartData = computed<ChartData<'doughnut'>>(() => ({
  labels: slices.value.map((s) => s.label),
  datasets: [
    {
      data: slices.value.map((s) => s.amount),
      backgroundColor: slices.value.map((s) => s.color),
      borderColor: SURFACE,
      borderWidth: 2,
      hoverOffset: 6,
    },
  ],
}))

const options: ChartOptions<'doughnut'> = {
  responsive: true,
  maintainAspectRatio: false,
  cutout: '70%',
  layout: { padding: 6 }, // ruang untuk hoverOffset agar potongan tidak terpotong
  plugins: {
    legend: { display: false }, // daftar berlabel di samping berfungsi sebagai legenda + tabel
    tooltip: { enabled: false }, // detail tampil di tengah donut
  },
  onHover: (_event: ChartEvent, elements: ActiveElement[]) => {
    activeIndex.value = elements.length ? elements[0].index : null
  },
}

function pct(i: CategoryBreakdownItem): string {
  return `${i.percentage.toLocaleString('id-ID', { maximumFractionDigits: 1 })}%`
}
</script>

<template>
  <section class="@container min-w-0 rounded-3xl bg-white p-5 shadow-sm sm:p-6" aria-labelledby="donut-title">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h2 id="donut-title" class="text-lg font-bold">Per kategori</h2>
      <div role="radiogroup" aria-label="Jenis" class="flex rounded-xl bg-krem-100 p-1">
        <button
          v-for="opt in [{ v: 'spending', label: 'Pengeluaran' }, { v: 'income', label: 'Pemasukan' }] as const"
          :key="opt.v"
          type="button"
          role="radio"
          :aria-checked="mode === opt.v"
          class="rounded-lg px-3 py-1.5 text-sm font-semibold transition"
          :class="mode === opt.v ? 'bg-white shadow-sm' : 'text-arang-600 hover:text-arang-900'"
          @click="mode = opt.v"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <p v-if="!items.length" class="py-16 text-center text-sm text-arang-600">
      Belum ada {{ mode === 'spending' ? 'pengeluaran' : 'pemasukan' }} di periode ini.
    </p>

    <!--
      Container query: donut & daftar berdampingan hanya jika KARTU ini cukup lebar (≥ 36rem),
      bukan berdasarkan lebar layar. Di kolom dashboard yang sempit keduanya ditumpuk.
    -->
    <div v-else class="mt-5 grid items-center gap-6 @xl:grid-cols-[212px_minmax(0,1fr)]">
      <div class="relative mx-auto h-[212px] w-[212px] shrink-0" @mouseleave="highlight(null)">
        <Doughnut
          ref="chartRef"
          :data="chartData"
          :options="options"
          aria-label="Diagram donat per kategori; rincian ada di daftar"
          role="img"
        />
        <!-- Label tengah: total, atau detail potongan yang sedang disorot -->
        <div
          class="pointer-events-none absolute inset-0 flex flex-col items-center justify-center px-9 text-center"
          aria-live="polite"
        >
          <template v-if="active">
            <span class="flex max-w-full items-center gap-1.5 text-xs font-semibold text-arang-600">
              <span class="h-2 w-2 shrink-0 rounded-full" :style="{ backgroundColor: active.color }" aria-hidden="true" />
              <span class="line-clamp-2 leading-tight">{{ active.label }}</span>
            </span>
            <span class="mt-1 text-base leading-tight font-extrabold tabular-nums text-arang-900">
              {{ formatRupiah(active.amount) }}
            </span>
            <span class="text-xs font-medium tabular-nums text-arang-600">{{ activePct }}</span>
          </template>
          <template v-else>
            <span class="text-xs font-medium text-arang-600">Total</span>
            <span class="text-base leading-tight font-extrabold tabular-nums text-arang-900">{{ formatRupiah(total) }}</span>
          </template>
        </div>
      </div>

      <!-- Daftar lengkap: legenda sekaligus alternatif tabel untuk grafik. Hover baris = sorot potongan. -->
      <table class="w-full table-fixed text-sm" @mouseleave="highlight(null)">
        <caption class="sr-only">Rincian {{ mode === 'spending' ? 'pengeluaran' : 'pemasukan' }} per kategori</caption>
        <colgroup>
          <col />
          <col class="w-[7.5rem]" />
          <col class="w-14" />
        </colgroup>
        <thead class="sr-only">
          <tr><th>Kategori</th><th>Jumlah</th><th>Persentase</th></tr>
        </thead>
        <tbody>
          <tr
            v-for="i in items"
            :key="i.categoryId"
            class="border-b border-krem-100 transition-colors last:border-0"
            :class="active && active.categoryIds.includes(i.categoryId) ? 'bg-krem-50' : ''"
            @mouseenter="highlight(sliceIndexOf(i))"
          >
            <td class="py-2.5 pr-3 pl-1">
              <span class="flex min-w-0 items-center gap-2">
                <span class="h-2.5 w-2.5 shrink-0 rounded-full" :style="{ backgroundColor: i.color }" aria-hidden="true" />
                <span class="shrink-0" aria-hidden="true">{{ i.icon }}</span>
                <span class="min-w-0 truncate font-medium text-arang-900" :title="i.name">{{ i.name }}</span>
                <span class="shrink-0 text-xs text-arang-400">{{ i.count }}×</span>
              </span>
            </td>
            <td class="py-2.5 text-right font-semibold whitespace-nowrap tabular-nums text-arang-900">
              {{ formatRupiah(i.amount) }}
            </td>
            <td class="py-2.5 pr-1 text-right whitespace-nowrap tabular-nums text-arang-600">{{ pct(i) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
