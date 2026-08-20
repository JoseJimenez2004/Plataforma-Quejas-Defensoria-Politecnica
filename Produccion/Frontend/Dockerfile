FROM docker.io/library/nginx:alpine
COPY dist/browser/ /usr/share/nginx/html
COPY config/static.conf /etc/nginx/conf.d/default.conf
EXPOSE 8090
