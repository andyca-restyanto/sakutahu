<!-- filepath: /frontend/src/modules/dashboard/components/PeriodFilter.vue -->
<script setup lang="ts">
import { ref, watch } from 'vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import { formatRange } from '@/shared/utils/date'
import type { PeriodPreset } from '../types/period.types'

const props = defineProps<{ preset: PeriodPreset; from: string; to: string; today: string }>()
const emit = defineEmits<{ change: [preset: PeriodPreset, from?: string, to?: string] }>()

const options: { value: PeriodPreset; label: string }[] = [
  { value: 'this_month', label: 'Bulan ini' },
  { value: 'last_month', label: 'Bulan lalu' },
  { value: '3m', label: '3 bulan' },
  { value: '6m', label: '6 bulan' },
  { value: '12m', label: '12 bulan' },
  { value: 'custom', label: 'Pilih tanggal' },
]

const showCustom = ref(props.preset === 'custom')
const customFrom = ref(props.from)
const customTo = ref(props.to)
const customError = ref<string | null>(null)

watch(
  () => [props.from, props.to, props.preset] as const,
  ([f, t, p]) => {
    customFrom.value = f
    customTo.value = t
    if (p !== 'custom') showCustom.value = false
  },
)

function select(value: PeriodPreset): void {
  if (value === 'custom') {
    showCustom.value = true
    return
  }
  emit('change', value)
}

function applyCustom(): void {
  if (!customFrom.value || !customTo.value) {
    customError.value = 'Isi tanggal awal dan akhir'
    return
  }
  if (customFrom.value > customTo.value) {
    customError.value = 'Tanggal awal harus sebelum tanggal akhir'
    return
  }
  customError.value = null
  emit('change', 'custom', customFrom.value, customTo.value)
}
</script>

<template>
  <div class="flex flex-col gap-3">
    <div class="flex flex-wrap items-center gap-2">
      <div role="radiogroup" aria-label="Periode" class="flex flex-wrap gap-1 rounded-xl bg-white p-1 shadow-sm">
        <button
          v-for="o in options"
          :key="o.value"
          type="button"
          role="radio"
          :aria-checked="(o.value === 'custom' ? showCustom : !showCustom && preset === o.value)"
          class="rounded-lg px-3 py-1.5 text-sm font-semibold transition"
          :class="
            (o.value === 'custom' ? showCustom : !showCustom && preset === o.value)
              ? 'bg-daun-600 text-white'
              : 'text-arang-600 hover:bg-krem-100 hover:text-arang-900'
          "
          @click="select(o.value)"
        >
          {{ o.label }}
        </button>
      </div>
      <span class="text-sm font-medium text-arang-600" aria-live="polite">{{ formatRange(from, to) }}</span>
    </div>

    <form
      v-if="showCustom"
      class="flex flex-wrap items-end gap-3 rounded-xl bg-white p-3 shadow-sm"
      @submit.prevent="applyCustom"
    >
      <label class="flex flex-col gap-1 text-sm font-semibold">
        Dari
        <input v-model="customFrom" type="date" :max="today" class="rounded-lg border border-krem-300 px-3 py-2 font-normal" />
      </label>
      <label class="flex flex-col gap-1 text-sm font-semibold">
        Sampai
        <input v-model="customTo" type="date" :max="today" class="rounded-lg border border-krem-300 px-3 py-2 font-normal" />
      </label>
      <BaseButton type="submit">Terapkan</BaseButton>
      <p class="basis-full text-xs text-arang-600">
        Maksimal 24 bulan. Rentang ≤ 31 hari ditampilkan per hari, lebih panjang per bulan.
      </p>
      <p v-if="customError" class="basis-full text-sm text-red-600">{{ customError }}</p>
    </form>
  </div>
</template>
