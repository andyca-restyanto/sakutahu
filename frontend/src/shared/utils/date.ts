// filepath: /frontend/src/shared/utils/date.ts
// Semua tanggal transaksi berupa string "yyyy-MM-dd" (LocalDate di backend), tanpa jam & zona waktu.

/** Hari ini sebagai "yyyy-MM-dd" menurut zona waktu user (bukan zona waktu browser). */
export function todayIn(timeZone: string | undefined): string {
  try {
    // en-CA menghasilkan format yyyy-MM-dd
    return new Intl.DateTimeFormat('en-CA', { timeZone: timeZone || undefined }).format(new Date())
  } catch {
    return new Intl.DateTimeFormat('en-CA').format(new Date())
  }
}

function parts(iso: string): [number, number, number] {
  const [y, m, d] = iso.split('-').map(Number)
  return [y, m, d]
}

function toIso(y: number, m: number, d: number): string {
  return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
}

export function daysInMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate()
}

/** Awal & akhir bulan yang berjarak `offset` bulan dari tanggal `iso` (0 = bulan itu sendiri). */
export function monthRange(iso: string, offset = 0): { from: string; to: string } {
  const [y, m] = parts(iso)
  const base = new Date(Date.UTC(y, m - 1 + offset, 1))
  const yy = base.getUTCFullYear()
  const mm = base.getUTCMonth() + 1
  return { from: toIso(yy, mm, 1), to: toIso(yy, mm, daysInMonth(yy, mm)) }
}

/** N bulan terakhir termasuk bulan berjalan: awal bulan (N-1) yang lalu s.d. akhir bulan ini. */
export function lastMonthsRange(iso: string, months: number): { from: string; to: string } {
  return { from: monthRange(iso, -(months - 1)).from, to: monthRange(iso, 0).to }
}

const longDate = new Intl.DateTimeFormat('id-ID', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' })
const shortDate = new Intl.DateTimeFormat('id-ID', { day: 'numeric', month: 'short', timeZone: 'UTC' })
const monthYear = new Intl.DateTimeFormat('id-ID', { month: 'long', year: 'numeric', timeZone: 'UTC' })
const shortMonth = new Intl.DateTimeFormat('id-ID', { month: 'short', year: '2-digit', timeZone: 'UTC' })

function asUtcDate(iso: string): Date {
  const [y, m, d] = parts(iso)
  return new Date(Date.UTC(y, m - 1, d || 1))
}

/** "2026-10-13" → "Selasa, 13 Oktober 2026" */
export function formatLongDate(iso: string): string {
  return longDate.format(asUtcDate(iso))
}

/** "2026-10-13" → "13 Okt" */
export function formatShortDate(iso: string): string {
  return shortDate.format(asUtcDate(iso))
}

/** "2026-10-01" atau "2026-10" → "Oktober 2026" */
export function formatMonthYear(iso: string): string {
  return monthYear.format(asUtcDate(iso.length === 7 ? `${iso}-01` : iso))
}

/** "2026-10" → "Okt 26" */
export function formatShortMonth(iso: string): string {
  return shortMonth.format(asUtcDate(iso.length === 7 ? `${iso}-01` : iso))
}

/** Label rentang: "1–31 Okt 2026", "1 Agu – 31 Okt 2026", "1 Des 2025 – 31 Jan 2026". */
export function formatRange(from: string, to: string): string {
  const [fy, fm] = parts(from)
  const [ty, tm, td] = parts(to)
  const [, , fd] = parts(from)
  const month = (m: number) =>
    new Intl.DateTimeFormat('id-ID', { month: 'short', timeZone: 'UTC' }).format(new Date(Date.UTC(2000, m - 1, 1)))
  if (fy === ty && fm === tm) return `${fd}–${td} ${month(tm)} ${ty}`
  if (fy === ty) return `${fd} ${month(fm)} – ${td} ${month(tm)} ${ty}`
  return `${fd} ${month(fm)} ${fy} – ${td} ${month(tm)} ${ty}`
}
