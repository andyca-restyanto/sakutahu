<!-- filepath: /frontend/src/shared/components/BaseInput.vue -->
<script setup lang="ts">
import { computed, useId } from 'vue'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    label: string
    type?: string
    error?: string
    hint?: string
    placeholder?: string
    autocomplete?: string
  }>(),
  { type: 'text' },
)

const model = defineModel<string>()
const id = useId()
const describedBy = computed(() => (props.error ? `${id}-error` : props.hint ? `${id}-hint` : undefined))
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <label :for="id" class="text-sm font-semibold text-arang-900">{{ label }}</label>
    <div class="relative">
      <input
        :id="id"
        v-model="model"
        v-bind="$attrs"
        :type="type"
        :placeholder="placeholder"
        :autocomplete="autocomplete"
        :aria-invalid="!!error"
        :aria-describedby="describedBy"
        class="w-full rounded-xl border bg-white px-4 py-3 text-[15px] text-arang-900 placeholder:text-arang-400 transition outline-none focus:ring-4"
        :class="
          error
            ? 'border-red-400 focus:border-red-500 focus:ring-red-100'
            : 'border-krem-300 focus:border-daun-500 focus:ring-daun-100'
        "
      />
      <div class="absolute inset-y-0 right-0 flex items-center pr-2">
        <slot name="trailing" />
      </div>
    </div>
    <p v-if="error" :id="`${id}-error`" class="text-sm text-red-600">{{ error }}</p>
    <p v-else-if="hint" :id="`${id}-hint`" class="text-sm text-arang-600">{{ hint }}</p>
    <slot name="below" />
  </div>
</template>
