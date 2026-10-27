<!-- filepath: /frontend/src/modules/transaction/views/TransactionsView.vue -->
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { toApiError } from '@/shared/api/errors'
import { formatMonthYear, monthRange, todayIn } from '@/shared/utils/date'
import { useAuthStore } from '@/modules/auth'
import { useCategoryStore, type TransactionType } from '@/modules/category'
import TransactionList from '../components/TransactionList.vue'
import { transactionService } from '../services/transaction.service'
import { useTransactionStore } from '../stores/useTransactionStore'
import type { TransactionResponse } from '../types/transaction.types'

const PAGE_SIZE = 30

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const categories = useCategoryStore()
const txStore = useTransactionStore()

const today = todayIn(auth.user?.timezone)

// Filter disimpan di URL agar bisa di-refresh / dibagikan: ?month=2026-10&type=EXPENSE&category=3
const month = computed(() => {
  const q = route.query.month
  return typeof q === 'string' && /^\d{4}-\d{2}$/.test(q) ? q : today.slice(0, 7)
})
const typeFilter = computed<TransactionType | undefined>(() =>
  route.query.type === 'EXPENSE' || route.query.type === 'INCOME' ? route.query.type : undefined,
)
const categoryFilter = computed(() => {
  const n = Number(route.query.category)
  return Number.isInteger(n) && n > 0 ? n : undefined
})

const items = ref<TransactionResponse[]>([])
const page = ref(0)
const totalPages = ref(0)
const totalItems = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)

const range = computed(() => monthRange(`${month.value}-01`))
const isCurrentMonth = computed(() => month.value === today.slice(0, 7))
const categoryOptions = computed(() =>
  typeFilter.value ? categories.ofType(typeFilter.value) : categories.items,
)

function setQuery(patch: Record<string, string | undefined>): void {
  const query = { ...route.query, ...patch }
  for (const k of Object.keys(query)) if (query[k] === undefined) delete query[k]
  router.replace({ query })
}

function shiftMonth(delta: number): void {
  setQuery({ month: monthRange(`${month.value}-01`, delta).from.slice(0, 7) })
}

function onTypeChange(t: TransactionType | undefined): void {
  const keepCategory = categoryFilter.value && t && categories.byId.get(categoryFilter.value)?.type === t
  setQuery({ type: t, category: keepCategory ? String(categoryFilter.value) : undefined })
}

async function load(reset: boolean): Promise<void> {
  loading.value = true
  error.value = null
  const nextPage = reset ? 0 : page.value + 1
  try {
    const res = await transactionService.list({
      type: typeFilter.value,
      categoryId: categoryFilter.value,
      from: range.value.from,
      to: range.value.to,
      page: nextPage,
      size: PAGE_SIZE,
    })
    items.value = reset ? res.items : [...items.value, ...res.items]
    page.value = res.page
    totalPages.value = res.totalPages
    totalItems.value = res.totalItems
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    loading.value = false
  }
}

watch([month, typeFilter, categoryFilter], () => load(true))
watch(() => txStore.version, () => load(true))

onMounted(() => {
  void categories.ensureLoaded()
  void load(true)
})
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-3xl font-extrabold tracking-tight">Transaksi</h1>
        <p class="mt-1 text-[15px] text-arang-600">{{ totalItems }} catatan di {{ formatMonthYear(month) }}</p>
      </div>
      <BaseButton @click="txStore.openCreate(typeFilter ?? 'EXPENSE')">+ Catat transaksi</BaseButton>
    </div>

    <!-- Filter -->
    <div class="flex flex-col gap-3 rounded-2xl bg-white p-3 shadow-sm sm:flex-row sm:items-center sm:justify-between">
      <div class="flex items-center gap-1">
        <button
          type="button"
          class="grid h-9 w-9 place-items-center rounded-lg text-arang-600 hover:bg-krem-100"
          aria-label="Bulan sebelumnya"
          @click="shiftMonth(-1)"
        >‹</button>
        <span class="min-w-36 text-center text-[15px] font-semibold" aria-live="polite">{{ formatMonthYear(month) }}</span>
        <button
          type="button"
          class="grid h-9 w-9 place-items-center rounded-lg text-arang-600 hover:bg-krem-100 disabled:opacity-30"
          aria-label="Bulan berikutnya"
          :disabled="isCurrentMonth"
          @click="shiftMonth(1)"
        >›</button>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <div role="radiogroup" aria-label="Jenis" class="flex rounded-xl bg-krem-100 p-1">
          <button
            v-for="opt in [{ v: undefined, label: 'Semua' }, { v: 'EXPENSE', label: 'Pengeluaran' }, { v: 'INCOME', label: 'Pemasukan' }] as const"
            :key="opt.label"
            type="button"
            role="radio"
            :aria-checked="typeFilter === opt.v"
            class="rounded-lg px-3 py-1.5 text-sm font-semibold transition"
            :class="typeFilter === opt.v ? 'bg-white shadow-sm' : 'text-arang-600 hover:text-arang-900'"
            @click="onTypeChange(opt.v)"
          >
            {{ opt.label }}
          </button>
        </div>
        <label class="sr-only" for="category-filter">Kategori</label>
        <select
          id="category-filter"
          class="rounded-xl border border-krem-300 bg-white px-3 py-2 text-sm"
          :value="categoryFilter ?? ''"
          @change="setQuery({ category: ($event.target as HTMLSelectElement).value || undefined })"
        >
          <option value="">Semua kategori</option>
          <option v-for="c in categoryOptions" :key="c.id" :value="c.id">{{ c.icon }} {{ c.name }}</option>
        </select>
      </div>
    </div>

    <FormAlert v-if="error">{{ error }}</FormAlert>

    <div v-if="loading && !items.length" class="flex flex-col gap-3" aria-hidden="true">
      <div v-for="i in 5" :key="i" class="h-16 animate-pulse rounded-2xl bg-white" />
    </div>

    <div
      v-else-if="!items.length && !error"
      class="grid place-items-center rounded-3xl border-2 border-dashed border-krem-300 px-6 py-16 text-center"
    >
      <p class="font-semibold text-arang-900">Belum ada transaksi di {{ formatMonthYear(month) }}</p>
      <p class="mt-1 max-w-sm text-sm text-arang-600">Catatan yang kamu tambahkan akan muncul di sini.</p>
      <BaseButton class="mt-5" @click="txStore.openCreate(typeFilter ?? 'EXPENSE')">+ Catat transaksi</BaseButton>
    </div>

    <template v-else>
      <TransactionList :items="items" @edit="txStore.openEdit" />
      <div v-if="page + 1 < totalPages" class="flex justify-center">
        <BaseButton variant="secondary" :loading="loading" @click="load(false)">Muat lebih banyak</BaseButton>
      </div>
    </template>
  </div>
</template>
