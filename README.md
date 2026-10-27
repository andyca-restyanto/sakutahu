# SakuTahu

Pencatat pengeluaran yang terintegrasi dengan Telegram (@SakuTahuBot).
Monorepo berbasis modul: **Vue 3 + TypeScript + Tailwind** (`/frontend`) dan **Java 21 + Spring Boot 3 + PostgreSQL** (`/backend`).

Fitur saat ini: **signup + verifikasi email (Gmail SMTP), login, penautan Telegram setelah login, pencatatan transaksi pemasukan/pengeluaran lewat web, dan dashboard grafik**.

## Struktur

```
backend/src/main/java/com/example/app/
├── shared/      config, security (JWT), exception, ratelimit, mail, util
└── modules/
    ├── auth/        controller · service · repository · entity · dto
    ├── telegram/    controller · service · repository · entity · dto
    ├── category/    master kategori (seed V3)
    ├── transaction/ CRUD pemasukan & pengeluaran
    └── dashboard/   agregasi baca-saja (native SQL)
backend/src/main/resources/db/migration/   V1 auth, V2 verifikasi email, V3 kategori & transaksi

frontend/src/
├── shared/      api (http, session, errors), components, utils
└── modules/
    ├── auth/      components · views · services · stores · types · index.ts
    ├── telegram/    views · services · types · index.ts
    ├── category/    store kategori (dimuat sekali)
    ├── transaction/ form, dialog global, daftar transaksi
    └── dashboard/   kartu ringkasan, donut kategori, grafik tren (vue-chartjs)
frontend/src/layouts/MainLayout.vue   navigasi + tombol "+ Catat" global
```

## Environment

| | Local (development) | Production |
|---|---|---|
| Backend profile | `local` (default) | `prod` (`SPRING_PROFILES_ACTIVE=prod`) |
| Config backend | `application-local.yml` | `application-prod.yml` |
| Secret backend | `backend/.env.local` (dari `.env.local.example`) | env var hosting, atau `backend/.env.production` (dari `.env.production.example`) |
| Config frontend | `frontend/.env.development` (`npm run dev`) | `frontend/.env.production` (`npm run build`) |
| Bot Telegram | `@SakuTahuDevBot`, webhook via ngrok | `@SakuTahuBot`, webhook via domain asli |
| Email | Mailpit (`localhost:8025`) | SMTP sungguhan, STARTTLS 587 |
| Cookie refresh | `Secure` opsional | `Secure` wajib (HTTPS) |

Profile `prod` tidak punya default untuk secret apa pun: backend gagal start jika ada variabel yang belum diisi.
Satu bot Telegram hanya bisa punya satu webhook, jadi local dan production memakai bot berbeda.
File `.env.development` dan `.env.production` di frontend aman di-commit karena variabel `VITE_*` memang publik. Jangan buat `frontend/.env.local`, karena Vite juga memuatnya saat build production.

## Menjalankan di lokal

Prasyarat: JDK 21, Maven 3.9+, Node 20+, Docker.

```bash
cp backend/.env.local.example backend/.env.local   # lalu isi bagian Telegram

docker compose up -d          # PostgreSQL + Mailpit (http://localhost:8025)

cd backend
mvn verify                    # unit test + integration test (embedded PostgreSQL)
mvn spring-boot:run           # http://localhost:8080, profile local

cd ../frontend
npm install
npm run dev                   # http://localhost:5173 (proxy /api → :8080)

ngrok http --url=<domain-ngrok> 5173   # hanya jika ingin tes Telegram
```

## Deploy production (Docker)

Arsitektur: **Caddy** (HTTPS otomatis + file frontend + proxy `/api`) → **backend** Spring Boot → **PostgreSQL**.
Semuanya berjalan lewat `docker-compose.prod.yml`; hanya port 80/443 yang terbuka ke internet.

```
deploy/
├── caddy/Caddyfile              # domain, HTTPS, header keamanan, SPA fallback, proxy /api
└── scripts/
    ├── deploy.sh                # backup → git pull → build → up → tunggu backend sehat
    ├── backup-db.sh             # pg_dump ke backups/ (simpan 14 hari)
    └── set-telegram-webhook.sh  # daftarkan webhook bot ke domain production
backend/Dockerfile               # build jar (Maven) → JRE 21 alpine, user non-root
frontend/Dockerfile              # build Vite → image Caddy berisi dist/
docker-compose.prod.yml
.env.example                     # DOMAIN, ACME_EMAIL, POSTGRES_*
.github/workflows/ci.yml         # test backend, build frontend, build image di setiap push
```

### Sebelum push pertama (di laptop)

```bash
cd frontend && npm install        # menghasilkan package-lock.json → WAJIB di-commit
cd ..
git add --renormalize .           # terapkan .gitattributes (line ending LF untuk script & config)
git status                        # pastikan TIDAK ada .env, .env.local, .env.production
git add . && git commit -m "Setup deploy production" && git push
```

### Server (sekali saja)

Kebutuhan: VPS Linux (Ubuntu 22.04/24.04), **RAM minimal 2 GB**, Docker Engine + plugin Compose,
DNS A-record domain mengarah ke IP server, port 80 & 443 terbuka.

```bash
# Install Docker (Ubuntu)
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER && newgrp docker

# Firewall
sudo ufw allow OpenSSH && sudo ufw allow 80/tcp && sudo ufw allow 443/tcp && sudo ufw allow 443/udp && sudo ufw enable

# Ambil kode
sudo mkdir -p /opt/sakutahu && sudo chown $USER /opt/sakutahu
git clone <url-repo-git> /opt/sakutahu && cd /opt/sakutahu

# Isi konfigurasi (keduanya TIDAK di-commit)
cp .env.example .env                                         # DOMAIN, ACME_EMAIL, POSTGRES_PASSWORD
cp backend/.env.production.example backend/.env.production  # JWT, Gmail, Telegram, Anthropic, APP_*_URL
nano .env && nano backend/.env.production

# Jalankan
bash deploy/scripts/deploy.sh
bash deploy/scripts/set-telegram-webhook.sh
```

Di `backend/.env.production`, `APP_FRONTEND_URL`, `APP_BACKEND_URL`, dan `APP_CORS_ORIGINS` diisi `https://<DOMAIN>`.
`DB_*` di file itu diabaikan karena koneksi database diatur compose dari `.env`.

### Update aplikasi

```bash
cd /opt/sakutahu && bash deploy/scripts/deploy.sh
```

Script otomatis backup database, `git pull`, build ulang image, dan menjalankan migrasi Flyway saat backend start.
Kalau backend tidak sehat dalam 200 detik, script berhenti dengan pesan error.

### Operasional

```bash
docker compose -f docker-compose.prod.yml ps                       # status
docker compose -f docker-compose.prod.yml logs -f --tail=200 backend
docker compose -f docker-compose.prod.yml exec postgres psql -U sakutahu sakutahu   # SQL (mis. set VIP)

# Backup harian jam 02:00 (crontab -e)
0 2 * * * cd /opt/sakutahu && bash deploy/scripts/backup-db.sh >> backups/backup.log 2>&1

# Restore (HATI-HATI: menimpa data)
gunzip -c backups/sakutahu-YYYYmmdd-HHMMSS.sql.gz | docker compose -f docker-compose.prod.yml exec -T postgres psql -U sakutahu sakutahu
```

Simpan salinan backup di luar server (mis. object storage), karena backup di disk yang sama ikut hilang bila server rusak.

### Troubleshooting

| Gejala | Penyebab umum |
|---|---|
| `Isi POSTGRES_PASSWORD di file .env` | `.env` di root belum dibuat/diisi |
| Backend restart terus, log `Could not resolve placeholder` | Variabel wajib di `backend/.env.production` kosong |
| HTTPS gagal / sertifikat tidak terbit | DNS belum mengarah ke server, atau port 80/443 tertutup |
| `bash\r: No such file or directory` | Script ter-commit dengan CRLF; jalankan `git add --renormalize .` di laptop lalu push |
| Bot Telegram diam | Jalankan ulang `set-telegram-webhook.sh`; cek `last_error_message` di output |

Tanpa Docker juga bisa: build `mvn -DskipTests package`, jalankan `SPRING_PROFILES_ACTIVE=prod java -jar target/sakutahu-backend-0.1.0.jar`
dari folder `backend` (membaca `backend/.env.production`), lalu sajikan `frontend/dist` lewat web server yang meneruskan `/api/` ke port 8080.

## Email via Gmail

1. Aktifkan **Verifikasi 2 Langkah** di akun Google pengirim.
2. Buat **App Password** di https://myaccount.google.com/apppasswords (16 karakter).
3. Isi env backend:

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_SMTP_AUTH=true      # local saja; prod selalu true
MAIL_STARTTLS=true       # local saja; prod selalu true
MAIL_USERNAME=akunkamu@gmail.com
MAIL_PASSWORD=<app-password-16-karakter>
MAIL_FROM=SakuTahu <akunkamu@gmail.com>
```

`MAIL_FROM` harus alamat Gmail yang sama (atau alias terverifikasi). Batas Gmail biasa sekitar 500 penerima/hari; untuk skala lebih besar cukup ganti nilai `MAIL_*` ke penyedia email transaksional.
Jika log menulis "Login SMTP ditolak", biasanya App Password salah atau verifikasi 2 langkah belum aktif.

## Setup bot Telegram

Login hanya lewat email + password. Telegram dihubungkan **setelah login**, dari halaman "Hubungkan Telegram" (deep link `t.me/<bot>?start=<token>` sekali pakai, berlaku 10 menit). Jadi bot tidak perlu `/setdomain`; yang dibutuhkan hanya URL publik HTTPS untuk webhook (ngrok di local, domain asli di production).

Lakukan untuk bot dev dan bot production:

1. Di @BotFather: `/newbot`, username wajib berakhiran `bot`.
2. Isi `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`, dan `TELEGRAM_WEBHOOK_SECRET` di env backend. Frontend menerima nama bot dari API, jadi tidak perlu diatur di sana.
3. Daftarkan webhook (buka di browser atau pakai curl):

```
https://api.telegram.org/bot<TOKEN>/setWebhook?url=https://<domain>/api/v1/telegram/webhook&secret_token=<TELEGRAM_WEBHOOK_SECRET>&allowed_updates=["message","callback_query"]
```

## Mencatat lewat bot Telegram

Setelah akun tertaut, kirim pesan ke bot:

| Pesan | Hasil |
|---|---|
| `kopi susu 25rb` | Pengeluaran Rp25.000 · Makan & Minum (kategori ditebak dari kata kunci) |
| `+gaji 5jt` atau `/masuk 5jt gaji` | Pemasukan Rp5.000.000 · Gaji & Tunjangan |
| `beli barang 50rb` | Kategori tidak tertebak → bot menampilkan tombol kategori (berlaku 15 menit) |
| `kemarin kopi 20rb` / `12/10` | Ditolak: catatan dari Telegram selalu untuk **hari ini** |

- Nominal: `25000`, `25.000`, `25rb`, `25k`, `1,5jt`, `Rp25.000`, `2 juta`.
- Setelah tersimpan, bot membalas konfirmasi dengan tombol **Ubah kategori** dan **Batalkan** (10 menit).
- Pilihan kategori user diingat (`user_category_keywords`), jadi kata yang sama berikutnya otomatis masuk kategori itu.
- Kata kunci bawaan ada di tabel `category_keywords` (migrasi V6); tambah lewat migrasi baru.
- Foto struk: lihat bagian [Catat dari foto struk](#catat-dari-foto-struk-telegram).
- Perintah: `/hariini`, `/kategori`, `/batal`, `/bantuan`.
- Webhook wajib didaftarkan dengan `allowed_updates=["message","callback_query"]`, kalau tidak tombol tidak berfungsi.
- Pesan yang dikirim ulang oleh Telegram tidak pernah tercatat dua kali (unique `telegram_chat_id` + `telegram_message_id`).

## Catat dari foto struk (Telegram)

1. User mengirim **foto** struk ke bot (atau gambar sebagai file JPG/PNG/WebP, maks 7 MB). Caption opsional menjadi catatan.
2. Bot langsung membalas "🧾 Membaca struk…", lalu backend membaca total belanja memakai **Claude Vision** secara asinkron.
3. Total terbaca yakin → langsung tercatat sebagai pengeluaran **🛍️ Belanja Pribadi** untuk **hari ini**, pesan bot diedit menjadi konfirmasi dengan tombol **Ubah kategori** / **Batalkan** (sama seperti pencatatan teks).
4. Total tidak jelas → bot meminta nominal (balas mis. `125rb` dalam 15 menit) atau tombol **✖️ Lewati**.
5. Bukan struk / gagal dibaca → tidak tercatat dan **tidak memotong kuota**.

- Foto yang sama dikirim ulang tidak tercatat dua kali (`receipt_scans.file_unique_id`).
- Gambar **tidak disimpan**; hanya hasil (total, merchant, tanggal struk) di `receipt_scans`.
- Wajib isi `ANTHROPIC_API_KEY` di `backend/.env.local` / `.env.production`. Model default `claude-haiku-5-5` (ubah via `RECEIPT_AI_MODEL`).
- Jika log berisi *"This API key is not scoped to a workspace … anthropic-workspace-id header"*, isi `ANTHROPIC_WORKSPACE_ID` (format `wrkspc_…`, lihat Console → Settings → Workspaces) atau buat API key yang terikat ke satu workspace.
- Tidak perlu daftar ulang webhook: foto masuk sebagai update `message`.

## Paket VIP & akses fitur

Sementara diatur manual lewat SQL (belum ada halaman admin). Perubahan `app_settings` berlaku maksimal 60 detik kemudian (cache).

```sql
-- Jadikan user VIP (tanpa batas waktu)
UPDATE users SET plan = 'VIP', plan_expires_at = NULL WHERE email = 'budi@mail.com';

-- VIP 30 hari (setelah lewat otomatis dianggap FREE)
UPDATE users SET plan = 'VIP', plan_expires_at = now() + interval '30 days' WHERE email = 'budi@mail.com';

-- Kembalikan ke FREE
UPDATE users SET plan = 'FREE', plan_expires_at = NULL WHERE email = 'budi@mail.com';

-- Fitur struk hanya untuk VIP (default)
UPDATE app_settings SET value = 'VIP_ONLY', updated_at = now() WHERE key = 'receipt_scan.access';

-- Fitur struk untuk semua user: VIP tanpa batas, FREE dibatasi kuota per bulan
UPDATE app_settings SET value = 'ALL', updated_at = now() WHERE key = 'receipt_scan.access';
UPDATE app_settings SET value = '3',   updated_at = now() WHERE key = 'receipt_scan.free_monthly_quota';

-- Cek pemakaian struk bulan ini per user
SELECT u.email, count(*) FILTER (WHERE r.status <> 'FAILED') AS terpakai
FROM receipt_scans r JOIN users u ON u.id = r.user_id
WHERE r.created_at >= date_trunc('month', now())
GROUP BY u.email ORDER BY terpakai DESC;
```

| Plan | `VIP_ONLY` | `ALL` |
|---|---|---|
| VIP | ✅ tanpa batas | ✅ tanpa batas |
| FREE | ❌ "Fitur khusus VIP ⭐" | ✅ 3 struk/bulan (reset tanggal 1, zona waktu user) |

Web menampilkan badge **⭐ VIP** di header dan status kuota di panduan Telegram (`GET /api/v1/me/features`).

## Kontrak API (`/api/v1`)

| Method | Endpoint | Auth | Response |
|---|---|---|---|
| POST | `/auth/register` | – | 201 `RegisterResponse` |
| POST | `/auth/verify-email` | – | 200 `AuthResponse` + cookie |
| POST | `/auth/resend-verification` | – | 200 `MessageResponse` |
| POST | `/auth/login` | – | 200 `AuthResponse` + cookie |
| POST | `/auth/refresh` | cookie | 200 `AuthResponse` + cookie baru |
| POST | `/auth/logout` | cookie | 204 |
| GET | `/auth/me` | Bearer | 200 `UserResponse` (termasuk `plan`) |
| GET | `/me/features` | Bearer | 200 `FeatureAccessResponse` |
| GET | `/categories?type=` | Bearer | 200 `CategoryResponse[]` |
| POST | `/transactions` | Bearer | 201 `TransactionResponse` |
| GET | `/transactions?type=&categoryId=&from=&to=&page=&size=` | Bearer | 200 `PageResponse<TransactionResponse>` |
| GET / PUT / DELETE | `/transactions/{id}` | Bearer | 200 / 200 / 204 |
| GET | `/dashboard?from=&to=` | Bearer | 200 `DashboardResponse` |
| POST | `/telegram/link-token` | Bearer | 200 `TelegramLinkResponse` |
| GET | `/telegram/status` | Bearer | 200 `TelegramStatusResponse` |
| DELETE | `/telegram/link` | Bearer | 204 |
| POST | `/telegram/webhook` | secret header | 200 |

DTO di `backend/.../modules/<modul>/dto` identik dengan `frontend/src/modules/<modul>/types`.
Kode error identik antara `ErrorCode.java` dan `shared/api/errors.ts`.

## Aturan dashboard

- `spending` = kategori `kind=SPENDING`; **Tabungan & Investasi** (`kind=SAVING`) dihitung terpisah sebagai `saving` dan tidak masuk donut pengeluaran.
- `balance` = pemasukan − pengeluaran − tabungan; `savingRate` = tabungan ÷ pemasukan.
- Tren: rentang ≤ 31 hari per hari, lebih panjang per bulan; bucket kosong diisi 0. Rentang maksimal 24 bulan.
- Pembanding: bulan penuh dibandingkan bulan penuh sebelumnya; rentang lain dengan jumlah hari yang sama.
- Transaksi memakai `occurred_on DATE` (tanggal lokal user), jadi pengelompokan tidak bergeser karena zona waktu.

## Catatan keamanan

- Password BCrypt cost 12; akun dikunci 15 menit setelah 10 kali gagal.
- Access token JWT 15 menit di memory; refresh token di cookie `HttpOnly; SameSite=Strict`, path `/api/v1/auth`, dirotasi setiap dipakai, dan pemakaian ulang token lama mencabut semua sesi.
- Semua token (refresh, verifikasi email, penautan Telegram) disimpan sebagai hash SHA-256.
- Rate limiter saat ini **in-memory per instance**. Ganti ke Redis sebelum backend di-scale ke beberapa instance.
- Di profile `prod`, cookie refresh selalu `Secure` (wajib HTTPS).
- `ANTHROPIC_API_KEY` hanya dari env, tidak pernah di-log. Foto struk dikirim ke Anthropic API untuk dibaca lalu dibuang; tidak disimpan di server.
- Baca struk dibatasi 10 foto per 10 menit per user (selain kuota bulanan), supaya biaya API tidak bocor.
