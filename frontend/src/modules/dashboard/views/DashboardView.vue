<!-- filepath: /frontend/src/modules/dashboard/views/DashboardView.vue -->
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import FormAlert from '@/shared/components/FormAlert.vue'
import { toApiError } from '@/shared/api/errors'
import { formatRange, lastMonthsRange, monthRange, todayIn } from '@/shared/utils/date'
import { useAuthStore } from '@/modules/auth'
import { useTransactionStore } from '@/modules/transaction'
import { TelegramUsageGuide } from '@/modules/telegram'
import '../components/chartTheme'
import CategoryDonut from '../components/CategoryDonut.vue'
import DashboardSkeleton from '../components/DashboardSkeleton.vue'
import EmptyState from '../components/EmptyState.vue'
import PeriodFilter from '../components/PeriodFilter.vue'
import SummaryCards from '../components/SummaryCards.vue'
import TrendChart from '../components/TrendChart.vue'
import { dashboardService } from '../services/dashboard.service'
import type { DashboardResponse } from '../types/dashboard.types'
import type { PeriodPreset } from '../types/period.types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const txStore = useTransactionStore()

const today = todayIn(auth.user?.timezone)
const PRESETS: PeriodPreset[] = ['this_month', 'last_month', '3m', '6m', '12m', 'custom']
const ISO = /^\d{4}-\d{2}-\d{2}$/

// Periode disimpan di URL (?period=3m atau ?period=custom&from=..&to=..) agar bisa di-refresh/dibagikan
const preset = computed<PeriodPreset>(() => {
  const p = route.query.period
  if (p === 'custom' && !(isIso(route.query.from) && isIso(route.query.to))) return 'this_month'
  return PRESETS.includes(p as PeriodPreset) ? (p as PeriodPreset) : 'this_month'
})

const range = computed(() => {
  switch (preset.value) {
    case 'last_month':
      return monthRange(today, -1)
    case '3m':
      return lastMonthsRange(today, 3)
    case '6m':
      return lastMonthsRange(today, 6)
    case '12m':
      return lastMonthsRange(today, 12)
    case 'custom':
      return { from: route.query.from as string, to: route.query.to as string }
    default:
      return monthRange(today, 0)
  }
})

const data = ref<DashboardResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

const isEmpty = computed(
  () => !!data.value && data.value.summary.income === 0 && data.value.summary.spending === 0 && data.value.summary.saving === 0,
)
const firstName = computed(() => auth.user?.fullName?.split(' ')[0] ?? '')

function isIso(v: unknown): v is string {
  return typeof v === 'string' && ISO.test(v)
}

function onPeriodChange(p: PeriodPreset, from?: string, to?: string): void {
  router.replace({
    query: p === 'custom' ? { period: p, from, to } : p === 'this_month' ? {} : { period: p },
  })
}

let requestId = 0
async function load(): Promise<void> {
  const id = ++requestId
  loading.value = true
  error.value = null
  try {
    const res = await dashboardService.get(range.value.from, range.value.to)
    if (id === requestId) data.value = res // abaikan respons lama jika filter sudah berganti
  } catch (e) {
    if (id === requestId) error.value = toApiError(e).message
  } finally {
    if (id === requestId) loading.value = false
  }
}

watch(range, load)
watch(() => txStore.version, load)
onMounted(load)

// Transaksi bisa masuk dari Telegram saat tab ini tidak dilihat: muat ulang ketika user kembali ke tab
let lastLoadedAt = Date.now()
watch(loading, (v) => {
  if (!v) lastLoadedAt = Date.now()
})
function onVisible(): void {
  if (document.visibilityState === 'visible' && Date.now() - lastLoadedAt > 30_000) void load()
}
onMounted(() => document.addEventListener('visibilitychange', onVisible))
onBeforeUnmount(() => document.removeEventListener('visibilitychange', onVisible))
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-3xl font-extrabold tracking-tight">Halo, {{ firstName }} 👋</h1>
        <p class="mt-1 text-[15px] text-arang-600">Ringkasan keuanganmu {{ formatRange(range.from, range.to) }}.</p>
      </div>
    </div>

    <div
      v-if="auth.user && !auth.user.telegramLinked"
      class="flex flex-col gap-4 rounded-3xl border border-kunyit-400/40 bg-kunyit-100 p-5 sm:flex-row sm:items-center sm:justify-between"
    >
      <div>
        <p class="font-bold text-arang-900">Telegram belum terhubung</p>
        <p class="mt-1 text-sm text-arang-600">Hubungkan agar bisa mencatat pengeluaran & pemasukan langsung dari chat.</p>
      </div>
      <RouterLink
        :to="{ name: 'connect-telegram' }"
        class="inline-flex shrink-0 items-center justify-center rounded-xl bg-daun-600 px-5 py-2.5 text-[15px] font-semibold text-white hover:bg-daun-700"
      >
        Hubungkan sekarang
      </RouterLink>
    </div>
    <TelegramUsageGuide v-else-if="auth.user?.telegramLinked" compact />

    <PeriodFilter :preset="preset" :from="range.from" :to="range.to" :today="today" @change="onPeriodChange" />

    <FormAlert v-if="error">{{ error }}</FormAlert>

    <DashboardSkeleton v-if="loading && !data" />

    <template v-else-if="data">
      <EmptyState v-if="isEmpty" :period-label="formatRange(range.from, range.to)" @add="txStore.openCreate()" />

      <div v-else class="flex flex-col gap-4 transition-opacity" :class="loading ? 'opacity-60' : ''" :aria-busy="loading">
        <SummaryCards :summary="data.summary" :comparison="data.comparison" />
        <div class="grid gap-4 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
          <CategoryDonut
            :by-category="data.byCategory"
            :total-spending="data.summary.spending"
            :total-income="data.summary.income"
          />
          <TrendChart :trend="data.trend" :granularity="data.period.granularity" />
        </div>
      </div>
    </template>
  </div>
</template>
