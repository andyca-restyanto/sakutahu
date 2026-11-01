<!-- filepath: /frontend/src/modules/admin/views/AdminLoginView.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import BaseInput from '@/shared/components/BaseInput.vue'
import PasswordInput from '@/shared/components/PasswordInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { fieldErrorsToRecord, toApiError } from '@/shared/api/errors'
import AdminAuthCard from '../components/AdminAuthCard.vue'
import { useAdminAuthStore } from '../stores/useAdminAuthStore'
import { adminLoginSchema, type AdminLoginForm } from '../types/admin.schemas'

const route = useRoute()
const router = useRouter()
const admin = useAdminAuthStore()

const sessionExpired = route.query.expired === '1'
const passwordReset = route.query.reset === '1'
const { handleSubmit, errors, defineField, setErrors, isSubmitting } = useForm({
  validationSchema: toTypedSchema(adminLoginSchema),
})
const cfg = { validateOnModelUpdate: false, validateOnBlur: true }
const [email, emailAttrs] = defineField('email', cfg)
const [password, passwordAttrs] = defineField('password', cfg)
const formError = ref<string | null>(null)

function redirectTarget(): string {
  const r = route.query.redirect
  return typeof r === 'string' && r.startsWith('/admin/') && !r.startsWith('//') ? r : '/admin/users'
}

const onSubmit = handleSubmit(async (values) => {
  formError.value = null
  try {
    await admin.login({ email: values.email, password: values.password })
    if (admin.mustChangePassword) {
      router.replace({ name: 'admin-change-password' })
    } else {
      router.replace(redirectTarget())
    }
  } catch (e) {
    const err = toApiError(e)
    if (err.code === 'VALIDATION_ERROR' && err.errors.length) {
      setErrors(fieldErrorsToRecord<keyof AdminLoginForm>(err.errors))
    } else {
      formError.value = err.message
    }
  }
})
</script>

<template>
  <AdminAuthCard title="Masuk panel admin" subtitle="Khusus admin SakuTahu. Akun user biasa tidak bisa dipakai di sini.">
    <form class="flex flex-col gap-5" novalidate @submit="onSubmit">
      <FormAlert v-if="passwordReset && !formError" variant="success">
        Password admin berhasil diganti. Silakan masuk dengan password baru.
      </FormAlert>
      <FormAlert v-else-if="sessionExpired && !formError" variant="info">Sesi admin berakhir. Silakan masuk kembali.</FormAlert>
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
      <div>
        <PasswordInput
          v-model="password"
          v-bind="passwordAttrs"
          label="Password"
          autocomplete="current-password"
          :error="errors.password"
        />
        <RouterLink
          :to="{ name: 'admin-forgot-password', query: email ? { email } : {} }"
          class="mt-2 inline-block text-sm font-semibold text-daun-700 underline-offset-2 hover:underline"
        >
          Lupa password?
        </RouterLink>
      </div>
      <BaseButton type="submit" :loading="isSubmitting" block>Masuk</BaseButton>
    </form>
  </AdminAuthCard>
</template>
