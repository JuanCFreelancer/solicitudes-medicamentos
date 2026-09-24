#!/usr/bin/env bash
# Prueba de resiliencia: el servicio compuesto "Radicar solicitud" debe seguir funcionando
# cuando notificaciones-service está caído (degradación elegante) y recuperarse solo al volver.
# Requiere el stack levantado con docker compose. Uso: ./scripts/resilience-test.sh
set -uo pipefail
cd "$(dirname "$0")/.."

AUTH="${1:-http://localhost:8081}"
SOL="${2:-http://localhost:8082}"
NOTIF="${3:-http://localhost:8083}"
EMAIL="resiliencia.$(date +%s)@correo.com"
PASS="Clave12345"

source scripts/lib.sh

# Pase lo que pase, dejar el servicio de notificaciones levantado.
trap 'docker compose start notificaciones-service > /dev/null 2>&1' EXIT

wait_for_health() {
  for _ in $(seq 1 40); do
    [ "$(curl -s -o /dev/null -w '%{http_code}' "$1/actuator/health")" = "200" ] && return 0
    sleep 2
  done
  return 1
}

TOKEN=$(register_and_login "$AUTH" "$EMAIL" "$PASS")
[ -n "$TOKEN" ] || { echo "  FAIL no se obtuvo token"; exit 1; }
POS_ID=$(body "$(http GET "$SOL/medicamentos" "$TOKEN")" | grep -o '"id":[0-9]*,"nombre":"[^"]*","esPos":true' | head -1 | sed 's/"id":\([0-9]*\).*/\1/')

echo "== Con notificaciones-service disponible =="
r=$(http POST "$SOL/solicitudes" "$TOKEN" "{\"medicamentoId\":$POS_ID}")
check "radicar responde 201" 201 "$(code "$r")"
assert_contains "notificación ENVIADA" "$(body "$r")" '"notificacion":"ENVIADA"'

echo "== Se apaga notificaciones-service =="
docker compose stop notificaciones-service > /dev/null 2>&1
r=$(http POST "$SOL/solicitudes" "$TOKEN" "{\"medicamentoId\":$POS_ID}")
check "radicar sigue respondiendo 201 (la solicitud NO se pierde)" 201 "$(code "$r")"
assert_contains "la respuesta informa NO_DISPONIBLE" "$(body "$r")" '"notificacion":"NO_DISPONIBLE"'
r=$(http GET "$SOL/solicitudes" "$TOKEN")
assert_contains "ambas solicitudes quedaron guardadas" "$(body "$r")" '"totalElements":2'
r=$(http GET "$SOL/medicamentos" "$TOKEN")
check "el resto de la API de solicitudes no se ve afectada" 200 "$(code "$r")"

echo "== Se recupera notificaciones-service =="
docker compose start notificaciones-service > /dev/null 2>&1
wait_for_health "$NOTIF" && echo "  OK   notificaciones-service volvió a estar saludable" || { echo "  FAIL no volvió a estar saludable"; FAILS=$((FAILS + 1)); }
r=$(http POST "$SOL/solicitudes" "$TOKEN" "{\"medicamentoId\":$POS_ID}")
assert_contains "sin intervención manual vuelve a ENVIADA" "$(body "$r")" '"notificacion":"ENVIADA"'

finish
