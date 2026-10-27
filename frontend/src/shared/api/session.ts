// filepath: /frontend/src/shared/api/session.ts
/**
 * Penyimpanan access token di memory (bukan localStorage, agar tidak bisa dicuri lewat XSS
 * yang membaca storage). Modul auth mendaftarkan handler refresh & expired di sini,
 * sehingga shared/http tidak perlu bergantung pada modul mana pun.
 */
type RefreshHandler = () => Promise<string | null>
type ExpiredHandler = () => void

let accessToken: string | null = null
let refreshHandler: RefreshHandler | null = null
let expiredHandler: ExpiredHandler | null = null

export const session = {
  getToken: (): string | null => accessToken,
  setToken: (token: string | null): void => {
    accessToken = token
  },
  setRefreshHandler: (handler: RefreshHandler): void => {
    refreshHandler = handler
  },
  setExpiredHandler: (handler: ExpiredHandler): void => {
    expiredHandler = handler
  },
  refresh: (): Promise<string | null> => (refreshHandler ? refreshHandler() : Promise.resolve(null)),
  expire: (): void => {
    accessToken = null
    expiredHandler?.()
  },
}
