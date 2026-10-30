<!-- filepath: /frontend/src/modules/admin/components/AuditHistory.vue -->
<script setup lang="ts">
import { formatDateTime, formatInstantDate } from '@/shared/utils/date'
import type { AdminAuditEntryResponse } from '../types/admin.types'

defineProps<{ entries: AdminAuditEntryResponse[] }>()

function describe(e: AdminAuditEntryResponse): string {
  const d = e.detail ?? {}
  switch (e.action) {
    case 'USER_PLAN_CHANGED': {
      const until = d.toPlan === 'VIP' ? (d.toExpiresAt ? ` s.d. ${formatInstantDate(d.toExpiresAt)}` : ' tanpa batas') : ''
      return `Plan diubah ${d.fromPlan ?? '?'} → ${d.toPlan ?? '?'}${until}`
    }
    case 'USER_BLOCKED':
      return `Diblokir: ${d.reason ?? '-'}`
    case 'USER_UNBLOCKED':
      return 'Blokir dibuka'
    default:
      return e.action
  }
}
</script>

<template>
  <ol v-if="entries.length" class="flex flex-col gap-3">
    <li v-for="e in entries" :key="e.id" class="border-l-2 border-krem-300 pl-3">
      <p class="text-sm font-semibold text-arang-900">{{ describe(e) }}</p>
      <p class="text-xs text-arang-600">{{ e.adminName ?? 'Admin terhapus' }} · {{ formatDateTime(e.createdAt) }}</p>
    </li>
  </ol>
  <p v-else class="text-sm text-arang-600">Belum ada perubahan oleh admin.</p>
</template>
