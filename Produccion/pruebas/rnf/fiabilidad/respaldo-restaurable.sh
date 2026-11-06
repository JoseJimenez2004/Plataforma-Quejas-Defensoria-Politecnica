#!/usr/bin/env bash
# ==============================================================================
# FIA-03 (Fiabilidad / Recuperabilidad) — un respaldo generado por la consola es restaurable.
#
#   1. Inicia sesión como la Defensora (DEFENSOR) y ejecuta un respaldo manual (RF-A06).
#   2. Descarga el .sql generado.
#   3. Lo restaura en una base VACÍA y desechable (nunca en la del sistema).
#   4. Verifica que existan las tablas núcleo y que tengan filas.
#
# SOLO contra el entorno de pruebas. Variables:
#   BASE_URL, CRED_DEFENSOR="correo:password"
#   PGHOST/PGPORT/PGUSER/PGPASSWORD   Postgres desechable (en CI: el contenedor de servicio)
#   TABLAS_NUCLEO="quejas usuarios personal_administrativo dependencias bitacora_acciones"
# ==============================================================================
set -euo pipefail
: "${BASE_URL:?}" "${CRED_DEFENSOR:?}"
TABLAS_NUCLEO="${TABLAS_NUCLEO:-quejas personal_administrativo dependencias}"
BD_DESTINO="restauracion_prueba_$(date +%s)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"; dropdb --if-exists "$BD_DESTINO" 2>/dev/null || true' EXIT

correo="${CRED_DEFENSOR%%:*}"; pass="${CRED_DEFENSOR#*:}"
token="$(curl -sf -H 'Content-Type: application/json' \
    -d "$(jq -n --arg c "$correo" --arg p "$pass" '{correo:$c,password:$p}')" \
    "$BASE_URL/api/admin/auth/login" | jq -r '.token')"
auth=(-H "Authorization: Bearer $token")

echo "== 1. Respaldo manual"
nombre="$(curl -sf -X POST "${auth[@]}" "$BASE_URL/api/admin/seguridad/respaldos/manual" | jq -r '.nombreArchivo')"
echo "   generado: $nombre"

echo "== 2. Descarga"
curl -sf "${auth[@]}" -o "$TMP/respaldo.sql" "$BASE_URL/api/admin/seguridad/respaldos/$nombre/descargar"
echo "   tamaño: $(du -h "$TMP/respaldo.sql" | cut -f1)"
grep -q 'PostgreSQL database dump' "$TMP/respaldo.sql" || { echo "FALLA: no parece un pg_dump"; exit 1; }

echo "== 3. Restauración en base desechable $BD_DESTINO"
createdb "$BD_DESTINO"
psql -v ON_ERROR_STOP=0 -q -d "$BD_DESTINO" -f "$TMP/respaldo.sql" > "$TMP/restaura.log" 2>&1 || true
errores="$(grep -c 'ERROR' "$TMP/restaura.log" || true)"
echo "   errores de psql: $errores"

echo "== 4. Verificación"
fallas=0
[ "$errores" -eq 0 ] || { echo "   [FALLA] la restauración tuvo $errores errores"; grep ERROR "$TMP/restaura.log" | head -5; fallas=1; }
for t in $TABLAS_NUCLEO; do
    n="$(psql -tA -d "$BD_DESTINO" -c "SELECT count(*) FROM $t" 2>/dev/null || echo X)"
    if [ "$n" = X ]; then echo "   [FALLA] tabla $t no existe"; fallas=1
    elif [ "$n" -eq 0 ]; then echo "   [FALLA] tabla $t vacía"; fallas=1
    else echo "   [OK]    $t: $n filas"; fi
done
echo "   tablas restauradas: $(psql -tA -d "$BD_DESTINO" -c "SELECT count(*) FROM information_schema.tables WHERE table_schema='public'")"
exit $fallas
