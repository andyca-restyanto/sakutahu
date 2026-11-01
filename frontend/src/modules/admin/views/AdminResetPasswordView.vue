<!-- filepath: /frontend/src/modules/admin/views/AdminResetPasswordView.vue -->
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import PasswordInput from '@/shared/components/PasswordInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { fieldErrorsToRecord, toApiError, type ApiErrorCode } from '@/shared/api/errors'
import AdminAuthCard from '../components/AdminAuthCard.vue'
import { adminAuthService } from '../services/adminAuth.service'
import { useAdminAuthStore } from '../stores/useAdminAuthStore'
import { adminResetSchema, type AdminResetForm } from '../types/admin.schemas'

const route = useRoute()
const router = useRouter()
const admin = useAdminAuthStore()

// Token dibaca sekali lalu dihapus dari address bar
const token = typeof route.query.token === 'string' ? route.query.token : ''
const maskedEmail = ref<string | null>(null)
const checking = ref(true)
const linkError = ref<{ code: ApiErrorCode; message: string } | null>(null)
const formError = ref<string | null>(null)

const { handleSubmit, errors, defineField, setErrors, isSubmitting } = useForm({
  validationSchema: toTypedSchema(adminResetSchema),
})
const cfg = { validateOnModelUpdate: false, validateOnBlur: true }
const [newPassword, newAttrs] = defineField('newPassword', cfg)
const [confirmPassword, confirmAttrs] = defineField('confirmPassword', cfg)

onMounted(async () => {
  router.replace({ query: {} })
  if (!token) {
    linkError.value = { code: 'RESET_TOKEN_INVALID', message: 'Link tidak lengkap. Buka lagi link dari email.' }
    checking.value = false
    return
  }
  try {
    maskedEmail.value = (await adminAuthService.inspectReset(token)).maskedEmail
  } catch (e) {
    const err = toApiError(e)
    linkError.value = { code: err.code, message: err.message }
  } finally {
    checking.value = false
  }
})

const onSubmit = handleSubmit(async (values) => {
  formError.value = null
  try {
    await adminAuthService.resetPassword({ token, newPassword: values.newPassword })
    // Semua sesi admin sudah dicabut server; bersihkan sesi di tab ini juga lalu masuk ulang
    if (admin.isAuthenticated) await admin.logout().catch(() => undefined)
    router.replace({ name: 'admin-login', query: { reset: '1' } })
  } catch (e) {
    const err = toApiError(e)
    if (err.code === 'RESET_TOKEN_INVALID' || err.code === 'RESET_TOKEN_EXPIRED') {
      linkError.value = { code: err.code, message: err.message }
    } else if (err.code === 'VALIDATION_ERROR' && err.errors.length) {
      setErrors(fieldErrorsToRecord<keyof AdminResetForm>(err.errors))
    } else {
      formError.value = err.message
    }
  }
})
</script>

<template>
  <AdminAuthCard title="Buat password admin baru" :subtitle="maskedEmail ? `Untuk akun ${maskedEmail}` : undefined">
    <p v-if="checking" class="text-[15px] text-arang-600">Memeriksa link…</p>

    <div v-else-if="linkError" class="flex flex-col gap-5">
      <FormAlert>
        {{ linkError.message }}
        <template v-if="linkError.code === 'RESET_TOKEN_EXPIRED'"> Link hanya berlaku 15 menit.</template>
      </FormAlert>
      <RouterLink
        :to="{ name: 'admin-forgot-password' }"
        class="inline-flex w-full items-center justify-center rounded-xl bg-daun-600 px-5 py-3 text-[15px] font-semibold text-white shadow-sm hover:bg-daun-700"
      >
        Kirim link baru
      </RouterLink>
    </div>

    <form v-else class="flex flex-col gap-5" novalidate @submit="onSubmit">
      <FormAlert v-if="formError">{{ formError }}</FormAlert>
      <PasswordInput
        v-model="newPassword"
        v-bind="newAttrs"
        label="Password baru"
        autocomplete="new-password"
        hint="Minimal 12 karakter, berisi huruf dan angka."
        show-strength
        :error="errors.newPassword"
      />
      <PasswordInput
        v-model="confirmPassword"
        v-bind="confirmAttrs"
        label="Ulangi password baru"
        autocomplete="new-password"
        :error="errors.confirmPassword"
      />
      <p class="text-sm text-arang-600">Setelah disimpan, semua sesi admin kamu akan keluar dan kamu perlu masuk lagi.</p>
      <BaseButton type="submit" :loading="isSubmitting" block>Simpan password baru</BaseButton>
    </form>

    <template #footer>
      <p class="mt-6 text-center">
        <RouterLink :to="{ name: 'admin-login' }" class="text-sm font-semibold text-krem-200 hover:text-white">
          ‹ Kembali ke halaman masuk
        </RouterLink>
      </p>
    </template>
  </AdminAuthCard>
</template>
