#!/usr/bin/env bash
# Revierte un despliegue fallido.
#  - Guarda los últimos logs de la versión que falló (evidencia).
#  - Si el tráfico ya se había cambiado, lo devuelve al color anterior (y a su frontend).
#  - Si no, no toca nada: el color activo siguió atendiendo todo el tiempo.
#  - Borra el contenedor nuevo.
# Es "mejor esfuerzo": intenta todos los pasos aunque alguno falle.
set -euo pipefail
# shellcheck source=scripts/lib.sh
source "$(dirname "$0")/lib.sh"

RESUMEN="$EVIDENCIAS/rollback.txt"
registrar() { log "$*" | tee -a "$RESUMEN"; }

NUEVO="$(leer_estado candidato)"
if [ -z "$NUEVO" ]; then
  log "No hay despliegue en curso; nada que revertir"
  exit 0
fi
CONT_NUEVO="$(contenedor_backend "$NUEVO")"
TAG_NUEVO="$(leer_estado candidato_tag)"

registrar "Reversión del despliegue ${TAG_NUEVO:-?} (color $NUEVO)"

# 1. Logs de la versión que falló.
if existe_contenedor "$CONT_NUEVO"; then
  LOGS="$EVIDENCIAS/rollback-logs-${NUEVO}.txt"
  docker logs --tail 200 "$CONT_NUEVO" > "$LOGS" 2>&1 || true
  echo "----- Últimas líneas del log de $CONT_NUEVO -----"
  tail -n 60 "$LOGS" || true
  echo "--------------------------------------------------"
fi

# 2. Tráfico.
if [ "$(leer_estado trafico_cambiado)" = si ]; then
  ANTERIOR="$(leer_estado previo)"
  CONT_ANTERIOR=""
  if [ -n "$ANTERIOR" ]; then CONT_ANTERIOR="$(contenedor_backend "$ANTERIOR")"; fi

  if [ -n "$CONT_ANTERIOR" ] && existe_contenedor "$CONT_ANTERIOR"; then
    registrar "El tráfico ya estaba en $NUEVO: se devuelve a $ANTERIOR"
    docker start "$CONT_ANTERIOR" >/dev/null || true
    if ! conectado_a_red "$CONT_ANTERIOR" "$RED_FRONT"; then
      docker network connect --alias backend "$RED_FRONT" "$CONT_ANTERIOR" || true
    fi
    if existe_contenedor "$CONT_NUEVO" && conectado_a_red "$CONT_NUEVO" "$RED_FRONT"; then
      docker network disconnect "$RED_FRONT" "$CONT_NUEVO" || true
    fi
    FRONT_PREVIO="$(leer_estado frontend_imagen_previa)"
    if [ -n "$FRONT_PREVIO" ]; then
      registrar "Restaurando frontend $FRONT_PREVIO"
      recrear_frontend "$FRONT_PREVIO" || true
    fi
    guardar_estado activo "$ANTERIOR"
    guardar_estado activo_tag "$(leer_estado previo_tag)"

    # Espera breve a que el color anterior vuelva a responder (no bloquea la reversión).
    PUERTO_ANTERIOR="$(puerto_color "$ANTERIOR")"
    for _ in $(seq 1 12); do
      if curl -fsS --max-time 3 "http://localhost:${PUERTO_ANTERIOR}/api/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
        registrar "$ANTERIOR responde UP de nuevo"
        break
      fi
      sleep 5
    done
  else
    # Primer despliegue del entorno: no hay versión anterior a la cual volver.
    registrar "No hay versión anterior: se retira el frontend del entorno"
    docker rm -f "$CONT_FRONT" >/dev/null 2>&1 || true
    borrar_estado activo activo_tag frontend_imagen
  fi
else
  registrar "El tráfico no se había cambiado: sigue en $(leer_estado activo ninguno)"
fi

# 3. Borra la versión fallida.
docker rm -f "$CONT_NUEVO" >/dev/null 2>&1 || true
registrar "Contenedor $CONT_NUEVO eliminado"

borrar_estado candidato candidato_tag trafico_cambiado previo previo_tag frontend_imagen_previa
registrar "Reversión terminada. Activo: $(leer_estado activo ninguno)"
