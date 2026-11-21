#!/usr/bin/env bash
# Funciones y nombres comunes del despliegue blue-green de SIGR. Se carga con "source".
#
# Por entorno se crean:
#   redes      sigr-<entorno>-datos (backend ↔ postgres, alias "db")
#              sigr-<entorno>-front (frontend ↔ backend activo, alias "backend")
#   contenedores sigr-<entorno>-db, sigr-<entorno>-backend-blue|green, sigr-<entorno>-frontend
#   volúmenes  sigr-<entorno>-pgdata (BD) y sigr-<entorno>-imagenes (fotos del menú)
#   puertos    público = base+80 (frontend) · blue = base+81 · green = base+82 (backend)
#   estado     ~/.sigr/<entorno>/ (color activo, candidato, imágenes, JWT generado)
# Las variables se usan en los scripts que cargan este archivo:
# shellcheck disable=SC2034
set -euo pipefail

ENTORNO="${ENTORNO:?Defina ENTORNO (desarrollo | staging | produccion)}"

case "$ENTORNO" in
  desarrollo) PUERTO_BASE=8000 ;;
  staging)    PUERTO_BASE=9000 ;;
  produccion) PUERTO_BASE=10000 ;;
  *) echo "Entorno desconocido: $ENTORNO" >&2; exit 1 ;;
esac

PUBLIC_PORT=$((PUERTO_BASE + 80))
PREFIJO="sigr-${ENTORNO}"
RED_DATOS="${PREFIJO}-datos"
RED_FRONT="${PREFIJO}-front"
CONT_DB="${PREFIJO}-db"
CONT_FRONT="${PREFIJO}-frontend"
VOL_DB="${PREFIJO}-pgdata"
VOL_IMG="${PREFIJO}-imagenes"
IMAGEN_DB="postgres:15.19-alpine"

# Prefijo de las imágenes: en el pipeline ghcr.io/<owner>; en pruebas locales, cualquier nombre.
REGISTRY="${REGISTRY:-sigr}"

# Administrador inicial: el pipeline pasa el secreto; si falta, valor de prueba.
SIGR_ADMIN_EMAIL="${SIGR_ADMIN_EMAIL:-admin@sigr.local}"
SIGR_ADMIN_PASSWORD="${SIGR_ADMIN_PASSWORD:-Admin123*}"

ESTADO_DIR="${SIGR_ESTADO_DIR:-$HOME/.sigr}/${ENTORNO}"
mkdir -p "$ESTADO_DIR" "${EVIDENCIAS_DIR:-evidencias}"
# Ruta absoluta: se monta como volumen en los contenedores de k6 y ZAP.
EVIDENCIAS="$(cd "${EVIDENCIAS_DIR:-evidencias}" && pwd)"

log() { echo "[$(date '+%H:%M:%S')] [$ENTORNO] $*"; }
error() { echo "::error::$*" >&2; }

imagen() { echo "${REGISTRY}/sigr-$1:${IMAGE_TAG:?Defina IMAGE_TAG}"; }

contenedor_backend() { echo "${PREFIJO}-backend-$1"; }

otro_color() { if [ "$1" = blue ]; then echo green; else echo blue; fi; }

puerto_color() {
  if [ "$1" = blue ]; then echo $((PUERTO_BASE + 81)); else echo $((PUERTO_BASE + 82)); fi
}

# Estado: un archivo por clave dentro de ESTADO_DIR.
leer_estado() { if [ -s "$ESTADO_DIR/$1" ]; then cat "$ESTADO_DIR/$1"; else echo "${2:-}"; fi; }
guardar_estado() { printf '%s\n' "$2" > "$ESTADO_DIR/$1"; }
borrar_estado() { for clave in "$@"; do rm -f "$ESTADO_DIR/$clave"; done; }

existe_contenedor() { docker container inspect "$1" >/dev/null 2>&1; }

asegurar_red() { docker network inspect "$1" >/dev/null 2>&1 || docker network create "$1" >/dev/null; }

conectado_a_red() {
  docker container inspect -f '{{range $red, $cfg := .NetworkSettings.Networks}}{{$red}} {{end}}' "$1" 2>/dev/null \
    | tr ' ' '\n' | grep -qx "$2"
}

# Exporta una variable para los pasos siguientes del job (GITHUB_ENV) y para este proceso.
exportar() {
  export "$1=$2"
  if [ -n "${GITHUB_ENV:-}" ]; then echo "$1=$2" >> "$GITHUB_ENV"; fi
}

# Escapa comillas y barras para insertar un valor dentro de un JSON.
json_escape() {
  local s="${1//\\/\\\\}"
  printf '%s' "${s//\"/\\\"}"
}

# Recrea el contenedor del frontend con la imagen indicada en la red front.
recrear_frontend() {
  docker rm -f "$CONT_FRONT" >/dev/null 2>&1 || true
  docker run -d --name "$CONT_FRONT" \
    --network "$RED_FRONT" \
    -p "${PUBLIC_PORT}:5173" \
    --restart unless-stopped \
    --label "sigr.entorno=${ENTORNO}" \
    "$1" >/dev/null
  guardar_estado frontend_imagen "$1"
}
