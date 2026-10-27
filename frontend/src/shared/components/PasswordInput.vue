<!-- filepath: /frontend/src/shared/components/PasswordInput.vue -->
<script setup lang="ts">
import { computed, ref } from 'vue'
import BaseInput from './BaseInput.vue'
import { passwordStrength } from '../utils/format'

defineOptions({ inheritAttrs: false })

const props = defineProps<{
  label: string
  error?: string
  hint?: string
  autocomplete?: string
  showStrength?: boolean
}>()

const model = defineModel<string>()
const visible = ref(false)
const strength = computed(() => passwordStrength(model.value ?? ''))

const barColor = computed(() => {
  switch (strength.value.score) {
    case 0:
    case 1:
      return 'bg-red-400'
    case 2:
      return 'bg-kunyit-400'
    default:
      return 'bg-daun-500'
  }
})
</script>

<template>
  <BaseInput
    v-model="model"
    v-bind="$attrs"
    :label="props.label"
    :type="visible ? 'text' : 'password'"
    :error="props.error"
    :hint="props.hint"
    :autocomplete="props.autocomplete"
    class="pr-12"
  >
    <template #trailing>
      <button
        type="button"
        class="rounded-lg p-2 text-arang-600 hover:bg-krem-100 hover:text-arang-900"
        :aria-label="visible ? 'Sembunyikan password' : 'Tampilkan password'"
        :aria-pressed="visible"
        @click="visible = !visible"
      >
        <svg v-if="!visible" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
          <circle cx="12" cy="12" r="3" />
        </svg>
        <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 3l18 18M10.6 10.6a3 3 0 0 0 4.2 4.2M9.9 5.2A9.9 9.9 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-3.2 4.2M6.6 6.6C3.9 8.4 2 12 2 12s3.5 7 10 7a9.6 9.6 0 0 0 5.4-1.6" />
        </svg>
      </button>
    </template>
    <template v-if="props.showStrength && model" #below>
      <div class="flex items-center gap-3" aria-live="polite">
        <div class="flex flex-1 gap-1">
          <span
            v-for="i in 4"
            :key="i"
            class="h-1.5 flex-1 rounded-full transition-colors"
            :class="i <= strength.score ? barColor : 'bg-krem-200'"
          />
        </div>
        <span class="text-xs font-medium text-arang-600">{{ strength.label }}</span>
      </div>
    </template>
  </BaseInput>
</template>
