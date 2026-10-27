<!-- filepath: /frontend/src/modules/transaction/components/CategoryPicker.vue -->
<script setup lang="ts">
import { useId } from 'vue'
import type { CategoryResponse } from '@/modules/category'

defineProps<{ categories: CategoryResponse[]; error?: string }>()
const model = defineModel<number | null | undefined>()
const labelId = useId()
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <span :id="labelId" class="text-sm font-semibold text-arang-900">Kategori</span>
    <div role="radiogroup" :aria-labelledby="labelId" class="grid grid-cols-3 gap-2 sm:grid-cols-4">
      <button
        v-for="c in categories"
        :key="c.id"
        type="button"
        role="radio"
        :aria-checked="model === c.id"
        class="flex flex-col items-center gap-1.5 rounded-2xl border-2 px-1.5 py-2.5 text-center transition"
        :class="model === c.id ? 'border-current bg-white shadow-sm' : 'border-transparent bg-krem-50 hover:bg-krem-100'"
        :style="model === c.id ? { color: c.color } : undefined"
        @click="model = c.id"
      >
        <span
          class="grid h-10 w-10 place-items-center rounded-xl text-xl"
          :style="{ backgroundColor: `${c.color}1f` }"
          aria-hidden="true"
        >{{ c.icon }}</span>
        <span class="text-[11px] leading-tight font-semibold text-arang-900">{{ c.name }}</span>
      </button>
    </div>
    <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
  </div>
</template>
