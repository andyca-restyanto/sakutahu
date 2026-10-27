// filepath: /frontend/src/shared/utils/format.ts

/** budi@mail.com → bu***@mail.com */
export function maskEmail(email: string): string {
  const at = email.indexOf('@')
  if (at <= 0) return email
  const name = email.slice(0, at)
  const visible = name.slice(0, Math.min(2, name.length))
  return `${visible}***${email.slice(at)}`
}

/** Zona waktu browser, fallback Asia/Jakarta. */
export function detectTimezone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Jakarta'
  } catch {
    return 'Asia/Jakarta'
  }
}

export type PasswordStrength = { score: 0 | 1 | 2 | 3 | 4; label: string }

export function passwordStrength(password: string): PasswordStrength {
  if (!password) return { score: 0, label: '' }
  let score = 0
  if (password.length >= 8) score++
  if (password.length >= 12) score++
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score++
  if (/\d/.test(password) && /[^A-Za-z0-9]/.test(password)) score++
  const labels = ['Terlalu lemah', 'Lemah', 'Cukup', 'Kuat', 'Sangat kuat'] as const
  const clamped = Math.min(score, 4) as PasswordStrength['score']
  return { score: clamped, label: labels[clamped] }
}
