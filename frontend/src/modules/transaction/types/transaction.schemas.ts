// filepath: /frontend/src/modules/transaction/types/transaction.schemas.ts
// Disamakan dengan anotasi @Valid di TransactionRequest.java + aturan tanggal di TransactionService.
import { z } from 'zod'

export function transactionSchema(today: string) {
  return z
    .object({
      type: z.enum(['EXPENSE', 'INCOME']),
      categoryId: z.number({ required_error: 'Pilih kategori', invalid_type_error: 'Pilih kategori' }),
      amount: z
        .number({ required_error: 'Nominal wajib diisi', invalid_type_error: 'Nominal wajib diisi' })
        .positive('Nominal harus lebih dari 0')
        .max(9_999_999_999_999, 'Nominal terlalu besar'),
      occurredOn: z
        .string({ required_error: 'Tanggal wajib diisi' })
        .regex(/^\d{4}-\d{2}-\d{2}$/, 'Tanggal wajib diisi'),
      note: z.string().max(255, 'Catatan maksimal 255 karakter').optional(),
    })
    .refine((v) => v.occurredOn <= today, { path: ['occurredOn'], message: 'Tanggal tidak boleh di masa depan' })
    .refine((v) => v.occurredOn >= '2000-01-01', { path: ['occurredOn'], message: 'Tanggal terlalu lama' })
}

export type TransactionForm = z.infer<ReturnType<typeof transactionSchema>>
