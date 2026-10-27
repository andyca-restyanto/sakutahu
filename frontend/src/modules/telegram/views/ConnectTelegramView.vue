<!-- filepath: /frontend/src/modules/telegram/views/ConnectTelegramView.vue -->
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { toDataURL } from 'qrcode'
import BrandLogo from '@/shared/components/BrandLogo.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { toApiError } from '@/shared/api/errors'
import { useAuthStore } from '@/modules/auth'
import TelegramUsageGuide from '../components/TelegramUsageGuide.vue'
import { telegramService } from '../services/telegram.service'
import type { TelegramLinkResponse } from '../types/telegram.types'

const POLL_INTERVAL_MS = 3000

const router = useRouter()
const auth = useAuthStore()

const link = ref<TelegramLinkResponse | null>(null)
const qrDataUrl = ref<string | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const linked = ref(false)
const linkedUsername = ref<string | null>(null)
const now = ref(Date.now())
/** Nama bot dikirim backend (satu sumber kebenaran: app.telegram.bot-username). */
const botUsername = ref<string | null>(null)
const botLabel = computed(() => (botUsername.value ? `@${botUsername.value}` : 'bot SakuTahu'))

let pollTimer: ReturnType<typeof setInterval> | undefined
let clockTimer: ReturnType<typeof setInterval> | undefined

const secondsLeft = computed(() => {
  if (!link.value) return 0
  return Math.max(0, Math.floor((new Date(link.value.expiresAt).getTime() - now.value) / 1000))
})
const expired = computed(() => !!link.value && secondsLeft.value === 0)
const countdown = computed(() => {
  const m = Math.floor(secondsLeft.value / 60)
  const s = secondsLeft.value % 60
  return `${m}:${s.toString().padStart(2, '0')}`
})

async function generateLink(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    link.value = await telegramService.createLinkToken()
    botUsername.value = link.value.botUsername
    qrDataUrl.value = await toDataURL(link.value.deepLink, {
      width: 220,
      margin: 1,
      color: { dark: '#15301f', light: '#ffffff' },
    })
    startPolling()
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    loading.value = false
  }
}

async function checkStatus(): Promise<void> {
  // Jangan polling saat tab tidak terlihat (hemat request)
  if (document.visibilityState !== 'visible') return
  try {
    const status = await telegramService.getStatus()
    if (status.linked) {
      linked.value = true
      linkedUsername.value = status.username
      auth.patchUser({ telegramLinked: true })
      stopPolling()
    }
  } catch {
    // Abaikan error sementara; polling berikutnya akan mencoba lagi
  }
}

function startPolling(): void {
  stopPolling()
  pollTimer = setInterval(() => {
    if (expired.value) {
      stopPolling()
      return
    }
    void checkStatus()
  }, POLL_INTERVAL_MS)
}

function stopPolling(): void {
  clearInterval(pollTimer)
  pollTimer = undefined
}

function goToDashboard(): void {
  router.push({ name: 'dashboard' })
}

onMounted(async () => {
  clockTimer = setInterval(() => (now.value = Date.now()), 1000)
  try {
    const status = await telegramService.getStatus()
    botUsername.value = status.botUsername
    if (status.linked) {
      linked.value = true
      linkedUsername.value = status.username
      loading.value = false
      return
    }
  } catch {
    // Lanjut buat link
  }
  await generateLink()
})

onBeforeUnmount(() => {
  stopPolling()
  clearInterval(clockTimer)
})
</script>

<template>
  <div class="min-h-screen px-4 py-8 sm:px-8">
    <header class="mx-auto flex max-w-3xl items-center justify-between">
      <BrandLogo />
      <RouterLink
        v-if="!linked"
        :to="{ name: 'dashboard' }"
        class="rounded-lg px-3 py-2 text-sm font-semibold text-arang-600 hover:bg-krem-200 hover:text-arang-900"
      >
        Lewati dulu
      </RouterLink>
    </header>

    <main class="mx-auto mt-10 max-w-3xl">
      <!-- Sukses -->
      <section v-if="linked" class="rounded-3xl bg-white p-8 text-center shadow-sm sm:p-12">
        <div class="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-daun-50 text-3xl" aria-hidden="true">✅</div>
        <h1 class="mt-6 text-3xl font-extrabold tracking-tight">Telegram terhubung!</h1>
        <p class="mx-auto mt-3 max-w-md text-[15px] text-arang-600">
          <template v-if="linkedUsername">Akun <span class="font-semibold text-arang-900">@{{ linkedUsername }}</span> sudah tersambung. </template>
          Sekarang kamu bisa mencatat pengeluaran langsung dari chat dengan {{ botLabel }}.
        </p>
        <BaseButton class="mt-8" @click="goToDashboard">Lanjut ke Dashboard</BaseButton>
      </section>
      <TelegramUsageGuide v-if="linked" class="mt-4" :bot-username="botUsername" />

      <!-- Langkah menghubungkan -->
      <section v-else class="rounded-3xl bg-white p-6 shadow-sm sm:p-10">
        <p class="text-sm font-semibold tracking-wide text-daun-600 uppercase">Langkah terakhir</p>
        <h1 class="mt-2 text-3xl font-extrabold tracking-tight">Hubungkan ke {{ botLabel }}</h1>
        <p class="mt-2 max-w-xl text-[15px] text-arang-600">
          Supaya kamu bisa mencatat pengeluaran cukup dengan kirim chat, misalnya
          <span class="rounded-md bg-krem-100 px-1.5 py-0.5 font-medium text-arang-900">makan siang 35rb</span>.
        </p>

        <FormAlert v-if="error" class="mt-6">{{ error }}</FormAlert>

        <div class="mt-8 grid gap-8 md:grid-cols-[1fr_auto] md:items-start">
          <ol class="flex flex-col gap-5">
            <li v-for="(step, i) in ['Klik tombol “Buka Telegram” di bawah', 'Tekan Start di chat bot', 'Kembali ke halaman ini, akun akan terhubung otomatis']" :key="i" class="flex gap-4">
              <span class="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-daun-50 text-sm font-bold text-daun-700">{{ i + 1 }}</span>
              <span class="pt-1 text-[15px] text-arang-900">{{ step }}</span>
            </li>
          </ol>

          <!-- QR untuk pengguna desktop yang Telegram-nya di HP -->
          <div class="hidden flex-col items-center gap-2 md:flex">
            <div class="grid h-[236px] w-[236px] place-items-center rounded-2xl border border-krem-200 p-2">
              <img
                v-if="qrDataUrl && !expired"
                :src="qrDataUrl"
                width="220"
                height="220"
                alt="QR code untuk membuka bot Telegram SakuTahu"
              />
              <span v-else class="text-sm text-arang-400">{{ loading ? 'Memuat…' : 'QR kedaluwarsa' }}</span>
            </div>
            <span class="text-xs text-arang-600">Scan dengan kamera HP</span>
          </div>
        </div>

        <div class="mt-8 flex flex-col gap-3 border-t border-krem-200 pt-6 sm:flex-row sm:items-center">
          <template v-if="!expired">
            <a
              v-if="link"
              :href="link.deepLink"
              target="_blank"
              rel="noopener"
              class="inline-flex items-center justify-center gap-2 rounded-xl bg-[#229ED9] px-5 py-3 text-[15px] font-semibold text-white shadow-sm transition hover:brightness-95"
            >
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M21.9 4.3 18.7 19.4c-.2 1-.9 1.3-1.7.8l-4.9-3.6-2.4 2.3c-.3.3-.5.5-1 .5l.4-5 9-8.2c.4-.4-.1-.6-.6-.2L6.4 13 1.6 11.5c-1-.3-1-1 .2-1.5L20.6 2.8c.9-.3 1.6.2 1.3 1.5Z" />
              </svg>
              Buka Telegram
            </a>
            <BaseButton v-else :loading="loading" disabled>Menyiapkan link…</BaseButton>
            <span v-if="link" class="flex items-center gap-2 text-sm text-arang-600" aria-live="polite">
              <span class="h-2 w-2 animate-pulse rounded-full bg-kunyit-400" aria-hidden="true" />
              Menunggu konfirmasi · link berlaku {{ countdown }}
            </span>
          </template>
          <template v-else>
            <BaseButton :loading="loading" @click="generateLink">Buat link baru</BaseButton>
            <span class="text-sm text-arang-600">Link sudah kedaluwarsa (berlaku 10 menit).</span>
          </template>
        </div>
      </section>
    </main>
  </div>
</template>
