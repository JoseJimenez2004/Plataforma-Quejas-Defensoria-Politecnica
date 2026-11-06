#!/usr/bin/env bash
# El patrón "[ cond ]; registrar ... $(( $? == 0 ))" usa a propósito el $? de la condición.
# shellcheck disable=SC2319
# ==============================================================================
# SEG-01 / RN-R02 / RNF-A06 / RNF-Q06 — Prueba DINÁMICA de autorización (caja negra).
#
# Recorre matriz-endpoints.csv y, para cada endpoint protegido:
#   1. Sin token                      -> debe responder 401 o 403.
#   2. Con token de un rol NO permitido -> debe responder 403 (se prueban todos los métodos:
#      Spring corta en el filtro de seguridad antes de ejecutar la lógica).
#   3. Con token de un rol permitido   -> NO debe responder 401/403 (solo se prueban GET,
#      para no crear datos; 404/400 cuentan como "pasó el filtro").
# Extras: token alterado -> 401 (RNF-Q06), vigencia del token = 1 h (RNF-Q06),
#         restaurar sin confirmar -> rechazado (RNF-A03), path traversal en descarga de
#         respaldos -> no 200.
#
# SOLO contra el entorno de pruebas. Variables:
#   BASE_URL=https://pruebas.ejemplo        (obligatoria)
#   CRED_QUEJOSO="correo:password"          (una por rol; las que falten se omiten)
#   CRED_RECEPCIONISTA, CRED_ANALISTA_PRIMER_CONTACTO, CRED_SUBDEFENSOR,
#   CRED_DEFENSOR
#   MATRIZ=ruta/al/csv (por defecto, la de esta carpeta)
# Salida: reporte en ${REPORTE:-matriz-roles-resultado.csv}; exit 1 si hay fallas.
# ==============================================================================
set -uo pipefail

: "${BASE_URL:?Define BASE_URL (entorno de pruebas)}"
DIR="$(cd "$(dirname "$0")" && pwd)"
MATRIZ="${MATRIZ:-$DIR/matriz-endpoints.csv}"
REPORTE="${REPORTE:-matriz-roles-resultado.csv}"
ROLES=(QUEJOSO RECEPCIONISTA ANALISTA_PRIMER_CONTACTO SUBDEFENSOR DEFENSOR)

declare -A TOKEN
fallas=0
pruebas=0
echo "requisito,metodo,ruta,rol,esperado,obtenido,resultado" > "$REPORTE"

registrar() { # requisito metodo ruta rol esperado obtenido ok
    pruebas=$((pruebas + 1))
    local res="APROBADA"
    if [ "$7" != "1" ]; then res="FALLIDA"; fallas=$((fallas + 1)); fi
    echo "$1,$2,$3,$4,$5,$6,$res" >> "$REPORTE"
    [ "$res" = "FALLIDA" ] && echo "  FALLA  $2 $3 rol=$4 esperado=$5 obtenido=$6"
}

http() { # metodo url [token] [body]
    local args=(-s -o /dev/null -w '%{http_code}' -X "$1" --max-time 20)
    [ -n "${3:-}" ] && args+=(-H "Authorization: Bearer $3")
    if [ -n "${4:-}" ]; then args+=(-H 'Content-Type: application/json' -d "$4"); fi
    curl "${args[@]}" "$2"
}

login() { # rol
    local var="CRED_$1" cred correo pass url
    cred="${!var:-}"
    [ -z "$cred" ] && return 1
    correo="${cred%%:*}"; pass="${cred#*:}"
    if [ "$1" = "QUEJOSO" ]; then url="$BASE_URL/api/auth/login"; else url="$BASE_URL/api/admin/auth/login"; fi
    curl -s --max-time 20 -H 'Content-Type: application/json' \
        -d "$(jq -n --arg c "$correo" --arg p "$pass" '{correo:$c,password:$p}')" "$url" | jq -r '.token // empty'
}

concretar() { # sustituye {variables} de la ruta por valores ficticios
    echo "$1" | sed -E 's/\{folio\}/FOL-00000000/g; s/\{[a-zA-Z]*[iI]d\}/0/g; s/\{nombreArchivo\}/no-existe.sql/g; s/\{[^}]*\}/x/g'
}

echo "== Obteniendo tokens por rol"
for rol in "${ROLES[@]}"; do
    t="$(login "$rol" || true)"
    if [ -n "$t" ]; then TOKEN[$rol]="$t"; echo "  $rol: OK"; else echo "  $rol: sin credenciales, se omite"; fi
done

echo "== Matriz rol x endpoint"
while IFS=, read -r servicio metodo ruta permitidos _; do
    [ "$servicio" = "servicio" ] && continue
    [ "$permitidos" = "PUBLICO" ] && continue
    [ "$metodo" = "ANY" ] && metodo=GET
    url="$BASE_URL$(concretar "$ruta")"

    c="$(http "$metodo" "$url")"
    [[ "$c" == 401 || "$c" == 403 ]]; registrar "RNF-Q02" "$metodo" "$ruta" "ANONIMO" "401/403" "$c" $(( $? == 0 ))

    for rol in "${!TOKEN[@]}"; do
        if [ "$permitidos" = "AUTENTICADO" ] || [[ "|$permitidos|" == *"|$rol|"* ]]; then
            [ "$metodo" != "GET" ] && continue
            c="$(http GET "$url" "${TOKEN[$rol]}")"
            [[ "$c" != 401 && "$c" != 403 ]]; registrar "SEG-01" GET "$ruta" "$rol" "!=401/403" "$c" $(( $? == 0 ))
        else
            c="$(http "$metodo" "$url" "${TOKEN[$rol]}")"
            [[ "$c" == 403 ]]; registrar "SEG-01" "$metodo" "$ruta" "$rol" "403" "$c" $(( $? == 0 ))
        fi
    done
done < "$MATRIZ"

echo "== Extras"
if [ -n "${TOKEN[QUEJOSO]:-}" ]; then
    t="${TOKEN[QUEJOSO]}"
    # RNF-Q06: vigencia de 1 hora (exp - iat = 3600)
    payload="$(echo "$t" | cut -d. -f2 | tr '_-' '/+')"
    while [ $(( ${#payload} % 4 )) -ne 0 ]; do payload="$payload="; done
    vig="$(echo "$payload" | base64 -d 2>/dev/null | jq -r '(.exp - .iat)' 2>/dev/null)"
    [ "$vig" = "3600" ]; registrar "RNF-Q06" "-" "vigencia-token" "QUEJOSO" "3600" "${vig:-?}" $(( $? == 0 ))
    # RNF-Q06: token con firma alterada debe rechazarse
    alterado="${t%?}$( [ "${t: -1}" = "A" ] && echo B || echo A )"
    c="$(http GET "$BASE_URL/api/quejoso/quejas/mias" "$alterado")"
    [[ "$c" == 401 || "$c" == 403 ]]; registrar "RNF-Q06" GET "/api/quejoso/quejas/mias" "TOKEN_ALTERADO" "401/403" "$c" $(( $? == 0 ))
fi
if [ -n "${TOKEN[DEFENSOR]:-}" ]; then
    t="${TOKEN[DEFENSOR]}"
    # RNF-A03: restaurar sin confirmación explícita
    c="$(http POST "$BASE_URL/api/admin/seguridad/restaurar" "$t" '{"nombreArchivo":"no-existe.sql"}')"
    [[ "$c" == 4* ]]; registrar "RNF-A03" POST "/api/admin/seguridad/restaurar" "DEFENSOR" "4xx" "$c" $(( $? == 0 ))
    # Path traversal en la descarga de respaldos
    c="$(http GET "$BASE_URL/api/admin/seguridad/respaldos/..%2F..%2F..%2Fetc%2Fpasswd/descargar" "$t")"
    [[ "$c" != 200 ]]; registrar "SEG-03" GET "/respaldos/{traversal}/descargar" "DEFENSOR" "!=200" "$c" $(( $? == 0 ))
fi

echo ""
echo "Pruebas: $pruebas  Fallidas: $fallas  Reporte: $REPORTE"
[ "$fallas" -eq 0 ]
