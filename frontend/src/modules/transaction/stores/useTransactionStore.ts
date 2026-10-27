// filepath: /frontend/src/modules/transaction/stores/useTransactionStore.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { TransactionType } from '@/modules/category'
import type { TransactionResponse } from '../types/transaction.types'

/**
 * State global form transaksi: satu dialog dipakai dari mana saja (tombol "+ Catat", daftar transaksi).
 * `version` naik setiap ada perubahan data, sehingga dashboard & daftar tahu kapan harus memuat ulang.
 */
export const useTransactionStore = defineStore('transaction', () => {
  const dialogOpen = ref(false)
  const editing = ref<TransactionResponse | null>(null)
  const defaultType = ref<TransactionType>('EXPENSE')
  const version = ref(0)

  function openCreate(type: TransactionType = 'EXPENSE'): void {
    editing.value = null
    defaultType.value = type
    dialogOpen.value = true
  }

  function openEdit(tx: TransactionResponse): void {
    editing.value = tx
    defaultType.value = tx.type
    dialogOpen.value = true
  }

  function close(): void {
    dialogOpen.value = false
  }

  function notifyChanged(): void {
    version.value++
  }

  return { dialogOpen, editing, defaultType, version, openCreate, openEdit, close, notifyChanged }
})
