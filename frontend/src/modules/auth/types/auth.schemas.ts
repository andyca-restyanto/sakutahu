// filepath: /frontend/src/modules/auth/types/auth.schemas.ts
// Aturan validasi client, disamakan dengan anotasi @Valid di backend (RegisterRequest/LoginRequest).
import { z } from 'zod'

const email = z
  .string({ required_error: 'Email wajib diisi' })
  .trim()
  .min(1, 'Email wajib diisi')
  .max(254, 'Email terlalu panjang')
  .email('Format email tidak valid')

export const loginSchema = z.object({
  email,
  password: z
    .string({ required_error: 'Password wajib diisi' })
    .min(1, 'Password wajib diisi')
    .max(72, 'Password terlalu panjang'),
})

export const registerSchema = z
  .object({
    fullName: z
      .string({ required_error: 'Nama wajib diisi' })
      .trim()
      .min(1, 'Nama wajib diisi')
      .max(100, 'Nama maksimal 100 karakter'),
    email,
    password: z
      .string({ required_error: 'Password wajib diisi' })
      .min(8, 'Password minimal 8 karakter')
      .max(72, 'Password maksimal 72 karakter')
      .regex(/^(?=.*[A-Za-z])(?=.*\d).+$/, 'Password harus mengandung huruf dan angka'),
    confirmPassword: z.string({ required_error: 'Ulangi password kamu' }).min(1, 'Ulangi password kamu'),
  })
  .refine((v) => v.password === v.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Password tidak sama',
  })

export const emailOnlySchema = z.object({ email })

/** Aturan password user, sama dengan RegisterRequest / ResetPasswordRequest / UpdatePasswordRequest. */
const newPassword = z
  .string({ required_error: 'Password baru wajib diisi' })
  .min(8, 'Password minimal 8 karakter')
  .max(72, 'Password maksimal 72 karakter')
  .regex(/^(?=.*[A-Za-z])(?=.*\d).+$/, 'Password harus mengandung huruf dan angka')

export const resetPasswordSchema = z
  .object({
    newPassword,
    confirmPassword: z.string({ required_error: 'Ulangi password baru' }).min(1, 'Ulangi password baru'),
  })
  .refine((v) => v.newPassword === v.confirmPassword, { path: ['confirmPassword'], message: 'Password tidak sama' })

export const changePasswordSchema = z
  .object({
    currentPassword: z
      .string({ required_error: 'Password saat ini wajib diisi' })
      .min(1, 'Password saat ini wajib diisi')
      .max(72, 'Password terlalu panjang'),
    newPassword,
    confirmPassword: z.string({ required_error: 'Ulangi password baru' }).min(1, 'Ulangi password baru'),
  })
  .refine((v) => v.newPassword === v.confirmPassword, { path: ['confirmPassword'], message: 'Password tidak sama' })
  .refine((v) => v.newPassword !== v.currentPassword, {
    path: ['newPassword'],
    message: 'Password baru harus berbeda dari password saat ini',
  })

export type LoginForm = z.infer<typeof loginSchema>
export type RegisterForm = z.infer<typeof registerSchema>
export type ResetPasswordForm = z.infer<typeof resetPasswordSchema>
export type ChangePasswordForm = z.infer<typeof changePasswordSchema>
