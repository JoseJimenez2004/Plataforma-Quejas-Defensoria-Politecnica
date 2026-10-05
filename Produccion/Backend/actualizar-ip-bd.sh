#!/bin/bash

# Script para actualizar la IP de la base de datos en todos los config-files
# Cambia de 2.25.78.22 a 169.58.62.99 (servidor BD)
# También corrige si alguien puso 156.67.26.73 por error

IP_VIEJA_1="2.25.78.22"
IP_VIEJA_2="156.67.26.73"
IP_NUEVA="169.58.62.99"
CONFIG_DIR="config-files"

echo "Actualizando IP de base de datos en config-files..."
echo "De: $IP_VIEJA"
echo "A: $IP_NUEVA"
echo ""

# Buscar y reemplazar en todos los archivos YAML
find "$CONFIG_DIR" -name "*.yml" -exec sed -i "s|$IP_VIEJA_1:5432|$IP_NUEVA:5432|g" {} \;
find "$CONFIG_DIR" -name "*.yaml" -exec sed -i "s|$IP_VIEJA_1:5432|$IP_NUEVA:5432|g" {} \;
find "$CONFIG_DIR" -name "*.yml" -exec sed -i "s|$IP_VIEJA_2:5432|$IP_NUEVA:5432|g" {} \;
find "$CONFIG_DIR" -name "*.yaml" -exec sed -i "s|$IP_VIEJA_2:5432|$IP_NUEVA:5432|g" {} \;

echo "✅ IPs actualizadas en todos los config-files"
echo ""
echo "Archivos modificados:"
find "$CONFIG_DIR" -name "*.yml" -o -name "*.yaml" | grep -E "\.(yml|yaml)$"
