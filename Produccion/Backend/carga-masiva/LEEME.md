# Carga masiva de quejas

Registra muchas quejas de golpe usando el mismo endpoint del formulario público. Por eso cada queja:

- pasa por las mismas validaciones;
- recibe su folio real;
- guarda su credencial y sus evidencias igual que si alguien la hubiera llenado a mano.

Si se le da una contraseña, también deja lista la cuenta del quejoso.

## Archivos

| Archivo | Para qué sirve |
|---|---|
| `cargar-quejas.sh` | Lee el `.csv` y manda cada queja con `curl`. |
| `generar-csv.py` | Crea un `.csv` con las 170 quejas del dataset del modelo de IA. |
| `quejas-ejemplo.csv` | Ejemplo con 2 quejas: una con el texto directo y otra con el texto en un archivo. |
| `descripciones/` | Textos largos, uno por archivo. En el `.csv` se ponen como `@descripciones/archivo.txt`. |

## Paso a paso (en la VPS backend)

```bash
# 1. Subir la carpeta a la VPS backend, p. ej. /apps/aplicaciones/defensoria/back/carga-masiva
cd /apps/aplicaciones/defensoria/back/carga-masiva

# 2a. Usar tu propio .csv (ver formato abajo), o
# 2b. generar uno con el dataset sintético (170 quejas, todas con contraseña Prueba2026!)
python3 generar-csv.py ../modelo/Dataset/quejas_sinteticas.json --password 'Prueba2026!'
#     -> lote/quejas.csv + lote/descripciones/*.txt   (--cantidad 20 para solo unas cuantas)

# 3. Cargar (primero prueba con quejas-ejemplo.csv)
bash cargar-quejas.sh lote/quejas.csv

# 4. Crear las cuentas: copiar lote/salida-*/cuentas-quejosos.sql a la VPS de BD y ahí:
podman exec -i defensoria-db psql -U postgres -d defensoria_db < cuentas-quejosos.sql
```

Cada corrida deja una carpeta `salida-AAAAMMDD-HHMMSS/` junto al `.csv` con:

- `resultados.csv`: el folio de cada queja o el motivo por el que falló;
- `cuentas-quejosos.sql`.

## Formato del .csv

Va separado con `|`, porque las redacciones llevan comas. La primera línea es el encabezado, y las líneas que empiezan con `#` se ignoran.

```
nombre|apellido1|apellido2|correo|fecha_nacimiento|tipo|numero|unidad|fecha_hechos|den_nombre|den_apellido1|den_apellido2|descripcion|password|credenciales|evidencias
```

| Campo | Valor |
|---|---|
| fechas | `AAAA-MM-DD` |
| tipo | `alumno` o `empleado` |
| unidad | Clave del catálogo de dependencias: `ESCOM`, `UPIICSA`, `ESIME-ZAC`, `ESCA-TEP`… |
| descripcion | El texto directo (sin `\|` ni saltos de línea) o `@ruta.txt`. No tiene límite de caracteres. |
| password | Contraseña de la cuenta del quejoso (8+ caracteres, una mayúscula y un número). Si va vacía se usa `PASSWORD_DEFAULT`; si tampoco hay, no se crea la cuenta. |
| credenciales | 1 o 2 imágenes JPG/PNG separadas por `;`. Vacío = una imagen de ejemplo. |
| evidencias | PDF/JPG/PNG/MP4/MP3 separados por `;`. Vacío = sin evidencias. `EJEMPLO` = un PDF de ejemplo. |

Las rutas relativas se buscan junto al `.csv`. Varias quejas pueden tener el mismo correo: la cuenta se crea una sola vez.

## Opciones (variables de entorno)

| Variable | Default | Uso |
|---|---|---|
| `CUENTAS` | `sql` | `sql` genera el `.sql` de cuentas sin mandar correos; `api` activa la cuenta por `/api/auth/activar-cuenta`; `no` no crea cuentas. |
| `PASSWORD_DEFAULT` | vacío | Contraseña para las filas sin contraseña. |
| `URL_QUEJAS` / `URL_AUTH` | `http://127.0.0.1:8084` / `:8083` | Para correrlo desde otra máquina: `https://defensoria-escom.ddns.net`. |
| `PAUSA` | `0` | Segundos de espera entre quejas. |

> **Ojo con `CUENTAS=api`:** auth-service manda un correo de bienvenida por cada cuenta. Con correos inventados (como los del dataset) esos correos rebotan, y Gmail puede bloquear la cuenta que envía. Por eso el modo por defecto es `sql`: la contraseña se cifra con BCrypt dentro de PostgreSQL (pgcrypto), en el mismo formato que usa auth-service.
>
> El registro de la queja no manda correo al quejoso; solo crea su notificación en el panel. La excepción es cuando la queja trae los datos de un tutor.
