// filepath: /frontend/src/modules/telegram/index.ts
import type { RouteRecordRaw } from 'vue-router'

export { telegramService } from './services/telegram.service'
export { default as TelegramUsageGuide } from './components/TelegramUsageGuide.vue'
export type * from './types/telegram.types'

export const telegramRoutes: RouteRecordRaw[] = [
  {
    path: '/onboarding/telegram',
    name: 'connect-telegram',
    component: () => import('./views/ConnectTelegramView.vue'),
    meta: { requiresAuth: true, title: 'Hubungkan Telegram' },
  },
]
