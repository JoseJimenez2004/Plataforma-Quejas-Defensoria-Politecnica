🌐 Accesos del Servidor VPS (Hostinger)
Estos son los datos para conectarte por SSH desde Termius:

IP Pública: 2.25.78.22

Usuario: root

Contraseña del servidor: Temporaljul2026@

🗄️ Datos de la Base de Datos (PostgreSQL)
Estos son los accesos internos de tu contenedor de Podman:

Motor: PostgreSQL 16

Nombre de la BD: defensoria_db

Usuario: postgres

Contraseña: Temporal2026@

Puerto: 5432

💻 Comandos Útiles de Podman
Para ejecutar estos comandos, recuerda que primero debes estar conectado a tu servidor VPS por SSH.

Para detener la base de datos:


podman stop defensoria-db

Para volver a levantar/iniciar la base de datos:
podman start defensoria-db

Para reiniciar el contenedor (útil si se queda pegado algo):
podman restart defensoria-db

Para ver si el contenedor está corriendo:
podman ps -a

Para entrar a la consola SQL y hacer consultas a mano:
podman exec -it defensoria-db psql -U postgres -d defensoria_db
(Recuerda que para salir de esta consola SQL solo escribes \q y presionas Enter).

🔌 Datos de Conexión para Spring Boot
Si alguien más del equipo necesita configurar su entorno local para conectarse a tu VPS, solo deben poner esto en su application.properties:

Properties
spring.datasource.url=jdbc:postgresql://2.25.78.22:5432/defensoria_db
spring.datasource.username=postgres
spring.datasource.password=Temporal2026@
spring.datasource.driverClassName=org.postgresql.Driver
