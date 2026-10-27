// filepath: /frontend/src/shared/utils/currency.ts

const full = new Intl.NumberFormat('id-ID', {
  style: 'currency',
  currency: 'IDR',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
})

/** 1250000 → "Rp 1.250.000" (format lengkap untuk tooltip, kartu, daftar). */
export function formatRupiah(value: number | null | undefined): string {
  return full.format(value ?? 0).replace(/ /g, ' ')
}

/** 1250000 → "Rp1,3 jt", 25000 → "Rp25 rb" (untuk sumbu grafik & ruang sempit). */
export function formatRupiahCompact(value: number): string {
  const abs = Math.abs(value)
  const sign = value < 0 ? '-' : ''
  const fmt = (n: number) => n.toLocaleString('id-ID', { maximumFractionDigits: 1 })
  if (abs >= 1_000_000_000) return `${sign}Rp${fmt(abs / 1_000_000_000)} M`
  if (abs >= 1_000_000) return `${sign}Rp${fmt(abs / 1_000_000)} jt`
  if (abs >= 1_000) return `${sign}Rp${fmt(abs / 1_000)} rb`
  return `${sign}Rp${fmt(abs)}`
}

/** "25.000" → 25000. Hanya digit yang dipakai (Rupiah tanpa desimal di input). */
export function parseRupiahInput(text: string): number | null {
  const digits = text.replace(/\D/g, '')
  return digits ? Number(digits) : null
}

/** 25000 → "25.000" (tampilan di dalam input). */
export function formatRupiahInput(value: number | null | undefined): string {
  return value == null ? '' : value.toLocaleString('id-ID', { maximumFractionDigits: 0 })
}
