<!-- filepath: /frontend/src/modules/admin/components/AdminLayout.vue -->
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from '../stores/useAdminAuthStore'

const admin = useAdminAuthStore()
const router = useRouter()
const route = useRoute()
const loggingOut = ref(false)

const nav = computed(() => [
  { to: { name: 'admin-users' }, label: 'User', icon: 'M16 19v-1a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v1M9 10a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm13 9v-1a4 4 0 0 0-3-3.9M16 2.1a4 4 0 0 1 0 7.8' },
  { to: { name: 'admin-receipt-settings' }, label: 'Kuota Struk', icon: 'M5 3h14v18l-3-2-2 2-2-2-2 2-2-2-3 2V3Zm4 5h6M9 12h6' },
  ...(admin.isOwner
    ? [{ to: { name: 'admin-accounts' }, label: 'Admin', icon: 'M12 3l8 4v5c0 5-3.5 8-8 9-4.5-1-8-4-8-9V7l8-4Z' }]
    : []),
])

// Sesi berakhir (refresh gagal / admin dinonaktifkan) → kembali ke login admin
watch(
  () => admin.isAuthenticated,
  (ok) => {
    if (!ok) router.replace({ name: 'admin-login', query: { redirect: route.fullPath, expired: admin.expired ? '1' : undefined } })
  },
)

async function logout(): Promise<void> {
  loggingOut.value = true
  try {
    await admin.logout()
  } finally {
    loggingOut.value = false
  }
}
</script>

<template>
  <div class="min-h-screen lg:grid lg:grid-cols-[15rem_minmax(0,1fr)]">
    <aside class="bg-arang-900 text-white lg:sticky lg:top-0 lg:h-screen">
      <div class="flex items-center justify-between gap-3 px-4 py-4 lg:block lg:px-5 lg:py-6">
        <p class="text-sm font-bold tracking-widest text-kunyit-400 uppercase">SakuTahu · Admin</p>
        <div class="flex gap-1 lg:hidden">
          <RouterLink
            :to="{ name: 'admin-change-password' }"
            class="rounded-lg px-3 py-1.5 text-sm font-semibold text-krem-200 hover:bg-white/10"
          >
            Password
          </RouterLink>
          <button
            type="button"
            class="rounded-lg px-3 py-1.5 text-sm font-semibold text-krem-200 hover:bg-white/10"
            :disabled="loggingOut"
            @click="logout"
          >
            Keluar
          </button>
        </div>
      </div>
      <nav class="flex gap-1 overflow-x-auto px-3 pb-3 lg:flex-col lg:px-3" aria-label="Menu admin">
        <RouterLink
          v-for="item in nav"
          :key="item.label"
          :to="item.to"
          class="flex shrink-0 items-center gap-3 rounded-xl px-3 py-2.5 text-[15px] font-semibold text-krem-200 hover:bg-white/10"
          active-class="bg-white/15! text-white!"
        >
          <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path :d="item.icon" />
          </svg>
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="hidden border-t border-white/10 px-5 py-5 lg:absolute lg:inset-x-0 lg:bottom-0 lg:block">
        <p class="truncate text-sm font-semibold">{{ admin.admin?.fullName }}</p>
        <p class="truncate text-xs text-krem-300">{{ admin.admin?.email }}</p>
        <span class="mt-2 inline-block rounded-full bg-white/10 px-2 py-0.5 text-xs font-bold text-kunyit-400">
          {{ admin.admin?.role }}
        </span>
        <div class="mt-4 flex flex-col gap-1">
          <RouterLink :to="{ name: 'admin-change-password' }" class="text-sm text-krem-200 hover:text-white">
            Ganti password
          </RouterLink>
          <button type="button" class="text-left text-sm text-krem-200 hover:text-white" :disabled="loggingOut" @click="logout">
            Keluar
          </button>
        </div>
      </div>
    </aside>
    <main class="mx-auto w-full max-w-6xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
      <RouterView />
    </main>
  </div>
</template>
