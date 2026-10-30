<!-- filepath: /frontend/src/modules/admin/components/PlanBadge.vue -->
<script setup lang="ts">
import { formatInstantDate } from '@/shared/utils/date'
import type { UserPlan } from '../types/admin.types'

defineProps<{ plan: UserPlan; expiresAt?: string | null }>()
</script>

<template>
  <span class="inline-flex flex-wrap items-center gap-1.5">
    <span
      class="rounded-full px-2.5 py-0.5 text-xs font-bold"
      :class="plan === 'VIP' ? 'bg-kunyit-100 text-kunyit-600' : 'bg-krem-200 text-arang-600'"
    >
      {{ plan === 'VIP' ? '⭐ VIP' : 'FREE' }}
    </span>
    <span v-if="plan === 'VIP'" class="text-xs text-arang-600">
      {{ expiresAt ? `s.d. ${formatInstantDate(expiresAt)}` : 'tanpa batas' }}
    </span>
  </span>
</template>
