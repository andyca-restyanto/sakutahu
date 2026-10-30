<!-- filepath: /frontend/src/modules/auth/views/LoginView.vue -->
<script setup lang="ts">
import { RouterLink, useRoute, useRouter } from 'vue-router'
import FormAlert from '@/shared/components/FormAlert.vue'
import AuthLayout from '../components/AuthLayout.vue'
import LoginForm from '../components/LoginForm.vue'

const route = useRoute()
const router = useRouter()

const sessionExpired = route.query.expired === '1'
const accountBlocked = route.query.blocked === '1'
const passwordReset = route.query.reset === '1'

function redirectTarget(): string {
  const redirect = route.query.redirect
  // Hanya izinkan path internal untuk mencegah open redirect
  return typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
    ? redirect
    : '/dashboard'
}

function onLoggedIn(): void {
  router.replace(redirectTarget())
}
</script>

<template>
  <AuthLayout title="Selamat datang kembali" subtitle="Masuk untuk melihat catatan pengeluaranmu.">
    <FormAlert v-if="accountBlocked" class="mb-5">
      Akun kamu diblokir oleh admin SakuTahu. Hubungi admin jika menurutmu ini keliru.
    </FormAlert>
    <FormAlert v-else-if="passwordReset" variant="success" class="mb-5">
      Password berhasil diganti. Silakan masuk dengan password baru.
    </FormAlert>
    <FormAlert v-else-if="sessionExpired" variant="info" class="mb-5">
      Sesi kamu telah berakhir. Silakan masuk kembali.
    </FormAlert>

    <LoginForm @success="onLoggedIn" />

    <p class="mt-8 text-center text-[15px] text-arang-600">
      Belum punya akun?
      <RouterLink :to="{ name: 'register' }" class="font-semibold text-daun-700 underline-offset-2 hover:underline">
        Daftar gratis
      </RouterLink>
    </p>
  </AuthLayout>
</template>
