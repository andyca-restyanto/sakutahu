<!-- filepath: /frontend/src/modules/telegram/components/TelegramUsageGuide.vue -->
<script setup lang="ts">
/**
 * Panduan singkat mencatat transaksi lewat bot. Aturan di sini harus sama dengan
 * /backend/.../modules/telegram/service/parser/MessageParser.java & TelegramMessageFormatter.HELP.
 */
import { ReceiptScanInfo } from '@/modules/subscription'

withDefaults(defineProps<{ botUsername?: string | null; compact?: boolean }>(), { botUsername: null, compact: false })

const examples = [
  { text: 'kopi susu 25rb', result: 'Pengeluaran Rp25.000 · 🍜 Makan & Minum' },
  { text: 'bensin 30rb', result: 'Pengeluaran Rp30.000 · ⛽ Bensin' },
  { text: '+gaji 5jt', result: 'Pemasukan Rp5.000.000 · 💼 Gaji & Tunjangan' },
] as const

const commands = [
  { cmd: '/hariini', desc: 'ringkasan hari ini' },
  { cmd: '/kategori', desc: 'daftar kategori' },
  { cmd: '/batal', desc: 'hapus catatan Telegram terakhir hari ini' },
  { cmd: '/bantuan', desc: 'contoh format' },
] as const
</script>

<template>
  <section
    class="rounded-3xl bg-white text-left shadow-sm"
    :class="compact ? 'p-5' : 'p-6 sm:p-8'"
    aria-labelledby="tg-guide-title"
  >
    <div class="flex flex-wrap items-center justify-between gap-2">
      <h2 id="tg-guide-title" class="font-bold text-arang-900" :class="compact ? 'text-base' : 'text-lg'">
        Catat lewat Telegram<span v-if="botUsername" class="font-semibold text-arang-600"> · @{{ botUsername }}</span>
      </h2>
      <span class="rounded-full bg-daun-50 px-2.5 py-1 text-xs font-semibold text-daun-700">Terhubung ✅</span>
    </div>

    <p class="mt-1 text-sm text-arang-600">
      Kirim pesan seperti contoh di bawah. Kategori ditebak otomatis; kalau belum yakin, bot akan bertanya.
    </p>

    <ul class="mt-4 flex flex-col gap-2" :class="compact ? '' : 'sm:gap-3'">
      <li v-for="e in examples" :key="e.text" class="flex flex-wrap items-center gap-x-3 gap-y-1">
        <code class="rounded-lg bg-krem-100 px-2.5 py-1 font-mono text-sm font-semibold text-arang-900">{{ e.text }}</code>
        <span class="text-sm text-arang-600" aria-hidden="true">→</span>
        <span class="text-sm text-arang-600">{{ e.result }}</span>
      </li>
    </ul>

    <template v-if="!compact">
      <p class="mt-5 text-sm text-arang-600">
        Nominal bisa ditulis <strong>25000</strong>, <strong>25.000</strong>, <strong>25rb</strong>,
        <strong>25k</strong>, <strong>1,5jt</strong>, atau <strong>Rp25.000</strong>. Awali dengan <strong>+</strong>
        untuk pemasukan.
      </p>
      <dl class="mt-4 grid gap-x-4 gap-y-1.5 text-sm sm:grid-cols-[auto_1fr]">
        <template v-for="c in commands" :key="c.cmd">
          <dt><code class="font-mono font-semibold text-daun-700">{{ c.cmd }}</code></dt>
          <dd class="text-arang-600">{{ c.desc }}</dd>
        </template>
      </dl>
    </template>

    <ReceiptScanInfo />

    <p class="mt-4 rounded-xl bg-kunyit-100 px-3 py-2 text-xs font-medium text-kunyit-600">
      📅 Catatan dari Telegram selalu untuk hari ini. Transaksi tanggal lain dicatat lewat web.
    </p>
  </section>
</template>
