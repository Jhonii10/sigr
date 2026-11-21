#!/usr/bin/env bash
# Prueba de humo: salud, menú público y login del administrador.
# Uso: smoke-test.sh <url-base-api> [etiqueta]
#   ej. smoke-test.sh http://localhost:8081/api nueva
set -euo pipefail
# shellcheck source=scripts/lib.sh
source "$(dirname "$0")/lib.sh"

BASE="${1:?Uso: smoke-test.sh <url-base-api> [etiqueta]}"
ETIQUETA="${2:-smoke}"
INTENTOS="${SMOKE_INTENTOS:-40}"
ESPERA="${SMOKE_ESPERA:-5}"
SALIDA="$EVIDENCIAS/smoke-${ETIQUETA}.txt"
CUERPO="$(mktemp)"
trap 'rm -f "$CUERPO"' EXIT

registrar() { log "$*" | tee -a "$SALIDA"; }
: > "$SALIDA"

# 1. Salud: Spring Boot tarda en arrancar (Flyway + JPA), por eso se reintenta.
registrar "GET $BASE/actuator/health (hasta $INTENTOS intentos cada ${ESPERA}s)"
salud=""
for i in $(seq 1 "$INTENTOS"); do
  salud="$(curl -fsS --max-time 5 "$BASE/actuator/health" 2>/dev/null || true)"
  if echo "$salud" | grep -q '"status":"UP"'; then
    registrar "  intento $i: $salud"
    break
  fi
  sleep "$ESPERA"
done
if ! echo "$salud" | grep -q '"status":"UP"'; then
  registrar "  FALLO: el servicio no reportó UP (última respuesta: ${salud:-sin respuesta})"
  error "Smoke test ($ETIQUETA): la salud no llegó a UP"
  exit 1
fi

# 2. Menú público (sin autenticación).
codigo="$(curl -sS --max-time 10 -o "$CUERPO" -w '%{http_code}' "$BASE/menu/platos")"
registrar "GET $BASE/menu/platos → HTTP $codigo"
if [ "$codigo" != 200 ]; then
  error "Smoke test ($ETIQUETA): /menu/platos respondió $codigo"
  exit 1
fi

# 3. Login del administrador: debe devolver un token JWT.
datos="{\"email\":\"$(json_escape "$SIGR_ADMIN_EMAIL")\",\"password\":\"$(json_escape "$SIGR_ADMIN_PASSWORD")\"}"
codigo="$(curl -sS --max-time 10 -o "$CUERPO" -w '%{http_code}' \
  -H 'Content-Type: application/json' -d "$datos" "$BASE/auth/login")"
registrar "POST $BASE/auth/login ($SIGR_ADMIN_EMAIL) → HTTP $codigo"
if [ "$codigo" != 200 ] || ! grep -q '"token":"[^"]\+' "$CUERPO"; then
  error "Smoke test ($ETIQUETA): el login del administrador no devolvió token"
  exit 1
fi

# 4. En la URL pública también debe responder la SPA servida por nginx.
if [ "$ETIQUETA" = publica ]; then
  raiz="${BASE%/api}/"
  codigo="$(curl -sS --max-time 10 -o "$CUERPO" -w '%{http_code}' "$raiz")"
  registrar "GET $raiz → HTTP $codigo"
  if [ "$codigo" != 200 ]; then
    error "Smoke test ($ETIQUETA): el frontend respondió $codigo"
    exit 1
  fi
fi

registrar "Smoke test ($ETIQUETA) OK"
