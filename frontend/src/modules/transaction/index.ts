// filepath: /frontend/src/modules/transaction/index.ts
import type { RouteRecordRaw } from 'vue-router'

export { useTransactionStore } from './stores/useTransactionStore'
export { transactionService } from './services/transaction.service'
export { default as TransactionFormDialog } from './components/TransactionFormDialog.vue'
export type * from './types/transaction.types'

/** Dipasang sebagai child dari layout utama (lihat /frontend/src/router/index.ts). */
export const transactionRoutes: RouteRecordRaw[] = [
  {
    path: '/transactions',
    name: 'transactions',
    component: () => import('./views/TransactionsView.vue'),
    meta: { requiresAuth: true, title: 'Transaksi' },
  },
]
