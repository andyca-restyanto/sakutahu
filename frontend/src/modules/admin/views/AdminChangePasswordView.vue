<!-- filepath: /frontend/src/modules/admin/views/AdminChangePasswordView.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import PasswordInput from '@/shared/components/PasswordInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { fieldErrorsToRecord, toApiError } from '@/shared/api/errors'
import AdminAuthCard from '../components/AdminAuthCard.vue'
import { useAdminAuthStore } from '../stores/useAdminAuthStore'
import { changePasswordSchema, type ChangePasswordForm } from '../types/admin.schemas'

const router = useRouter()
const admin = useAdminAuthStore()
const forced = admin.mustChangePassword

const { handleSubmit, errors, defineField, setErrors, isSubmitting } = useForm({
  validationSchema: toTypedSchema(changePasswordSchema),
})
const cfg = { validateOnModelUpdate: false, validateOnBlur: true }
const [currentPassword, currentAttrs] = defineField('currentPassword', cfg)
const [newPassword, newAttrs] = defineField('newPassword', cfg)
const [confirmPassword, confirmAttrs] = defineField('confirmPassword', cfg)
const formError = ref<string | null>(null)

const onSubmit = handleSubmit(async (values) => {
  formError.value = null
  try {
    await admin.changePassword({ currentPassword: values.currentPassword, newPassword: values.newPassword })
    router.replace({ name: 'admin-users', query: { passwordChanged: '1' } })
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
  <AdminAuthCard
    :title="forced ? 'Ganti password awal' : 'Ganti password'"
    :subtitle="
      forced
        ? 'Demi keamanan, password awal wajib diganti sebelum memakai panel admin.'
        : 'Semua sesi admin kamu di perangkat lain akan keluar.'
    "
  >
    <form class="flex flex-col gap-5" novalidate @submit="onSubmit">
      <FormAlert v-if="formError">{{ formError }}</FormAlert>
      <PasswordInput
        v-model="currentPassword"
        v-bind="currentAttrs"
        :label="forced ? 'Password awal' : 'Password saat ini'"
        autocomplete="current-password"
        :error="errors.currentPassword"
      />
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
      <BaseButton type="submit" :loading="isSubmitting" block>Simpan password</BaseButton>
    </form>
    <template v-if="!forced" #footer>
      <p class="mt-6 text-center">
        <RouterLink :to="{ name: 'admin-users' }" class="text-sm font-semibold text-krem-200 hover:text-white">
          ‹ Kembali ke panel
        </RouterLink>
      </p>
    </template>
  </AdminAuthCard>
</template>
