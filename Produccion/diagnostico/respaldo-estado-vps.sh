#!/bin/bash
# =============================================================================
# respaldo-estado-vps.sh — Foto completa de la configuración de una VPS
# antes de reinstalarla (TT 2026-B009, Plataforma de Quejas – Defensoría).
#
# Se corre EN CADA VPS (frontend, backend y BD) como root:
#     sudo bash respaldo-estado-vps.sh            # configuración + estado
#     sudo bash respaldo-estado-vps.sh --con-bd   # además, volcado de Postgres
#
# Deja un archivo /root/respaldo-estado-<host>-<fecha>.tar.gz
#
# ⚠  El respaldo contiene contraseñas (config-files, variables de entorno de
#    los contenedores, certificados). Guárdalo en privado y NO lo subas a git
#    ni lo pegues en el Manual de Instalación.
# =============================================================================
set -u
APP_DIR="${APP_DIR:-/apps}"          # raíz donde vive el sistema
FECHA="$(date +%Y-%m-%d_%H%M)"
HOST="$(hostname -s)"
OUT="/root/respaldo-estado-${HOST}-${FECHA}"
mkdir -p "$OUT"/{sistema,red,firewall,podman,nginx,app,servicios,bd}
cd "$OUT" || exit 1

# Corre un comando solo si existe y guarda su salida (y errores) en un archivo
run() { local f="$1"; shift; command -v "$1" >/dev/null 2>&1 && { echo "\$ $*"; "$@"; } >"$f" 2>&1 || true; }
copia() { for p in "$@"; do [ -e "$p" ] && cp -a --parents "$p" "$OUT/app/archivos" 2>/dev/null; done; }
mkdir -p "$OUT/app/archivos"
echo "==> Respaldando estado de $HOST en $OUT"

# ---------------------------------------------------------------- sistema ---
run sistema/os.txt          cat /etc/os-release
run sistema/kernel.txt      uname -a
run sistema/recursos.txt    bash -c 'nproc; free -h; df -hT; lsblk'
run sistema/fstab.txt       cat /etc/fstab
run sistema/hosts.txt       cat /etc/hosts
run sistema/timezone.txt    timedatectl
run sistema/usuarios.txt    bash -c "awk -F: '\$3>=1000 || \$1==\"root\"' /etc/passwd; echo; getent group wheel sudo podman"
run sistema/sudoers.txt     bash -c 'cat /etc/sudoers.d/* 2>/dev/null'
run sistema/sshd.txt        bash -c 'grep -vE "^\s*(#|$)" /etc/ssh/sshd_config; cat /etc/ssh/sshd_config.d/*.conf 2>/dev/null'
run sistema/selinux.txt     bash -c 'getenforce; sestatus'
run sistema/versiones.txt   bash -c 'for c in java mvn node npm podman podman-compose nginx psql git python3; do printf "%-15s " $c; command -v $c >/dev/null && ($c --version 2>&1 || $c -v 2>&1) | head -1 || echo "(no instalado)"; done'
if command -v rpm >/dev/null; then run sistema/paquetes.txt rpm -qa --qf '%{NAME} %{VERSION}-%{RELEASE}\n'
else run sistema/paquetes.txt dpkg -l; fi
[ -f /etc/dnf/dnf.conf ] && run sistema/paquetes-manuales.txt dnf repoquery --userinstalled
command -v apt-mark >/dev/null && run sistema/paquetes-manuales.txt apt-mark showmanual

# -------------------------------------------------------------------- red ---
run red/ip.txt              ip -br addr
run red/rutas.txt           ip route
run red/dns.txt             cat /etc/resolv.conf
run red/puertos-escucha.txt ss -tulpn
run red/puertos-netstat.txt netstat -tulpn
run red/conexiones.txt      ss -tnp state established

# --------------------------------------------------------------- firewall ---
run firewall/ufw.txt            ufw status verbose
run firewall/ufw-numerado.txt   ufw status numbered
[ -d /etc/ufw ] && cp -a /etc/ufw firewall/etc-ufw 2>/dev/null
run firewall/firewalld.txt      firewall-cmd --list-all-zones
run firewall/firewalld-perm.txt firewall-cmd --permanent --list-all-zones
[ -d /etc/firewalld ] && cp -a /etc/firewalld firewall/etc-firewalld 2>/dev/null
run firewall/iptables.rules     iptables-save
run firewall/ip6tables.rules    ip6tables-save
run firewall/nftables.txt       nft list ruleset
run firewall/fail2ban.txt       fail2ban-client status

# ----------------------------------------------------------------- podman ---
if command -v podman >/dev/null; then
  run podman/contenedores.txt podman ps -a --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
  run podman/imagenes.txt     podman images
  run podman/volumenes.txt    podman volume ls
  run podman/redes.txt        podman network ls
  run podman/pods.txt         podman pod ps
  for n in $(podman network ls --format '{{.Name}}'); do podman network inspect "$n" >"podman/red-$n.json" 2>&1; done
  for c in $(podman ps -a --format '{{.Names}}'); do
    podman inspect "$c" >"podman/inspect-$c.json" 2>&1
    # Resumen legible: imagen, puertos, volúmenes, reinicio, memoria
    podman inspect "$c" --format \
'Contenedor: {{.Name}}
Imagen:     {{.ImageName}}
Puertos:    {{range $p,$v := .HostConfig.PortBindings}}{{$p}}->{{range $v}}{{.HostIp}}:{{.HostPort}} {{end}}{{end}}
Montajes:   {{range .Mounts}}{{.Source}} -> {{.Destination}}; {{end}}
Red:        {{.HostConfig.NetworkMode}}
Reinicio:   {{.HostConfig.RestartPolicy.Name}}
Memoria:    {{.HostConfig.Memory}}
' >>podman/resumen-contenedores.txt 2>&1
  done
  # Unidades systemd generadas para arrancar contenedores
  ls /etc/systemd/system/*container* ~/.config/systemd/user/* 2>/dev/null >podman/units-systemd.txt
  [ -d /etc/containers ] && cp -a /etc/containers podman/etc-containers 2>/dev/null
fi

# ------------------------------------------------------------------ nginx ---
[ -d /etc/nginx ] && cp -a /etc/nginx nginx/etc-nginx 2>/dev/null
run nginx/nginx-T.txt nginx -T
# nginx dentro de contenedores
if command -v podman >/dev/null; then
  for c in $(podman ps --format '{{.Names}}'); do
    podman exec "$c" sh -c 'command -v nginx >/dev/null && nginx -T' >"nginx/contenedor-$c.txt" 2>/dev/null || rm -f "nginx/contenedor-$c.txt"
  done
fi
[ -d /etc/letsencrypt ] && cp -a /etc/letsencrypt nginx/letsencrypt 2>/dev/null

# --------------------------------------------------- aplicación (rutas) ---
# Árbol de carpetas (sin entrar a node_modules ni datos de Postgres)
if [ -d "$APP_DIR" ]; then
  find "$APP_DIR" -maxdepth 6 \( -name node_modules -o -name pgdata -o -name data -path '*db*' \) -prune -o -type d -print \
       | sort >app/arbol-carpetas.txt
  find "$APP_DIR" -maxdepth 6 -type f \( -name '*.jar' -o -name '*.sql' -o -name '*.tgz' -o -name '*.gz' \) \
       -printf '%TY-%Tm-%Td %10s  %p\n' 2>/dev/null | sort -k3 >app/artefactos.txt
  stat -c '%A %U:%G %n' $(find "$APP_DIR" -maxdepth 4 -type d 2>/dev/null) >app/permisos.txt 2>/dev/null
  # Copia de los archivos de configuración y scripts (no jars, no dist, no datos)
  find "$APP_DIR" -type f \
       \( -name '*.sh' -o -name '*.yml' -o -name '*.yaml' -o -name '*.properties' -o -name '*.conf' \
          -o -name '*.env' -o -name '.env' -o -name 'Containerfile' -o -name 'Dockerfile' -o -name '*.json' -o -name '*.sql' \) \
       -not -path '*/node_modules/*' -not -path '*/dist/*' -not -path '*/pgdata/*' -size -5M \
       -exec cp --parents {} "$OUT/app/archivos" \; 2>/dev/null
fi

# --------------------------------------------------- servicios y tareas ---
run servicios/habilitados.txt systemctl list-unit-files --state=enabled
run servicios/activos.txt     systemctl list-units --type=service --state=running
ls -la /etc/systemd/system >servicios/units-propias.txt 2>&1
for f in /etc/systemd/system/*.service; do [ -f "$f" ] && cp "$f" servicios/; done
run servicios/crontab-root.txt crontab -l
for u in $(awk -F: '$3>=1000 && $3<65000 {print $1}' /etc/passwd); do crontab -l -u "$u" >"servicios/crontab-$u.txt" 2>/dev/null; done
[ -d /etc/cron.d ] && cp -a /etc/cron.d servicios/cron.d 2>/dev/null

# ------------------------------------------------------- base de datos ---
if [ "${1:-}" = "--con-bd" ] && command -v podman >/dev/null; then
  for c in $(podman ps --format '{{.Names}} {{.Image}}' | awk '/postgres/ {print $1}'); do
    echo "==> Volcando Postgres del contenedor $c"
    podman exec "$c" pg_dumpall -U postgres --globals-only >"bd/$c-roles.sql" 2>"bd/$c-error.txt"
    for db in $(podman exec "$c" psql -U postgres -Atc "select datname from pg_database where not datistemplate and datname<>'postgres'"); do
      podman exec "$c" pg_dump -U postgres -Fc "$db" >"bd/$c-$db.dump" 2>>"bd/$c-error.txt"
    done
    podman exec "$c" psql -U postgres -c '\l+' >"bd/$c-bases.txt" 2>&1
  done
fi

# ---------------------------------------------------------------- empaque ---
find "$OUT" -type f -empty -delete
cd /root && tar -czf "${OUT}.tar.gz" "$(basename "$OUT")" && rm -rf "$OUT"
chmod 600 "${OUT}.tar.gz"
echo
echo "Listo: ${OUT}.tar.gz ($(du -h "${OUT}.tar.gz" | cut -f1))"
echo "Bájalo a tu computadora con:"
echo "  scp root@<IP-de-esta-VPS>:${OUT}.tar.gz ."
