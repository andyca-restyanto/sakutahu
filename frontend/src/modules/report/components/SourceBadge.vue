<!-- filepath: /frontend/src/modules/report/components/SourceBadge.vue -->
<script setup lang="ts">
import type { JournalEntryResponse } from '../types/report.types'

/** Gaya sama dengan badge di TransactionList. */
defineProps<{ entry: Pick<JournalEntryResponse, 'source' | 'receiptScanId'> }>()
</script>

<template>
  <span v-if="entry.source === 'TELEGRAM'" class="rounded bg-sky-50 px-1 text-xs font-medium text-sky-700">Telegram</span>
  <span v-if="entry.receiptScanId" class="ml-1 rounded bg-kunyit-100 px-1 text-xs font-medium text-kunyit-600">🧾 Struk</span>
</template>
