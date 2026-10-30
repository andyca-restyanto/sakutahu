<!-- filepath: /frontend/src/modules/admin/views/AdminUserDetailView.vue -->
<script setup lang="ts">
import { ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import BaseButton from '@/shared/components/BaseButton.vue'
import ConfirmDialog from '@/shared/components/ConfirmDialog.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { toApiError } from '@/shared/api/errors'
import { formatDateTime } from '@/shared/utils/date'
import AuditHistory from '../components/AuditHistory.vue'
import BlockUserDialog from '../components/BlockUserDialog.vue'
import ChangePlanDialog from '../components/ChangePlanDialog.vue'
import PlanBadge from '../components/PlanBadge.vue'
import StatCard from '../components/StatCard.vue'
import { adminUserService } from '../services/adminUser.service'
import type { AdminUserDetailResponse } from '../types/admin.types'

const route = useRoute()
const user = ref<AdminUserDetailResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const notice = ref<string | null>(null)

const planOpen = ref(false)
const blockOpen = ref(false)
const unblockOpen = ref(false)
const unblocking = ref(false)

async function load(id: string): Promise<void> {
  loading.value = true
  error.value = null
  try {
    user.value = await adminUserService.get(id)
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id as string, (id) => id && load(id), { immediate: true })

function onSaved(updated: AdminUserDetailResponse, message: string): void {
  user.value = updated
  planOpen.value = false
  blockOpen.value = false
  notice.value = message
}

async function unblock(): Promise<void> {
  if (!user.value) return
  unblocking.value = true
  try {
    onSaved(await adminUserService.unblock(user.value.id), 'Blokir dibuka. User bisa login dan mencatat lagi.')
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    unblocking.value = false
    unblockOpen.value = false
  }
}
</script>

<template>
  <RouterLink :to="{ name: 'admin-users' }" class="text-sm font-semibold text-daun-700 hover:underline">‹ Semua user</RouterLink>

  <p v-if="loading && !user" class="mt-6 text-arang-600">Memuat…</p>
  <FormAlert v-if="error" class="mt-4">{{ error }}</FormAlert>

  <template v-if="user">
    <header class="mt-3 flex flex-wrap items-start justify-between gap-3">
      <div class="min-w-0">
        <h1 class="truncate text-2xl font-extrabold tracking-tight">{{ user.fullName }}</h1>
        <p class="truncate text-arang-600">{{ user.email }}</p>
        <div class="mt-2 flex flex-wrap items-center gap-2">
          <PlanBadge :plan="user.plan" :expires-at="user.planExpiresAt" />
          <span v-if="user.blocked" class="rounded-full bg-red-50 px-2.5 py-0.5 text-xs font-bold text-red-700">Diblokir</span>
          <span v-if="!user.emailVerified" class="rounded-full bg-krem-200 px-2.5 py-0.5 text-xs font-semibold text-arang-600">
            Email belum diverifikasi
          </span>
        </div>
      </div>
      <div class="flex flex-wrap gap-2">
        <BaseButton variant="secondary" @click="planOpen = true">Ubah plan</BaseButton>
        <BaseButton v-if="user.blocked" variant="secondary" @click="unblockOpen = true">Buka blokir</BaseButton>
        <BaseButton v-else class="bg-red-600! hover:bg-red-700!" @click="blockOpen = true">Blokir</BaseButton>
      </div>
    </header>

    <FormAlert v-if="notice" variant="success" class="mt-4">{{ notice }}</FormAlert>
    <FormAlert v-if="user.blocked" variant="error" class="mt-4">
      Diblokir sejak {{ formatDateTime(user.blockedAt) }} WIB. Alasan: {{ user.blockedReason }}
    </FormAlert>

    <div class="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <StatCard label="Transaksi tercatat" :value="user.transactionCount.toLocaleString('id-ID')" />
      <StatCard label="Struk bulan ini" :value="user.receiptScansThisMonth" hint="Tidak termasuk yang gagal dibaca" />
      <StatCard label="Telegram" :value="user.telegramLinked ? 'Terhubung' : 'Belum'" :hint="user.telegramUsername ? `@${user.telegramUsername}` : undefined" />
      <StatCard label="Terdaftar" :value="formatDateTime(user.createdAt).split(',')[0]" :hint="`Zona waktu ${user.timezone}`" />
    </div>

    <section class="mt-6 rounded-2xl border border-krem-200 bg-white p-5">
      <h2 class="font-bold">Riwayat perubahan oleh admin</h2>
      <div class="mt-4"><AuditHistory :entries="user.history" /></div>
    </section>

    <p class="mt-4 text-xs text-arang-400">Isi transaksi user tidak ditampilkan di panel admin untuk menjaga privasi.</p>

    <ChangePlanDialog :open="planOpen" :user="user" @close="planOpen = false" @saved="(u) => onSaved(u, 'Plan berhasil diubah.')" />
    <BlockUserDialog :open="blockOpen" :user="user" @close="blockOpen = false" @saved="(u) => onSaved(u, 'User diblokir.')" />
    <ConfirmDialog
      :open="unblockOpen"
      title="Buka blokir?"
      :message="`${user.fullName} akan bisa login dan mencatat lewat Telegram lagi.`"
      confirm-label="Buka blokir"
      :loading="unblocking"
      @confirm="unblock"
      @cancel="unblockOpen = false"
    />
  </template>
</template>
