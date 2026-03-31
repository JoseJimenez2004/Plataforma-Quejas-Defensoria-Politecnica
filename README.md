# 🛡️ Plataforma Web para la Denuncia Segura y Prevención de Riesgos Digitales
### Trabajo Terminal: TT 2026-B009 | IPN - ESCOM

Sistema integral diseñado para la **Defensoría de los Derechos Politécnicos** del **Instituto Politécnico Nacional**, enfocado en la gestión, trazabilidad y auditoría de quejas digitales bajo estándares de seguridad y calidad de software (ISO/IEC 25010).

---

## 🚀 Arquitectura del Sistema
El proyecto utiliza una arquitectura de microservicios contenida en **Docker**, lo que garantiza que el entorno de desarrollo sea idéntico al de producción.

### Tecnologías Principales:
* **Frontend:** Angular 17+ (Puerto `4200`) - Interfaz reactiva y modular.
* **Backend:** Spring Boot 3.2+ / Java 21 (Puerto `8080`) - API REST robusta con seguridad integrada.
* **Base de Datos:** PostgreSQL 15-Alpine (Puerto `5432`) - Motor relacional con persistencia optimizada.
* **Gestión de Datos:** Spring Data JPA / Hibernate (Modo `validate`).

---

## 📊 Modelo de Datos (Diagrama ER)
Este es el esquema simplificado de las relaciones principales del sistema:

```mermaid
erDiagram
    usuarios ||--o| perfiles_quejoso : tiene
    usuarios ||--o| perfiles_personal : tiene
    usuarios ||--o{ usuario_rol : asignado
    cat_roles ||--o{ usuario_rol : define
    perfiles_quejoso ||--o{ quejas : interpone
    quejas ||--o{ denunciados : registra
    quejas ||--o{ documentos : adjunta
    quejas ||--o{ historial_estatus_queja : rastrea
    cat_estatus_queja ||--o{ quejas : define
    cat_dependencias ||--o{ perfiles_quejoso : pertenece
🛠️ Requisitos de Instalación (Linux/Ubuntu)
Para desplegar el entorno correctamente en tu máquina local:

Docker & Docker Compose V2: Asegúrate de tener las versiones más recientes.

Liberar Puertos Críticos: El puerto 5432 debe estar disponible. Si tienes un PostgreSQL local instalado, detén el servicio:

Bash
sudo systemctl stop postgresql
sudo systemctl disable postgresql
DBeaver Community Edition: Recomendado para la inspección visual de las 20+ tablas del sistema.

📦 Despliegue del Entorno
Sigue estos comandos en la raíz del proyecto para levantar todos los servicios:

Limpieza Profunda (Reset de BD y Volúmenes):

Bash
sudo docker compose down -v
Construcción y Arranque:

Bash
sudo docker compose up --build
Validación de Inicio:
El sistema estará listo cuando veas en los logs:

postgres-db | /docker-entrypoint-initdb.d/init.sql: running... (Base de datos creada)

spring-backend | Started DefensoriaApplication (API lista)

angular-frontend | Local: http://localhost:4200/ (Interfaz lista)

🗄️ Diccionario de Conexión a Base de Datos
Parámetro	Configuración para DBeaver / Host	Configuración para Spring (Interna)
Host	localhost o 127.0.0.1	db-defensoria
Puerto	5432	5432
Database	defensoria_db	defensoria_db
Usuario	defensoria	defensoria
Password	Bicho_IPN_2026!	Bicho_IPN_2026!
Comandos de Inspección Rápida:
Acceso directo a la consola SQL de Docker:

Bash
sudo docker exec -it postgres-db psql -U defensoria -d defensoria_db
Ver tablas desde el prompt (defensoria_db=#):

SQL
\dt
🧪 Pruebas de Integración (CURL)
Prueba que el flujo Request -> API -> DB funcione correctamente:

1. Crear un Usuario Admin (POST):

Bash
curl -X POST http://localhost:8080/api/usuarios \
     -H "Content-Type: application/json" \
     -d '{"username": "anthony_admin", "password_hash": "argon2_hash", "email": "admin@escom.ipn.mx"}'
2. Listar Usuarios (GET):

Bash
curl -X GET http://localhost:8080/api/usuarios
📝 Troubleshooting (Solución de Errores)
Error failed to bind host port 5432: Otro proceso usa el puerto. Identifícalo con sudo lsof -i :5432 y termina el proceso con sudo kill -9 <PID>.

Error FATAL: password authentication failed: Ocurre si cambiaste la clave en el .yml pero no borraste el volumen viejo. Ejecuta sudo docker compose down -v para resetear.

Error UnknownHostException: db-defensoria: Spring inició antes que la red de Docker. El servicio se reintentará automáticamente.

📂 Estructura de Módulos (init.sql)
Catálogos: Unidades académicas (ESCOM, ESIME), Temas y Estatus.

Usuarios y Roles: Control de acceso (RBAC).

Perfiles: Separación de datos de Quejosos y Personal.

Núcleo: Gestión de Quejas, Denunciados y Documentos (Sello Digital).

Auditoría: Bitácora para cumplimiento de normatividad.

