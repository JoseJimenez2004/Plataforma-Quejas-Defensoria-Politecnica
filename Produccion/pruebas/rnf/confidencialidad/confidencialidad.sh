#!/usr/bin/env bash
# ==============================================================================
# RNF-Q02 (Confidencialidad) — tráfico cifrado y credenciales que nunca viajan en claro.
#
# Pruebas pasivas (solo leen): se pueden correr contra pruebas o contra producción.
#   C-01  HTTP (puerto 80) redirige con 301 a HTTPS
#   C-02  TLS 1.2 y 1.3 aceptados
#   C-03  TLS 1.0 y 1.1 rechazados
#   C-04  Certificado válido para el dominio y con >= 14 días de vigencia
#   C-05  Encabezado Strict-Transport-Security (HSTS) presente
#   C-06  Respuestas JSON de la API sin compresión (mitigación BREACH, ver defensoria.conf)
#   C-07  Endpoints internos de notificaciones bloqueados desde fuera (403)
#   C-08  El login no acepta credenciales por query string ni devuelve Set-Cookie de sesión
#         (la autenticación viaja como token Bearer en el encabezado Authorization)
#   C-09  Encabezados X-Content-Type-Options / X-Frame-Options (recomendados)
#
# Uso: DOMINIO=defensoria-escom.ddns.net bash confidencialidad.sh
# ==============================================================================
set -uo pipefail
DOMINIO="${DOMINIO:?Define DOMINIO, p. ej. defensoria-escom.ddns.net}"
BASE="https://$DOMINIO"
fallas=0; avisos=0

ok()    { echo "  [OK]    $1"; }
falla() { echo "  [FALLA] $1"; fallas=$((fallas + 1)); }
aviso() { echo "  [AVISO] $1"; avisos=$((avisos + 1)); }

echo "== RNF-Q02 Confidencialidad sobre $DOMINIO"

# C-01
read -r code loc < <(curl -s -o /dev/null -w '%{http_code} %{redirect_url}\n' --max-time 15 "http://$DOMINIO/")
if [ "$code" = "301" ] && [[ "$loc" == https://* ]]; then ok "C-01 HTTP -> HTTPS (301 a $loc)"; else falla "C-01 HTTP responde $code (redirige a '$loc')"; fi

# C-02 / C-03
# SECLEVEL=0 para que el OpenSSL del runner SÍ intente TLS 1.0/1.1 y la prueba mida al servidor.
tls() {
    local extra=()
    [[ "$1" == -tls1 || "$1" == -tls1_1 ]] && extra=(-cipher 'DEFAULT@SECLEVEL=0')
    echo | timeout 15 openssl s_client -connect "$DOMINIO:443" -servername "$DOMINIO" "$1" "${extra[@]}" 2>/dev/null \
        | grep -q 'Cipher is [^(]'
}
for v in -tls1_2 -tls1_3; do
    if tls "$v"; then ok "C-02 ${v#-} aceptado"; else falla "C-02 ${v#-} no disponible"; fi
done
for v in -tls1 -tls1_1; do
    if tls "$v"; then falla "C-03 ${v#-} aceptado (debe rechazarse)"; else ok "C-03 ${v#-} rechazado"; fi
done

# C-04
fin="$(echo | timeout 15 openssl s_client -connect "$DOMINIO:443" -servername "$DOMINIO" 2>/dev/null \
      | openssl x509 -noout -enddate 2>/dev/null | cut -d= -f2)"
if [ -n "$fin" ]; then
    dias=$(( ( $(date -d "$fin" +%s) - $(date +%s) ) / 86400 ))
    if curl -s -o /dev/null --max-time 15 "$BASE/" && [ "$dias" -ge 14 ]; then ok "C-04 certificado válido, vence en $dias días"
    else falla "C-04 certificado inválido o vence en $dias días"; fi
else
    falla "C-04 no se pudo leer el certificado"
fi

cabeceras="$(curl -s -D - -o /dev/null --max-time 15 "$BASE/")"

# C-05
if echo "$cabeceras" | grep -qi '^strict-transport-security:'; then ok "C-05 HSTS presente"
else falla "C-05 falta Strict-Transport-Security (agregar en nginx: add_header Strict-Transport-Security \"max-age=31536000\" always;)"; fi

# C-06
enc="$(curl -s -D - -o /dev/null -H 'Accept-Encoding: gzip, br' --max-time 15 "$BASE/api/catalogos/dependencias" \
      | grep -i '^content-encoding' | tr -d '\r')"
if [ -z "$enc" ]; then ok "C-06 la API no comprime JSON (BREACH)"; else falla "C-06 la API comprime JSON: $enc"; fi

# C-07
for p in /api/notificaciones/enviar /api/notificaciones/registrar; do
    c="$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{}' --max-time 15 "$BASE$p")"
    if [ "$c" = "403" ]; then ok "C-07 $p bloqueado desde fuera (403)"; else falla "C-07 $p responde $c desde fuera"; fi
done

# C-08
r="$(curl -s -D - -o /dev/null -X POST --max-time 15 "$BASE/api/auth/login?correo=x@ipn.mx&password=x")"
if echo "$r" | grep -qi '^set-cookie:'; then falla "C-08 el login emite Set-Cookie"; else ok "C-08 el login no usa cookies de sesión"; fi
c="$(echo "$r" | head -1 | awk '{print $2}')"
if [[ "$c" == 2* ]]; then falla "C-08 el login acepta credenciales por query string ($c)"; else ok "C-08 credenciales por query string rechazadas ($c)"; fi

# C-09
for h in x-content-type-options x-frame-options; do
    if echo "$cabeceras" | grep -qi "^$h:"; then ok "C-09 $h presente"; else aviso "C-09 falta $h (recomendado)"; fi
done

echo ""
echo "Fallas: $fallas  Avisos: $avisos"
[ "$fallas" -eq 0 ]
