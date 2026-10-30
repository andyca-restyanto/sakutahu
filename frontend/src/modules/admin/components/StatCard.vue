<!-- filepath: /frontend/src/modules/admin/components/StatCard.vue -->
<script setup lang="ts">
defineProps<{ label: string; value: number | string; hint?: string; tone?: 'default' | 'good' | 'bad' | 'warn' }>()
</script>

<template>
  <div class="rounded-2xl border border-krem-200 bg-white p-4">
    <p class="text-sm text-arang-600">{{ label }}</p>
    <p
      class="mt-1 text-2xl font-extrabold tabular-nums"
      :class="{
        'text-arang-900': !tone || tone === 'default',
        'text-daun-700': tone === 'good',
        'text-red-700': tone === 'bad',
        'text-kunyit-600': tone === 'warn',
      }"
    >
      {{ value }}
    </p>
    <p v-if="hint" class="mt-0.5 text-xs text-arang-400">{{ hint }}</p>
  </div>
</template>
