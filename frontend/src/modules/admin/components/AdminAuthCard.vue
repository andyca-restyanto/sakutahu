<!-- filepath: /frontend/src/modules/admin/components/AdminAuthCard.vue -->
<script setup lang="ts">
/** Kartu terpusat untuk halaman admin sebelum masuk: login, terima undangan, ganti password awal. */
defineProps<{ title: string; subtitle?: string }>()
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-arang-900 px-4 py-10">
    <div class="w-full max-w-md">
      <p class="mb-6 text-center text-sm font-bold tracking-widest text-kunyit-400 uppercase">SakuTahu · Admin</p>
      <section class="rounded-3xl bg-white p-6 shadow-xl sm:p-8">
        <h1 class="text-2xl font-extrabold tracking-tight text-arang-900">{{ title }}</h1>
        <p v-if="subtitle" class="mt-1.5 text-[15px] text-arang-600">{{ subtitle }}</p>
        <div class="mt-6">
          <slot />
        </div>
      </section>
      <slot name="footer" />
    </div>
  </div>
</template>
