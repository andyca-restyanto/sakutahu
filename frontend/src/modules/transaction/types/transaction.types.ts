// filepath: /frontend/src/modules/transaction/types/transaction.types.ts
// Harus match 100% dengan /backend/.../modules/transaction/{dto,entity}/*.java
// BigDecimal → number, LocalDate → string "yyyy-MM-dd", Instant → string ISO, UUID → string.
import type { CategorySummaryResponse, TransactionType } from '@/modules/category'

/** TransactionSource.java */
export type TransactionSource = 'WEB' | 'TELEGRAM'

/** TransactionRequest.java (body POST & PUT) */
export interface TransactionRequest {
  type: TransactionType
  categoryId: number
  amount: number
  occurredOn: string
  note?: string | null
}

/** TransactionResponse.java */
export interface TransactionResponse {
  id: string
  type: TransactionType
  category: CategorySummaryResponse
  amount: number
  occurredOn: string
  note: string | null
  source: TransactionSource
  /** Terisi bila dicatat dari foto struk di Telegram */
  receiptScanId: string | null
  createdAt: string
}

/** Query param GET /transactions */
export interface TransactionListQuery {
  type?: TransactionType
  categoryId?: number
  from?: string
  to?: string
  page?: number
  size?: number
}
