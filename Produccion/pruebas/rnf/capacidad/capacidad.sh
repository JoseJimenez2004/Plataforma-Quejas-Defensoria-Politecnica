#!/usr/bin/env bash
# ==============================================================================
# RNF-Q03 (Capacidad) — tamaño máximo de petición multiparte.
#
# Manda al registro público una petición multiparte con un archivo de N MB y el resto del
# formulario VACÍO a propósito: si el tamaño se acepta, el backend llega a validar y
# responde 400 (no se crea ninguna queja); si se excede, responde 413.
#   CAP-01  LIMITE-5 MB por el proxy      -> NO 413
#   CAP-02  LIMITE+5 MB por el proxy      -> 413
#   CAP-03/04 Lo mismo directo al backend (si BACKEND_URL está definida)
#
# Variables:
#   BASE_URL=https://pruebas.ejemplo     proxy público (nginx)
#   LIMITE_PROXY_MB=100                  client_max_body_size de defensoria.conf
#   BACKEND_URL=http://host:8084         opcional, queja-service directo
#   LIMITE_BACKEND_MB=100                max-request-size de queja-service
#                                        (OJO: RNF-Q03 dice 60 MB y el yaml dice 100 MB)
# ==============================================================================
set -uo pipefail
: "${BASE_URL:?Define BASE_URL}"
LIMITE_PROXY_MB="${LIMITE_PROXY_MB:-100}"
LIMITE_BACKEND_MB="${LIMITE_BACKEND_MB:-100}"
RUTA="/api/quejoso/quejas/registro-publico"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
fallas=0

archivo() { # MB -> ruta de un PDF falso de ese tamaño
    local f="$TMP/evidencia-$1MB.pdf"
    [ -f "$f" ] || { printf '%%PDF-1.4\n' > "$f"; head -c $(( $1 * 1024 * 1024 )) /dev/zero >> "$f"; }
    echo "$f"
}

probar() { # id base_url mb espera(413|no413)
    local c
    c="$(curl -s -o /dev/null -w '%{http_code}' --max-time 300 -F "archivos=@$(archivo "$3");type=application/pdf" "$2$RUTA")"
    if { [ "$4" = 413 ] && [ "$c" = 413 ]; } || { [ "$4" = no413 ] && [ "$c" != 413 ] && [ "$c" != 000 ]; }; then
        echo "  [OK]    $1 $3 MB -> $c"
    else
        echo "  [FALLA] $1 $3 MB -> $c (se esperaba $4)"; fallas=$((fallas + 1))
    fi
}

echo "== RNF-Q03 Capacidad (proxy $BASE_URL, límite $LIMITE_PROXY_MB MB)"
probar CAP-01 "$BASE_URL" $(( LIMITE_PROXY_MB - 5 )) no413
probar CAP-02 "$BASE_URL" $(( LIMITE_PROXY_MB + 5 )) 413

if [ -n "${BACKEND_URL:-}" ]; then
    echo "== Backend directo $BACKEND_URL (límite $LIMITE_BACKEND_MB MB)"
    probar CAP-03 "$BACKEND_URL" $(( LIMITE_BACKEND_MB - 5 )) no413
    probar CAP-04 "$BACKEND_URL" $(( LIMITE_BACKEND_MB + 5 )) 413
fi

echo "Fallas: $fallas"
[ "$fallas" -eq 0 ]
