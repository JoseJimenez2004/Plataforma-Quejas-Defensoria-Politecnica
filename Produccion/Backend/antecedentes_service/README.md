# DDP — Antecedentes V5 (Python)

Microservicio independiente. No modifica Java ni escribe en PostgreSQL.

## Fuentes

- `defensoria_db` / `public.quejas` (PostgreSQL puerto host 5432)
- `defensoria_historico_db` / `public.quejas_historicas` (puerto host 5433)

Ambas consultas son **solo lectura**. Los nombres de columnas se verificaron con los metadatos compartidos. El esquema de credenciales, red y permisos todavía debe validarse en el servidor.

## API

**Compatibilidad con Java existente:** `POST /api/antecedentes/buscar`

```json
{"texto":"Narración de la queja", "umbral":0.0, "topK":500}
```

Devuelve `{"antecedentes": [...]}`. Cada elemento es plano e incluye `folio`, `similitud` (0–1), `historico`, `quejoso`, `denunciado`, `texto_original`, `fragmento`, `fecha_hechos`, `tipo_violencia`, `unidad_academica` y `estatus`, compatibles con `MotorAntecedentesModelo.java` del ZIP recibido. **Java solo envía texto y filtra las personas después**. No es posible calcular las tres dimensiones para la queja entrante a partir de ese contrato sin información adicional.

**API completa V5:** `POST /api/v5/antecedentes/buscar`

```json
{
  "folio":"FOL-001",
  "descripcion":"Narración de los hechos",
  "fechaHechos":"2026-09-15",
  "unidadAcademicaClave":"ESCOM",
  "quejoso":{"numeroIdentificacion":"BOLETA-001"},
  "denunciado":{"numeroIdentificacion":"EMP-001"},
  "topK":10
}
```

Devuelve `por_hechos`, `por_involucrados` y `coincidencias_fuertes` por separado. V5 usa identificaciones exactas normalizadas; no equipara personas por nombres. La base actual no tiene ID del denunciado en `quejas`, por lo que la dimensión correspondiente será limitada. `folio` excluye la propia queja de origen `SISTEMA`. No enviar nombres ni narraciones reales en tickets o registros.

## Algoritmo

Modelo `paraphrase-multilingual-MiniLM-L12-v2` con embeddings normalizados. Similaridad textual = 0.85 × coseno semántico + 0.15 × coseno TF-IDF (unigramas/bigramas, `min_df=2` si es posible). Identidad independiente por `numeroIdentificacion`. Coincidencia fuerte = identidad + similitud de hechos ≥ 0.50 entre los primeros 100 candidatos. Metadatos de resolución no intervienen en el puntaje. Resultados son evidencia para revisión, no veredictos.

## Ejecutar localmente

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export SYSTEM_DATABASE_URL='postgresql://...'
export HISTORIC_DATABASE_URL='postgresql://...'
uvicorn app.main:app --host 127.0.0.1 --port 8080
```

En contenedor:

```bash
podman build -t ddp-antecedentes-v5 -f Containerfile .
podman run --rm --name antecedentes-v5-test --env-file .env -p 127.0.0.1:8095:8080 ddp-antecedentes-v5
```

No usar puerto 8093 del servicio anterior hasta validar. `GET /health` solo verifica que el proceso esté vivo. La primera búsqueda descarga/carga el modelo y puede superar el timeout de lectura de Java (15 segundos); precalentar el modelo antes de cambiar tráfico. La implementación actual vuelve a leer y reindexar los casos en cada petición: adecuada para prototipo con pocos registros, no para producción con corpus grande. Para producción, implementar caché/versionado del índice, actualización segura, métricas y controles de acceso/autorización HTTP.

## Pruebas

```bash
pip install pytest
pytest -q
```

## Pendientes del equipo (sin cambios automáticos)

1. **Infraestructura/BD:** conexión privada desde el host de Python a los puertos PostgreSQL del servidor de BD, usuarios separados con permisos `SELECT` exclusivamente y restricciones de firewall/TLS apropiadas.
2. **Primer Contacto (opcional para V5 completo):** enviar folio y objeto de queja con identificadores al nuevo endpoint `/api/v5/antecedentes/buscar`, o exponer un mecanismo autenticado para obtener el folio entrante. El endpoint antiguo ya puede recibir peticiones sin tocar Java, pero solo ofrece ranking por hechos.
3. **Primer Contacto (operación):** confirmar `antecedentes.modelo.url` y cómo se enrutará al nuevo contenedor, así como el timeout de 15 s.
4. **Seguridad:** autenticación entre servicios, autorización de consulta, trazabilidad sin datos sensibles, tratamiento de datos personales y no exponer las bases de datos públicamente.
5. **Validación:** probar con casos sintéticos/anonimizados y verificar el comportamiento con la base histórica vacía.

**No se ha desplegado ni probado contra las bases reales.**

## Actualización: personas como entrada (sin romper el contrato anterior)

El endpoint legado `POST /api/antecedentes/buscar` **ahora acepta opcionalmente** `quejoso`, `denunciado`, `folio` y `fechaHechos`, además de `texto`, `umbral` y `topK`:

```json
{
  "texto": "Relato ficticio de los hechos",
  "quejoso": {
    "nombre": "Ana", "apellido1": "Ejemplo", "apellido2": "Prueba",
    "numeroIdentificacion": "ID-001"
  },
  "denunciado": {
    "nombre": "Luis", "apellido1": "Ejemplo", "apellido2": "Prueba",
    "numeroIdentificacion": "ID-002"
  },
  "umbral": 0.0,
  "topK": 10
}
```

**Compatibilidad:** las peticiones antiguas con solo `texto` siguen siendo válidas y el endpoint legado **sigue devolviendo únicamente** `{"antecedentes":[...]}` con el esquema habitual. El endpoint legado **no devuelve** los rankings de identidad, aunque admite los datos de personas; para consultar las tres dimensiones se debe usar el endpoint estructurado `POST /api/v5/antecedentes/buscar` (campo narrativo `descripcion`, no `texto`):

```json
{
  "descripcion": "Relato ficticio de los hechos",
  "quejoso": {"nombre":"Ana","apellido1":"Ejemplo","apellido2":"Prueba", "numeroIdentificacion":"ID-001"},
  "denunciado": {"nombre":"Luis","apellido1":"Ejemplo","apellido2":"Prueba", "numeroIdentificacion":"ID-002"},
  "topK": 10
}
```

El endpoint estructurado devuelve `por_hechos`, `por_involucrados` y `coincidencias_fuertes`. En `por_involucrados`, `coincide_nombre_quejoso` y `coincide_nombre_denunciado` son **señales tentativas** basadas en el nombre completo normalizado (nombre y apellidos), no pruebas de identidad. `mismo_quejoso` y `mismo_denunciado` solo son verdaderos con identificación exacta normalizada. **Las coincidencias fuertes requieren identificación**, además de similitud textual; un nombre coincidente por sí solo nunca produce una coincidencia fuerte. La fuente `public.quejas` actualmente no suministra número de identificación del denunciado, por lo que ese criterio no podrá verificarse en registros de esa tabla.

**Despliegue:** este ZIP es código fuente actualizado; **no se ha reconstruido ni desplegado** sobre el contenedor que atiende el puerto 8093. Antes de sustituirlo, ejecutar pruebas, validar con casos sintéticos, revisar permisos y el contrato de Primer Contacto, y disponer de reversión. Nunca colocar credenciales ni datos reales en el repositorio o los logs.

## Optimización CPU — versión de este paquete

- Modelo cargado **solo en CPU** (`device='cpu'`), un único worker de Uvicorn y 2 hilos de PyTorch/BLAS por defecto. Imagen con rueda PyTorch de CPU; sin CUDA.
- El corpus se consulta a PostgreSQL y se vectoriza **una sola vez por ciclo de actualización**; cada búsqueda solo calcula el embedding de la consulta, un producto matriz-vector y TF-IDF de la consulta. No se recalculan embeddings del corpus por petición.
- Caché en memoria con intercambio atómico al refrescar, TTL configurable (`INDEX_TTL_SECONDS`, 300 s por defecto), margen de obsolescencia (`INDEX_MAX_STALE_SECONDS`, 60 s) y limitación de reintentos fallidos. Si la BD falla y la caché supera el margen, la API devuelve 503; no inventa resultados vacíos.
- `/health`: proceso vivo. `/ready`: intenta cargar/actualizar el índice y devuelve 503 si no puede; **verificar `/ready` y una búsqueda de prueba antes de cambiar tráfico**. El primer calentamiento puede tardar bastante con corpus grande.
- Para operar sin Internet montar en `/models` una caché Hugging Face que incluya `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`. `MODEL_OFFLINE=1`, `HF_HUB_OFFLINE=1`, `TRANSFORMERS_OFFLINE=1` vienen activados. Si el modelo no está precargado, el índice fallará (503); no descarga automáticamente.
- Se recomienda `--memory 2g` **solo como punto de partida**: verificar RAM, p95 de latencia y número de casos con la base real antes de considerar el sistema listo para producción. Mantener una sola réplica mientras se use esta caché por proceso.
- El TTL significa que altas/cambios en la BD pueden tardar hasta 5 minutos en reflejarse. Para actualización inmediata se necesita un mecanismo autenticado de invalidación o refresco coordinado; no se incluye un endpoint público para hacerlo.
- La comparación de nombres es una coincidencia tentativa; las coincidencias fuertes requieren identificación exacta. La tabla del sistema no contiene identificador del denunciado, por lo que esa coincidencia fuerte solo será posible con fuentes que lo incluyan.
- **No se ha probado contra el servidor de producción ni se ha sustituido el contenedor actual.** Antes del despliegue: probar `podman build` en un entorno aislado, comprobar esquema PostgreSQL, revisar memoria y tiempos, confirmar integración y conservar la reversión Java.

### Ejemplo de prueba aislada (NO sustituir 8093)

```bash
podman build -t localhost/defensoria-antecedentes-v5:cpu-optimizado -f Containerfile .
podman run --rm -d --name antecedentes-v5-cpu-opt-test \
  -p 127.0.0.1:8100:8080 --memory 2g --env-file .env \
  -v /apps/aplicaciones/defensoria/back/antecedentes-v5/model-cache:/models:ro \
  localhost/defensoria-antecedentes-v5:cpu-optimizado
curl -i http://127.0.0.1:8100/ready
```

No compartir archivos `.env` ni salidas con datos personales.
