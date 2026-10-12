# DDP Resumen Service

Microservicio experimental para generar un **borrador narrativo de resumen** de una queja de la Defensoría de Derechos Politécnicos (DDP). La arquitectura conserva el extractor D9/MMR evaluado y utiliza Qwen únicamente como editor de la captura extractiva.

> La salida de IA es una sugerencia y **requiere revisión humana**. No sustituye la valoración ni la redacción jurídica del personal de la Defensoría.

## Arquitectura

1. FastAPI recibe `descripcion`.
2. D9/MMR selecciona fragmentos de alto impacto conservando el texto de la queja.
3. D9 mantiene su comportamiento congelado: Skip-gram local por queja y `D9_EPOCHS=500` por defecto.
4. `qwen.py` envía la captura D9 por HTTP a un `llama-server` independiente.
5. Qwen actúa como editor conservador para mejorar cohesión y legibilidad; no recibe la instrucción de volver a resumir libremente.
6. La respuesta incluye tanto el borrador como `captura_d9` para facilitar la revisión humana.

La versión actual **no utiliza el validador heurístico anterior** como autoridad de fidelidad. Las pruebas mostraron que una validación superficial no garantiza que un modelo generativo preserve actores, temporalidad o causalidad.

## Estructura

```text
resumen-service/
├── app/
│   ├── __init__.py
│   ├── d9.py
│   ├── main.py
│   ├── pipeline.py
│   ├── qwen.py
│   └── schemas.py
├── data/README.txt
├── models/README.txt
├── scripts/smoke_test.py
├── .env.example
├── .gitignore
├── Containerfile
├── Containerfile.llama
├── requirements.txt
└── README.md
```

El GGUF se distribuye por separado para no incorporar varios GB al ZIP del código.

## Variables

```text
D9_EPOCHS=500
QWEN_SERVER_URL=http://10.89.0.2:8080
QWEN_MODEL_ID=Qwen/Qwen3-4B-Instruct-2507-Q4_K_M
QWEN_TIMEOUT=120
```

La IP de `QWEN_SERVER_URL` corresponde al despliegue probado y debe adaptarse si cambia la red de contenedores.

## Construcción del microservicio

```bash
podman build -t defensoria-resumen-service:final -f Containerfile .
```

## Construcción de llama.cpp

```bash
podman build -t ddp-llama-cpu:final -f Containerfile.llama .
```

El despliegue probado monta el GGUF en `/models` y ejecuta `llama-server` con CPU:

```bash
podman run -d \
  --name qwen-gguf \
  --network ddp-ia-net \
  --ip 10.89.0.2 \
  -v "$(pwd)/models/gguf:/models:ro" \
  --entrypoint llama-server \
  ddp-llama-cpu:final \
  -m /models/qwen3-4b-instruct-2507-Q4_K_M.gguf \
  --host 0.0.0.0 --port 8080 -t 8 -c 4096 -np 1
```

Después puede iniciarse FastAPI:

```bash
podman run -d \
  --name resumen-service \
  --network ddp-ia-net \
  -p 8090:8080 \
  -e D9_EPOCHS=500 \
  -e QWEN_SERVER_URL=http://10.89.0.2:8080 \
  -e QWEN_TIMEOUT=120 \
  defensoria-resumen-service:final
```

## API

`GET /health` comprueba que el proceso FastAPI está disponible; no constituye una prueba de conectividad de Qwen.

`POST /resumir` recibe:

```json
{
  "descripcion": "Texto de la queja...",
  "usar_fallback": true
}
```

`usar_fallback` se conserva temporalmente por compatibilidad con clientes anteriores, pero la arquitectura editor actual no utiliza el antiguo fallback del validador.

Respuesta:

```json
{
  "resumen": "Borrador editado...",
  "requiere_revision": true,
  "fuente_final": "D9_QWEN_EDITOR",
  "captura_d9": "Fragmentos seleccionados por D9...",
  "version_pipeline": "d9-qwen-editor-1.0"
}
```

## Prueba rápida

Con ambos servicios activos:

```bash
python scripts/smoke_test.py
```

## Dependencias

El microservicio no carga Qwen mediante PyTorch/Transformers. Qwen vive en el proceso separado de llama.cpp. Por ello se eliminaron `torch`, `transformers`, `accelerate` y `sentencepiece` del contenedor FastAPI. TensorFlow CPU se conserva porque D9 lo importa internamente para su Skip-gram local.

## Nota de investigación

D9/MMR se mantiene congelado para conservar comparabilidad con la evaluación realizada. Cambiar sus épocas, selección o entrenamiento local implica una nueva variante y debe volver a evaluarse. En una etapa posterior, las correcciones de especialistas pueden conservarse como pares de entrenamiento (fragmentos D9 / borrador IA / versión final revisada) para estudiar un ajuste fino especializado en edición conservadora.
