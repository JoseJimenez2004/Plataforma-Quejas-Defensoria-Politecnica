# Mismo patron que Frontend-Admin (defensoria-admin-img): nginx sirviendo el build estatico
# de Angular en su propio contenedor ("revision-web"), puerto 80 INTERNO -- se publica hacia
# afuera con "-p 22347:80" en podman-compose-front-revision.sh (admin-web usa 22346).
FROM docker.io/library/nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/browser /usr/share/nginx/html

EXPOSE 80
