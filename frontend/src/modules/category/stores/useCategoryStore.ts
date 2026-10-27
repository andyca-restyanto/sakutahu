// filepath: /frontend/src/modules/category/stores/useCategoryStore.ts
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { categoryService } from '../services/category.service'
import type { CategoryResponse, TransactionType } from '../types/category.types'

/** Kategori jarang berubah: diambil sekali per sesi lalu dipakai bersama. */
export const useCategoryStore = defineStore('category', () => {
  const items = ref<CategoryResponse[]>([])
  const loaded = ref(false)
  let inflight: Promise<void> | null = null

  const byId = computed(() => new Map(items.value.map((c) => [c.id, c])))

  function ofType(type: TransactionType): CategoryResponse[] {
    return items.value.filter((c) => c.type === type)
  }

  async function ensureLoaded(): Promise<void> {
    if (loaded.value) return
    inflight ??= categoryService
      .list()
      .then((data) => {
        items.value = data
        loaded.value = true
      })
      .finally(() => {
        inflight = null
      })
    return inflight
  }

  return { items, loaded, byId, ofType, ensureLoaded }
})
