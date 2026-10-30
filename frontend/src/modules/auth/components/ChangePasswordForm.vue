<!-- filepath: /frontend/src/modules/auth/components/ChangePasswordForm.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import PasswordInput from '@/shared/components/PasswordInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { fieldErrorsToRecord, toApiError } from '@/shared/api/errors'
import { useAuthStore } from '../stores/useAuthStore'
import { changePasswordSchema, type ChangePasswordForm } from '../types/auth.schemas'

const auth = useAuthStore()
const { handleSubmit, errors, defineField, setErrors, resetForm, isSubmitting } = useForm({
  validationSchema: toTypedSchema(changePasswordSchema),
})
const cfg = { validateOnModelUpdate: false, validateOnBlur: true }
const [currentPassword, currentAttrs] = defineField('currentPassword', cfg)
const [newPassword, newAttrs] = defineField('newPassword', cfg)
const [confirmPassword, confirmAttrs] = defineField('confirmPassword', cfg)

const formError = ref<string | null>(null)
const success = ref(false)

const onSubmit = handleSubmit(async (values) => {
  formError.value = null
  success.value = false
  try {
    await auth.changePassword({ currentPassword: values.currentPassword, newPassword: values.newPassword })
    resetForm()
    success.value = true
  } catch (e) {
    const err = toApiError(e)
    if (err.code === 'CURRENT_PASSWORD_INVALID') {
      setErrors({ currentPassword: err.message })
    } else if (err.code === 'VALIDATION_ERROR' && err.errors.length) {
      setErrors(fieldErrorsToRecord<keyof ChangePasswordForm>(err.errors))
    } else {
      formError.value = err.message
    }
  }
})
</script>

<template>
  <form class="flex flex-col gap-5" novalidate @submit="onSubmit">
    <FormAlert v-if="success" variant="success">
      Password berhasil diganti. Perangkat lain sudah otomatis keluar; kamu tetap masuk di perangkat ini.
    </FormAlert>
    <FormAlert v-if="formError">{{ formError }}</FormAlert>
    <div>
      <PasswordInput
        v-model="currentPassword"
        v-bind="currentAttrs"
        label="Password saat ini"
        autocomplete="current-password"
        :error="errors.currentPassword"
      />
      <RouterLink
        :to="{ name: 'forgot-password', query: { email: auth.user?.email } }"
        class="mt-1.5 inline-block text-sm font-semibold text-daun-700 underline-offset-2 hover:underline"
      >
        Lupa password saat ini?
      </RouterLink>
    </div>
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
    <div>
      <BaseButton type="submit" :loading="isSubmitting">Ganti password</BaseButton>
    </div>
  </form>
</template>
