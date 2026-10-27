<!-- filepath: /frontend/src/modules/auth/components/RegisterForm.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { RouterLink } from 'vue-router'
import BaseInput from '@/shared/components/BaseInput.vue'
import PasswordInput from '@/shared/components/PasswordInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { fieldErrorsToRecord, toApiError } from '@/shared/api/errors'
import { detectTimezone } from '@/shared/utils/format'
import { useAuthStore } from '../stores/useAuthStore'
import { registerSchema, type RegisterForm } from '../types/auth.schemas'
import type { RegisterResponse } from '../types/auth.types'

const emit = defineEmits<{ success: [response: RegisterResponse] }>()
const auth = useAuthStore()

const { handleSubmit, errors, defineField, setErrors, setFieldError, isSubmitting } = useForm({
  validationSchema: toTypedSchema(registerSchema),
})
const fieldConfig = { validateOnModelUpdate: false, validateOnBlur: true }
const [fullName, fullNameAttrs] = defineField('fullName', fieldConfig)
const [email, emailAttrs] = defineField('email', fieldConfig)
const [password, passwordAttrs] = defineField('password', fieldConfig)
const [confirmPassword, confirmAttrs] = defineField('confirmPassword', fieldConfig)

const formError = ref<string | null>(null)
const emailTaken = ref(false)

const onSubmit = handleSubmit(async (values) => {
  formError.value = null
  emailTaken.value = false
  try {
    const response = await auth.register({
      fullName: values.fullName,
      email: values.email,
      password: values.password,
      currency: 'IDR',
      timezone: detectTimezone(),
    })
    emit('success', response)
  } catch (e) {
    const err = toApiError(e)
    switch (err.code) {
      case 'EMAIL_ALREADY_EXISTS':
        emailTaken.value = true
        setFieldError('email', 'Email ini sudah terdaftar')
        break
      case 'VALIDATION_ERROR':
        setErrors(fieldErrorsToRecord<keyof RegisterForm>(err.errors))
        break
      default:
        formError.value = err.message
    }
  }
})
</script>

<template>
  <form class="flex flex-col gap-5" novalidate @submit="onSubmit">
    <FormAlert v-if="formError">{{ formError }}</FormAlert>

    <BaseInput
      v-model="fullName"
      v-bind="fullNameAttrs"
      label="Nama lengkap"
      autocomplete="name"
      placeholder="Budi Santoso"
      :error="errors.fullName"
    />

    <BaseInput
      v-model="email"
      v-bind="emailAttrs"
      label="Email"
      type="email"
      inputmode="email"
      autocomplete="email"
      placeholder="nama@email.com"
      :error="errors.email"
    >
      <template v-if="emailTaken" #below>
        <p class="text-sm text-arang-600">
          Sudah punya akun?
          <RouterLink :to="{ name: 'login' }" class="font-semibold text-daun-700 underline-offset-2 hover:underline">
            Masuk di sini
          </RouterLink>
        </p>
      </template>
    </BaseInput>

    <PasswordInput
      v-model="password"
      v-bind="passwordAttrs"
      label="Password"
      autocomplete="new-password"
      hint="Minimal 8 karakter, berisi huruf dan angka"
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

    <BaseButton type="submit" :loading="isSubmitting" block>Buat akun</BaseButton>

    <p class="text-center text-xs leading-relaxed text-arang-600">
      Dengan mendaftar, kamu menyetujui Syarat &amp; Ketentuan serta Kebijakan Privasi SakuTahu.
    </p>
  </form>
</template>
