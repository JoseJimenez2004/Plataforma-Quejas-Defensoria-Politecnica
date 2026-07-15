# Igual que el patrón del front de quejosos (defensoria-front-img): nginx sirviendo el build
# estático de Angular en su propio contenedor ("admin-web"), puerto 80 INTERNO -- se publica
# hacia afuera con "-p 22346:80" en podman-compose-front-admin.sh, igual que el front de
# quejosos publica su 80 interno como 22345.
FROM docker.io/library/nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/browser /usr/share/nginx/html

EXPOSE 80
