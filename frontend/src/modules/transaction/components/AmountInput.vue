<!-- filepath: /frontend/src/modules/transaction/components/AmountInput.vue -->
<script setup lang="ts">
import { ref, useId, watch } from 'vue'
import { formatRupiahInput, parseRupiahInput } from '@/shared/utils/currency'

defineOptions({ inheritAttrs: false })
defineProps<{ label: string; error?: string }>()
const model = defineModel<number | null | undefined>()

const id = useId()
const text = ref(formatRupiahInput(model.value))

// Sinkron jika nilai diubah dari luar (mis. form di-reset untuk edit)
watch(model, (v) => {
  if (parseRupiahInput(text.value) !== (v ?? null)) text.value = formatRupiahInput(v)
})

function onInput(e: Event): void {
  const input = e.target as HTMLInputElement
  const value = parseRupiahInput(input.value)
  model.value = value
  text.value = formatRupiahInput(value)
  // Paksa DOM ikut ter-format walau string-nya sama (mis. user mengetik huruf)
  input.value = text.value
}
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <label :for="id" class="text-sm font-semibold text-arang-900">{{ label }}</label>
    <div
      class="flex items-baseline gap-2 rounded-2xl border bg-white px-4 py-3 transition focus-within:ring-4"
      :class="error ? 'border-red-400 focus-within:ring-red-100' : 'border-krem-300 focus-within:border-daun-500 focus-within:ring-daun-100'"
    >
      <span class="text-xl font-bold text-arang-400" aria-hidden="true">Rp</span>
      <input
        :id="id"
        v-bind="$attrs"
        :value="text"
        inputmode="numeric"
        autocomplete="off"
        placeholder="0"
        :aria-invalid="!!error"
        :aria-describedby="error ? `${id}-error` : undefined"
        class="w-full bg-transparent text-3xl font-extrabold tracking-tight text-arang-900 outline-none placeholder:text-krem-300"
        @input="onInput"
      />
    </div>
    <p v-if="error" :id="`${id}-error`" class="text-sm text-red-600">{{ error }}</p>
  </div>
</template>
