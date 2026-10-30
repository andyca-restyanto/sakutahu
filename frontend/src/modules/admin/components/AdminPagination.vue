<!-- filepath: /frontend/src/modules/admin/components/AdminPagination.vue -->
<script setup lang="ts">
const props = defineProps<{ page: number; totalPages: number; totalItems: number; size: number }>()
const emit = defineEmits<{ change: [page: number] }>()

function go(p: number): void {
  if (p >= 0 && p < props.totalPages) emit('change', p)
}
</script>

<template>
  <nav class="flex flex-wrap items-center justify-between gap-3 text-sm text-arang-600" aria-label="Halaman">
    <span>
      <template v-if="totalItems > 0">
        {{ page * size + 1 }}–{{ Math.min((page + 1) * size, totalItems) }} dari {{ totalItems }}
      </template>
      <template v-else>0 data</template>
    </span>
    <div class="flex gap-2">
      <button
        type="button"
        class="rounded-lg border border-krem-300 bg-white px-3 py-1.5 font-semibold hover:bg-krem-50 disabled:opacity-40"
        :disabled="page <= 0"
        @click="go(page - 1)"
      >
        ‹ Sebelumnya
      </button>
      <button
        type="button"
        class="rounded-lg border border-krem-300 bg-white px-3 py-1.5 font-semibold hover:bg-krem-50 disabled:opacity-40"
        :disabled="page + 1 >= totalPages"
        @click="go(page + 1)"
      >
        Berikutnya ›
      </button>
    </div>
  </nav>
</template>
