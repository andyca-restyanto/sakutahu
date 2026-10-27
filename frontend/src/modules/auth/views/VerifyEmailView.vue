<!-- filepath: /frontend/src/modules/auth/views/VerifyEmailView.vue -->
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import BaseInput from '@/shared/components/BaseInput.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { toApiError } from '@/shared/api/errors'
import AuthLayout from '../components/AuthLayout.vue'
import ResendVerificationButton from '../components/ResendVerificationButton.vue'
import { useAuthStore } from '../stores/useAuthStore'

type State = 'loading' | 'success' | 'expired' | 'invalid' | 'error'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const state = ref<State>('loading')
const errorMessage = ref('')
const resendEmail = ref('')
const resendInfo = ref<string | null>(null)

onMounted(async () => {
  const token = typeof route.query.token === 'string' ? route.query.token : ''
  // Hapus token dari URL agar tidak tersimpan di riwayat browser
  router.replace({ query: {} })

  if (!token) {
    state.value = 'invalid'
    return
  }
  try {
    // POST dari halaman ini (bukan GET dari email) agar link tidak "termakan" pemindai link email
    await auth.verifyEmail(token)
    state.value = 'success'
    setTimeout(() => router.replace({ name: 'connect-telegram' }), 1200)
  } catch (e) {
    const err = toApiError(e)
    errorMessage.value = err.message
    state.value =
      err.code === 'VERIFICATION_TOKEN_EXPIRED' ? 'expired' : err.code === 'VERIFICATION_TOKEN_INVALID' ? 'invalid' : 'error'
  }
})
</script>

<template>
  <AuthLayout
    :title="
      state === 'loading'
        ? 'Memverifikasi email…'
        : state === 'success'
          ? 'Email terverifikasi 🎉'
          : state === 'expired'
            ? 'Link sudah kedaluwarsa'
            : 'Link tidak valid'
    "
  >
    <div v-if="state === 'loading'" class="flex items-center gap-3 text-arang-600" role="status">
      <svg class="h-5 w-5 animate-spin text-daun-600" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" class="opacity-25" />
        <path d="M22 12a10 10 0 0 0-10-10" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
      </svg>
      Mohon tunggu sebentar.
    </div>

    <div v-else-if="state === 'success'" class="flex flex-col gap-4">
      <FormAlert variant="success">Akun kamu sudah aktif. Mengarahkan ke langkah berikutnya…</FormAlert>
      <RouterLink :to="{ name: 'connect-telegram' }" class="text-[15px] font-semibold text-daun-700 hover:underline">
        Lanjut hubungkan Telegram →
      </RouterLink>
    </div>

    <div v-else class="flex flex-col gap-5">
      <p class="text-[15px] leading-relaxed text-arang-600">
        <template v-if="state === 'expired'">Link verifikasi hanya berlaku 24 jam.</template>
        <template v-else-if="state === 'invalid'">Link ini sudah dipakai atau tidak lengkap.</template>
        <template v-else>{{ errorMessage }}</template>
        Masukkan email kamu untuk mendapatkan link baru. Jika akunmu sudah aktif, langsung
        <RouterLink :to="{ name: 'login' }" class="font-semibold text-daun-700 hover:underline">masuk</RouterLink>
        saja.
      </p>

      <BaseInput v-model="resendEmail" label="Email" type="email" autocomplete="email" placeholder="nama@email.com" />
      <ResendVerificationButton
        :email="resendEmail.trim()"
        variant="primary"
        @sent="(msg) => (resendInfo = msg)"
        @failed="(msg) => (resendInfo = msg)"
      />
      <FormAlert v-if="resendInfo" variant="info">{{ resendInfo }}</FormAlert>
    </div>
  </AuthLayout>
</template>
