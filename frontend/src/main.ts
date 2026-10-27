// filepath: /frontend/src/main.ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'
import { session } from './shared/api/session'
import { useAuthStore } from './modules/auth'
import './style.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

// Hubungkan shared/http dengan modul auth tanpa membuat shared bergantung ke modul
const auth = useAuthStore(pinia)
session.setRefreshHandler(() => auth.refresh())
session.setExpiredHandler(() => {
  auth.clear()
  const current = router.currentRoute.value
  if (current.meta.requiresAuth) {
    router.replace({ name: 'login', query: { redirect: current.fullPath, expired: '1' } })
  }
})

app.use(router)
app.mount('#app')
