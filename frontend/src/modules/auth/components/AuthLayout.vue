<!-- filepath: /frontend/src/modules/auth/components/AuthLayout.vue -->
<script setup lang="ts">
import BrandLogo from '@/shared/components/BrandLogo.vue'

defineProps<{ title: string; subtitle?: string }>()
</script>

<template>
  <div class="min-h-screen lg:grid lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)]">
    <!-- Panel brand (desktop) -->
    <aside class="relative hidden overflow-hidden bg-daun-600 p-12 text-white lg:flex lg:flex-col lg:justify-between">
      <BrandLogo light />

      <div class="relative z-10 max-w-md">
        <h2 class="text-4xl leading-tight font-extrabold tracking-tight">
          Catat pengeluaran semudah kirim chat.
        </h2>
        <p class="mt-4 text-lg text-daun-100">
          Ketik di Telegram, SakuTahu yang merapikan. Lihat ringkasannya kapan saja di web.
        </p>

        <!-- Ilustrasi percakapan bot -->
        <div class="mt-10 flex flex-col gap-3" aria-hidden="true">
          <div class="max-w-[75%] self-end rounded-2xl rounded-br-md bg-white px-4 py-2.5 text-[15px] text-arang-900 shadow-lg shadow-daun-900/20">
            kopi susu 25rb
          </div>
          <div class="max-w-[85%] self-start rounded-2xl rounded-bl-md bg-daun-700 px-4 py-2.5 text-[15px] shadow-lg shadow-daun-900/20">
            ✅ Tercatat: <span class="font-semibold">Rp25.000</span> · Makan &amp; Minum
            <div class="mt-1 text-xs text-daun-100">Total hari ini Rp68.000</div>
          </div>
          <div class="max-w-[75%] self-end rounded-2xl rounded-br-md bg-white px-4 py-2.5 text-[15px] text-arang-900 shadow-lg shadow-daun-900/20">
            bensin 50rb
          </div>
        </div>
      </div>

      <p class="relative z-10 text-sm text-daun-100">© {{ new Date().getFullYear() }} SakuTahu</p>

      <!-- Dekorasi -->
      <div class="pointer-events-none absolute -right-24 -bottom-24 h-80 w-80 rounded-[3rem] bg-daun-500/40 rotate-12" />
      <div class="pointer-events-none absolute top-24 -right-10 h-32 w-32 rounded-3xl bg-kunyit-400/25 -rotate-6" />
    </aside>

    <!-- Form -->
    <main class="flex min-h-screen flex-col px-4 py-8 sm:px-8 lg:min-h-0 lg:justify-center lg:px-16">
      <div class="mb-10 lg:hidden">
        <BrandLogo />
      </div>
      <div class="mx-auto w-full max-w-md">
        <h1 class="text-3xl font-extrabold tracking-tight text-arang-900">{{ title }}</h1>
        <p v-if="subtitle" class="mt-2 text-[15px] text-arang-600">{{ subtitle }}</p>
        <div class="mt-8">
          <slot />
        </div>
      </div>
    </main>
  </div>
</template>
