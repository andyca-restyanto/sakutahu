// filepath: /frontend/src/modules/dashboard/types/dashboard.types.ts
// Harus match 100% dengan /backend/.../modules/dashboard/dto/*.java
// BigDecimal → number, LocalDate → string "yyyy-MM-dd".

/** Granularity.java */
export type Granularity = 'DAY' | 'MONTH'

/** DashboardPeriod.java */
export interface DashboardPeriod {
  from: string
  to: string
  previousFrom: string
  previousTo: string
  granularity: Granularity
  currency: string
}

/** DashboardSummary.java: spending = konsumsi saja, saving = Tabungan & Investasi */
export interface DashboardSummary {
  income: number
  spending: number
  saving: number
  balance: number
  savingRate: number | null
}

/** DashboardComparison.java: % perubahan vs periode sebelumnya; null jika sebelumnya 0 */
export interface DashboardComparison {
  income: number | null
  spending: number | null
  saving: number | null
}

/** CategoryBreakdownItem.java */
export interface CategoryBreakdownItem {
  categoryId: number
  code: string
  name: string
  color: string
  icon: string
  amount: number
  percentage: number
  count: number
}

/** DashboardByCategory.java */
export interface DashboardByCategory {
  spending: CategoryBreakdownItem[]
  income: CategoryBreakdownItem[]
}

/** TrendPoint.java: bucket "yyyy-MM-dd" (DAY) atau "yyyy-MM" (MONTH) */
export interface TrendPoint {
  bucket: string
  income: number
  spending: number
  saving: number
}

/** DashboardResponse.java */
export interface DashboardResponse {
  period: DashboardPeriod
  summary: DashboardSummary
  comparison: DashboardComparison
  byCategory: DashboardByCategory
  trend: TrendPoint[]
}
