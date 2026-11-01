<!-- filepath: /frontend/src/modules/report/views/JournalReportView.vue -->
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FormAlert from '@/shared/components/FormAlert.vue'
import { toApiError } from '@/shared/api/errors'
import { formatRange, lastMonthsRange, monthRange, todayIn } from '@/shared/utils/date'
import { useAuthStore } from '@/modules/auth'
import { useCategoryStore, type TransactionType } from '@/modules/category'
import { PeriodFilter, type PeriodPreset } from '@/modules/dashboard'
import { useTransactionStore } from '@/modules/transaction'
import ExportExcelButton from '../components/ExportExcelButton.vue'
import JournalSummaryCards from '../components/JournalSummaryCards.vue'
import JournalTable from '../components/JournalTable.vue'
import { reportService } from '../services/report.service'
import type { JournalQuery, JournalReportResponse } from '../types/report.types'

const PAGE_SIZE = 100
const PRESETS: PeriodPreset[] = ['this_month', 'last_month', '3m', '6m', '12m', 'custom']
const ISO = /^\d{4}-\d{2}-\d{2}$/

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const categories = useCategoryStore()
const txStore = useTransactionStore()
const today = todayIn(auth.user?.timezone)

// Semua filter disimpan di URL agar bisa di-refresh/dibagikan
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
const type = computed<TransactionType | undefined>(() =>
  route.query.type === 'INCOME' || route.query.type === 'EXPENSE' ? route.query.type : undefined,
)
const categoryId = computed<number | undefined>(() => {
  const n = Number(route.query.categoryId)
  return Number.isInteger(n) && n > 0 ? n : undefined
})
const page = computed(() => Math.max(0, Number(route.query.page) || 0))

const query = computed<JournalQuery>(() => ({
  from: range.value.from,
  to: range.value.to,
  type: type.value,
  categoryId: categoryId.value,
  page: page.value,
  size: PAGE_SIZE,
}))

const categoryOptions = computed(() =>
  type.value ? categories.ofType(type.value) : [...categories.ofType('INCOME'), ...categories.ofType('EXPENSE')],
)

const data = ref<JournalReportResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const exportInfo = ref<{ text: string; ok: boolean } | null>(null)

function isIso(v: unknown): v is string {
  return typeof v === 'string' && ISO.test(v)
}

function setQuery(patch: Record<string, string | undefined>): void {
  const next: Record<string, string | undefined> = { ...(route.query as Record<string, string>), ...patch }
  for (const k of Object.keys(next)) if (!next[k]) delete next[k]
  router.replace({ query: next })
}

function onPeriodChange(p: PeriodPreset, from?: string, to?: string): void {
  setQuery({
    period: p === 'this_month' ? undefined : p,
    from: p === 'custom' ? from : undefined,
    to: p === 'custom' ? to : undefined,
    page: undefined,
  })
}

function onTypeChange(value: string): void {
  // Kategori lama mungkin tidak cocok dengan jenis baru
  const keepCategory = categoryId.value && categoryOptions.value.some((c) => c.id === categoryId.value && c.type === value)
  setQuery({ type: value || undefined, categoryId: keepCategory ? String(categoryId.value) : undefined, page: undefined })
}

function onCategoryChange(value: string): void {
  setQuery({ categoryId: value || undefined, page: undefined })
}

function goTo(p: number): void {
  setQuery({ page: p > 0 ? String(p) : undefined })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

let requestId = 0
async function load(): Promise<void> {
  const id = ++requestId
  loading.value = true
  error.value = null
  try {
    const res = await reportService.getJournal(query.value)
    if (id === requestId) data.value = res
  } catch (e) {
    if (id === requestId) error.value = toApiError(e).message
  } finally {
    if (id === requestId) loading.value = false
  }
}

onMounted(() => {
  categories.ensureLoaded()
  load()
})
watch(query, load, { deep: true })
// Transaksi baru dari tombol "+ Catat" langsung terlihat di laporan
watch(() => txStore.version, load)

const entries = computed(() => data.value?.entries)
const isEmpty = computed(() => !!data.value && data.value.summary.entryCount === 0)

const selectClass =
  'rounded-xl border border-krem-300 bg-white px-3 py-2.5 text-[15px] focus:border-daun-500 focus:ring-4 focus:ring-daun-100 focus:outline-none'
</script>

<template>
  <div class="flex flex-col gap-6">
    <header class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-extrabold tracking-tight text-arang-900">Laporan</h1>
        <p class="text-sm text-arang-600">Jurnal buku kas · {{ formatRange(range.from, range.to) }}</p>
      </div>
      <ExportExcelButton
        :query="query"
        :disabled="loading || isEmpty"
        @done="(name) => (exportInfo = { text: `${name} berhasil diunduh.`, ok: true })"
        @failed="(msg) => (exportInfo = { text: msg, ok: false })"
      />
    </header>

    <div class="flex flex-col gap-3">
      <PeriodFilter :preset="preset" :from="range.from" :to="range.to" :today="today" @change="onPeriodChange" />
      <div class="grid grid-cols-2 gap-2 sm:flex">
        <select :value="type ?? ''" :class="selectClass" aria-label="Filter jenis" @change="onTypeChange(($event.target as HTMLSelectElement).value)">
          <option value="">Semua jenis</option>
          <option value="INCOME">Masuk</option>
          <option value="EXPENSE">Keluar</option>
        </select>
        <select
          :value="categoryId ?? ''"
          :class="selectClass"
          aria-label="Filter kategori"
          @change="onCategoryChange(($event.target as HTMLSelectElement).value)"
        >
          <option value="">Semua kategori</option>
          <option v-for="c in categoryOptions" :key="c.id" :value="c.id">{{ c.icon }} {{ c.name }}</option>
        </select>
      </div>
    </div>

    <FormAlert v-if="exportInfo" :variant="exportInfo.ok ? 'success' : 'error'">{{ exportInfo.text }}</FormAlert>
    <FormAlert v-if="error">{{ error }}</FormAlert>

    <!-- Skeleton saat pertama memuat -->
    <div v-if="loading && !data" class="flex flex-col gap-3" aria-busy="true" aria-label="Memuat laporan">
      <div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <div v-for="i in 4" :key="i" class="h-20 animate-pulse rounded-2xl bg-white/70" />
      </div>
      <div class="h-72 animate-pulse rounded-2xl bg-white/70" />
    </div>

    <template v-else-if="data">
      <JournalSummaryCards :summary="data.summary" :filtered="data.filtered" />

      <FormAlert v-if="data.filtered" variant="info">
        Kolom saldo disembunyikan karena laporan difilter. Hapus filter jenis &amp; kategori untuk melihat saldo berjalan.
      </FormAlert>

      <div v-if="isEmpty" class="rounded-3xl bg-white p-10 text-center shadow-sm">
        <p class="text-3xl" aria-hidden="true">📒</p>
        <p class="mt-2 font-semibold text-arang-900">Belum ada transaksi di periode ini</p>
        <p class="mt-1 text-sm text-arang-600">Ubah periode atau filter, atau catat transaksi baru.</p>
      </div>

      <template v-else-if="entries">
        <div :class="loading ? 'opacity-60 transition-opacity' : ''">
          <JournalTable
            :entries="entries.items"
            :summary="data.summary"
            :filtered="data.filtered"
            :from="data.from"
            :first-page="entries.page === 0"
            :last-page="entries.page + 1 >= entries.totalPages"
          />
        </div>
        <nav v-if="entries.totalPages > 1" class="flex flex-wrap items-center justify-between gap-3 text-sm text-arang-600" aria-label="Halaman laporan">
          <span>
            {{ entries.page * entries.size + 1 }}–{{ Math.min((entries.page + 1) * entries.size, entries.totalItems) }}
            dari {{ entries.totalItems }} transaksi
          </span>
          <div class="flex gap-2">
            <button
              type="button"
              class="rounded-lg border border-krem-300 bg-white px-3 py-1.5 font-semibold hover:bg-krem-50 disabled:opacity-40"
              :disabled="entries.page === 0"
              @click="goTo(entries.page - 1)"
            >
              ‹ Sebelumnya
            </button>
            <button
              type="button"
              class="rounded-lg border border-krem-300 bg-white px-3 py-1.5 font-semibold hover:bg-krem-50 disabled:opacity-40"
              :disabled="entries.page + 1 >= entries.totalPages"
              @click="goTo(entries.page + 1)"
            >
              Berikutnya ›
            </button>
          </div>
        </nav>
      </template>
    </template>
  </div>
</template>
