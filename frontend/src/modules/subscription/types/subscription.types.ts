// filepath: /frontend/src/modules/subscription/types/subscription.types.ts
// Harus match 100% dengan /backend/.../modules/subscription/{dto,entity}/*.java
import type { UserPlan } from '@/modules/auth'

export type { UserPlan }

/** FeatureAccessMode.java — diatur admin lewat tabel app_settings */
export type FeatureAccessMode = 'ALL' | 'VIP_ONLY'

/** FeatureDenyReason.java */
export type FeatureDenyReason = 'VIP_ONLY' | 'QUOTA_EXCEEDED'

/** ReceiptScanAccess.java */
export interface ReceiptScanAccess {
  available: boolean
  access: FeatureAccessMode
  reason: FeatureDenyReason | null
  /** null = tanpa batas (VIP) */
  monthlyQuota: number | null
  usedThisMonth: number
}

/** FeatureAccessResponse.java */
export interface FeatureAccessResponse {
  plan: UserPlan
  planExpiresAt: string | null
  receiptScan: ReceiptScanAccess
}
