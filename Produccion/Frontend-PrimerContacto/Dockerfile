FROM docker.io/library/nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/browser /usr/share/nginx/html

EXPOSE 80
