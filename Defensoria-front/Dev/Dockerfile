FROM node:22-alpine
WORKDIR /app
# Copiamos los archivos de dependencias de Angular
COPY package*.json ./
RUN npm install
# Copiamos el resto del código de Angular
COPY . .
EXPOSE 4200
# Comando para levantar el servidor de desarrollo
CMD ["npx", "ng", "serve", "--host", "0.0.0.0"]