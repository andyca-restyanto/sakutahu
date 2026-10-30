<!-- filepath: /frontend/src/modules/admin/views/AdminAccountsView.vue -->
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import ConfirmDialog from '@/shared/components/ConfirmDialog.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { toApiError } from '@/shared/api/errors'
import { formatDateTime } from '@/shared/utils/date'
import InviteAdminDialog from '../components/InviteAdminDialog.vue'
import { adminAccountService } from '../services/adminAccount.service'
import { useAdminAuthStore } from '../stores/useAdminAuthStore'
import type { AdminInvitationResponse, AdminResponse } from '../types/admin.types'

const me = useAdminAuthStore()
const admins = ref<AdminResponse[]>([])
const invitations = ref<AdminInvitationResponse[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const notice = ref<string | null>(null)

const inviteOpen = ref(false)
const statusTarget = ref<AdminResponse | null>(null)
const revokeTarget = ref<AdminInvitationResponse | null>(null)
const busy = ref(false)
const resendingId = ref<string | null>(null)

async function load(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    const [a, i] = await Promise.all([adminAccountService.list(), adminAccountService.listInvitations()])
    admins.value = a
    invitations.value = i
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    loading.value = false
  }
}

onMounted(load)

function onInvited(inv: AdminInvitationResponse): void {
  inviteOpen.value = false
  invitations.value = [inv, ...invitations.value.filter((i) => i.email !== inv.email)]
  notice.value = `Undangan dikirim ke ${inv.email}.`
}

async function confirmStatus(): Promise<void> {
  const target = statusTarget.value
  if (!target) return
  busy.value = true
  try {
    const updated = await adminAccountService.changeStatus(target.id, {
      status: target.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE',
    })
    admins.value = admins.value.map((a) => (a.id === updated.id ? updated : a))
    notice.value = updated.status === 'DISABLED' ? `${updated.fullName} dinonaktifkan dan langsung keluar.` : `${updated.fullName} diaktifkan kembali.`
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    busy.value = false
    statusTarget.value = null
  }
}

async function resend(inv: AdminInvitationResponse): Promise<void> {
  resendingId.value = inv.id
  error.value = null
  try {
    const updated = await adminAccountService.resendInvitation(inv.id)
    invitations.value = invitations.value.map((i) => (i.id === updated.id ? updated : i))
    notice.value = `Undangan baru dikirim ke ${updated.email}. Link lama tidak berlaku lagi.`
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    resendingId.value = null
  }
}

async function confirmRevoke(): Promise<void> {
  const target = revokeTarget.value
  if (!target) return
  busy.value = true
  try {
    await adminAccountService.revokeInvitation(target.id)
    invitations.value = invitations.value.filter((i) => i.id !== target.id)
    notice.value = `Undangan untuk ${target.email} dibatalkan.`
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    busy.value = false
    revokeTarget.value = null
  }
}
</script>

<template>
  <header class="flex flex-wrap items-end justify-between gap-3">
    <div>
      <h1 class="text-2xl font-extrabold tracking-tight">Admin</h1>
      <p class="text-sm text-arang-600">Hanya OWNER yang bisa mengundang dan menonaktifkan admin.</p>
    </div>
    <BaseButton @click="inviteOpen = true">+ Undang admin</BaseButton>
  </header>

  <FormAlert v-if="notice" variant="success" class="mt-4">{{ notice }}</FormAlert>
  <FormAlert v-if="error" class="mt-4">{{ error }}</FormAlert>
  <p v-if="loading" class="mt-6 text-arang-600">Memuat…</p>

  <section v-if="!loading" class="mt-6 overflow-hidden rounded-2xl border border-krem-200 bg-white">
    <ul class="divide-y divide-krem-200">
      <li v-for="a in admins" :key="a.id" class="flex flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-5">
        <div class="min-w-0">
          <p class="flex flex-wrap items-center gap-2 font-semibold">
            {{ a.fullName }}
            <span
              class="rounded-full px-2 py-0.5 text-xs font-bold"
              :class="a.role === 'OWNER' ? 'bg-kunyit-100 text-kunyit-600' : 'bg-krem-200 text-arang-600'"
            >
              {{ a.role }}
            </span>
            <span v-if="a.status === 'DISABLED'" class="rounded-full bg-red-50 px-2 py-0.5 text-xs font-bold text-red-700">Nonaktif</span>
            <span v-if="a.id === me.admin?.id" class="text-xs font-normal text-arang-400">(kamu)</span>
          </p>
          <p class="truncate text-sm text-arang-600">{{ a.email }}</p>
          <p class="text-xs text-arang-400">
            Login terakhir: {{ a.lastLoginAt ? `${formatDateTime(a.lastLoginAt)} WIB` : 'belum pernah' }}
          </p>
        </div>
        <BaseButton v-if="a.role !== 'OWNER'" variant="secondary" @click="statusTarget = a">
          {{ a.status === 'ACTIVE' ? 'Nonaktifkan' : 'Aktifkan' }}
        </BaseButton>
      </li>
    </ul>
  </section>

  <section v-if="!loading" class="mt-8">
    <h2 class="font-bold">Undangan menunggu</h2>
    <p v-if="invitations.length === 0" class="mt-2 text-sm text-arang-600">Tidak ada undangan yang menunggu.</p>
    <ul v-else class="mt-3 divide-y divide-krem-200 overflow-hidden rounded-2xl border border-krem-200 bg-white">
      <li v-for="inv in invitations" :key="inv.id" class="flex flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-5">
        <div class="min-w-0">
          <p class="font-semibold">{{ inv.fullName }}</p>
          <p class="truncate text-sm text-arang-600">{{ inv.email }}</p>
          <p class="text-xs" :class="inv.expired ? 'font-semibold text-red-700' : 'text-arang-400'">
            {{ inv.expired ? 'Kedaluwarsa' : `Berlaku s.d. ${formatDateTime(inv.expiresAt)} WIB` }}
            <template v-if="inv.invitedByName"> · diundang {{ inv.invitedByName }}</template>
          </p>
        </div>
        <div class="flex gap-2">
          <BaseButton variant="secondary" :loading="resendingId === inv.id" @click="resend(inv)">Kirim ulang</BaseButton>
          <BaseButton variant="ghost" class="text-red-700! hover:bg-red-50!" @click="revokeTarget = inv">Batalkan</BaseButton>
        </div>
      </li>
    </ul>
  </section>

  <InviteAdminDialog :open="inviteOpen" @close="inviteOpen = false" @invited="onInvited" />
  <ConfirmDialog
    :open="!!statusTarget"
    :title="statusTarget?.status === 'ACTIVE' ? 'Nonaktifkan admin?' : 'Aktifkan admin?'"
    :message="
      statusTarget?.status === 'ACTIVE'
        ? `${statusTarget?.fullName} akan langsung keluar dan tidak bisa login sampai diaktifkan lagi.`
        : `${statusTarget?.fullName} bisa login kembali ke panel admin.`
    "
    :confirm-label="statusTarget?.status === 'ACTIVE' ? 'Nonaktifkan' : 'Aktifkan'"
    :danger="statusTarget?.status === 'ACTIVE'"
    :loading="busy"
    @confirm="confirmStatus"
    @cancel="statusTarget = null"
  />
  <ConfirmDialog
    :open="!!revokeTarget"
    title="Batalkan undangan?"
    :message="`Link undangan untuk ${revokeTarget?.email ?? ''} tidak bisa dipakai lagi.`"
    confirm-label="Batalkan undangan"
    danger
    :loading="busy"
    @confirm="confirmRevoke"
    @cancel="revokeTarget = null"
  />
</template>
