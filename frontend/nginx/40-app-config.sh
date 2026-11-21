#!/bin/sh
# Genera /app-config.json a partir de variables de entorno al iniciar el contenedor, para que la
# misma imagen sirva en cualquier entorno (12-factor).
set -eu
cat > /usr/share/nginx/html/app-config.json <<EOF
{
  "apiBaseUrl": "${API_BASE_URL:-/api/v1}"
}
EOF
echo "app-config.json generado (apiBaseUrl=${API_BASE_URL:-/api/v1})"
