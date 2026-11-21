#!/usr/bin/env bash
# Escaneo dinámico pasivo (OWASP ZAP baseline) contra el menú público de la versión nueva.
# Uso: zap-scan.sh <url-base-api>
# -I: las advertencias (WARN) no fallan el paso; sí lo hacen las alertas FAIL o un error de ZAP.
set -euo pipefail
# shellcheck source=scripts/lib.sh
source "$(dirname "$0")/lib.sh"

BASE="${1:?Uso: zap-scan.sh <url-base-api>}"
ZAP_IMAGEN="zaproxy/zap-stable:2.17.0"

# El contenedor corre como el usuario "zap" y escribe los reportes en /zap/wrk.
chmod 777 "$EVIDENCIAS"

log "ZAP baseline contra $BASE/menu/platos"
docker run --rm --network host \
  -v "$EVIDENCIAS:/zap/wrk:rw" \
  "$ZAP_IMAGEN" zap-baseline.py \
  -t "$BASE/menu/platos" \
  -r zap-reporte.html \
  -J zap-reporte.json \
  -I
