#!/bin/bash
# Se corre EN CADA VPS (backend, frontend y BD): muestra si el servidor está saturado.
echo "== $(hostname) =="
echo "-- Carga (los 3 números deberían ser menores al número de CPUs: $(nproc)) --"; uptime
echo "-- Memoria --"; free -h
echo "-- Procesos que más CPU usan --"; ps -eo pid,pcpu,pmem,rss,comm --sort=-pcpu | head -8
if command -v podman >/dev/null; then
  echo "-- Contenedores (CPU, memoria y límite) --"
  podman stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"
  echo "-- Contenedores que se han reiniciado --"
  podman ps -a --format "table {{.Names}}\t{{.Status}}" | grep -viE "Up [0-9]+ (hours|days|weeks)" 
fi
echo "-- Últimos 'Out of memory' del kernel --"; dmesg 2>/dev/null | grep -i "out of memory\|oom-kill" | tail -5
