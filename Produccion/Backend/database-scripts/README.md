# Scripts de Base de Datos

Este directorio contiene todos los scripts SQL necesarios para inicializar las bases de datos del sistema.

## Archivos

### 00-init.sql
Script de inicialización que se ejecuta primero al levantar el contenedor por primera vez.
- Configura extensiones de Postgres (uuid-ossp)
- Configura timezone a America/Mexico_City

### dependencias_seed.sql
Seed del catálogo de dependencias del IPN (208 registros).
- Se ejecuta automáticamente cuando el contenedor se levanta por primera vez
- Puebla la tabla `dependencias` con el catálogo completo

### chatbot_seed.sql
Seed del contenido del chatbot (mini-chat/tutorial del portal del quejoso).
- Se ejecuta automáticamente cuando el contenedor se levanta por primera vez
- Puebla la tabla `preguntas_chatbot` con las preguntas frecuentes

### [PENDING] estructura_base.sql
Si tienes un script con la estructura de la base vieja/nueva, agrégalo aquí.
- Renómbralo a `estructura_base.sql` o `migracion_estructura.sql`
- Se ejecutará automáticamente si está en este directorio

## Orden de Ejecución

Postgres ejecuta los scripts en orden alfabético al levantar el contenedor por primera vez:

1. `00-init.sql` - Configuración inicial
2. `dependencias_seed.sql` - Catálogo de dependencias
3. `chatbot_seed.sql` - Preguntas del chatbot
4. [Cualquier otro script .sql que agregues]

## Cómo Agregar Nuevos Scripts

1. Coloca el archivo `.sql` en este directorio
2. Nómbralo con un prefijo numérico si necesitas controlar el orden (ej: `01-mi-script.sql`)
3. El script se ejecutará automáticamente la próxima vez que levantes el contenedor

**IMPORTANTE**: Los scripts solo se ejecutan la PRIMERA vez que el contenedor se levanta.
Si necesitas volver a ejecutarlos, debes eliminar el contenedor y volver a crearlo:

```bash
sudo bash podman-compose-bd.sh down-defensoria
sudo bash podman-compose-bd.sh up-defensoria
```

O ejecutar manualmente:

```bash
sudo podman exec -i defensoria-db psql -U postgres -d defensoria_db < /ruta/al/script.sql
```
