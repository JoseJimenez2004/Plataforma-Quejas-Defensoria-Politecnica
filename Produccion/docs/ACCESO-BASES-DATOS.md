# Acceso a las bases de datos

> ⚠️ **ESTE ARCHIVO CONTIENE CREDENCIALES DE PRODUCCIÓN.**
> No debe subirse a GitHub ni compartirse fuera del equipo. Está incluido en el
> `.gitignore` de `Produccion/` — verifica con `git check-ignore -v docs/ACCESO-BASES-DATOS.md`
> antes de cualquier `git add`. Si alguna vez se sube por error, no basta con borrarlo:
> hay que rotar todas las contraseñas que aparecen aquí.

Última actualización: 2026-09-17

---

## 1. Dónde vive todo

Hay **un solo servidor Postgres** para las dos bases. Corre en un contenedor Podman
llamado `defensoria-db` (imagen `postgres:16`) dentro de la VPS de backend.

| Dato | Valor |
|---|---|
| VPS backend | `2.25.78.22` (srv1804187) |
| Contenedor | `defensoria-db` |
| Imagen | `docker.io/library/postgres:16` |
| Puerto publicado | `5432` → `5432` |
| Usuario | `postgres` |
| Contraseña | `Temporal2026@` |

**Dos bases distintas no necesitan dos puertos.** Un servidor Postgres aloja varias bases
en el mismo 5432; lo único que cambia es el nombre al final de la cadena de conexión.

| Base | Para qué | La usan |
|---|---|---|
| `defensoria_db` | Todo el sistema en operación: quejas, usuarios, expedientes, catálogos, notificaciones, personal | auth · quejas · notificaciones · catalogo · admin · revision · chatbot · primer-contacto · subdefensoria |
| `defensoria_historico_db` | Quejas de años anteriores al lanzamiento, capturadas a mano. Solo consulta | historico-service |

Cadenas de conexión tal como quedan en `config-files/*/config/*.yml`:

```
jdbc:postgresql://2.25.78.22:5432/defensoria_db
jdbc:postgresql://2.25.78.22:5432/defensoria_historico_db
```

---

## 2. Cómo entrar

### Opción A — Ya estás en la VPS por SSH (lo más rápido)

No pide contraseña porque entras por dentro del contenedor.

```bash
# base de operación
podman exec -it defensoria-db psql -U postgres -d defensoria_db

# base histórica
podman exec -it defensoria-db psql -U postgres -d defensoria_historico_db
```

Para una sola consulta, sin abrir el prompt:

```bash
podman exec -it defensoria-db psql -U postgres -d defensoria_historico_db \
  -c "SELECT count(*) FROM quejas_historicas;"
```

Ejecutar un archivo `.sql` completo (ojo: `-i`, no `-it`):

```bash
podman exec -i defensoria-db psql -U postgres -d defensoria_historico_db < archivo.sql
```

### Opción B — Desde tu máquina, con cliente gráfico

**No apuntes el cliente a `2.25.78.22:5432` directamente.** Abre un túnel SSH y conéctate
a tu propia máquina; el tráfico va cifrado y no hace falta exponer el puerto a internet.

```bash
# deja esta terminal abierta mientras trabajas
ssh -L 5433:localhost:5432 root@2.25.78.22
```

Y en DBeaver / VS Code / el cliente que uses:

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Puerto | `5433` |
| Base | `defensoria_db` o `defensoria_historico_db` |
| Usuario | `postgres` |
| Contraseña | `Temporal2026@` |

---

## 3. Comandos que vas a necesitar

Dentro del prompt de `psql`:

```
\l                      -- listar todas las bases
\c defensoria_db        -- cambiar de base sin salir
\dt                     -- listar tablas de la base actual
\d quejas               -- ver columnas y restricciones de una tabla
\di                     -- listar índices
\q                      -- salir
```

Conteos rápidos:

```sql
-- defensoria_db
SELECT count(*) FROM quejas;
SELECT estatus, count(*) FROM quejas GROUP BY 1;

-- defensoria_historico_db
SELECT count(*) FROM quejas_historicas;
SELECT motivo, count(*) FROM quejas_historicas GROUP BY 1 ORDER BY 2 DESC;
```

---

## 4. Respaldo y restauración

Cada base se respalda por separado — restaurar una no toca la otra, que es justo el
motivo por el que el histórico vive aparte.

```bash
# estructura + datos
podman exec -t defensoria-db pg_dump -U postgres -F p defensoria_db \
  > respaldo_defensoria_$(date +%Y%m%d).sql

podman exec -t defensoria-db pg_dump -U postgres -F p defensoria_historico_db \
  > respaldo_historico_$(date +%Y%m%d).sql

# solo estructura (chico, útil para comparar cambios de esquema)
podman exec -t defensoria-db pg_dump -U postgres --schema-only defensoria_db \
  > estructura_$(date +%Y%m%d).sql

# restaurar
podman exec -i defensoria-db psql -U postgres -d defensoria_db < respaldo.sql
```

`defensoria_db` también se respalda sola todos los días a las 4:00 AM, y desde el panel
de administración (CU-A48 / CU-A49 / CU-A50) se puede generar, descargar y restaurar.
**Ese panel solo conoce `defensoria_db`**: el histórico hay que respaldarlo a mano hasta
que se agregue al módulo.

---

## 5. Otras credenciales del sistema

No son de base de datos, pero se necesitan para levantar el entorno completo.

| Qué | Dónde se configura | Valor |
|---|---|---|
| Secreto JWT | `jwt.secret` en los `config-files/*/config/*.yml` | `defensoria-ddp-jwt-secret-produccion-muy-segura-2026` |
| SMTP (host / puerto) | `spring.mail` en auth-service y notificaciones-service | `smtp.gmail.com` · `587` |
| SMTP (cuenta) | igual | `josebryanomar2004@gmail.com` |
| SMTP (App Password) | igual | `kstwxawsnsqkzhnk` |

El secreto JWT **es el mismo en todos los microservicios a propósito**: es lo que permite
que un token emitido por auth-service lo valide quejas-service. Si se cambia, hay que
cambiarlo en los diez a la vez o el sistema deja de reconocer sesiones.

La contraseña SMTP es una App Password de Google y está amarrada a esa cuenta exacta.
Si se cambia la cuenta remitente, hay que generar una nueva desde esa cuenta — una App
Password de una cuenta no funciona con otra. (Ese fue el origen del `Authentication
failed` del 2026-09-17.)

---

## 6. Pendientes de seguridad

Cosas que hoy funcionan pero conviene arreglar antes de que esto se use en serio:

1. **Los diez microservicios se conectan como `postgres`**, el superusuario. Lo correcto
   sería un usuario por servicio con permisos solo sobre sus tablas. Así, comprometer un
   servicio no da control de toda la base.
2. **`historico-service` también entra como superusuario** a una base que solo necesita
   leer y escribir dos tablas.
3. **El puerto 5432 se publica en `0.0.0.0`.** Si el firewall de Hostinger no lo tiene
   cerrado hacia internet, cualquiera puede intentar conectarse. Con `podman exec` y el
   túnel SSH no hace falta que esté abierto.
4. **Las contraseñas están en texto plano** en los `config-files`. Lo estándar sería
   pasarlas por variables de entorno o un gestor de secretos.
5. **`Temporal2026@` se llama "temporal" y lleva meses en producción.**
