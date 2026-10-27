<!-- filepath: /frontend/src/modules/auth/views/CheckEmailView.vue -->
<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import FormAlert from '@/shared/components/FormAlert.vue'
import { maskEmail } from '@/shared/utils/format'
import AuthLayout from '../components/AuthLayout.vue'
import ResendVerificationButton from '../components/ResendVerificationButton.vue'

const route = useRoute()
const email = computed(() => (typeof route.query.email === 'string' ? route.query.email : ''))
const info = ref<{ text: string; ok: boolean } | null>(null)
</script>

<template>
  <AuthLayout title="Cek email kamu">
    <div class="flex flex-col items-start gap-6">
      <!-- Ilustrasi amplop -->
      <div class="grid h-16 w-16 place-items-center rounded-2xl bg-daun-50 text-daun-600" aria-hidden="true">
        <svg class="h-8 w-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <rect x="3" y="5" width="18" height="14" rx="2.5" />
          <path d="M3.5 7l8.5 6 8.5-6" stroke-linejoin="round" />
        </svg>
      </div>

      <p class="text-[15px] leading-relaxed text-arang-600">
        Kami sudah mengirim link verifikasi ke
        <span v-if="email" class="font-semibold text-arang-900">{{ maskEmail(email) }}</span>
        <span v-else>email kamu</span>.
        Klik link tersebut untuk mengaktifkan akun. Link berlaku 24 jam.
      </p>

      <FormAlert variant="info" class="w-full">
        Tidak menemukan emailnya? Cek folder <strong>Spam</strong> atau <strong>Promosi</strong>.
      </FormAlert>

      <div v-if="email" class="w-full">
        <!-- Email pertama baru saja dikirim saat register, jadi mulai dengan cooldown -->
        <ResendVerificationButton
          :email="email"
          :initial-cooldown="60"
          @sent="(msg) => (info = { text: msg, ok: true })"
          @failed="(msg) => (info = { text: msg, ok: false })"
        />
        <p v-if="info" class="mt-2 text-sm" :class="info.ok ? 'text-daun-700' : 'text-red-600'">{{ info.text }}</p>
      </div>

      <p class="text-[15px] text-arang-600">
        Salah email?
        <RouterLink :to="{ name: 'register' }" class="font-semibold text-daun-700 underline-offset-2 hover:underline">
          Daftar ulang
        </RouterLink>
        <span class="mx-2 text-arang-400">·</span>
        <RouterLink :to="{ name: 'login' }" class="font-semibold text-daun-700 underline-offset-2 hover:underline">
          Ke halaman masuk
        </RouterLink>
      </p>
    </div>
  </AuthLayout>
</template>
