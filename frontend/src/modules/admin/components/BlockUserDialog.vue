<!-- filepath: /frontend/src/modules/admin/components/BlockUserDialog.vue -->
<script setup lang="ts">
import { ref, watch } from 'vue'
import BaseDialog from '@/shared/components/BaseDialog.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { toApiError } from '@/shared/api/errors'
import { adminUserService } from '../services/adminUser.service'
import type { AdminUserDetailResponse } from '../types/admin.types'

const props = defineProps<{ open: boolean; user: AdminUserDetailResponse }>()
const emit = defineEmits<{ close: []; saved: [user: AdminUserDetailResponse] }>()

const reason = ref('')
const saving = ref(false)
const error = ref<string | null>(null)

watch(
  () => props.open,
  (open) => {
    if (open) {
      reason.value = ''
      error.value = null
    }
  },
)

async function submit(): Promise<void> {
  const text = reason.value.trim()
  if (!text) {
    error.value = 'Alasan blokir wajib diisi.'
    return
  }
  saving.value = true
  error.value = null
  try {
    emit('saved', await adminUserService.block(props.user.id, { reason: text }))
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseDialog :open="open" title="Blokir user" @close="emit('close')">
    <form class="flex flex-col gap-4" @submit.prevent="submit">
      <FormAlert v-if="error">{{ error }}</FormAlert>
      <FormAlert variant="warning">
        <strong>{{ user.fullName }}</strong> akan langsung keluar dari web, tidak bisa login, dan pesan Telegram-nya
        tidak dicatat sampai blokir dibuka. Data transaksinya tetap tersimpan.
      </FormAlert>
      <div class="flex flex-col gap-1.5">
        <label for="block-reason" class="text-sm font-semibold text-arang-900">Alasan (hanya terlihat admin)</label>
        <textarea
          id="block-reason"
          v-model="reason"
          rows="3"
          maxlength="255"
          class="w-full rounded-xl border border-krem-300 bg-white px-4 py-3 text-[15px] focus:border-daun-500 focus:ring-4 focus:ring-daun-100 focus:outline-none"
          placeholder="mis. Spam ke bot, penyalahgunaan fitur struk"
        />
        <p class="text-right text-xs text-arang-400">{{ reason.length }}/255</p>
      </div>
      <div class="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <BaseButton variant="secondary" @click="emit('close')">Batal</BaseButton>
        <BaseButton type="submit" :loading="saving" class="bg-red-600! hover:bg-red-700!">Blokir user</BaseButton>
      </div>
    </form>
  </BaseDialog>
</template>
