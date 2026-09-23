# Despliegue — Validaciones CU-Q01

**Fecha:** 2026-09-08
**Alcance:** validaciones del formulario público de quejas, previsualización de archivos y
aviso de privacidad.

---

## Orden de despliegue (importante)

```
1. Base de datos  →  2. Frontend  →  3. Backend
```

**No cambies este orden.** El motivo:

- El **frontend nuevo** manda dos campos que el backend viejo no conoce
  (`avisoPrivacidadAceptado`, `apellidoMaternoDenunciado`). Spring ignora los parámetros
  multipart que no existen en el DTO, así que el frontend nuevo funciona contra el backend
  viejo — solo sin las validaciones de servidor.
- El **backend nuevo** exige `avisoPrivacidadAceptado=true`. Si lo subes antes que el
  frontend, el formulario viejo (que no manda ese campo) deja de poder registrar quejas.

Con este orden no hay ventana de caída.

---

## 1. Base de datos — VPS backend (2.25.78.22)

Postgres corre **dentro del contenedor `defensoria-db`**, no en el host: en el host no hay
cliente `psql` ni socket en `/var/run/postgresql`. Por eso el archivo hace dos saltos —
laptop → servidor → contenedor.

### Paso 1 — Desde TU LAPTOP (no desde el servidor)

Abre una terminal en tu equipo, en la carpeta del proyecto:

```bash
cd ~/Documents/Plataforma-Quejas-Defensoria-Politecnica/Produccion
scp Backend/migracion_cu_q01_validaciones.sql \
  root@2.25.78.22:/apps/aplicaciones/defensoria/basedatos/
```

> Los `.sql` del servidor viven en `/apps/aplicaciones/defensoria/basedatos/`, no en la
> carpeta del backend (ver la entrada del 2026-09-08 en `CAMBIOS.md`).

### Paso 2 — Ya en el servidor (`root@srv1804187`)

Respaldo rápido del esquema antes de tocar nada:

```bash
podman exec defensoria-db pg_dump -U postgres -s -d defensoria_db \
  > ~/esquema-antes-cu-q01-$(date +%F).sql
ls -lh ~/esquema-antes-cu-q01-*.sql
```

Meter el script al contenedor y ejecutarlo:

```bash
cd /apps/aplicaciones/defensoria/basedatos
podman cp migracion_cu_q01_validaciones.sql defensoria-db:/tmp/
podman exec -i defensoria-db \
  psql -U postgres -d defensoria_db -v ON_ERROR_STOP=1 \
  -f /tmp/migracion_cu_q01_validaciones.sql
```

`ON_ERROR_STOP=1` hace que se detenga al primer error en vez de seguir a medias.

### Qué hace

Agrega `apellido_materno_denunciado` y las tres columnas del aviso de privacidad en `quejas`,
la columna `tipo` en `queja_evidencias`, y crea las restricciones `CHECK` de formato.

### Qué esperar en pantalla

Varios `ALTER TABLE`, un `UPDATE` (el backfill de `tipo`), `COMMIT`, y al final cuatro
consultas de verificación: las columnas nuevas, la columna `tipo`, las restricciones creadas
(con `convalidated = f`, que es lo correcto) y un conteo de filas viejas que no cumplirían las
reglas nuevas.

Ese último conteo es **solo informativo**: las restricciones se crean como `NOT VALID`, así que
aplican a los registros nuevos sin fallar por los datos de prueba que ya están en la tabla.

### Si algo sale mal

```bash
podman exec -i defensoria-db psql -U postgres -d defensoria_db <<'SQL'
ALTER TABLE public.quejas DROP CONSTRAINT IF EXISTS ck_quejas_nombre_quejoso;
ALTER TABLE public.quejas DROP CONSTRAINT IF EXISTS ck_quejas_apellido_paterno_quejoso;
ALTER TABLE public.quejas DROP CONSTRAINT IF EXISTS ck_quejas_apellido_materno_quejoso;
ALTER TABLE public.quejas DROP CONSTRAINT IF EXISTS ck_quejas_nombre_denunciado;
ALTER TABLE public.quejas DROP CONSTRAINT IF EXISTS ck_quejas_apellido_denunciado;
ALTER TABLE public.quejas DROP CONSTRAINT IF EXISTS ck_quejas_apellido_materno_denunciado;
ALTER TABLE public.quejas DROP CONSTRAINT IF EXISTS ck_quejas_numero_identificacion;
ALTER TABLE public.quejas DROP CONSTRAINT IF EXISTS ck_quejas_fecha_nacimiento_min;
ALTER TABLE public.queja_evidencias DROP CONSTRAINT IF EXISTS ck_evidencias_tamanio;
ALTER TABLE public.queja_evidencias DROP CONSTRAINT IF EXISTS ck_evidencias_tipo;
SQL
```

Las columnas nuevas se pueden dejar: son nullable y no estorban al backend viejo.

---

## 2. Frontend — VPS frontend (2.25.64.47)

### Compilar en tu equipo

```bash
cd Frontend
npm ci                # solo si cambiaste dependencias
ng build --configuration production
# genera dist/defensoria-front/browser/
```

> El build de producción necesita internet: inserta las fuentes de Google en el CSS.

### Subir y reconstruir el contenedor

```bash
# desde tu equipo
rsync -av --delete dist/defensoria-front/browser/ \
  usuario@2.25.64.47:/apps/aplicaciones/defensoria/front/dist/browser/

# en la VPS frontend
cd /apps/aplicaciones/defensoria/front
bash podman-compose-front.sh up
podman ps -f name=defensoria-web
```

Eso reconstruye la imagen `defensoria-front-img` y levanta `defensoria-web` en el 8090.
`router-nginx` no se toca.

### Comprobación

Abre https://defensoria-escom.ddns.net/queja/registro en una ventana de incógnito (para
saltarte la caché) y confirma que aparece el aviso de privacidad.

---

## 3. Backend — VPS backend (2.25.78.22)

### Compilar en tu equipo

```bash
cd Backend/queja-service
mvn clean package -DskipTests
# genera target/quejas-service.jar
```

O con el script que ya reconstruye todos los servicios:

```bash
cd Backend
bash rebuild-jars.sh      # deja los jars en Backend/_jars-listos/
```

### Subir y levantar

```bash
# desde tu equipo
scp Backend/queja-service/target/quejas-service.jar \
  usuario@2.25.78.22:/apps/aplicaciones/defensoria/back/artifact/quejas-service.jar

# en la VPS backend
cd /apps/aplicaciones/defensoria/back
bash podman-compose.sh up-container quejas-service
podman logs -f quejas-service
```

> **El nombre del jar importa:** el script busca `artifact/quejas-service.jar` (en plural),
> aunque la carpeta del código se llame `queja-service`. Ya está así en el `finalName` del
> `pom.xml`; solo no lo renombres al subirlo.

**La configuración NO cambia.** `config-files/quejas-service/config/quejas-service.yml` se
queda igual: el tope de 3MB de la credencial se aplica en el código, y `max-file-size: 30MB`
sigue siendo correcto para las evidencias.

### Comprobación

```bash
podman logs quejas-service | tail -30      # arranque sin errores
curl -k https://defensoria-escom.ddns.net/api/quejoso/quejas/validar-folio \
  -H 'Content-Type: application/json' -d '{"folio":"X","correo":"y@z.com"}'
```

---

## Resumen: qué archivos suben a dónde

| Qué | A dónde | Servidor |
|---|---|---|
| `migracion_cu_q01_validaciones.sql` | `/apps/aplicaciones/defensoria/basedatos/` → `podman cp` al contenedor `defensoria-db` | 2.25.78.22 |
| `dist/defensoria-front/browser/` (contenido) | `/apps/aplicaciones/defensoria/front/dist/browser/` | 2.25.64.47 |
| `quejas-service.jar` | `/apps/aplicaciones/defensoria/back/artifact/quejas-service.jar` | 2.25.78.22 |

Nada más. La config de Nginx, los yml de producción y los demás microservicios no se tocan.

---

## Pruebas después de desplegar

En https://defensoria-escom.ddns.net/queja/registro:

| # | Prueba | Resultado esperado |
|---|---|---|
| 1 | Entrar a la pantalla | Sale el aviso de privacidad; la casilla está gris hasta deslizar al final |
| 2 | Marcar "He leído y acepto" | El modal se cierra y se puede capturar |
| 3 | Escribir "Juan3" en Nombre | No deja teclear el 3 |
| 4 | Pegar "Juan3" en Nombre y enviar | Error: solo se permiten letras |
| 5 | Escribir "María José" y "Pérez-Gómez" | Se aceptan |
| 6 | Correo `juan_perez@gmail.com` | Error de reglas de Gmail |
| 7 | Correo `juan-perez@outlook.com` | Se acepta |
| 8 | Fecha de nacimiento 2026 | El calendario no la ofrece; si se fuerza, el backend la rechaza |
| 9 | Boleta con letras | No deja teclearlas; el 11º dígito tampoco entra |
| 10 | Subir un PDF como identificación | Error: debe ser JPG o PNG |
| 11 | Subir una imagen de 4MB como identificación | Error: máximo 3MB |
| 12 | Subir 3 imágenes de identificación | La tercera se rechaza |
| 13 | Subir 1 o 2 imágenes válidas | Se ve la miniatura, con nombre y peso |
| 14 | Adjuntar evidencias | Se ven las miniaturas; las no-imagen muestran su extensión |
| 15 | Sección 2 | Se titula "Lugar de los hechos" |
| 16 | Sección 3 | Tiene "Segundo Apellido del denunciado" |
| 17 | Descripción de 5 caracteres | Error: mínimo 20 |
| 18 | Enviar el caso feliz completo | Folio generado, acuse con el segundo apellido y la versión del aviso |
| 19 | Consultar en BD | `SELECT apellido_materno_denunciado, aviso_privacidad_aceptado, aviso_privacidad_version FROM quejas ORDER BY id DESC LIMIT 1;` |
| 20 | Tipo de evidencia en BD | `SELECT nombre_archivo, tipo, tamanio_bytes FROM queja_evidencias WHERE queja_id = (SELECT max(id) FROM quejas);` |

### Prueba del control real (sin pasar por el formulario)

El navegador no es la única forma de llegar al endpoint. Para comprobar que la validación
vive de verdad en el servidor:

```bash
# Renombrar un archivo no-imagen a .jpg NO debe alcanzar para pasar como credencial
curl -X POST https://defensoria-escom.ddns.net/api/quejoso/quejas/registro-publico \
  -F 'nombre=Juan' -F 'apellidoPaterno=Perez' -F 'correo=juan@dominio.com' \
  -F 'fechaNacimiento=2000-01-01' -F 'tipoIdentificacion=alumno' \
  -F 'numeroIdentificacion=2019630123' -F 'unidadAcademicaClave=ESCOM' \
  -F 'fechaHechos=2026-08-01' -F 'descripcion=Descripcion suficientemente larga de prueba' \
  -F 'avisoPrivacidadAceptado=true' \
  -F 'archivos=@cualquier-cosa.exe;filename=IDENTIFICACION_credencial.jpg'
# Esperado: 400 con "La identificación oficial debe ser una imagen JPG o PNG."
```

---

## Respaldo

Los archivos originales que se modificaron quedaron en
`_backups/cu-q01-20260908/`. Si algo sale mal, se restauran desde ahí y se vuelve a compilar.

En esa misma carpeta quedó `queja-service-src.tgz`, que se usó para compilar y verificar el
paquete de validaciones. Se puede borrar.
