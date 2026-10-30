// filepath: /frontend/src/modules/admin/types/admin.schemas.ts
// Disamakan dengan anotasi @Valid di backend (modules/admin/dto).
import { z } from 'zod'

const email = z
  .string({ required_error: 'Email wajib diisi' })
  .trim()
  .min(1, 'Email wajib diisi')
  .max(254, 'Email terlalu panjang')
  .email('Format email tidak valid')

/** Password admin: 12-72 karakter, huruf + angka (lebih ketat dari password user). */
export const adminPassword = z
  .string({ required_error: 'Password wajib diisi' })
  .min(12, 'Password admin minimal 12 karakter')
  .max(72, 'Password maksimal 72 karakter')
  .regex(/^(?=.*[A-Za-z])(?=.*\d).+$/, 'Password harus mengandung huruf dan angka')

export const adminLoginSchema = z.object({
  email,
  password: z.string({ required_error: 'Password wajib diisi' }).min(1, 'Password wajib diisi').max(72),
})

export const changePasswordSchema = z
  .object({
    currentPassword: z.string({ required_error: 'Password saat ini wajib diisi' }).min(1, 'Password saat ini wajib diisi'),
    newPassword: adminPassword,
    confirmPassword: z.string({ required_error: 'Ulangi password baru' }).min(1, 'Ulangi password baru'),
  })
  .refine((v) => v.newPassword === v.confirmPassword, { message: 'Password tidak sama', path: ['confirmPassword'] })
  .refine((v) => v.newPassword !== v.currentPassword, {
    message: 'Password baru harus berbeda dari password saat ini',
    path: ['newPassword'],
  })

export const acceptInvitationSchema = z
  .object({
    password: adminPassword,
    confirmPassword: z.string({ required_error: 'Ulangi password' }).min(1, 'Ulangi password'),
  })
  .refine((v) => v.password === v.confirmPassword, { message: 'Password tidak sama', path: ['confirmPassword'] })

export const inviteAdminSchema = z.object({
  fullName: z.string({ required_error: 'Nama wajib diisi' }).trim().min(1, 'Nama wajib diisi').max(100, 'Nama maksimal 100 karakter'),
  email,
})

export type AdminLoginForm = z.infer<typeof adminLoginSchema>
export type ChangePasswordForm = z.infer<typeof changePasswordSchema>
export type AcceptInvitationForm = z.infer<typeof acceptInvitationSchema>
export type InviteAdminForm = z.infer<typeof inviteAdminSchema>
