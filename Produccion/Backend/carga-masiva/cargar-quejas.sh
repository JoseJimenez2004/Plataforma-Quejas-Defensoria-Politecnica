#!/bin/bash
# ==============================================================================
# Carga masiva de quejas - Plataforma Defensoria
#
# Registra quejas por el MISMO endpoint que usa el formulario publico
# (POST /api/quejoso/quejas/registro-publico), asi que cada queja pasa por todas las
# validaciones, recibe su folio real y guarda credencial + evidencias en BYTEA, igual
# que si alguien la hubiera llenado a mano en la pagina.
#
# Ademas puede dejar lista la cuenta del quejoso (correo + contrasena) para que entre
# a su panel:
#   CUENTAS=sql  (default) genera cuentas-quejosos.sql; se corre en el servidor de BD.
#                No manda ningun correo.
#   CUENTAS=api  activa cada cuenta con /api/auth/activar-cuenta. OJO: auth-service
#                manda un correo de bienvenida por cada cuenta; con correos inventados
#                rebotan y Gmail puede bloquear la cuenta remitente.
#   CUENTAS=no   solo registra las quejas.
#
# Uso (en la VPS backend, donde los puertos 8083/8084 responden en 127.0.0.1):
#   bash cargar-quejas.sh quejas.csv
#   CUENTAS=api bash cargar-quejas.sh quejas.csv
#   URL_QUEJAS=https://defensoria-escom.ddns.net URL_AUTH=https://defensoria-escom.ddns.net \
#       bash cargar-quejas.sh quejas.csv          # desde cualquier otra maquina
#
# Formato del archivo (separado por |, la primera linea es el encabezado y se ignora,
# las lineas que empiezan con # tambien se ignoran):
#   nombre|apellido1|apellido2|correo|fecha_nacimiento|tipo|numero|unidad|fecha_hechos|
#   den_nombre|den_apellido1|den_apellido2|descripcion|password|credenciales|evidencias
#
#   fecha_*       AAAA-MM-DD
#   tipo          alumno | empleado
#   unidad        clave del catalogo de dependencias (ESCOM, UPIICSA, ESIME-ZAC, ...)
#   descripcion   el texto directo (sin | ni saltos de linea) o @ruta/archivo.txt para
#                 textos largos o con varios parrafos (no tiene limite de caracteres)
#   password      contrasena de la cuenta del quejoso (8+ caracteres, una mayuscula y un
#                 numero). Vacio = usa $PASSWORD_DEFAULT; si tampoco hay, no se crea cuenta.
#   credenciales  rutas JPG/PNG separadas por ; (1 o 2, max 3MB c/u). Vacio = imagen de
#                 ejemplo generada por el script.
#   evidencias    rutas PDF/JPG/PNG/MP4/MP3 separadas por ;. Vacio = sin evidencias.
#                 EJEMPLO = un PDF de ejemplo generado por el script.
#   Las rutas relativas se buscan junto al archivo .csv.
# ==============================================================================

set -u

URL_QUEJAS="${URL_QUEJAS:-http://127.0.0.1:8084}"
URL_AUTH="${URL_AUTH:-http://127.0.0.1:8083}"
CUENTAS="${CUENTAS:-sql}"
PASSWORD_DEFAULT="${PASSWORD_DEFAULT:-}"
PAUSA="${PAUSA:-0}"                      # segundos entre quejas (0 = sin pausa)
AVISO_VERSION="${AVISO_VERSION:-1.0}"

CSV="${1:-}"
if [ -z "$CSV" ] || [ ! -f "$CSV" ]; then
    sed -n '2,45p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
fi
case "$CUENTAS" in sql|api|no) ;; *) echo "CUENTAS debe ser sql, api o no"; exit 1 ;; esac
command -v curl >/dev/null || { echo "Falta curl (apt install curl)"; exit 1; }

DIR_CSV="$(cd "$(dirname "$CSV")" && pwd)"
SALIDA="$DIR_CSV/salida-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$SALIDA/ejemplos"
RESULTADOS="$SALIDA/resultados.csv"
SQL="$SALIDA/cuentas-quejosos.sql"
echo "linea|correo|folio|estado|detalle" > "$RESULTADOS"

# ---------- archivos de ejemplo (validos por firma binaria) ----------
CRED_EJEMPLO="$SALIDA/ejemplos/credencial.png"
PDF_EJEMPLO="$SALIDA/ejemplos/evidencia.pdf"
echo 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==' \
    | base64 -d > "$CRED_EJEMPLO"
printf '%%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]>>endobj\ntrailer<</Root 1 0 R>>\n%%%%EOF\n' \
    > "$PDF_EJEMPLO"

if [ "$CUENTAS" = "sql" ]; then
    cat > "$SQL" <<'EOF'
-- Cuentas de quejosos generadas por cargar-quejas.sh
-- Correr en el servidor de BD:
--   podman exec -i defensoria-db psql -U postgres -d defensoria_db < cuentas-quejosos.sql
-- La contrasena se cifra con BCrypt dentro de PostgreSQL (pgcrypto, $2a$), el mismo
-- formato que usa auth-service, asi que el quejoso entra con su correo y esa contrasena.
-- ON CONFLICT DO NOTHING: si el correo o la boleta ya tienen cuenta, se deja como esta.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
BEGIN;
EOF
fi

# ---------- utilidades ----------
ruta() {            # resuelve rutas relativas contra la carpeta del csv
    case "$1" in /*) echo "$1" ;; *) echo "$DIR_CSV/$1" ;; esac
}
sql_txt() {         # escapa comillas simples para SQL
    printf "'%s'" "$(printf '%s' "$1" | sed "s/'/''/g")"
}
json_txt() {        # escapa para JSON
    printf '"%s"' "$(printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g')"
}
mime() {
    case "${1,,}" in
        *.png) echo image/png ;; *.jpg|*.jpeg) echo image/jpeg ;; *.pdf) echo application/pdf ;;
        *.mp4) echo video/mp4 ;; *.mp3) echo audio/mpeg ;; *) echo application/octet-stream ;;
    esac
}
password_valido() { [[ ${#1} -ge 8 && "$1" =~ [A-Z] && "$1" =~ [0-9] ]]; }

declare -A CUENTA_HECHA=()
total=0; ok=0; fallidas=0; cuentas=0
linea=0

while IFS='|' read -r nombre ap1 ap2 correo fnac tipo numero unidad fhechos \
                      dnom dap1 dap2 descripcion password creds evids || [ -n "${nombre:-}" ]; do
    linea=$((linea + 1))
    [ "$linea" -eq 1 ] && continue                       # encabezado
    [ -z "${nombre// /}" ] && continue                   # linea vacia
    [[ "$nombre" == \#* ]] && continue                   # comentario
    total=$((total + 1))
    evids="${evids%$'\r'}"; creds="${creds%$'\r'}"      # csv guardado en Windows

    args=(-sS --max-time 120 -X POST "$URL_QUEJAS/api/quejoso/quejas/registro-publico"
          --form-string "nombre=$nombre" --form-string "apellido1=$ap1"
          --form-string "apellido2=$ap2" --form-string "correo=$correo"
          --form-string "fechaNacimiento=$fnac" --form-string "tipoIdentificacion=$tipo"
          --form-string "numeroIdentificacion=$numero" --form-string "unidadAcademicaClave=$unidad"
          --form-string "fechaHechos=$fhechos" --form-string "nombreDenunciado=$dnom"
          --form-string "apellido1Denunciado=$dap1" --form-string "apellido2Denunciado=$dap2"
          --form-string "avisoPrivacidadAceptado=true"
          --form-string "avisoPrivacidadVersion=$AVISO_VERSION")

    # Descripcion: texto directo o @archivo.txt (curl lee el contenido con "<")
    if [[ "$descripcion" == @* ]]; then
        archivo_desc="$(ruta "${descripcion#@}")"
        if [ ! -f "$archivo_desc" ]; then
            echo "$linea|$correo||ERROR|no existe $archivo_desc" >> "$RESULTADOS"
            fallidas=$((fallidas + 1)); echo "[$linea] ERROR  $correo: no existe $archivo_desc"; continue
        fi
        args+=(-F "descripcion=<$archivo_desc")
    else
        args+=(--form-string "descripcion=$descripcion")
    fi

    # Credenciales: el backend las distingue de las evidencias por el prefijo del nombre
    [ -z "$creds" ] && creds="$CRED_EJEMPLO"
    faltante=""
    IFS=';' read -ra lista <<< "$creds"
    for c in "${lista[@]}"; do
        [ -z "$c" ] && continue
        f="$(ruta "$c")"; [ "$c" = "$CRED_EJEMPLO" ] && f="$c"
        [ -f "$f" ] || { faltante="$f"; break; }
        args+=(-F "archivos=@$f;filename=IDENTIFICACION_$(basename "$f");type=$(mime "$f")")
    done
    if [ -z "$faltante" ] && [ -n "$evids" ]; then
        [ "$evids" = "EJEMPLO" ] && evids="$PDF_EJEMPLO"
        IFS=';' read -ra lista <<< "$evids"
        for e in "${lista[@]}"; do
            [ -z "$e" ] && continue
            f="$(ruta "$e")"; [ "$e" = "$PDF_EJEMPLO" ] && f="$e"
            [ -f "$f" ] || { faltante="$f"; break; }
            args+=(-F "archivos=@$f;filename=$(basename "$f");type=$(mime "$f")")
        done
    fi
    if [ -n "$faltante" ]; then
        echo "$linea|$correo||ERROR|no existe $faltante" >> "$RESULTADOS"
        fallidas=$((fallidas + 1)); echo "[$linea] ERROR  $correo: no existe $faltante"; continue
    fi

    respuesta="$(curl "${args[@]}" -w $'\n%{http_code}' 2>&1)"
    codigo="${respuesta##*$'\n'}"
    cuerpo="${respuesta%$'\n'*}"

    if [ "$codigo" != "200" ]; then
        detalle="$(printf '%s' "$cuerpo" | grep -o '"mensaje" *: *"[^"]*"' | head -1 | sed 's/.*: *"//; s/"$//')"
        [ -z "$detalle" ] && detalle="HTTP $codigo $(printf '%s' "$cuerpo" | tr '\n|' '  ' | cut -c1-150)"
        echo "$linea|$correo||ERROR|$detalle" >> "$RESULTADOS"
        fallidas=$((fallidas + 1)); echo "[$linea] ERROR  $correo: $detalle"
        continue
    fi

    folio="$(printf '%s' "$cuerpo" | grep -o '"numeroFolio" *: *"[^"]*"' | head -1 | sed 's/.*: *"//; s/"$//')"
    ok=$((ok + 1))
    estado_cuenta="sin cuenta"

    # ---------- cuenta del quejoso ----------
    [ -z "$password" ] && password="$PASSWORD_DEFAULT"
    correo_min="${correo,,}"
    if [ "$CUENTAS" != "no" ] && [ -n "$password" ]; then
        if ! password_valido "$password"; then
            estado_cuenta="cuenta NO creada: la contrasena necesita 8+ caracteres, una mayuscula y un numero"
        elif [ -n "${CUENTA_HECHA[$correo_min]:-}" ]; then
            estado_cuenta="cuenta ya creada en una linea anterior"
        elif [ "$CUENTAS" = "sql" ]; then
            nombre_completo="$(echo "$nombre $ap1 $ap2" | tr -s ' ' | sed 's/ $//')"
            printf 'INSERT INTO usuarios (nombre, correo_institucional, boleta, password, activo, intentos_codigo) VALUES (%s, %s, %s, crypt(%s, gen_salt(%s, 10)), true, 0) ON CONFLICT DO NOTHING;\n' \
                "$(sql_txt "$nombre_completo")" "$(sql_txt "$correo_min")" "$(sql_txt "$numero")" \
                "$(sql_txt "$password")" "'bf'" >> "$SQL"
            CUENTA_HECHA[$correo_min]=1; cuentas=$((cuentas + 1))
            estado_cuenta="cuenta en cuentas-quejosos.sql"
        else
            r="$(curl -sS --max-time 60 -X POST "$URL_AUTH/api/auth/activar-cuenta" \
                 -H 'Content-Type: application/json' \
                 -d "{\"numeroFolio\":$(json_txt "$folio"),\"correo\":$(json_txt "$correo_min"),\"password\":$(json_txt "$password"),\"confirmarPassword\":$(json_txt "$password")}" \
                 -w $'\n%{http_code}' 2>&1)"
            if [ "${r##*$'\n'}" = "200" ]; then
                CUENTA_HECHA[$correo_min]=1; cuentas=$((cuentas + 1)); estado_cuenta="cuenta activada"
            else
                estado_cuenta="cuenta NO activada: $(printf '%s' "${r%$'\n'*}" | tr '\n|' '  ' | cut -c1-120)"
            fi
        fi
    fi

    echo "$linea|$correo|$folio|OK|$estado_cuenta" >> "$RESULTADOS"
    echo "[$linea] OK     $correo -> $folio ($estado_cuenta)"
    [ "$PAUSA" != "0" ] && sleep "$PAUSA"
done < "$CSV"

[ "$CUENTAS" = "sql" ] && echo "COMMIT;" >> "$SQL"

echo ""
echo "================================================"
echo " Quejas leidas:     $total"
echo " Registradas:       $ok"
echo " Con error:         $fallidas"
echo " Cuentas ($CUENTAS):   $cuentas"
echo " Resultados:        $RESULTADOS"
if [ "$CUENTAS" = "sql" ] && [ "$cuentas" -gt 0 ]; then
    echo " SQL de cuentas:    $SQL"
    echo ""
    echo " Falta crear las cuentas: copia ese archivo al servidor de BD y corre"
    echo "   podman exec -i defensoria-db psql -U postgres -d defensoria_db < cuentas-quejosos.sql"
fi
echo "================================================"
