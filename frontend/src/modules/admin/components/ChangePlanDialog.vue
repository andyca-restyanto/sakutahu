<!-- filepath: /frontend/src/modules/admin/components/ChangePlanDialog.vue -->
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import BaseDialog from '@/shared/components/BaseDialog.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { toApiError } from '@/shared/api/errors'
import { todayIn } from '@/shared/utils/date'
import { adminUserService } from '../services/adminUser.service'
import type { AdminUserDetailResponse, UserPlan } from '../types/admin.types'

const props = defineProps<{ open: boolean; user: AdminUserDetailResponse }>()
const emit = defineEmits<{ close: []; saved: [user: AdminUserDetailResponse] }>()

const ZONE = 'Asia/Jakarta'
const plan = ref<UserPlan>('FREE')
const unlimited = ref(true)
const until = ref('')
const saving = ref(false)
const error = ref<string | null>(null)

/** "yyyy-MM-dd" + n hari, dihitung dari hari ini di WIB. */
function addDays(days: number): string {
  const [y, m, d] = todayIn(ZONE).split('-').map(Number)
  const date = new Date(Date.UTC(y, m - 1, d + days))
  return date.toISOString().slice(0, 10)
}

const minDate = computed(() => addDays(1))

watch(
  () => props.open,
  (open) => {
    if (!open) return
    error.value = null
    plan.value = props.user.plan
    unlimited.value = !props.user.planExpiresAt
    until.value = props.user.planExpiresAt
      ? new Intl.DateTimeFormat('en-CA', { timeZone: ZONE }).format(new Date(props.user.planExpiresAt))
      : addDays(30)
  },
  { immediate: true },
)

/** VIP berlaku sampai akhir hari yang dipilih (23:59:59 WIB). */
function expiresAtIso(): string | null {
  if (plan.value !== 'VIP' || unlimited.value) return null
  return new Date(`${until.value}T23:59:59+07:00`).toISOString()
}

async function save(): Promise<void> {
  error.value = null
  if (plan.value === 'VIP' && !unlimited.value && (!until.value || until.value < minDate.value)) {
    error.value = 'Pilih tanggal berakhir mulai besok.'
    return
  }
  saving.value = true
  try {
    const updated = await adminUserService.changePlan(props.user.id, { plan: plan.value, planExpiresAt: expiresAtIso() })
    emit('saved', updated)
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    saving.value = false
  }
}

const presets = [30, 90, 365] as const
const planOptions: UserPlan[] = ['FREE', 'VIP']
</script>

<template>
  <BaseDialog :open="open" title="Ubah plan" @close="emit('close')">
    <form class="flex flex-col gap-5" @submit.prevent="save">
      <FormAlert v-if="error">{{ error }}</FormAlert>
      <p class="text-sm text-arang-600">{{ user.fullName }} · {{ user.email }}</p>

      <fieldset class="grid grid-cols-2 gap-2">
        <legend class="mb-2 text-sm font-semibold text-arang-900">Plan</legend>
        <label
          v-for="p in planOptions"
          :key="p"
          class="flex cursor-pointer items-center gap-2 rounded-xl border px-4 py-3 text-[15px] font-semibold"
          :class="plan === p ? 'border-daun-500 bg-daun-50 text-daun-700' : 'border-krem-300 bg-white text-arang-900'"
        >
          <input v-model="plan" type="radio" name="plan" :value="p" class="accent-daun-600" />
          {{ p === 'VIP' ? '⭐ VIP' : 'FREE' }}
        </label>
      </fieldset>

      <fieldset v-if="plan === 'VIP'" class="flex flex-col gap-3">
        <legend class="mb-2 text-sm font-semibold text-arang-900">Masa berlaku</legend>
        <label class="flex items-center gap-2 text-[15px]">
          <input v-model="unlimited" type="radio" :value="true" name="duration" class="accent-daun-600" />
          Tanpa batas waktu
        </label>
        <label class="flex items-center gap-2 text-[15px]">
          <input v-model="unlimited" type="radio" :value="false" name="duration" class="accent-daun-600" />
          Sampai tanggal
        </label>
        <div v-if="!unlimited" class="flex flex-col gap-2 pl-6">
          <input
            v-model="until"
            type="date"
            :min="minDate"
            class="w-full rounded-xl border border-krem-300 bg-white px-4 py-2.5 text-[15px] focus:border-daun-500 focus:ring-4 focus:ring-daun-100 focus:outline-none sm:w-56"
            aria-label="Tanggal berakhir VIP"
          />
          <div class="flex flex-wrap gap-2">
            <button
              v-for="d in presets"
              :key="d"
              type="button"
              class="rounded-full border border-krem-300 px-3 py-1 text-xs font-semibold text-arang-600 hover:bg-krem-50"
              @click="until = addDays(d)"
            >
              +{{ d }} hari
            </button>
          </div>
          <p class="text-xs text-arang-600">Berakhir pukul 23.59 WIB. Setelah itu otomatis kembali FREE.</p>
        </div>
      </fieldset>

      <div class="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <BaseButton variant="secondary" @click="emit('close')">Batal</BaseButton>
        <BaseButton type="submit" :loading="saving">Simpan plan</BaseButton>
      </div>
    </form>
  </BaseDialog>
</template>
