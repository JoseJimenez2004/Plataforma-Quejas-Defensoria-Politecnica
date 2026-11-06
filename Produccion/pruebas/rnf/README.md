# Pruebas de requerimientos no funcionales (pipeline)

Pipeline de GitHub Actions + scripts que verifican los RNF de la Tabla 5.5 agrupados por
característica de ISO/IEC 25010. El detalle (casos, criterios, trazabilidad) está en
`claude/PIPELINE-PRUEBAS-RNF.md` del proyecto (y §6 de `PLAN-PRUEBAS.md`).

## Instalación del pipeline

1. Copiar `ci/pruebas-rnf.yml` a la **raíz** del repositorio: `.github/workflows/pruebas-rnf.yml`
   (GitHub no lee workflows dentro de `Produccion/`).
2. En GitHub → *Settings → Secrets and variables → Actions*:
   - **Variables:** `URL_PRUEBAS` (entorno de pruebas, nunca producción) y `DOMINIO_TLS`.
   - **Secrets:** `CRED_QUEJOSO`, `CRED_RECEPCIONISTA`, `CRED_ANALISTA_PRIMER_CONTACTO`,
     `CRED_SUBDEFENSOR` y `CRED_DEFENSOR` con formato `correo:password`
     de cuentas **de prueba**.
3. Las etapas 1–3 corren solas en cada push a `Produccion`. Las 4–8 se lanzan con
   *Actions → Pruebas RNF → Run workflow* o cada lunes.

## Etapas

| # | Etapa | Característica | Requerimientos | Herramienta | Cuándo |
|---|---|---|---|---|---|
| 1 | Mantenibilidad | Mantenibilidad | Coding style, EST-01 | Checkstyle 10 | cada push |
| 2 | Funcionalidad | Funcionalidad + Fiabilidad unitaria | RF/RN, RNF-Q04, RNF-R04, RNF-A04 | JUnit 5, Mockito, JaCoCo, `ng build`/`ng test` | cada push |
| 3 | Seguridad estática | Seguridad | RNF-A06, RN-R02, RNF-R02, SEG-04, SEG-05 | `verificar_autorizacion.py`, gitleaks, Trivy | cada push |
| 4 | Seguridad dinámica | Seguridad | SEG-01, RNF-Q06, RNF-A03, SEG-02/03 | `matriz-roles.sh`, OWASP ZAP | manual / semanal |
| 5 | Confidencialidad | Seguridad › Confidencialidad | RNF-Q02 | `confidencialidad.sh` (openssl, curl) | manual / semanal |
| 6 | Capacidad | Eficiencia › Capacidad | RNF-Q03, REN-01 | `capacidad.sh`, k6 | manual / semanal |
| 7 | Fiabilidad | Fiabilidad › Recuperabilidad | FIA-03 (RF-A06) | `respaldo-restaurable.sh` | manual / semanal |
| 8 | Usabilidad *(por definir)* | Usabilidad | RNF-Q05, RNF-Q07, RNF-A05 | Lighthouse CI (+ SUS manual) | manual / semanal |
| 9 | Informe | — | todos | resumen en la pestaña *Summary* | siempre |

## Correr una prueba en local

```bash
cd Produccion/pruebas/rnf
# Estática (no necesita el sistema levantado)
python3 seguridad/extraer_endpoints.py ../../Backend seguridad/matriz-endpoints.csv
python3 seguridad/verificar_autorizacion.py seguridad/matriz-endpoints.csv seguridad/reglas-autorizacion.csv
java -jar checkstyle-10.21.4-all.jar -c estilo/checkstyle-defensoria.xml ../../Backend/queja-service/src/main/java

# Dinámicas (contra el entorno de pruebas)
DOMINIO=defensoria-escom.ddns.net bash confidencialidad/confidencialidad.sh
BASE_URL=https://pruebas... CRED_DEFENSOR='defensora@ipn.mx:****' bash seguridad/matriz-roles.sh
BASE_URL=https://pruebas... bash capacidad/capacidad.sh
k6 run -e BASE_URL=https://pruebas... capacidad/carga.k6.js
```

## Archivos

```
pruebas/rnf/
├── ci/pruebas-rnf.yml                 workflow (copiar a .github/workflows/)
├── estilo/checkstyle-defensoria.xml   guía de estilo = Google Java Style + desviaciones D1–D4
├── mantenibilidad/cobertura.py        compuerta JaCoCo >= 70 % en service/validacion/util
├── seguridad/
│   ├── extraer_endpoints.py           genera la matriz rol x endpoint desde los controllers
│   ├── matriz-endpoints.csv           92 endpoints de los 8 servicios propios
│   ├── reglas-autorizacion.csv        lo que exigen los RNF/RN (editable)
│   ├── verificar_autorizacion.py      compara código vs. reglas (prueba estática)
│   └── matriz-roles.sh                misma matriz en vivo con un token por rol (dinámica)
├── confidencialidad/confidencialidad.sh
├── capacidad/{capacidad.sh, carga.k6.js}
├── fiabilidad/respaldo-restaurable.sh
└── usabilidad/lighthouserc.json
```

Pruebas JUnit nuevas (Fiabilidad, RNF-Q04/RNF-R04):
`Backend/queja-service/src/test/.../NotificacionClienteServiceResilienciaTest.java` y
`Backend/revision-service/src/test/.../NotificacionQuejaServiceResilienciaTest.java`.
