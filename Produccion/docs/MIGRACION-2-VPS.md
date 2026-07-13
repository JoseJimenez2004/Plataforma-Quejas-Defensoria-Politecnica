# Migración a 2 VPS (frontend separado)

Objetivo: VPS actual (2.25.78.22) se queda con Backend + Base de Datos; una VPS nueva aloja
solo el frontend (Nginx + estáticos de Angular).

## ✅ Estado: VPS ya comprada

Segunda VPS adquirida en Hostinger: `srv1824254.hstgr.cloud` (plan KVM 1). Pendiente de
completar el asistente "Setup" (elegir SO/región) para obtener su IP pública.

El paso a paso completo (firewall, DNS, nginx, HTTPS) ya está en
`docs/CONFIGURAR-VPS-FRONTEND.md`. Decisiones ya tomadas, a falta de confirmar si Hostinger
ofrece red privada entre VPS para este plan (no encontré confirmación pública de esa
característica para KVM 1, así que el runbook asume que no existe y usa firewall por IP
de origen sobre la red pública, que es la opción segura garantizada):

- **Dominio**: se reutiliza `defensoria-escom.ddns.net`, apuntando ahora a la IP de la VPS
  nueva (la que da la cara al público).
- **Firewall backend**: puertos 8083-8085 y 5432 restringidos por IP de origen (solo la IP
  de la VPS del frontend), no abiertos a todo Internet.
- **HTTPS**: se termina en la VPS del frontend con Certbot, ya que es la que tiene el dominio.
- **CORS**: no hace falta tocarlo — el nginx del frontend sigue haciendo de proxy hacia
  `/api/*`, así que el navegador sigue viendo "mismo origen".

## Preguntas que quedaban (para referencia)

1. **Dominio**: ¿el mismo `defensoria-escom.ddns.net` para el frontend, o uno nuevo?
   Recomendado: mismo dominio apuntando a la IP de la VPS del frontend (es la que da la cara
   al público). El backend puede quedarse sin dominio propio, solo IP + puertos internos.
2. **Red entre VPS**: ¿Hostinger te da red privada entre tus VPS (VLAN/private networking),
   o solo se pueden ver por IP pública? Si hay red privada, el `proxy_pass` del nginx nuevo
   debería usar esa IP privada del backend (más seguro y sin depender de que el backend
   tenga sus puertos abiertos a todo Internet).
3. **Firewall del backend**: si solo hay IP pública disponible entre VPS, hay que restringir
   `ufw`/`firewalld` en la VPS backend para que los puertos 8083-8085 y 5432 solo acepten
   conexiones desde la IP de la nueva VPS frontend (no de cualquier IP de Internet).
4. **HTTPS**: se recomienda terminar TLS en la VPS del frontend (Certbot + Nginx), ya que es
   la que tiene el dominio público. El tráfico frontend→backend puede quedarse en HTTP simple
   si va por red privada, o también con TLS si solo hay IP pública disponible.
5. **CORS**: si en algún momento el frontend llama directo al backend por IP (en vez de que el
   nginx del frontend haga de proxy hacia `/api/*`), hay que revisar `allowedOriginPatterns` en
   `WebConfig` de `auth.service` y `queja-service` para que incluya el dominio/IP real del
   frontend. Si se mantiene el patrón actual (nginx del frontend reenviando `/api/*` al
   backend), el navegador sigue viendo "mismo origen" y no hace falta tocar CORS.
6. **Recursos de la nueva VPS**: el doc de specs original (`1 Servidor Frontend Proxy.txt`)
   pedía 4 vCPU / 8 GB para el frontend — sobredimensionado para servir estáticos + Nginx.
   Al comprar, se puede ir con algo bastante más chico; ajusta según el plan real que tengas
   en mente antes de decidir.

## Qué preparar de este lado mientras tanto

- Terminar el frontend (Angular) apuntando siempre a rutas relativas `/api/...` (ya es así en
  `AuthService`) para que no importe si el proxy vive en la misma VPS o en otra.
- Mantener `defensoria.conf` parametrizado por variable/IP fácil de cambiar (hoy está hardcodeado
  a `2.25.78.22` en 3 lugares — al separar VPS bastará con cambiar esa IP una vez, o mejor,
  moverla a una sola variable/`upstream` en el nginx.conf).

Cuando tengas definida tu idea concreta para la segunda VPS, retomamos este archivo y lo
convertimos en el plan de ejecución paso a paso.
