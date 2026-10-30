<!-- filepath: /frontend/src/modules/auth/views/ResetPasswordView.vue -->
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import PasswordInput from '@/shared/components/PasswordInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { fieldErrorsToRecord, toApiError, type ApiErrorCode } from '@/shared/api/errors'
import AuthLayout from '../components/AuthLayout.vue'
import { authService } from '../services/auth.service'
import { useAuthStore } from '../stores/useAuthStore'
import { resetPasswordSchema, type ResetPasswordForm } from '../types/auth.schemas'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// Token dibaca sekali lalu dihapus dari address bar agar tidak tersimpan di riwayat browser
const token = typeof route.query.token === 'string' ? route.query.token : ''
const maskedEmail = ref<string | null>(null)
const checking = ref(true)
const linkError = ref<{ code: ApiErrorCode; message: string } | null>(null)
const formError = ref<string | null>(null)

const { handleSubmit, errors, defineField, setErrors, isSubmitting } = useForm({
  validationSchema: toTypedSchema(resetPasswordSchema),
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
    maskedEmail.value = (await authService.inspectReset(token)).maskedEmail
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
    await authService.resetPassword({ token, newPassword: values.newPassword })
    // Sesi lama di semua perangkat sudah dicabut server; user masuk ulang dengan password baru
    auth.clear()
    router.replace({ name: 'login', query: { reset: '1' } })
  } catch (e) {
    const err = toApiError(e)
    if (err.code === 'RESET_TOKEN_INVALID' || err.code === 'RESET_TOKEN_EXPIRED') {
      linkError.value = { code: err.code, message: err.message }
    } else if (err.code === 'VALIDATION_ERROR' && err.errors.length) {
      setErrors(fieldErrorsToRecord<keyof ResetPasswordForm>(err.errors))
    } else {
      formError.value = err.message
    }
  }
})
</script>

<template>
  <AuthLayout title="Buat password baru" :subtitle="maskedEmail ? `Untuk akun ${maskedEmail}` : undefined">
    <p v-if="checking" class="text-[15px] text-arang-600">Memeriksa link…</p>

    <div v-else-if="linkError" class="flex flex-col gap-5">
      <FormAlert>
        {{ linkError.message }}
        <template v-if="linkError.code === 'RESET_TOKEN_EXPIRED'"> Link hanya berlaku 15 menit.</template>
      </FormAlert>
      <RouterLink
        :to="{ name: 'forgot-password' }"
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
        hint="Minimal 8 karakter, berisi huruf dan angka."
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
      <p class="text-sm text-arang-600">Setelah disimpan, kamu akan keluar dari semua perangkat dan perlu masuk lagi.</p>
      <BaseButton type="submit" :loading="isSubmitting" block>Simpan password baru</BaseButton>
    </form>

    <p class="mt-8 text-center text-[15px] text-arang-600">
      <RouterLink :to="{ name: 'login' }" class="font-semibold text-daun-700 underline-offset-2 hover:underline">
        Kembali ke halaman masuk
      </RouterLink>
    </p>
  </AuthLayout>
</template>
