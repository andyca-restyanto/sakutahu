// filepath: /frontend/src/modules/report/types/report.types.ts
// Harus match 100% dengan /backend/.../modules/report/dto/*.java
// BigDecimal → number; LocalDate → "yyyy-MM-dd"; Instant/UUID → string.
import type { CategoryKind, CategorySummaryResponse, TransactionType } from '@/modules/category'
import type { TransactionSource } from '@/modules/transaction'
import type { PageResponse } from '@/shared/api/page'

/** JournalSummaryResponse.java — saldo null jika laporan difilter; totalExpense tanpa tabungan */
export interface JournalSummaryResponse {
  openingBalance: number | null
  totalIncome: number
  totalExpense: number
  totalSaving: number
  closingBalance: number | null
  entryCount: number
}

/** JournalEntryResponse.java — income = Masuk, expense = Keluar (termasuk tabungan) */
export interface JournalEntryResponse {
  id: string
  occurredOn: string
  description: string
  category: CategorySummaryResponse
  type: TransactionType
  source: TransactionSource
  receiptScanId: string | null
  income: number
  expense: number
  balance: number | null
  createdAt: string
}

/** JournalReportResponse.java */
export interface JournalReportResponse {
  from: string
  to: string
  filtered: boolean
  summary: JournalSummaryResponse
  entries: PageResponse<JournalEntryResponse>
}

/** CategoryRecapResponse.java (hanya dipakai di Excel; disertakan agar kontrak lengkap) */
export interface CategoryRecapResponse {
  categoryId: number
  name: string
  icon: string
  kind: CategoryKind
  count: number
  total: number
  share: number
}

/** Query param GET /reports/journal & /reports/journal/export */
export interface JournalQuery {
  from: string
  to: string
  type?: TransactionType
  categoryId?: number
  page?: number
  size?: number
}
