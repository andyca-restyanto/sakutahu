<!-- filepath: /frontend/src/modules/transaction/components/TransactionFormDialog.vue -->
<script setup lang="ts">
import { computed, ref } from 'vue'
import BaseDialog from '@/shared/components/BaseDialog.vue'
import ConfirmDialog from '@/shared/components/ConfirmDialog.vue'
import { toApiError } from '@/shared/api/errors'
import { formatRupiah } from '@/shared/utils/currency'
import { transactionService } from '../services/transaction.service'
import { useTransactionStore } from '../stores/useTransactionStore'
import type { TransactionResponse } from '../types/transaction.types'
import TransactionForm from './TransactionForm.vue'

/** Dialog global untuk tambah & ubah transaksi. Dipasang sekali di layout utama. */
const store = useTransactionStore()

const title = computed(() => (store.editing ? 'Ubah transaksi' : 'Catat transaksi'))
// Key baru setiap dialog dibuka agar form selalu mulai dari nilai awal
const formKey = ref(0)
const pendingDelete = ref<TransactionResponse | null>(null)
const deleting = ref(false)
const deleteError = ref<string | null>(null)

store.$onAction(({ name }) => {
  if (name === 'openCreate' || name === 'openEdit') formKey.value++
})

function onSaved(): void {
  store.notifyChanged()
  store.close()
}

async function confirmDelete(): Promise<void> {
  if (!pendingDelete.value) return
  deleting.value = true
  deleteError.value = null
  try {
    await transactionService.remove(pendingDelete.value.id)
    pendingDelete.value = null
    store.notifyChanged()
    store.close()
  } catch (e) {
    deleteError.value = toApiError(e).message
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <BaseDialog :open="store.dialogOpen && !pendingDelete" :title="title" @close="store.close()">
    <TransactionForm
      :key="formKey"
      :initial="store.editing"
      :default-type="store.defaultType"
      @saved="onSaved"
      @cancel="store.close()"
      @delete="(tx) => (pendingDelete = tx)"
    />
  </BaseDialog>

  <ConfirmDialog
    :open="!!pendingDelete"
    title="Hapus transaksi?"
    :message="
      deleteError ??
      (pendingDelete
        ? `${pendingDelete.category.name} · ${formatRupiah(pendingDelete.amount)} akan dihapus permanen.`
        : '')
    "
    confirm-label="Hapus"
    danger
    :loading="deleting"
    @confirm="confirmDelete"
    @cancel="pendingDelete = null"
  />
</template>
