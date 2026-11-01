<!-- filepath: /frontend/src/modules/report/components/ExportExcelButton.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import { toApiError } from '@/shared/api/errors'
import { reportService } from '../services/report.service'
import type { JournalQuery } from '../types/report.types'

const props = defineProps<{ query: JournalQuery; disabled?: boolean }>()
const emit = defineEmits<{ done: [filename: string]; failed: [message: string] }>()
const loading = ref(false)

async function download(): Promise<void> {
  loading.value = true
  try {
    emit('done', await reportService.exportJournal(props.query))
  } catch (e) {
    emit('failed', toApiError(e).message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <BaseButton variant="secondary" :loading="loading" :disabled="disabled" @click="download">
    <svg v-if="!loading" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
      <path d="M12 3v12m0 0-4-4m4 4 4-4M5 21h14" />
    </svg>
    {{ loading ? 'Menyiapkan file…' : 'Unduh Excel' }}
  </BaseButton>
</template>
