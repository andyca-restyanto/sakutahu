<!-- filepath: /frontend/src/shared/components/BaseButton.vue -->
<script setup lang="ts">
withDefaults(
  defineProps<{
    type?: 'button' | 'submit'
    variant?: 'primary' | 'secondary' | 'ghost'
    loading?: boolean
    disabled?: boolean
    block?: boolean
  }>(),
  { type: 'button', variant: 'primary' },
)

const variants = {
  primary: 'bg-daun-600 text-white hover:bg-daun-700 shadow-sm shadow-daun-900/10',
  secondary: 'bg-white text-arang-900 border border-krem-300 hover:bg-krem-50',
  ghost: 'text-daun-700 hover:bg-daun-50',
} as const
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :aria-busy="loading"
    class="inline-flex items-center justify-center gap-2 rounded-xl px-5 py-3 text-[15px] font-semibold transition disabled:cursor-not-allowed disabled:opacity-60"
    :class="[variants[variant], block ? 'w-full' : '']"
  >
    <svg v-if="loading" class="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" class="opacity-25" />
      <path d="M22 12a10 10 0 0 0-10-10" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
    </svg>
    <slot />
  </button>
</template>
