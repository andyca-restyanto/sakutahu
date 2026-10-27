<!-- filepath: /frontend/src/modules/auth/views/RegisterView.vue -->
<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router'
import AuthLayout from '../components/AuthLayout.vue'
import RegisterForm from '../components/RegisterForm.vue'
import type { RegisterResponse } from '../types/auth.types'

const router = useRouter()

function onRegistered(response: RegisterResponse): void {
  router.push({ name: 'check-email', query: { email: response.email } })
}
</script>

<template>
  <AuthLayout title="Buat akun SakuTahu" subtitle="Gratis. Mulai catat pengeluaran dalam hitungan menit.">
    <RegisterForm @success="onRegistered" />

    <p class="mt-8 text-center text-[15px] text-arang-600">
      Sudah punya akun?
      <RouterLink :to="{ name: 'login' }" class="font-semibold text-daun-700 underline-offset-2 hover:underline">
        Masuk
      </RouterLink>
    </p>
  </AuthLayout>
</template>
