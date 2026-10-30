<!-- filepath: /frontend/src/modules/auth/components/LoginForm.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import BaseInput from '@/shared/components/BaseInput.vue'
import PasswordInput from '@/shared/components/PasswordInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { fieldErrorsToRecord, toApiError } from '@/shared/api/errors'
import { useAuthStore } from '../stores/useAuthStore'
import { loginSchema, type LoginForm } from '../types/auth.schemas'
import ResendVerificationButton from './ResendVerificationButton.vue'

const emit = defineEmits<{ success: [] }>()
const auth = useAuthStore()

const { handleSubmit, errors, defineField, setErrors, isSubmitting } = useForm({
  validationSchema: toTypedSchema(loginSchema),
})
const fieldConfig = { validateOnModelUpdate: false, validateOnBlur: true }
const [email, emailAttrs] = defineField('email', fieldConfig)
const [password, passwordAttrs] = defineField('password', fieldConfig)

const formError = ref<string | null>(null)
const unverifiedEmail = ref<string | null>(null)
const resendInfo = ref<string | null>(null)

const onSubmit = handleSubmit(async (values) => {
  formError.value = null
  unverifiedEmail.value = null
  resendInfo.value = null
  try {
    await auth.login({ email: values.email, password: values.password })
    emit('success')
  } catch (e) {
    const err = toApiError(e)
    switch (err.code) {
      case 'EMAIL_NOT_VERIFIED':
        unverifiedEmail.value = values.email
        break
      case 'VALIDATION_ERROR':
        setErrors(fieldErrorsToRecord<keyof LoginForm>(err.errors))
        break
      default:
        // INVALID_CREDENTIALS, ACCOUNT_LOCKED, TOO_MANY_REQUESTS, NETWORK_ERROR, dll.
        formError.value = err.message
    }
  }
})
</script>

<template>
  <form class="flex flex-col gap-5" novalidate @submit="onSubmit">
    <FormAlert v-if="formError">{{ formError }}</FormAlert>

    <FormAlert v-if="unverifiedEmail" variant="warning">
      <p class="font-semibold">Email kamu belum diverifikasi.</p>
      <p class="mt-1">Cek kotak masuk (atau folder spam) untuk link verifikasi, atau kirim ulang di bawah.</p>
      <div class="mt-3">
        <ResendVerificationButton
          :email="unverifiedEmail"
          @sent="(msg) => (resendInfo = msg)"
          @failed="(msg) => (resendInfo = msg)"
        />
      </div>
      <p v-if="resendInfo" class="mt-2 text-xs">{{ resendInfo }}</p>
    </FormAlert>

    <BaseInput
      v-model="email"
      v-bind="emailAttrs"
      label="Email"
      type="email"
      inputmode="email"
      autocomplete="email"
      placeholder="nama@email.com"
      :error="errors.email"
    />

    <div>
      <PasswordInput
        v-model="password"
        v-bind="passwordAttrs"
        label="Password"
        autocomplete="current-password"
        :error="errors.password"
      />
      <RouterLink
        :to="{ name: 'forgot-password', query: email ? { email } : {} }"
        class="mt-2 inline-block text-sm font-semibold text-daun-700 underline-offset-2 hover:underline"
      >
        Lupa password?
      </RouterLink>
    </div>

    <BaseButton type="submit" :loading="isSubmitting" block>Masuk</BaseButton>
  </form>
</template>
