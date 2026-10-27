<!-- filepath: /frontend/src/shared/components/FormAlert.vue -->
<script setup lang="ts">
withDefaults(defineProps<{ variant?: 'error' | 'warning' | 'success' | 'info' }>(), { variant: 'error' })

const styles = {
  error: 'border-red-200 bg-red-50 text-red-800',
  warning: 'border-kunyit-400/50 bg-kunyit-100 text-kunyit-600',
  success: 'border-daun-100 bg-daun-50 text-daun-700',
  info: 'border-krem-300 bg-krem-50 text-arang-600',
} as const
</script>

<template>
  <div
    :role="variant === 'error' ? 'alert' : 'status'"
    class="rounded-xl border px-4 py-3 text-sm leading-relaxed"
    :class="styles[variant]"
  >
    <slot />
  </div>
</template>
