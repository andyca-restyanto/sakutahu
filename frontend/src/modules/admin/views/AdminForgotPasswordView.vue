<!-- filepath: /frontend/src/modules/admin/views/AdminForgotPasswordView.vue -->
<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import BaseInput from '@/shared/components/BaseInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { fieldErrorsToRecord, toApiError } from '@/shared/api/errors'
import { maskEmail } from '@/shared/utils/format'
import AdminAuthCard from '../components/AdminAuthCard.vue'
import { adminAuthService } from '../services/adminAuth.service'
import { adminForgotSchema } from '../types/admin.schemas'

const COOLDOWN_SECONDS = 60
const route = useRoute()

const { handleSubmit, errors, defineField, setErrors, isSubmitting } = useForm({
  validationSchema: toTypedSchema(adminForgotSchema),
  initialValues: { email: typeof route.query.email === 'string' ? route.query.email : '' },
})
const [email, emailAttrs] = defineField('email', { validateOnModelUpdate: false, validateOnBlur: true })

const sentTo = ref<string | null>(null)
const formError = ref<string | null>(null)
const remaining = ref(0)
const resending = ref(false)
const resendInfo = ref<{ text: string; ok: boolean } | null>(null)
let timer: ReturnType<typeof setInterval> | undefined

function startCooldown(): void {
  remaining.value = COOLDOWN_SECONDS
  clearInterval(timer)
  timer = setInterval(() => {
    remaining.value -= 1
    if (remaining.value <= 0) clearInterval(timer)
  }, 1000)
}
onBeforeUnmount(() => clearInterval(timer))

const onSubmit = handleSubmit(async (values) => {
  formError.value = null
  try {
    await adminAuthService.forgotPassword(values.email)
    sentTo.value = values.email
    startCooldown()
  } catch (e) {
    const err = toApiError(e)
    if (err.code === 'VALIDATION_ERROR' && err.errors.length) {
      setErrors(fieldErrorsToRecord<'email'>(err.errors))
    } else {
      formError.value = err.message
    }
  }
})

async function resend(): Promise<void> {
  if (!sentTo.value || remaining.value > 0) return
  resending.value = true
  resendInfo.value = null
  try {
    await adminAuthService.forgotPassword(sentTo.value)
    resendInfo.value = { text: 'Link baru sudah dikirim. Link sebelumnya tidak berlaku lagi.', ok: true }
    startCooldown()
  } catch (e) {
    const err = toApiError(e)
    resendInfo.value = { text: err.message, ok: false }
    if (err.code === 'TOO_MANY_REQUESTS') startCooldown()
  } finally {
    resending.value = false
  }
}

const resendLabel = computed(() => (remaining.value > 0 ? `Kirim ulang dalam ${remaining.value} dtk` : 'Kirim ulang link'))
</script>

<template>
  <AdminAuthCard
    :title="sentTo ? 'Cek email kamu' : 'Lupa password admin'"
    :subtitle="sentTo ? undefined : 'Masukkan email akun admin. Kami kirim link untuk membuat password baru.'"
  >
    <form v-if="!sentTo" class="flex flex-col gap-5" novalidate @submit="onSubmit">
      <FormAlert v-if="formError">{{ formError }}</FormAlert>
      <BaseInput
        v-model="email"
        v-bind="emailAttrs"
        label="Email admin"
        type="email"
        inputmode="email"
        autocomplete="username"
        :error="errors.email"
      />
      <BaseButton type="submit" :loading="isSubmitting" block>Kirim link reset</BaseButton>
    </form>

    <div v-else class="flex flex-col gap-5">
      <p class="text-[15px] leading-relaxed text-arang-600">
        Jika <span class="font-semibold text-arang-900">{{ maskEmail(sentTo) }}</span> terdaftar sebagai admin, kami
        sudah mengirim link untuk membuat password baru. Link berlaku <strong>15 menit</strong> dan hanya bisa dipakai
        sekali.
      </p>
      <FormAlert variant="info">Tidak menemukan emailnya? Cek folder <strong>Spam</strong>.</FormAlert>
      <div>
        <BaseButton variant="secondary" :loading="resending" :disabled="remaining > 0" @click="resend">
          {{ resendLabel }}
        </BaseButton>
        <p v-if="resendInfo" class="mt-2 text-sm" :class="resendInfo.ok ? 'text-daun-700' : 'text-red-600'">
          {{ resendInfo.text }}
        </p>
      </div>
    </div>

    <template #footer>
      <p class="mt-6 text-center">
        <RouterLink :to="{ name: 'admin-login' }" class="text-sm font-semibold text-krem-200 hover:text-white">
          ‹ Kembali ke halaman masuk
        </RouterLink>
      </p>
    </template>
  </AdminAuthCard>
</template>
