<!-- filepath: /frontend/src/modules/admin/views/AdminAcceptInviteView.vue -->
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import PasswordInput from '@/shared/components/PasswordInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { fieldErrorsToRecord, toApiError } from '@/shared/api/errors'
import { formatDateTime } from '@/shared/utils/date'
import AdminAuthCard from '../components/AdminAuthCard.vue'
import { adminAuthService } from '../services/adminAuth.service'
import { useAdminAuthStore } from '../stores/useAdminAuthStore'
import { acceptInvitationSchema, type AcceptInvitationForm } from '../types/admin.schemas'
import type { InvitationInfoResponse } from '../types/admin.types'

const route = useRoute()
const router = useRouter()
const admin = useAdminAuthStore()

const token = typeof route.query.token === 'string' ? route.query.token : ''
const info = ref<InvitationInfoResponse | null>(null)
const loading = ref(true)
const loadError = ref<string | null>(null)
const formError = ref<string | null>(null)

const { handleSubmit, errors, defineField, setErrors, isSubmitting } = useForm({
  validationSchema: toTypedSchema(acceptInvitationSchema),
})
const cfg = { validateOnModelUpdate: false, validateOnBlur: true }
const [password, passwordAttrs] = defineField('password', cfg)
const [confirmPassword, confirmAttrs] = defineField('confirmPassword', cfg)

onMounted(async () => {
  // Token dihapus dari address bar agar tidak tersimpan di riwayat browser
  router.replace({ query: {} })
  if (!token) {
    loadError.value = 'Link undangan tidak lengkap. Buka lagi link dari email undangan.'
    loading.value = false
    return
  }
  try {
    info.value = await adminAuthService.inspectInvitation(token)
  } catch (e) {
    loadError.value = toApiError(e).message
  } finally {
    loading.value = false
  }
})

const onSubmit = handleSubmit(async (values) => {
  formError.value = null
  try {
    await admin.acceptInvitation({ token, password: values.password })
    router.replace({ name: 'admin-users' })
  } catch (e) {
    const err = toApiError(e)
    if (err.code === 'VALIDATION_ERROR' && err.errors.length) {
      setErrors(fieldErrorsToRecord<keyof AcceptInvitationForm>(err.errors))
    } else {
      formError.value = err.message
    }
  }
})
</script>

<template>
  <AdminAuthCard title="Aktifkan akun admin" subtitle="Buat password untuk masuk ke panel admin SakuTahu.">
    <p v-if="loading" class="text-[15px] text-arang-600">Memeriksa undangan…</p>

    <template v-else-if="loadError">
      <FormAlert>{{ loadError }}</FormAlert>
      <p class="mt-4 text-sm text-arang-600">Minta OWNER untuk mengirim ulang undangan.</p>
    </template>

    <form v-else-if="info" class="flex flex-col gap-5" novalidate @submit="onSubmit">
      <div class="rounded-xl bg-krem-100 px-4 py-3 text-sm">
        <p class="font-semibold text-arang-900">{{ info.fullName }}</p>
        <p class="text-arang-600">{{ info.email }}</p>
        <p class="mt-1 text-xs text-arang-400">Undangan berlaku sampai {{ formatDateTime(info.expiresAt) }} WIB</p>
      </div>
      <FormAlert v-if="formError">{{ formError }}</FormAlert>
      <PasswordInput
        v-model="password"
        v-bind="passwordAttrs"
        label="Password"
        autocomplete="new-password"
        hint="Minimal 12 karakter, berisi huruf dan angka."
        show-strength
        :error="errors.password"
      />
      <PasswordInput
        v-model="confirmPassword"
        v-bind="confirmAttrs"
        label="Ulangi password"
        autocomplete="new-password"
        :error="errors.confirmPassword"
      />
      <BaseButton type="submit" :loading="isSubmitting" block>Aktifkan akun</BaseButton>
    </form>

    <template #footer>
      <p class="mt-6 text-center">
        <RouterLink :to="{ name: 'admin-login' }" class="text-sm font-semibold text-krem-200 hover:text-white">
          Sudah punya akun admin? Masuk
        </RouterLink>
      </p>
    </template>
  </AdminAuthCard>
</template>
