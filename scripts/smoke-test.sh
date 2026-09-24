#!/usr/bin/env bash
# Prueba de humo end-to-end contra los servicios levantados (docker compose up).
# Uso: ./scripts/smoke-test.sh [AUTH_URL] [SOLICITUDES_URL]
set -uo pipefail

AUTH="${1:-http://localhost:8081}"
SOL="${2:-http://localhost:8082}"
EMAIL="smoke.$(date +%s)@correo.com"
PASS="Clave12345"
FAILS=0

# check <descripción> <código esperado> <código real>
check() {
  if [ "$2" = "$3" ]; then echo "  OK   $1 ($3)"; else echo "  FAIL $1 (esperado $2, obtenido $3)"; FAILS=$((FAILS + 1)); fi
}
# http <método> <url> [token] [body]  -> imprime "codigo|cuerpo"
http() {
  local method="$1" url="$2" token="${3:-}" body="${4:-}"
  local args=(-s -w '|%{http_code}' -X "$method" "$url" -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ] && args+=(-d "$body")
  curl "${args[@]}"
}
code() { echo "${1##*|}"; }
body() { echo "${1%|*}"; }

echo "== Autenticación =="
r=$(http POST "$AUTH/auth/register" "" "{\"nombre\":\"Smoke Test\",\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")
check "registro válido" 201 "$(code "$r")"
r=$(http POST "$AUTH/auth/register" "" "{\"nombre\":\"Smoke Test\",\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")
check "registro con correo duplicado" 409 "$(code "$r")"
r=$(http POST "$AUTH/auth/register" "" '{"nombre":"","email":"x","password":"1"}')
check "registro con datos inválidos" 400 "$(code "$r")"
r=$(http POST "$AUTH/auth/login" "" "{\"email\":\"$EMAIL\",\"password\":\"incorrecta1\"}")
check "login con password incorrecto" 401 "$(code "$r")"
r=$(http POST "$AUTH/auth/login" "" "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")
check "login correcto" 200 "$(code "$r")"
TOKEN=$(body "$r" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
[ -n "$TOKEN" ] || { echo "  FAIL no se obtuvo token"; exit 1; }

echo "== Solicitudes =="
r=$(http GET "$SOL/solicitudes")
check "listar sin token" 401 "$(code "$r")"
r=$(http GET "$SOL/solicitudes" "token.invalido.aqui")
check "listar con token inválido" 401 "$(code "$r")"

r=$(http GET "$SOL/medicamentos" "$TOKEN")
check "catálogo de medicamentos" 200 "$(code "$r")"
# ids del seed: primer POS y primer NO POS
POS_ID=$(body "$r" | grep -o '"id":[0-9]*,"nombre":"[^"]*","esPos":true' | head -1 | sed 's/"id":\([0-9]*\).*/\1/')
NOPOS_ID=$(body "$r" | grep -o '"id":[0-9]*,"nombre":"[^"]*","esPos":false' | head -1 | sed 's/"id":\([0-9]*\).*/\1/')

r=$(http POST "$SOL/solicitudes" "$TOKEN" "{\"medicamentoId\":$POS_ID}")
check "crear solicitud POS (sin campos adicionales)" 201 "$(code "$r")"
r=$(http POST "$SOL/solicitudes" "$TOKEN" "{\"medicamentoId\":$NOPOS_ID}")
check "crear NO POS sin campos -> validación" 400 "$(code "$r")"
r=$(http POST "$SOL/solicitudes" "$TOKEN" "{\"medicamentoId\":$NOPOS_ID,\"numeroOrden\":\"ORD-1\",\"direccion\":\"Calle 1 # 2-3\",\"telefono\":\"3001234567\",\"correoContacto\":\"correo-malo\"}")
check "crear NO POS con correo inválido" 400 "$(code "$r")"
for n in 1 2 3; do
  r=$(http POST "$SOL/solicitudes" "$TOKEN" "{\"medicamentoId\":$NOPOS_ID,\"numeroOrden\":\"ORD-$n\",\"direccion\":\"Calle $n # 2-3\",\"telefono\":\"300 123 4567\",\"correoContacto\":\"paciente$n@correo.com\"}")
  check "crear NO POS completo #$n" 201 "$(code "$r")"
done
r=$(http POST "$SOL/solicitudes" "$TOKEN" '{"medicamentoId":999999}')
check "crear con medicamento inexistente" 422 "$(code "$r")"
r=$(http POST "$SOL/solicitudes" "$TOKEN" '{}')
check "crear sin medicamento" 400 "$(code "$r")"

echo "== Paginación (4 solicitudes creadas por este usuario) =="
r=$(http GET "$SOL/solicitudes?page=0&size=3" "$TOKEN")
check "página 0, size 3" 200 "$(code "$r")"
b=$(body "$r")
echo "$b" | grep -q '"totalElements":4' && echo "  OK   totalElements = 4" || { echo "  FAIL totalElements != 4: $b"; FAILS=$((FAILS + 1)); }
echo "$b" | grep -q '"totalPages":2' && echo "  OK   totalPages = 2" || { echo "  FAIL totalPages != 2"; FAILS=$((FAILS + 1)); }
r=$(http GET "$SOL/solicitudes?page=1&size=3" "$TOKEN")
echo "$(body "$r")" | grep -o '"numeroOrden"' | wc -l | grep -q '^1$' && echo "  OK   página 1 tiene 1 elemento" || { echo "  FAIL página 1"; FAILS=$((FAILS + 1)); }
r=$(http GET "$SOL/solicitudes?size=500" "$TOKEN")
check "size fuera de rango" 400 "$(code "$r")"

echo "== Aislamiento entre usuarios =="
EMAIL2="otro.$(date +%s)@correo.com"
http POST "$AUTH/auth/register" "" "{\"nombre\":\"Otro\",\"email\":\"$EMAIL2\",\"password\":\"$PASS\"}" > /dev/null
r=$(http POST "$AUTH/auth/login" "" "{\"email\":\"$EMAIL2\",\"password\":\"$PASS\"}")
TOKEN2=$(body "$r" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
r=$(http GET "$SOL/solicitudes" "$TOKEN2")
echo "$(body "$r")" | grep -q '"totalElements":0' && echo "  OK   el segundo usuario no ve solicitudes ajenas" || { echo "  FAIL fuga de datos entre usuarios"; FAILS=$((FAILS + 1)); }

echo
if [ "$FAILS" -eq 0 ]; then echo "TODAS LAS PRUEBAS OK"; else echo "$FAILS PRUEBA(S) FALLARON"; exit 1; fi
