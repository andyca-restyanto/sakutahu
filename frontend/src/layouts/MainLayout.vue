<!-- filepath: /frontend/src/layouts/MainLayout.vue -->
<!--
  Layout aplikasi setelah login. Berada di level app (bukan shared) karena merangkai beberapa modul:
  navigasi, tombol "+ Catat" global, dan dialog transaksi.
-->
<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import BrandLogo from '@/shared/components/BrandLogo.vue'
import { useAuthStore } from '@/modules/auth'
import { TransactionFormDialog, useTransactionStore } from '@/modules/transaction'

const auth = useAuthStore()
const txStore = useTransactionStore()
const router = useRouter()
const loggingOut = ref(false)

const nav = [
  { to: { name: 'dashboard' }, label: 'Dashboard', icon: 'M3 13h8V3H3v10Zm10 8h8V11h-8v10ZM3 21h8v-6H3v6Zm10-18v6h8V3h-8Z' },
  { to: { name: 'transactions' }, label: 'Transaksi', icon: 'M4 6h16M4 12h16M4 18h10' },
] as const

async function logout(): Promise<void> {
  loggingOut.value = true
  try {
    await auth.logout()
  } finally {
    loggingOut.value = false
    router.replace({ name: 'login' })
  }
}
</script>

<template>
  <div class="min-h-screen pb-24 sm:pb-10">
    <header class="sticky top-0 z-30 border-b border-krem-200 bg-krem-100/90 backdrop-blur">
      <div class="mx-auto flex h-16 max-w-6xl items-center gap-6 px-4 sm:px-8">
        <RouterLink :to="{ name: 'dashboard' }" aria-label="SakuTahu, ke dashboard">
          <BrandLogo />
        </RouterLink>

        <nav class="hidden items-center gap-1 sm:flex" aria-label="Navigasi utama">
          <RouterLink
            v-for="item in nav"
            :key="item.label"
            :to="item.to"
            class="rounded-lg px-3 py-2 text-[15px] font-semibold text-arang-600 hover:bg-krem-200 hover:text-arang-900"
            active-class="text-daun-700! bg-white shadow-sm"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <div class="ml-auto flex items-center gap-2">
          <button
            type="button"
            class="hidden rounded-xl bg-daun-600 px-4 py-2 text-[15px] font-semibold text-white shadow-sm hover:bg-daun-700 sm:inline-flex"
            @click="txStore.openCreate()"
          >
            + Catat
          </button>
          <span
            v-if="auth.user?.plan === 'VIP'"
            class="rounded-full bg-kunyit-100 px-2.5 py-1 text-xs font-bold text-kunyit-600"
            title="Akun VIP"
          >
            ⭐ VIP
          </span>
          <RouterLink
            :to="{ name: 'account' }"
            class="flex items-center gap-2 rounded-lg px-2 py-2 text-sm font-semibold text-arang-600 hover:bg-krem-200 hover:text-arang-900"
            active-class="bg-white text-daun-700! shadow-sm"
            aria-label="Akun"
          >
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <circle cx="12" cy="8" r="4" />
              <path d="M4 21a8 8 0 0 1 16 0" stroke-linecap="round" />
            </svg>
            <span class="hidden max-w-[10rem] truncate lg:inline">{{ auth.user?.fullName }}</span>
          </RouterLink>
          <button
            type="button"
            class="rounded-lg px-3 py-2 text-sm font-semibold text-arang-600 hover:bg-krem-200 hover:text-arang-900 disabled:opacity-50"
            :disabled="loggingOut"
            @click="logout"
          >
            Keluar
          </button>
        </div>
      </div>
    </header>

    <main class="mx-auto max-w-6xl px-4 py-8 sm:px-8">
      <RouterView />
    </main>

    <!-- Tab bar mobile -->
    <nav
      class="fixed inset-x-0 bottom-0 z-30 border-t border-krem-200 bg-white/95 backdrop-blur sm:hidden"
      aria-label="Navigasi utama"
    >
      <div class="mx-auto grid max-w-md grid-cols-3 items-center px-4 pt-2 pb-[max(0.5rem,env(safe-area-inset-bottom))]">
        <RouterLink
          :to="nav[0].to"
          class="flex flex-col items-center gap-0.5 py-1 text-xs font-semibold text-arang-400"
          active-class="text-daun-700!"
        >
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
            <path :d="nav[0].icon" stroke-linejoin="round" />
          </svg>
          {{ nav[0].label }}
        </RouterLink>
        <button
          type="button"
          class="mx-auto -mt-7 grid h-14 w-14 place-items-center rounded-2xl bg-daun-600 text-3xl font-light text-white shadow-lg shadow-daun-900/25"
          aria-label="Catat transaksi"
          @click="txStore.openCreate()"
        >
          +
        </button>
        <RouterLink
          :to="nav[1].to"
          class="flex flex-col items-center gap-0.5 py-1 text-xs font-semibold text-arang-400"
          active-class="text-daun-700!"
        >
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
            <path :d="nav[1].icon" stroke-linecap="round" />
          </svg>
          {{ nav[1].label }}
        </RouterLink>
      </div>
    </nav>

    <TransactionFormDialog />
  </div>
</template>
