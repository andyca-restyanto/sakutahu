<!-- filepath: /frontend/src/modules/auth/components/ResendVerificationButton.vue -->
<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import { toApiError } from '@/shared/api/errors'
import { authService } from '../services/auth.service'

const props = withDefaults(
  defineProps<{ email: string; initialCooldown?: number; variant?: 'primary' | 'secondary' | 'ghost' }>(),
  { initialCooldown: 0, variant: 'secondary' },
)
const emit = defineEmits<{ sent: [message: string]; failed: [message: string] }>()

const COOLDOWN_SECONDS = 60
const remaining = ref(props.initialCooldown)
const sending = ref(false)
let timer: ReturnType<typeof setInterval> | undefined

const label = computed(() =>
  remaining.value > 0 ? `Kirim ulang dalam ${remaining.value} dtk` : 'Kirim ulang email verifikasi',
)

function startCooldown(seconds = COOLDOWN_SECONDS): void {
  remaining.value = seconds
  clearInterval(timer)
  timer = setInterval(() => {
    remaining.value -= 1
    if (remaining.value <= 0) clearInterval(timer)
  }, 1000)
}

if (props.initialCooldown > 0) startCooldown(props.initialCooldown)

async function resend(): Promise<void> {
  if (!props.email || remaining.value > 0) return
  sending.value = true
  try {
    const { message } = await authService.resendVerification({ email: props.email })
    emit('sent', message)
    startCooldown()
  } catch (e) {
    const err = toApiError(e)
    emit('failed', err.message)
    if (err.code === 'TOO_MANY_REQUESTS') startCooldown()
  } finally {
    sending.value = false
  }
}

onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <BaseButton :variant="variant" :loading="sending" :disabled="remaining > 0 || !email" block @click="resend">
    {{ label }}
  </BaseButton>
</template>
