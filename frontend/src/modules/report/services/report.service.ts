// filepath: /frontend/src/modules/report/services/report.service.ts
import { isAxiosError } from 'axios'
import { http } from '@/shared/api/http'
import type { JournalQuery, JournalReportResponse } from '../types/report.types'

/** Buang parameter kosong agar URL bersih. */
function params(q: JournalQuery): Record<string, string | number> {
  const p: Record<string, string | number> = { from: q.from, to: q.to }
  if (q.type) p.type = q.type
  if (q.categoryId) p.categoryId = q.categoryId
  if (q.page !== undefined) p.page = q.page
  if (q.size !== undefined) p.size = q.size
  return p
}

/** attachment; filename="sakutahu-jurnal-2026-10-01_2026-10-31.xlsx" → nama file */
export function filenameFromDisposition(header: string | undefined): string | null {
  if (!header) return null
  const star = /filename\*=UTF-8''([^;]+)/i.exec(header)
  if (star) return decodeURIComponent(star[1])
  const plain = /filename="?([^";]+)"?/i.exec(header)
  return plain ? plain[1] : null
}

export const reportService = {
  async getJournal(q: JournalQuery): Promise<JournalReportResponse> {
    const { data } = await http.get<JournalReportResponse>('/reports/journal', { params: params(q) })
    return data
  },

  /**
   * Unduh Excel lewat request ber-token (access token hanya ada di memory, jadi tidak bisa memakai link biasa),
   * lalu simpan dengan link sementara.
   */
  async exportJournal(q: JournalQuery): Promise<string> {
    try {
      const res = await http.get<Blob>('/reports/journal/export', {
        params: params({ from: q.from, to: q.to, type: q.type, categoryId: q.categoryId }),
        responseType: 'blob',
        timeout: 120_000,
      })
      const filename = filenameFromDisposition(res.headers['content-disposition']) ?? `sakutahu-jurnal-${q.from}_${q.to}.xlsx`
      const url = URL.createObjectURL(res.data)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      a.remove()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
      return filename
    } catch (e) {
      // Dengan responseType blob, body error JSON ikut menjadi Blob; ubah kembali agar toApiError() bisa membacanya
      if (isAxiosError(e) && e.response?.data instanceof Blob) {
        try {
          e.response.data = JSON.parse(await e.response.data.text())
        } catch {
          // bukan JSON; biarkan apa adanya
        }
      }
      throw e
    }
  },
}
