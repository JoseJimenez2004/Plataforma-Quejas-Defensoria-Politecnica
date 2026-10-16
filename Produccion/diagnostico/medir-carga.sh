#!/bin/bash
# ==============================================================================
# Mide qué parte de la carga del sitio es la lenta. Se corre desde TU computadora:
#   bash medir-carga.sh
#   TOKEN=<token> bash medir-carga.sh        # opcional: mide también las APIs con sesión
# El token se saca del navegador: F12 > Application > Local Storage > ddp_admin_token
# ==============================================================================
SITIO="${SITIO:-https://defensoria-escom.ddns.net}"
FMT='dns=%{time_namelookup}s conexion=%{time_connect}s tls=%{time_appconnect}s primer_byte=%{time_starttransfer}s total=%{time_total}s bytes=%{size_download} http=%{http_code}'

linea() { printf '%-42s ' "$1"; }

echo "== Páginas (HTML) =="
for p in / /revision/ /admin/; do
    linea "$p"; curl -s -o /dev/null -w "$FMT\n" "$SITIO$p"
done

echo ""
echo "== Archivos JS/CSS de cada front (con compresión) =="
for p in / /revision/ /admin/; do
    html="$(curl -s "$SITIO$p")"
    for a in $(printf '%s' "$html" | grep -oE '(main|styles|polyfills)-[A-Z0-9]+\.(js|css)' | sort -u); do
        url="$SITIO${p}$a"
        linea "${p}$a"
        curl -s -o /dev/null -H 'Accept-Encoding: gzip, br' -w "$FMT" "$url"
        enc=$(curl -sI -H 'Accept-Encoding: gzip, br' "$url" | grep -i '^content-encoding' | tr -d '\r' | cut -d' ' -f2)
        cache=$(curl -sI "$url" | grep -i '^cache-control' | tr -d '\r' | cut -d' ' -f2-)
        echo " compresion=${enc:-NINGUNA} cache=${cache:-NINGUNA}"
    done
done

echo ""
echo "== APIs públicas =="
for api in /api/catalogos/dependencias /api/chatbot/menu; do
    linea "$api"; curl -s -o /dev/null -w "$FMT\n" "$SITIO$api"
done

if [ -n "${TOKEN:-}" ]; then
    echo ""
    echo "== APIs con sesión =="
    for api in /api/admin/seguimiento/tablero /api/admin/seguimiento/quejas /api/revision/bandeja /api/notificaciones/mias; do
        linea "$api"; curl -s -o /dev/null -H "Authorization: Bearer $TOKEN" -w "$FMT\n" "$SITIO$api"
    done
fi

echo ""
echo "Cómo leerlo: dns/tls altos = red o dominio; primer_byte alto en HTML = servidor frontend;"
echo "primer_byte alto en /api = microservicio o base de datos; compresion=NINGUNA = falta subir defensoria.conf."
