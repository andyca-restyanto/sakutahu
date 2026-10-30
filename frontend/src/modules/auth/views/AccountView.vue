<!-- filepath: /frontend/src/modules/auth/views/AccountView.vue -->
<script setup lang="ts">
import { formatInstantDate } from '@/shared/utils/date'
import ChangePasswordForm from '../components/ChangePasswordForm.vue'
import { useAuthStore } from '../stores/useAuthStore'

const auth = useAuthStore()
</script>

<template>
  <div class="mx-auto flex max-w-2xl flex-col gap-6">
    <header>
      <h1 class="text-2xl font-extrabold tracking-tight text-arang-900">Akun</h1>
      <p class="text-sm text-arang-600">Info akun dan keamanan login kamu.</p>
    </header>

    <section v-if="auth.user" class="rounded-3xl bg-white p-6 shadow-sm" aria-labelledby="account-info">
      <h2 id="account-info" class="font-bold text-arang-900">Info akun</h2>
      <dl class="mt-4 grid gap-x-6 gap-y-3 text-[15px] sm:grid-cols-[auto_1fr]">
        <dt class="text-arang-600">Nama</dt>
        <dd class="font-semibold text-arang-900">{{ auth.user.fullName }}</dd>
        <dt class="text-arang-600">Email</dt>
        <dd class="font-semibold break-all text-arang-900">{{ auth.user.email }}</dd>
        <dt class="text-arang-600">Paket</dt>
        <dd class="font-semibold text-arang-900">{{ auth.user.plan === 'VIP' ? '⭐ VIP' : 'FREE' }}</dd>
        <dt class="text-arang-600">Telegram</dt>
        <dd class="font-semibold text-arang-900">{{ auth.user.telegramLinked ? 'Terhubung ✅' : 'Belum terhubung' }}</dd>
        <dt class="text-arang-600">Bergabung</dt>
        <dd class="font-semibold text-arang-900">{{ formatInstantDate(auth.user.createdAt, auth.user.timezone) }}</dd>
      </dl>
    </section>

    <section class="rounded-3xl bg-white p-6 shadow-sm" aria-labelledby="change-password">
      <h2 id="change-password" class="font-bold text-arang-900">Ganti password</h2>
      <p class="mt-1 mb-5 text-sm text-arang-600">
        Setelah diganti, semua perangkat lain otomatis keluar dan kami kirim email pemberitahuan.
      </p>
      <ChangePasswordForm />
    </section>
  </div>
</template>
