<!-- filepath: /frontend/src/shared/components/ConfirmDialog.vue -->
<script setup lang="ts">
import BaseDialog from './BaseDialog.vue'
import BaseButton from './BaseButton.vue'

withDefaults(
  defineProps<{
    open: boolean
    title: string
    message: string
    confirmLabel?: string
    danger?: boolean
    loading?: boolean
  }>(),
  { confirmLabel: 'Ya, lanjutkan', danger: false, loading: false },
)
const emit = defineEmits<{ confirm: []; cancel: [] }>()
</script>

<template>
  <BaseDialog :open="open" :title="title" size="sm" @close="emit('cancel')">
    <p class="text-[15px] leading-relaxed text-arang-600">{{ message }}</p>
    <div class="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
      <BaseButton variant="secondary" @click="emit('cancel')">Batal</BaseButton>
      <BaseButton
        :loading="loading"
        :class="danger ? 'bg-red-600! hover:bg-red-700!' : ''"
        @click="emit('confirm')"
      >
        {{ confirmLabel }}
      </BaseButton>
    </div>
  </BaseDialog>
</template>
