#!/bin/bash
# ==============================================================================
# Firewall del servidor BACKEND (156.67.26.73)
#   bash firewall-backend.sh            -> aplica la regla
#   bash firewall-backend.sh boot       -> la deja instalada para cada reinicio (systemd)
#   bash firewall-backend.sh quitar     -> elimina la regla
# ==============================================================================
# 8082-8094 solo aceptan conexiones que lleguen por la interfaz PÚBLICA desde el frontend.
#
# OJO: la regla se limita a la interfaz pública (-i $IFACE) a propósito. Los microservicios se
# llaman entre sí por la IP pública (156.67.26.73:8084, :8085...) y ese tráfico sale de un
# contenedor y entra por la red de Podman (podman0), no por la interfaz pública. Una regla sin
# "-i" lo tira sin responder, y como RestTemplate no tenía timeout, registrar una queja se
# quedaba colgado esperando a notificaciones-service (incidente del 2026-10-07).
# Se usa la tabla raw porque Podman publica puertos con DNAT y se salta ufw.

IP_FRONTEND="169.58.62.111"
PUERTOS="8082:8094"
IFACE=$(ip route show default | awk '{print $5; exit}')
[ -n "$IFACE" ] || { echo "❌ No pude detectar la interfaz pública"; exit 1; }

REGLA=(PREROUTING -i "$IFACE" -p tcp --dport "$PUERTOS" ! -s "$IP_FRONTEND" -j DROP)

aplicar() {
    # Rango anterior (antes de denunciado-service en 8094): se reemplaza por el nuevo
    while iptables -t raw -D PREROUTING -i "$IFACE" -p tcp --dport 8082:8093 ! -s "$IP_FRONTEND" -j DROP 2>/dev/null; do :; done
    # Limpia la regla vieja (sin -i) si alguien la puso a mano
    while iptables -t raw -D PREROUTING -p tcp --dport "$PUERTOS" ! -s "$IP_FRONTEND" ! -i lo -j DROP 2>/dev/null; do :; done
    iptables -t raw -C "${REGLA[@]}" 2>/dev/null || iptables -t raw -I "${REGLA[@]}"
    echo "✅ $PUERTOS solo desde $IP_FRONTEND (interfaz $IFACE)"
    iptables -t raw -S PREROUTING | grep -- "--dport"
}

case "$1" in
    ""|aplicar) aplicar ;;
    quitar)
        while iptables -t raw -D "${REGLA[@]}" 2>/dev/null; do :; done
        while iptables -t raw -D PREROUTING -p tcp --dport "$PUERTOS" ! -s "$IP_FRONTEND" ! -i lo -j DROP 2>/dev/null; do :; done
        echo "Regla eliminada"; iptables -t raw -S PREROUTING ;;
    boot)
        RUTA=$(readlink -f "$0")
        cat > /etc/systemd/system/defensoria-backend-firewall.service <<UNIT
[Unit]
Description=Defensoria - restringe 8082-8094 al servidor frontend
After=network-online.target
Wants=network-online.target

[Service]
Type=oneshot
ExecStart=/bin/bash $RUTA aplicar
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
UNIT
        systemctl daemon-reload && systemctl enable --now defensoria-backend-firewall.service && echo "✅ Instalado para cada reinicio" ;;
    *) echo "Uso: bash firewall-backend.sh [aplicar|boot|quitar]" ;;
esac
