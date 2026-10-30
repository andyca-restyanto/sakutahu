// filepath: /frontend/src/router/route-meta.d.ts
import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    guestOnly?: boolean
    title?: string
    /** Panel admin: 'guest' (halaman login), 'public', 'auth' (wajib login admin), 'owner' (wajib OWNER). */
    admin?: 'guest' | 'public' | 'auth' | 'owner'
  }
}

export {}
