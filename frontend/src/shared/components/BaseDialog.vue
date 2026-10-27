<!-- filepath: /frontend/src/shared/components/BaseDialog.vue -->
<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'

/** Modal di desktop, bottom sheet di mobile. Tutup dengan Esc, klik latar, atau tombol ×. */
const props = withDefaults(defineProps<{ open: boolean; title: string; size?: 'sm' | 'md' }>(), { size: 'md' })
const emit = defineEmits<{ close: [] }>()

const titleId = useId()
const panel = ref<HTMLElement | null>(null)
let lastFocused: HTMLElement | null = null

function onKeydown(e: KeyboardEvent): void {
  if (e.key === 'Escape') {
    e.stopPropagation()
    emit('close')
  }
}

watch(
  () => props.open,
  async (open) => {
    if (open) {
      lastFocused = document.activeElement as HTMLElement | null
      document.body.style.overflow = 'hidden'
      document.addEventListener('keydown', onKeydown)
      await nextTick()
      const first = panel.value?.querySelector<HTMLElement>('[autofocus], input, select, textarea, button:not([data-close])')
      ;(first ?? panel.value)?.focus()
    } else {
      document.body.style.overflow = ''
      document.removeEventListener('keydown', onKeydown)
      lastFocused?.focus()
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      leave-active-class="transition duration-150 ease-in"
      leave-to-class="opacity-0"
    >
      <div v-if="open" class="fixed inset-0 z-50 flex items-end justify-center bg-arang-900/40 sm:items-center sm:p-4" @click.self="emit('close')">
        <div
          ref="panel"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="titleId"
          tabindex="-1"
          class="flex max-h-[92vh] w-full flex-col rounded-t-3xl bg-white shadow-xl outline-none sm:rounded-3xl"
          :class="size === 'sm' ? 'sm:max-w-sm' : 'sm:max-w-lg'"
        >
          <div class="flex items-center justify-between gap-4 px-5 pt-5 pb-3 sm:px-6">
            <h2 :id="titleId" class="text-lg font-bold text-arang-900">{{ title }}</h2>
            <button
              type="button"
              data-close
              class="grid h-9 w-9 place-items-center rounded-full text-arang-600 hover:bg-krem-100"
              aria-label="Tutup"
              @click="emit('close')"
            >
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M6 6l12 12M18 6 6 18" stroke-linecap="round" />
              </svg>
            </button>
          </div>
          <div class="overflow-y-auto px-5 pb-5 sm:px-6 sm:pb-6">
            <slot />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
