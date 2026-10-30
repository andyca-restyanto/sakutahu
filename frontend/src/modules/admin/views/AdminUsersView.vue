<!-- filepath: /frontend/src/modules/admin/views/AdminUsersView.vue -->
<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import FormAlert from '@/shared/components/FormAlert.vue'
import { toApiError } from '@/shared/api/errors'
import type { PageResponse } from '@/shared/api/page'
import { formatInstantDate } from '@/shared/utils/date'
import AdminPagination from '../components/AdminPagination.vue'
import PlanBadge from '../components/PlanBadge.vue'
import { adminUserService } from '../services/adminUser.service'
import type { AdminUserStatusFilter, AdminUserSummaryResponse, UserPlan } from '../types/admin.types'

const PAGE_SIZE = 20
const route = useRoute()
const router = useRouter()

// Filter disimpan di URL agar tombol "kembali" dari halaman detail tetap di posisi yang sama
const q = ref(typeof route.query.q === 'string' ? route.query.q : '')
const plan = ref<UserPlan | ''>(route.query.plan === 'VIP' || route.query.plan === 'FREE' ? route.query.plan : '')
const status = ref<AdminUserStatusFilter | ''>(
  route.query.status === 'ACTIVE' || route.query.status === 'BLOCKED' ? route.query.status : '',
)
const page = ref(Number(route.query.page) > 0 ? Number(route.query.page) : 0)

const data = ref<PageResponse<AdminUserSummaryResponse> | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const passwordChanged = route.query.passwordChanged === '1'
let requestSeq = 0

async function load(): Promise<void> {
  const seq = ++requestSeq
  loading.value = true
  error.value = null
  router.replace({
    query: {
      q: q.value || undefined,
      plan: plan.value || undefined,
      status: status.value || undefined,
      page: page.value || undefined,
    },
  })
  try {
    const result = await adminUserService.list({
      q: q.value.trim() || undefined,
      plan: plan.value || undefined,
      status: status.value || undefined,
      page: page.value,
      size: PAGE_SIZE,
    })
    if (seq === requestSeq) data.value = result
  } catch (e) {
    if (seq === requestSeq) error.value = toApiError(e).message
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

let debounce: ReturnType<typeof setTimeout> | undefined
watch(q, () => {
  clearTimeout(debounce)
  debounce = setTimeout(() => {
    page.value = 0
    load()
  }, 350)
})
watch([plan, status], () => {
  page.value = 0
  load()
})
onBeforeUnmount(() => clearTimeout(debounce))

function goTo(p: number): void {
  page.value = p
  load()
}

load()

const selectClass =
  'rounded-xl border border-krem-300 bg-white px-3 py-2.5 text-[15px] focus:border-daun-500 focus:ring-4 focus:ring-daun-100 focus:outline-none'
</script>

<template>
  <header class="flex flex-wrap items-end justify-between gap-3">
    <div>
      <h1 class="text-2xl font-extrabold tracking-tight">User</h1>
      <p class="text-sm text-arang-600">Cari user, ubah plan, atau blokir akun.</p>
    </div>
  </header>

  <FormAlert v-if="passwordChanged" variant="success" class="mt-4">Password berhasil diganti.</FormAlert>

  <div class="mt-5 flex flex-col gap-2 sm:flex-row">
    <input
      v-model="q"
      type="search"
      placeholder="Cari nama atau email…"
      aria-label="Cari user"
      class="flex-1 rounded-xl border border-krem-300 bg-white px-4 py-2.5 text-[15px] focus:border-daun-500 focus:ring-4 focus:ring-daun-100 focus:outline-none"
    />
    <div class="grid grid-cols-2 gap-2 sm:flex">
      <select v-model="plan" :class="selectClass" aria-label="Filter plan">
        <option value="">Semua plan</option>
        <option value="VIP">VIP</option>
        <option value="FREE">FREE</option>
      </select>
      <select v-model="status" :class="selectClass" aria-label="Filter status">
        <option value="">Semua status</option>
        <option value="ACTIVE">Aktif</option>
        <option value="BLOCKED">Diblokir</option>
      </select>
    </div>
  </div>

  <FormAlert v-if="error" class="mt-4">{{ error }}</FormAlert>

  <section class="mt-4 overflow-hidden rounded-2xl border border-krem-200 bg-white" :aria-busy="loading">
    <!-- Desktop: tabel -->
    <table class="hidden w-full text-left text-sm md:table">
      <thead class="bg-krem-50 text-xs font-semibold tracking-wide text-arang-600 uppercase">
        <tr>
          <th class="px-4 py-3">User</th>
          <th class="px-4 py-3">Plan</th>
          <th class="px-4 py-3">Telegram</th>
          <th class="px-4 py-3">Status</th>
          <th class="px-4 py-3">Terdaftar</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="u in data?.items ?? []"
          :key="u.id"
          class="cursor-pointer border-t border-krem-200 hover:bg-krem-50"
          @click="router.push({ name: 'admin-user-detail', params: { id: u.id } })"
        >
          <td class="px-4 py-3">
            <RouterLink :to="{ name: 'admin-user-detail', params: { id: u.id } }" class="font-semibold text-arang-900" @click.stop>
              {{ u.fullName }}
            </RouterLink>
            <p class="text-arang-600">{{ u.email }}</p>
          </td>
          <td class="px-4 py-3"><PlanBadge :plan="u.plan" :expires-at="u.planExpiresAt" /></td>
          <td class="px-4 py-3">{{ u.telegramLinked ? '✅' : '–' }}</td>
          <td class="px-4 py-3">
            <span v-if="u.blocked" class="rounded-full bg-red-50 px-2.5 py-0.5 text-xs font-bold text-red-700">Diblokir</span>
            <span v-else-if="!u.emailVerified" class="rounded-full bg-krem-200 px-2.5 py-0.5 text-xs font-semibold text-arang-600">
              Belum verifikasi
            </span>
            <span v-else class="text-xs font-semibold text-daun-700">Aktif</span>
          </td>
          <td class="px-4 py-3 text-arang-600">{{ formatInstantDate(u.createdAt) }}</td>
        </tr>
      </tbody>
    </table>

    <!-- Mobile: kartu -->
    <ul class="divide-y divide-krem-200 md:hidden">
      <li v-for="u in data?.items ?? []" :key="u.id">
        <RouterLink :to="{ name: 'admin-user-detail', params: { id: u.id } }" class="block px-4 py-3 hover:bg-krem-50">
          <div class="flex items-start justify-between gap-2">
            <div class="min-w-0">
              <p class="truncate font-semibold">{{ u.fullName }}</p>
              <p class="truncate text-sm text-arang-600">{{ u.email }}</p>
            </div>
            <span v-if="u.blocked" class="shrink-0 rounded-full bg-red-50 px-2.5 py-0.5 text-xs font-bold text-red-700">
              Diblokir
            </span>
          </div>
          <div class="mt-2 flex flex-wrap items-center gap-3 text-xs text-arang-600">
            <PlanBadge :plan="u.plan" :expires-at="u.planExpiresAt" />
            <span v-if="u.telegramLinked">Telegram ✅</span>
            <span>{{ formatInstantDate(u.createdAt) }}</span>
          </div>
        </RouterLink>
      </li>
    </ul>

    <p v-if="!loading && data && data.items.length === 0" class="px-4 py-10 text-center text-sm text-arang-600">
      Tidak ada user yang cocok.
    </p>
    <p v-if="loading && !data" class="px-4 py-10 text-center text-sm text-arang-600">Memuat…</p>
  </section>

  <AdminPagination
    v-if="data"
    class="mt-4"
    :page="data.page"
    :size="data.size"
    :total-items="data.totalItems"
    :total-pages="data.totalPages"
    @change="goTo"
  />
</template>
