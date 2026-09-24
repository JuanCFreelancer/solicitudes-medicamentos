#!/usr/bin/env bash
# Utilidades compartidas por los scripts de verificación end-to-end.

FAILS=0

# check <descripción> <esperado> <obtenido>
check() {
  if [ "$2" = "$3" ]; then echo "  OK   $1 ($3)"; else echo "  FAIL $1 (esperado $2, obtenido $3)"; FAILS=$((FAILS + 1)); fi
}
# assert_contains <descripción> <texto> <fragmento>
assert_contains() {
  if [[ "$2" == *"$3"* ]]; then echo "  OK   $1"; else echo "  FAIL $1 (no contiene '$3'): $2"; FAILS=$((FAILS + 1)); fi
}
# http <método> <url> [token] [body]  -> imprime "cuerpo|codigo"
http() {
  local method="$1" url="$2" token="${3:-}" body="${4:-}"
  local args=(-s -w '|%{http_code}' -X "$method" "$url" -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ] && args+=(-d "$body")
  curl "${args[@]}"
}
code() { echo "${1##*|}"; }
body() { echo "${1%|*}"; }

# register_and_login <authUrl> <email> <password>  -> imprime el token
register_and_login() {
  http POST "$1/auth/register" "" "{\"nombre\":\"Prueba E2E\",\"email\":\"$2\",\"password\":\"$3\"}" > /dev/null
  local r; r=$(http POST "$1/auth/login" "" "{\"email\":\"$2\",\"password\":\"$3\"}")
  body "$r" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p'
}

finish() {
  echo
  if [ "$FAILS" -eq 0 ]; then echo "TODAS LAS PRUEBAS OK"; else echo "$FAILS PRUEBA(S) FALLARON"; exit 1; fi
}
