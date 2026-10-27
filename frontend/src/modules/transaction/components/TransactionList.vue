<!-- filepath: /frontend/src/modules/transaction/components/TransactionList.vue -->
<script setup lang="ts">
import { computed } from 'vue'
import { formatRupiah } from '@/shared/utils/currency'
import { formatLongDate } from '@/shared/utils/date'
import type { TransactionResponse } from '../types/transaction.types'

const props = defineProps<{ items: TransactionResponse[] }>()
const emit = defineEmits<{ edit: [tx: TransactionResponse] }>()

/** Dikelompokkan per tanggal (data dari API sudah urut terbaru dulu). */
const groups = computed(() => {
  const map = new Map<string, TransactionResponse[]>()
  for (const tx of props.items) {
    const list = map.get(tx.occurredOn) ?? []
    list.push(tx)
    map.set(tx.occurredOn, list)
  }
  return [...map.entries()].map(([date, items]) => ({
    date,
    items,
    net: items.reduce((sum, t) => sum + (t.type === 'INCOME' ? t.amount : -t.amount), 0),
  }))
})

function amountClass(tx: TransactionResponse): string {
  if (tx.type === 'INCOME') return 'text-daun-600'
  if (tx.category.kind === 'SAVING') return 'text-blue-700'
  return 'text-arang-900'
}
</script>

<template>
  <div class="flex flex-col gap-6">
    <section v-for="g in groups" :key="g.date" :aria-label="formatLongDate(g.date)">
      <header class="mb-2 flex items-baseline justify-between px-1">
        <h3 class="text-sm font-semibold text-arang-600">{{ formatLongDate(g.date) }}</h3>
        <span class="text-sm font-semibold" :class="g.net >= 0 ? 'text-daun-600' : 'text-arang-600'">
          {{ g.net >= 0 ? '+' : '−' }}{{ formatRupiah(Math.abs(g.net)) }}
        </span>
      </header>
      <ul class="divide-y divide-krem-100 overflow-hidden rounded-2xl bg-white shadow-sm">
        <li v-for="tx in g.items" :key="tx.id">
          <button
            type="button"
            class="flex w-full items-center gap-3 px-4 py-3 text-left transition hover:bg-krem-50"
            :aria-label="`Ubah ${tx.category.name} ${formatRupiah(tx.amount)}`"
            @click="emit('edit', tx)"
          >
            <span
              class="grid h-10 w-10 shrink-0 place-items-center rounded-xl text-lg"
              :style="{ backgroundColor: `${tx.category.color}1f` }"
              aria-hidden="true"
            >{{ tx.category.icon }}</span>
            <span class="min-w-0 flex-1">
              <span class="block truncate text-[15px] font-semibold text-arang-900">{{ tx.category.name }}</span>
              <span v-if="tx.note || tx.source === 'TELEGRAM'" class="block truncate text-sm text-arang-600">
                <span v-if="tx.source === 'TELEGRAM'" class="mr-1 rounded bg-sky-50 px-1 text-xs font-medium text-sky-700">Telegram</span>
                <span v-if="tx.receiptScanId" class="mr-1 rounded bg-kunyit-100 px-1 text-xs font-medium text-kunyit-600">🧾 Struk</span>
                {{ tx.note }}
              </span>
            </span>
            <span class="shrink-0 text-[15px] font-bold tabular-nums" :class="amountClass(tx)">
              {{ tx.type === 'INCOME' ? '+' : '−' }}{{ formatRupiah(tx.amount) }}
            </span>
          </button>
        </li>
      </ul>
    </section>
  </div>
</template>
