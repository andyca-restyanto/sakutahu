// filepath: /frontend/src/modules/category/types/category.types.ts
// Harus match 100% dengan /backend/.../modules/category/{dto,entity}/*.java

/** TransactionType.java */
export type TransactionType = 'EXPENSE' | 'INCOME'

/** CategoryKind.java: SAVING = Tabungan & Investasi (dipisah dari pengeluaran di dashboard) */
export type CategoryKind = 'SPENDING' | 'SAVING' | 'INCOME'

/** CategoryResponse.java */
export interface CategoryResponse {
  id: number
  code: string
  name: string
  type: TransactionType
  kind: CategoryKind
  color: string
  icon: string
  sortOrder: number
}

/** CategorySummaryResponse.java */
export interface CategorySummaryResponse {
  id: number
  code: string
  name: string
  kind: CategoryKind
  color: string
  icon: string
}
