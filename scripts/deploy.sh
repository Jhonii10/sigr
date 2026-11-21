#!/usr/bin/env bash
# Despliega la versión IMAGE_TAG del backend en el color inactivo (blue/green).
# No toca el tráfico: el frontend sigue apuntando al color activo hasta switch-traffic.sh.
#
# Variables: ENTORNO, IMAGE_TAG, REGISTRY y, opcionales (secretos de GitHub),
#            DB_PASSWORD, JWT_SECRET, SIGR_ADMIN_EMAIL, SIGR_ADMIN_PASSWORD, CORS_ORIGENES.
set -euo pipefail
# shellcheck source=scripts/lib.sh
source "$(dirname "$0")/lib.sh"

: "${IMAGE_TAG:?Defina IMAGE_TAG}"

# Limpia el estado del intento anterior ANTES de cualquier otra cosa: si este script falla a
# mitad de camino, rollback.sh no debe confundir el color activo con un candidato.
borrar_estado candidato candidato_tag trafico_cambiado previo previo_tag frontend_imagen_previa

DB_NAME="sigr"
DB_USER="sigr"
DB_PASSWORD="${DB_PASSWORD:-sigr-${ENTORNO}-pruebas}"

# Sin secreto JWT_SECRET: se genera uno aleatorio una sola vez y se conserva en el estado,
# para que los tokens sigan siendo válidos entre despliegues del mismo entorno.
if [ -z "${JWT_SECRET:-}" ]; then
  JWT_SECRET="$(leer_estado jwt_secret)"
  if [ -z "$JWT_SECRET" ]; then
    JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')"
    guardar_estado jwt_secret "$JWT_SECRET"
    chmod 600 "$ESTADO_DIR/jwt_secret"
    log "JWT_SECRET no definido: se generó uno aleatorio en $ESTADO_DIR/jwt_secret"
  fi
fi

# Para simular un despliegue fallido (y ver la reversión), cambie esta línea por una URL
# inválida, por ejemplo: DB_URL="jdbc:postgresql://db-inexistente:5432/${DB_NAME}"
DB_URL="jdbc:postgresql://db:5432/${DB_NAME}"

CORS_ORIGENES="${CORS_ORIGENES:-http://localhost:${PUBLIC_PORT}}"

# --- Redes y base de datos -------------------------------------------------------------------
asegurar_red "$RED_DATOS"
asegurar_red "$RED_FRONT"

if ! existe_contenedor "$CONT_DB"; then
  log "Creando base de datos $CONT_DB"
  docker run -d --name "$CONT_DB" \
    --network "$RED_DATOS" --network-alias db \
    --restart unless-stopped \
    --label "sigr.entorno=${ENTORNO}" \
    -e POSTGRES_DB="$DB_NAME" \
    -e POSTGRES_USER="$DB_USER" \
    -e POSTGRES_PASSWORD="$DB_PASSWORD" \
    -v "${VOL_DB}:/var/lib/postgresql/data" \
    "$IMAGEN_DB" >/dev/null
else
  docker start "$CONT_DB" >/dev/null
fi

log "Esperando a que PostgreSQL acepte conexiones"
for _ in $(seq 1 30); do
  if docker exec "$CONT_DB" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then break; fi
  sleep 2
done
docker exec "$CONT_DB" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null \
  || { error "PostgreSQL no respondió a tiempo"; exit 1; }

# --- Backend nuevo en el color inactivo ------------------------------------------------------
ACTIVO="$(leer_estado activo)"
if [ -n "$ACTIVO" ]; then NUEVO="$(otro_color "$ACTIVO")"; else NUEVO=blue; fi
CONT_NUEVO="$(contenedor_backend "$NUEVO")"
NEXT_PORT="$(puerto_color "$NUEVO")"
IMAGEN_BACKEND="$(imagen backend)"

# En el color inactivo puede quedar la versión de hace dos despliegues (detenida): se reemplaza.
docker rm -f "$CONT_NUEVO" >/dev/null 2>&1 || true

guardar_estado candidato "$NUEVO"
guardar_estado candidato_tag "$IMAGE_TAG"

log "Arrancando $IMAGEN_BACKEND como $CONT_NUEVO (puerto $NEXT_PORT, activo: ${ACTIVO:-ninguno})"
# Solo en la red de datos: el frontend no lo ve hasta que switch-traffic.sh le asigne el alias.
# El puerto se publica en 127.0.0.1 para el smoke test, k6 y ZAP desde el propio runner.
docker run -d --name "$CONT_NUEVO" \
  --network "$RED_DATOS" \
  -p "127.0.0.1:${NEXT_PORT}:8080" \
  --restart unless-stopped \
  --label "sigr.entorno=${ENTORNO}" --label "sigr.color=${NUEVO}" \
  -e DB_URL="$DB_URL" \
  -e DB_USER="$DB_USER" \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e JWT_SECRET="$JWT_SECRET" \
  -e SIGR_ADMIN_EMAIL="$SIGR_ADMIN_EMAIL" \
  -e SIGR_ADMIN_PASSWORD="$SIGR_ADMIN_PASSWORD" \
  -e CORS_ORIGENES="$CORS_ORIGENES" \
  -e TZ=America/Bogota \
  -v "${VOL_IMG}:/app/data/imagenes" \
  "$IMAGEN_BACKEND" >/dev/null

exportar NEXT_COLOR "$NUEVO"
exportar NEXT_PORT "$NEXT_PORT"
exportar PUBLIC_PORT "$PUBLIC_PORT"

{
  echo "Entorno:        $ENTORNO"
  echo "Fecha:          $(date '+%Y-%m-%d %H:%M:%S %z')"
  echo "Versión:        $IMAGE_TAG"
  echo "Imagen backend: $IMAGEN_BACKEND"
  echo "Color activo:   ${ACTIVO:-ninguno}"
  echo "Color nuevo:    $NUEVO (puerto $NEXT_PORT)"
  echo "Puerto público: $PUBLIC_PORT"
} > "$EVIDENCIAS/despliegue-${ENTORNO}.txt"
cat "$EVIDENCIAS/despliegue-${ENTORNO}.txt"
