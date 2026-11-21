#!/usr/bin/env bash
# Prueba de carga de "hora pico" con k6 (en contenedor) contra la versión nueva.
# Uso: load-test.sh <url-base-api>   (VUS=100 por defecto)
# Falla si no se cumplen los umbrales: p95 < 500 ms y errores < 1 %.
set -euo pipefail
# shellcheck source=scripts/lib.sh
source "$(dirname "$0")/lib.sh"

BASE="${1:?Uso: load-test.sh <url-base-api>}"
K6_IMAGEN="grafana/k6:2.3.0"
VUS="${VUS:-100}"
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"

# El contenedor de k6 no corre como el usuario del runner: necesita poder escribir el resumen.
chmod 777 "$EVIDENCIAS"

log "k6 con $VUS usuarios virtuales contra $BASE"
docker run --rm --network host \
  -v "$RAIZ/tests/load:/scripts:ro" \
  -v "$EVIDENCIAS:/evidencias" \
  -e BASE_URL="$BASE" \
  -e VUS="$VUS" \
  -e ADMIN_EMAIL="$SIGR_ADMIN_EMAIL" \
  -e ADMIN_PASSWORD="$SIGR_ADMIN_PASSWORD" \
  "$K6_IMAGEN" run /scripts/hora-pico.js 2>&1 | tee "$EVIDENCIAS/k6-salida.txt"
