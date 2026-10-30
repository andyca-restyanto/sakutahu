<!-- filepath: /frontend/src/modules/admin/views/AdminReceiptSettingsView.vue -->
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { toApiError } from '@/shared/api/errors'
import { formatDateTime } from '@/shared/utils/date'
import StatCard from '../components/StatCard.vue'
import { adminSettingsService } from '../services/adminSettings.service'
import type { FeatureAccessMode, ReceiptScanSettingsResponse } from '../types/admin.types'

const settings = ref<ReceiptScanSettingsResponse | null>(null)
const access = ref<FeatureAccessMode>('VIP_ONLY')
const quota = ref(3)
const loading = ref(true)
const saving = ref(false)
const error = ref<string | null>(null)
const saved = ref(false)

const dirty = computed(
  () => !!settings.value && (settings.value.access !== access.value || settings.value.freeMonthlyQuota !== quota.value),
)
const quotaValid = computed(() => Number.isInteger(quota.value) && quota.value >= 0 && quota.value <= 100)

const monthLabel = computed(() => {
  if (!settings.value) return ''
  const [y, m] = settings.value.usage.month.split('-').map(Number)
  return new Intl.DateTimeFormat('id-ID', { month: 'long', year: 'numeric', timeZone: 'UTC' }).format(new Date(Date.UTC(y, m - 1, 1)))
})

function apply(s: ReceiptScanSettingsResponse): void {
  settings.value = s
  access.value = s.access
  quota.value = s.freeMonthlyQuota
}

onMounted(async () => {
  try {
    apply(await adminSettingsService.getReceiptScan())
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    loading.value = false
  }
})

async function save(): Promise<void> {
  if (!quotaValid.value) return
  saving.value = true
  error.value = null
  saved.value = false
  try {
    apply(await adminSettingsService.updateReceiptScan({ access: access.value, freeMonthlyQuota: quota.value }))
    saved.value = true
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    saving.value = false
  }
}

const options: { value: FeatureAccessMode; title: string; desc: string }[] = [
  { value: 'VIP_ONLY', title: 'Hanya VIP', desc: 'User FREE melihat "Fitur khusus VIP ⭐" dan tidak bisa membaca struk.' },
  { value: 'ALL', title: 'Semua user', desc: 'VIP tanpa batas; user FREE dibatasi kuota per bulan di bawah.' },
]
</script>

<template>
  <header>
    <h1 class="text-2xl font-extrabold tracking-tight">Kuota Struk</h1>
    <p class="text-sm text-arang-600">Atur siapa yang bisa mencatat dari foto struk di Telegram. Perubahan langsung berlaku.</p>
  </header>

  <p v-if="loading" class="mt-6 text-arang-600">Memuat…</p>
  <FormAlert v-if="error" class="mt-4">{{ error }}</FormAlert>

  <template v-if="settings">
    <form class="mt-6 rounded-2xl border border-krem-200 bg-white p-5 sm:p-6" @submit.prevent="save">
      <fieldset>
        <legend class="font-bold">Akses fitur</legend>
        <div class="mt-3 grid gap-3 sm:grid-cols-2">
          <label
            v-for="o in options"
            :key="o.value"
            class="flex cursor-pointer gap-3 rounded-xl border p-4"
            :class="access === o.value ? 'border-daun-500 bg-daun-50' : 'border-krem-300'"
          >
            <input v-model="access" type="radio" name="access" :value="o.value" class="mt-1 accent-daun-600" />
            <span>
              <span class="block font-semibold">{{ o.title }}</span>
              <span class="block text-sm text-arang-600">{{ o.desc }}</span>
            </span>
          </label>
        </div>
      </fieldset>

      <div class="mt-6 flex flex-col gap-1.5">
        <label for="quota" class="font-bold">Kuota user FREE per bulan</label>
        <div class="flex items-center gap-3">
          <input
            id="quota"
            v-model.number="quota"
            type="number"
            min="0"
            max="100"
            step="1"
            class="w-28 rounded-xl border bg-white px-4 py-2.5 text-[15px] tabular-nums focus:ring-4 focus:outline-none"
            :class="quotaValid ? 'border-krem-300 focus:border-daun-500 focus:ring-daun-100' : 'border-red-400 focus:ring-red-100'"
            :aria-invalid="!quotaValid"
          />
          <span class="text-sm text-arang-600">struk / user / bulan</span>
        </div>
        <p v-if="!quotaValid" class="text-sm text-red-600">Isi angka bulat 0–100.</p>
        <p v-else class="text-sm text-arang-600">
          {{ access === 'ALL' ? 'Struk yang gagal dibaca tidak memotong kuota. Reset setiap tanggal 1.' : 'Hanya dipakai saat akses "Semua user".' }}
        </p>
      </div>

      <div class="mt-6 flex flex-wrap items-center gap-3">
        <BaseButton type="submit" :loading="saving" :disabled="!dirty || !quotaValid">Simpan</BaseButton>
        <span v-if="saved && !dirty" class="text-sm font-semibold text-daun-700" role="status">Tersimpan ✓</span>
        <span v-if="settings.updatedAt" class="text-xs text-arang-400">Terakhir diubah {{ formatDateTime(settings.updatedAt) }} WIB</span>
      </div>
    </form>

    <section class="mt-8">
      <h2 class="font-bold">Pemakaian {{ monthLabel }}</h2>
      <div class="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total struk dikirim" :value="settings.usage.totalScans" />
        <StatCard label="Berhasil dicatat" :value="settings.usage.successScans" tone="good" />
        <StatCard label="Gagal dibaca" :value="settings.usage.failedScans" :tone="settings.usage.failedScans ? 'bad' : 'default'" hint="Tidak memotong kuota" />
        <StatCard
          label="User FREE kuota habis"
          :value="settings.access === 'ALL' ? settings.usage.usersAtQuota : '–'"
          :tone="settings.usage.usersAtQuota ? 'warn' : 'default'"
          :hint="settings.access === 'ALL' ? undefined : 'Akses hanya VIP'"
        />
      </div>
    </section>
  </template>
</template>
