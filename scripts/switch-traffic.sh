#!/usr/bin/env bash
# Cambia el tráfico al color nuevo: le da el alias "backend" en la red front, recrea el frontend
# con la imagen del mismo commit y detiene (sin borrar) el color anterior para poder revertir.
set -euo pipefail
# shellcheck source=scripts/lib.sh
source "$(dirname "$0")/lib.sh"

: "${IMAGE_TAG:?Defina IMAGE_TAG}"

NUEVO="$(leer_estado candidato)"
if [ -z "$NUEVO" ]; then
  error "No hay un despliegue candidato; ejecute deploy.sh primero"
  exit 1
fi
ANTERIOR="$(leer_estado activo)"
CONT_NUEVO="$(contenedor_backend "$NUEVO")"
IMAGEN_FRONT="$(imagen frontend)"

asegurar_red "$RED_FRONT"

# Se registra antes de tocar nada: si algo falla desde aquí, rollback.sh sabe que debe
# devolver el tráfico al color anterior.
guardar_estado trafico_cambiado si
guardar_estado previo "$ANTERIOR"
guardar_estado previo_tag "$(leer_estado activo_tag)"
guardar_estado frontend_imagen_previa "$(leer_estado frontend_imagen)"

log "Conectando $CONT_NUEVO a $RED_FRONT con alias backend"
if ! conectado_a_red "$CONT_NUEVO" "$RED_FRONT"; then
  docker network connect --alias backend "$RED_FRONT" "$CONT_NUEVO"
fi

# Se desconecta el anterior para que el alias "backend" resuelva solo al color nuevo.
if [ -n "$ANTERIOR" ] && [ "$ANTERIOR" != "$NUEVO" ]; then
  CONT_ANTERIOR="$(contenedor_backend "$ANTERIOR")"
  if existe_contenedor "$CONT_ANTERIOR" && conectado_a_red "$CONT_ANTERIOR" "$RED_FRONT"; then
    log "Desconectando $CONT_ANTERIOR de $RED_FRONT"
    docker network disconnect "$RED_FRONT" "$CONT_ANTERIOR"
  fi
fi

log "Recreando el frontend con $IMAGEN_FRONT (puerto $PUBLIC_PORT)"
recrear_frontend "$IMAGEN_FRONT"

# El anterior se detiene pero no se borra: es el punto de retorno de rollback.sh.
if [ -n "$ANTERIOR" ] && [ "$ANTERIOR" != "$NUEVO" ] && existe_contenedor "$CONT_ANTERIOR"; then
  log "Deteniendo $CONT_ANTERIOR (se conserva para reversión)"
  docker stop "$CONT_ANTERIOR" >/dev/null
fi

guardar_estado activo "$NUEVO"
guardar_estado activo_tag "$IMAGE_TAG"

log "Tráfico en $NUEVO (antes: ${ANTERIOR:-ninguno})" | tee -a "$EVIDENCIAS/despliegue-${ENTORNO}.txt"
