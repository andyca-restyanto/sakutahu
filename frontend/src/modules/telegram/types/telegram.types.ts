// filepath: /frontend/src/modules/telegram/types/telegram.types.ts
// Harus match 100% dengan /backend/.../modules/telegram/dto/*.java

/** TelegramLinkResponse.java */
export interface TelegramLinkResponse {
  deepLink: string
  botUsername: string
  expiresAt: string
}

/** TelegramStatusResponse.java */
export interface TelegramStatusResponse {
  linked: boolean
  botUsername: string
  username: string | null
  linkedAt: string | null
}
