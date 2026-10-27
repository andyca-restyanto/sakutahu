# filepath: /frontend/Dockerfile
# Build SPA lalu sajikan dengan Caddy (HTTPS otomatis + proxy /api ke backend).
# Caddyfile dipasang dari deploy/caddy/Caddyfile lewat docker-compose.prod.yml.

# ---------- 1. Build ----------
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
# package-lock.json WAJIB di-commit agar versi dependency sama persis dengan di laptop
RUN --mount=type=cache,target=/root/.npm \
    if [ -f package-lock.json ]; then npm ci; else npm install; fi
COPY . .
# Memakai .env.production (VITE_API_BASE_URL kosong = API di domain yang sama)
RUN npm run build

# ---------- 2. Serve ----------
FROM caddy:2-alpine
COPY --from=build /app/dist /srv
