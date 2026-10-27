<!-- filepath: /frontend/src/shared/components/BrandLogo.vue -->
<script setup lang="ts">
withDefaults(defineProps<{ light?: boolean }>(), { light: false })
</script>

<template>
  <div class="flex items-center gap-2.5">
    <!-- Ikon: kotak tahu dengan tanda centang -->
    <span
      class="grid h-9 w-9 place-items-center rounded-xl"
      :class="light ? 'bg-white/15 text-white' : 'bg-daun-600 text-white'"
      aria-hidden="true"
    >
      <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
        <rect x="3.5" y="6" width="17" height="13" rx="3" />
        <path d="M8 12.5l2.5 2.5L16 9.5" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
    </span>
    <span class="text-lg font-extrabold tracking-tight" :class="light ? 'text-white' : 'text-arang-900'">
      Saku<span :class="light ? 'text-kunyit-400' : 'text-daun-600'">Tahu</span>
    </span>
  </div>
</template>
