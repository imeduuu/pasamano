# syntax=docker/dockerfile:1
# Imagen multi-etapa del frontend: compila la SPA con Node LTS y la sirve con nginx.

# ---------- Etapa 1: compilación ----------
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# ---------- Etapa 2: servidor estático ----------
FROM nginx:1.29-alpine
# BACKEND_URL: destino del proxy /api (en docker compose, el servicio backend).
# API_BASE_URL: URL base que usa el navegador para llamar a la API.
ENV BACKEND_URL=http://backend:8080 \
    API_BASE_URL=/api/v1
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/40-app-config.sh /docker-entrypoint.d/40-app-config.sh
COPY --from=build /app/dist/trato-frontend/browser /usr/share/nginx/html
RUN chmod +x /docker-entrypoint.d/40-app-config.sh

EXPOSE 80
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
  CMD wget -qO- http://127.0.0.1/healthz || exit 1
