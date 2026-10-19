package ipn.escom.defensoria.admin_service.service;

import static ipn.escom.defensoria.admin_service.service.SeguimientoQuejasService.ETAPA_SQL;
import static ipn.escom.defensoria.admin_service.service.SeguimientoQuejasService.etiquetaEstatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.admin_service.model.PanelModels.Actividad;
import ipn.escom.defensoria.admin_service.model.PanelModels.Carga;
import ipn.escom.defensoria.admin_service.model.PanelModels.CargaKpis;
import ipn.escom.defensoria.admin_service.model.PanelModels.Caso;
import ipn.escom.defensoria.admin_service.model.PanelModels.ContactoRequest;
import ipn.escom.defensoria.admin_service.model.PanelModels.DetallePersonal;
import ipn.escom.defensoria.admin_service.model.PanelModels.FilaUnidad;
import ipn.escom.defensoria.admin_service.model.PanelModels.MiembroCarga;
import ipn.escom.defensoria.admin_service.model.PanelModels.MiembroPersonal;
import ipn.escom.defensoria.admin_service.model.PanelModels.MotivoRechazo;
import ipn.escom.defensoria.admin_service.model.PanelModels.PaginaCasos;
import ipn.escom.defensoria.admin_service.model.PanelModels.Panel;
import ipn.escom.defensoria.admin_service.model.PanelModels.PanelKpis;
import ipn.escom.defensoria.admin_service.model.PanelModels.QuejaAsignada;
import ipn.escom.defensoria.admin_service.model.PanelModels.Regla;
import ipn.escom.defensoria.admin_service.model.PanelModels.ResumenUnidades;
import ipn.escom.defensoria.admin_service.model.PanelModels.TablaUnidades;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Conteo;

/**
 * Panel de la Defensora con el diseño del 2026-10-18 (vistas "Panel general", "Quejas por
 * unidad", "Carga de trabajo" y "Personal de la Defensoría"). Solo SQL de lectura sobre
 * {@code quejas}, {@code dependencias}, {@code personal_administrativo} y {@code bitacora_acciones};
 * la única escritura son el área y el teléfono del personal ({@link #guardarContacto}).
 *
 * <p>El periodo ({@code dias}) acepta 7, 30, 90, 365 o 0 (= todo el historial). El periodo
 * anterior es el bloque del mismo tamaño justo antes, para calcular el "↑ 12 %".
 */
@Service
public class PanelDefensoraService {

    private static final Logger log = LoggerFactory.getLogger(PanelDefensoraService.class);

    // ======================================================================================
    // Reglas de "Casos que requieren atención". Los umbrales son días naturales.
    // ======================================================================================

    static final int DIAS_SIN_TOMAR = 3;
    static final int DIAS_VALIDACION_DETENIDA = 1;
    static final int DIAS_OBSERVACIONES = 5;
    static final int DIAS_PRIMER_CONTACTO = 10;
    static final int DIAS_SIN_TURNAR_SUBDEFENSORIA = 15;
    static final int DIAS_SEGUIMIENTO = 60;

    static final List<Regla> REGLAS = List.of(
            new Regla("SIN_TOMAR", "Sin tomar",
                    "Lleva más de " + DIAS_SIN_TOMAR + " días en Recepción y ninguna recepcionista la ha abierto.", DIAS_SIN_TOMAR),
            new Regla("CORREGIDA_SIN_REVISAR", "Corregida sin revisar",
                    "El quejoso ya la corrigió hace más de " + DIAS_SIN_TOMAR + " días y nadie la ha vuelto a revisar.", DIAS_SIN_TOMAR),
            new Regla("VALIDACION_DETENIDA", "Validación detenida",
                    "Una recepcionista la abrió hace más de " + DIAS_VALIDACION_DETENIDA + " día y no la ha turnado ni regresado.",
                    DIAS_VALIDACION_DETENIDA),
            new Regla("OBSERVACIONES_PENDIENTES", "Observaciones pendientes",
                    "Se regresó al quejoso con observaciones hace más de " + DIAS_OBSERVACIONES + " días y no la ha corregido.",
                    DIAS_OBSERVACIONES),
            new Regla("SIN_ABOGADO", "Sin abogado asignado",
                    "Está en Primer Contacto y no tiene abogado responsable.", 0),
            new Regla("VENCIDA_PRIMER_CONTACTO", "Vencida (primer contacto)",
                    "Lleva más de " + DIAS_PRIMER_CONTACTO + " días turnada o en análisis sin dictamen.", DIAS_PRIMER_CONTACTO),
            new Regla("SIN_TURNAR_SUBDEFENSORIA", "Sin turnar a Subdefensoría",
                    "Se declaró procedente hace más de " + DIAS_SIN_TURNAR_SUBDEFENSORIA + " días y no ha pasado a Subdefensoría.",
                    DIAS_SIN_TURNAR_SUBDEFENSORIA),
            new Regla("VENCIDA_SEGUIMIENTO", "Vencida (seguimiento)",
                    "Lleva más de " + DIAS_SEGUIMIENTO + " días en Subdefensoría sin concluir.", DIAS_SEGUIMIENTO));

    private static final Map<String, String> ETIQUETA_REGLA = new LinkedHashMap<>();

    static {
        REGLAS.forEach(r -> ETIQUETA_REGLA.put(r.clave(), r.etiqueta()));
    }

    /**
     * Una fila por queja: si cumple varias reglas se queda la de menor "orden" (la más grave
     * o la que hay que resolver primero). {@code ref} es desde cuándo corre el plazo.
     */
    private static final String CASOS_SQL = """
            SELECT DISTINCT ON (c.folio) c.folio, c.motivo, c.ref FROM (
              SELECT q.numero_folio AS folio, 'SIN_TOMAR' AS motivo, 1 AS orden, q.fecha_creacion AS ref
                FROM quejas q WHERE coalesce(q.estatus,'RECIBIDA') = 'RECIBIDA'
                 AND q.fecha_creacion < now() - (:dSinTomar * interval '1 day')
              UNION ALL
              SELECT q.numero_folio, 'CORREGIDA_SIN_REVISAR', 2, coalesce(q.fecha_validacion, q.fecha_creacion)
                FROM quejas q WHERE q.estatus = 'CORREGIDA'
                 AND coalesce(q.fecha_validacion, q.fecha_creacion) < now() - (:dSinTomar * interval '1 day')
              UNION ALL
              SELECT q.numero_folio, 'VALIDACION_DETENIDA', 3, q.fecha_inicio_revision
                FROM quejas q WHERE q.estatus = 'EN_VALIDACION'
                 AND q.fecha_inicio_revision < now() - (:dValidacion * interval '1 day')
              UNION ALL
              SELECT q.numero_folio, 'OBSERVACIONES_PENDIENTES', 4, q.fecha_validacion
                FROM quejas q WHERE q.estatus = 'RECHAZADA'
                 AND q.fecha_validacion < now() - (:dObservaciones * interval '1 day')
              UNION ALL
              SELECT q.numero_folio, 'VENCIDA_PRIMER_CONTACTO', 5, q.fecha_turnado
                FROM quejas q WHERE q.estatus IN ('TURNADA','EN_ANALISIS')
                 AND q.fecha_turnado < now() - (:dPrimerContacto * interval '1 day')
              UNION ALL
              SELECT q.numero_folio, 'SIN_ABOGADO', 6, coalesce(q.fecha_turnado, q.fecha_validacion, q.fecha_creacion)
                FROM quejas q WHERE q.estatus IN ('TURNADA','EN_ANALISIS','PROCEDENTE')
                 AND (q.defensor_asignado IS NULL OR trim(q.defensor_asignado) = '')
              UNION ALL
              SELECT q.numero_folio, 'SIN_TURNAR_SUBDEFENSORIA', 7, q.fecha_turnado
                FROM quejas q WHERE q.estatus = 'PROCEDENTE'
                 AND q.fecha_turnado < now() - (:dSinTurnar * interval '1 day')
              UNION ALL
              SELECT q.numero_folio, 'VENCIDA_SEGUIMIENTO', 8, q.fecha_turnado
                FROM quejas q WHERE q.estatus IN ('RECIBIDA_SUBDEFENSORIA','EN_INVESTIGACION','EN_ESPERA_OFICIO',
                                                  'ELABORO_ACUERDO','PENDIENTE_CONCILIACION','PENDIENTE_CONCLUSION')
                 AND q.fecha_turnado < now() - (:dSeguimiento * interval '1 day')
            ) c
            ORDER BY c.folio, c.orden
            """;

    // ======================================================================================
    // Motivos de rechazo: textos estándar que manda Recepción (validacion.ts, MOTIVO_SI_NO)
    // ======================================================================================

    /** clave → {motivo corto, categoría, inicio del texto estándar}. "OTRO" no tiene texto. */
    static final Map<String, String[]> MOTIVOS = new LinkedHashMap<>();

    static {
        MOTIVOS.put("NOMBRE", new String[] {"El nombre no coincide con la identificación", "Identidad",
                "El nombre asentado en la queja no coincide"});
        MOTIVOS.put("IDENTIFICACION", new String[] {"Sin identificación oficial válida", "Documentación",
                "No se exhibió una identificación oficial"});
        MOTIVOS.put("BOLETA", new String[] {"Número de boleta incorrecto", "Matrícula",
                "El número de boleta registrado"});
        MOTIVOS.put("EMPLEADO", new String[] {"Número de empleado incorrecto", "Matrícula",
                "El número de empleado registrado"});
        MOTIVOS.put("PLAZO", new String[] {"Fuera del plazo para presentar la queja", "Oportunidad",
                "La queja no fue promovida dentro del término"});
        MOTIVOS.put("DENUNCIADO", new String[] {"Datos insuficientes de la persona señalada", "Persona señalada",
                "La queja no aporta elementos suficientes"});
        MOTIVOS.put("OTRO", new String[] {"Otro motivo / solo observaciones", "Otros", null});
    }

    /** Condición SQL (sobre q.) para un motivo del catálogo. */
    static String condicionMotivo(String clave) {
        String[] m = MOTIVOS.get(clave);
        if (m == null) {
            return null;
        }
        String primeraParte = "split_part(q.motivo_rechazo, E'\\n\\n', 1)";
        if (m[2] != null) {
            return "q.motivo_rechazo IS NOT NULL AND position('" + m[2].replace("'", "''") + "' in " + primeraParte + ") > 0";
        }
        StringBuilder ninguno = new StringBuilder("q.motivo_rechazo IS NOT NULL AND trim(q.motivo_rechazo) <> ''");
        MOTIVOS.values().stream().filter(v -> v[2] != null).forEach(v -> ninguno
                .append(" AND position('").append(v[2].replace("'", "''")).append("' in ").append(primeraParte).append(") = 0"));
        return ninguno.toString();
    }

    // ======================================================================================
    // Tipo de unidad, a partir de la categoría del catálogo de dependencias del IPN
    // ======================================================================================

    static final String TIPO_UNIDAD_SQL = """
            CASE
              WHEN d.clave IS NULL THEN 'OTROS'
              WHEN d.categoria ILIKE '%Medio Superior%' THEN 'MEDIO_SUPERIOR'
              WHEN d.categoria ILIKE '%Nivel Superior%' AND d.nombre ILIKE 'Unidad Profesional%' THEN 'UNIDAD_PROFESIONAL'
              WHEN d.categoria ILIKE '%Nivel Superior%' THEN 'ESCUELA_SUPERIOR'
              WHEN d.categoria ILIKE '%Investigaci%' THEN 'INVESTIGACION'
              ELSE 'OTROS'
            END""";

    static final Map<String, String[]> TIPOS = new LinkedHashMap<>();

    static {
        // clave → {singular (tabla), plural (gráfica)}
        TIPOS.put("ESCUELA_SUPERIOR", new String[] {"Escuela Superior", "Escuelas Superiores"});
        TIPOS.put("MEDIO_SUPERIOR", new String[] {"Nivel Medio Superior", "Escuelas de Nivel Medio Superior"});
        TIPOS.put("INVESTIGACION", new String[] {"Centro de Investigación", "Centros de Investigación"});
        TIPOS.put("UNIDAD_PROFESIONAL", new String[] {"Unidad Profesional", "Unidades Profesionales"});
        TIPOS.put("OTROS", new String[] {"Administración central / otra", "Dirección General y otros"});
    }

    private static final LocalDateTime INICIO = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final SeguimientoQuejasService seguimiento;

    public PanelDefensoraService(JdbcTemplate jdbc, NamedParameterJdbcTemplate named,
                                 SeguimientoQuejasService seguimiento) {
        this.jdbc = jdbc;
        this.named = named;
        this.seguimiento = seguimiento;
    }

    /**
     * Área de adscripción y teléfono del personal: columnas nuevas que la entidad JPA no mapea
     * (Hibernate no las toca). Se crean aquí para no depender de una migración manual.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void asegurarColumnasContacto() {
        try {
            jdbc.execute("ALTER TABLE personal_administrativo ADD COLUMN IF NOT EXISTS unidad_clave varchar(40)");
            jdbc.execute("ALTER TABLE personal_administrativo ADD COLUMN IF NOT EXISTS telefono varchar(40)");
        } catch (RuntimeException e) {
            log.warn("No se pudieron crear las columnas de contacto del personal: {}", e.getMessage());
        }
    }

    // ======================================================================================
    // Panel general
    // ======================================================================================

    public Panel panel(int dias, int meses) {
        int d = normalizarDias(dias);
        LocalDateTime[] p = periodo(d);
        MapSqlParameterSource prm = new MapSqlParameterSource()
                .addValue("desde", Timestamp.valueOf(p[0])).addValue("anterior", Timestamp.valueOf(p[1]));

        Map<String, Object> k = named.queryForMap("""
                SELECT count(*)                                                            AS total,
                       count(*) FILTER (WHERE q.fecha_creacion >= :desde)                  AS periodo,
                       count(*) FILTER (WHERE q.fecha_creacion >= :anterior
                                          AND q.fecha_creacion < :desde)                   AS anterior,
                       count(*) FILTER (WHERE %1$s = 'RECEPCION')                          AS recepcion,
                       count(*) FILTER (WHERE coalesce(q.estatus,'RECIBIDA') IN ('RECIBIDA','CORREGIDA')
                                          AND q.fecha_creacion < now() - interval '3 days') AS sin_tomar,
                       count(*) FILTER (WHERE %1$s = 'CON_QUEJOSO')                        AS con_quejoso,
                       count(*) FILTER (WHERE q.motivo_rechazo IS NOT NULL AND q.motivo_rechazo <> '') AS rechazadas,
                       count(*) FILTER (WHERE %1$s = 'PRIMER_CONTACTO')                    AS primer_contacto,
                       count(*) FILTER (WHERE %1$s = 'SUBDEFENSORIA')                      AS subdefensoria,
                       count(*) FILTER (WHERE %1$s = 'CERRADA')                            AS cerradas
                  FROM quejas q
                """.formatted(ETAPA_SQL), prm);
        PanelKpis kpis = new PanelKpis(n(k, "total"), n(k, "periodo"), d == 0 ? 0 : n(k, "anterior"), n(k, "recepcion"),
                n(k, "sin_tomar"), n(k, "con_quejoso"), n(k, "rechazadas"), n(k, "primer_contacto"),
                n(k, "subdefensoria"), n(k, "cerradas"));

        Carga carga = carga(d);
        List<MiembroCarga> recepcion = carga.recepcionistas().stream().limit(5).toList();

        return new Panel(d, kpis, seguimiento.etapas(), seguimiento.porMes(meses <= 0 ? 12 : Math.min(meses, 36)),
                unidades(null, null, null, null, null, d, 0, 8).filas(),
                casos(null, null, null, null, null, null, null, null, 0, 5),
                recepcion, motivos(null, null, null, d));
    }

    // ======================================================================================
    // Casos que requieren atención
    // ======================================================================================

    public PaginaCasos casos(String texto, String motivo, String etapa, String unidad, String responsable,
                             Integer diasMin, Integer diasMax, String severidad, int pagina, int tamanio) {
        MapSqlParameterSource p = parametrosReglas();
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        if (vacio(texto) != null) {
            where.append(" AND (lower(f.folio) LIKE :t OR lower(coalesce(f.quejoso,'')) LIKE :t"
                    + " OR lower(coalesce(f.responsable,'')) LIKE :t OR lower(coalesce(f.descripcion,'')) LIKE :t) ");
            p.addValue("t", "%" + texto.trim().toLowerCase().replace("%", "") + "%");
        }
        if (vacio(motivo) != null) {
            where.append(" AND f.motivo = :motivo ");
            p.addValue("motivo", motivo.trim().toUpperCase());
        }
        if (vacio(etapa) != null) {
            where.append(" AND f.etapa = :etapa ");
            p.addValue("etapa", etapa.trim().toUpperCase());
        }
        if (vacio(unidad) != null) {
            where.append(" AND f.unidad_clave = :unidad ");
            p.addValue("unidad", unidad.trim());
        }
        if (vacio(responsable) != null) {
            if ("SIN_RESPONSABLE".equalsIgnoreCase(responsable.trim())) {
                where.append(" AND f.responsable IS NULL ");
            } else {
                where.append(" AND lower(f.responsable) = lower(:resp) ");
                p.addValue("resp", responsable.trim());
            }
        }
        if (diasMin != null) {
            where.append(" AND f.dias >= :dmin ");
            p.addValue("dmin", diasMin);
        }
        if (diasMax != null) {
            where.append(" AND f.dias <= :dmax ");
            p.addValue("dmax", diasMax);
        }
        if (vacio(severidad) != null) {
            where.append(" AND f.severidad = :sev ");
            p.addValue("sev", severidad.trim().toUpperCase());
        }

        String base = """
                WITH c AS (%s),
                f AS (
                  SELECT c.folio, c.motivo, q.fecha_creacion, q.descripcion,
                         floor(extract(epoch FROM (now() - c.ref)) / 86400)::bigint AS dias,
                         trim(concat_ws(' ', q.nombre_quejoso, q.apellido1_quejoso, q.apellido2_quejoso)) AS quejoso,
                         %s AS etapa, coalesce(q.estatus,'RECIBIDA') AS estatus,
                         q.unidad_academica_clave AS unidad_clave, coalesce(d.abreviatura, d.nombre, q.unidad_academica_clave) AS unidad_nombre,
                         CASE
                           WHEN q.estatus = 'EN_VALIDACION' THEN coalesce(pr.nombre_completo, q.revisando_por_nombre, q.revisando_por)
                           WHEN q.estatus = 'RECHAZADA' THEN coalesce(pv.nombre_completo, q.validado_por)
                           WHEN coalesce(q.estatus,'RECIBIDA') IN ('RECIBIDA','CORREGIDA') THEN NULL
                           ELSE nullif(trim(q.defensor_asignado), '')
                         END AS responsable,
                         CASE c.motivo
                           WHEN 'SIN_TOMAR' THEN :dSinTomar WHEN 'CORREGIDA_SIN_REVISAR' THEN :dSinTomar
                           WHEN 'VALIDACION_DETENIDA' THEN :dValidacion WHEN 'OBSERVACIONES_PENDIENTES' THEN :dObservaciones
                           WHEN 'VENCIDA_PRIMER_CONTACTO' THEN :dPrimerContacto WHEN 'SIN_TURNAR_SUBDEFENSORIA' THEN :dSinTurnar
                           WHEN 'VENCIDA_SEGUIMIENTO' THEN :dSeguimiento ELSE 0 END AS umbral
                    FROM c
                    JOIN quejas q ON q.numero_folio = c.folio
                    LEFT JOIN dependencias d ON d.clave = q.unidad_academica_clave
                    LEFT JOIN personal_administrativo pr ON lower(pr.correo_institucional) = lower(q.revisando_por)
                    LEFT JOIN personal_administrativo pv ON lower(pv.correo_institucional) = lower(q.validado_por)
                ),
                g AS (
                  SELECT f.*, CASE WHEN f.umbral > 0 AND f.dias >= f.umbral * 2 THEN 'ALTA'
                                   WHEN f.umbral = 0 AND f.dias >= 3 THEN 'ALTA' ELSE 'MEDIA' END AS severidad
                    FROM f
                )
                """.formatted(CASOS_SQL, ETAPA_SQL);
        String desde = base + " SELECT * FROM g f " + where;

        Long total = named.queryForObject(base + " SELECT count(*) FROM g f " + where, p, Long.class);
        long totalElementos = total == null ? 0 : total;
        int tam = Math.max(1, Math.min(tamanio <= 0 ? 10 : tamanio, 500));
        int pag = Math.max(0, pagina);
        p.addValue("limite", tam).addValue("desplazamiento", (long) pag * tam);

        List<Caso> filas = named.query(desde + " ORDER BY f.dias DESC, f.folio LIMIT :limite OFFSET :desplazamiento", p,
                (rs, i) -> new Caso(rs.getString("folio"), ts(rs.getTimestamp("fecha_creacion")), rs.getString("quejoso"),
                        rs.getString("motivo"), ETIQUETA_REGLA.getOrDefault(rs.getString("motivo"), rs.getString("motivo")),
                        rs.getLong("dias"), rs.getString("severidad"), rs.getString("etapa"), rs.getString("estatus"),
                        etiquetaEstatus(rs.getString("estatus")), rs.getString("unidad_clave"), rs.getString("unidad_nombre"),
                        rs.getString("responsable")));
        return new PaginaCasos(filas, pag, tam, totalElementos, (int) Math.ceil(totalElementos / (double) tam), REGLAS);
    }

    private MapSqlParameterSource parametrosReglas() {
        return new MapSqlParameterSource()
                .addValue("dSinTomar", DIAS_SIN_TOMAR)
                .addValue("dValidacion", DIAS_VALIDACION_DETENIDA)
                .addValue("dObservaciones", DIAS_OBSERVACIONES)
                .addValue("dPrimerContacto", DIAS_PRIMER_CONTACTO)
                .addValue("dSinTurnar", DIAS_SIN_TURNAR_SUBDEFENSORIA)
                .addValue("dSeguimiento", DIAS_SEGUIMIENTO);
    }

    // ======================================================================================
    // Quejas por unidad
    // ======================================================================================

    /**
     * Unidades con quejas en el periodo. Si vienen {@code desde}/{@code hasta} mandan sobre
     * {@code dias}. La posición, el porcentaje, el resumen y la gráfica se calculan sobre todas
     * las unidades del periodo; el texto, el tipo y el estatus solo filtran la tabla.
     */
    public TablaUnidades unidades(String texto, String tipo, String estatus, LocalDate desde, LocalDate hasta,
                                  int dias, int pagina, int tamanio) {
        int d = normalizarDias(dias);
        LocalDateTime[] p = periodo(d);
        LocalDateTime inicio = desde != null ? desde.atStartOfDay() : p[0];
        LocalDateTime fin = hasta != null ? hasta.plusDays(1).atStartOfDay() : LocalDateTime.now().plusYears(1);
        // Periodo anterior del mismo tamaño (solo cuando se usa "últimos N días")
        LocalDateTime antInicio = desde == null && hasta == null && d > 0 ? p[1] : null;

        MapSqlParameterSource prm = new MapSqlParameterSource()
                .addValue("ini", Timestamp.valueOf(inicio)).addValue("fin", Timestamp.valueOf(fin));
        String agrupado = """
                WITH u AS (
                  SELECT q.unidad_academica_clave AS clave, max(d.nombre) AS nombre,
                         coalesce(max(d.abreviatura), q.unidad_academica_clave) AS siglas,
                         max(%s) AS tipo,
                         count(*) AS total,
                         count(*) FILTER (WHERE %s <> 'CERRADA') AS en_tramite,
                         count(*) FILTER (WHERE %s = 'CERRADA') AS cerradas,
                         max(q.fecha_creacion) AS ultima
                    FROM quejas q
                    LEFT JOIN dependencias d ON d.clave = q.unidad_academica_clave
                   WHERE q.unidad_academica_clave IS NOT NULL AND q.unidad_academica_clave <> ''
                     AND q.fecha_creacion >= :ini AND q.fecha_creacion < :fin
                   GROUP BY q.unidad_academica_clave
                ),
                r AS (
                  SELECT u.*, row_number() OVER (ORDER BY u.total DESC, u.siglas) AS posicion,
                         sum(u.total) OVER () AS gran_total
                    FROM u
                )
                """.formatted(TIPO_UNIDAD_SQL, ETAPA_SQL, ETAPA_SQL);

        List<FilaUnidad> todas = named.query(agrupado + " SELECT * FROM r ORDER BY posicion", prm, (rs, i) -> fila(rs));

        // Filtros de la tabla
        String t = vacio(texto) == null ? null : texto.trim().toLowerCase();
        List<FilaUnidad> filtradas = todas.stream()
                .filter(f -> t == null || contiene(f.clave(), t) || contiene(f.nombre(), t) || contiene(f.siglas(), t))
                .filter(f -> vacio(tipo) == null || f.tipo().equalsIgnoreCase(tipo.trim()))
                .filter(f -> vacio(estatus) == null
                        || ("EN_TRAMITE".equalsIgnoreCase(estatus) && f.enTramite() > 0)
                        || ("CERRADAS".equalsIgnoreCase(estatus) && f.cerradas() > 0)
                        || ("SOLO_CERRADAS".equalsIgnoreCase(estatus) && f.enTramite() == 0))
                .toList();

        int tam = Math.max(1, Math.min(tamanio <= 0 ? 10 : tamanio, 500));
        int pag = Math.max(0, pagina);
        List<FilaUnidad> pagFilas = filtradas.stream().skip((long) pag * tam).limit(tam).toList();

        long totalQuejas = todas.stream().mapToLong(FilaUnidad::total).sum();
        long unidadesAnt = 0;
        long totalAnt = 0;
        if (antInicio != null) {
            Map<String, Object> a = named.queryForMap("""
                    SELECT count(DISTINCT q.unidad_academica_clave) AS unidades, count(*) AS total
                      FROM quejas q
                     WHERE q.unidad_academica_clave IS NOT NULL AND q.unidad_academica_clave <> ''
                       AND q.fecha_creacion >= :ant AND q.fecha_creacion < :ini
                    """, new MapSqlParameterSource().addValue("ant", Timestamp.valueOf(antInicio))
                    .addValue("ini", Timestamp.valueOf(inicio)));
            unidadesAnt = n(a, "unidades");
            totalAnt = n(a, "total");
        }
        FilaUnidad top = todas.isEmpty() ? null : todas.get(0);
        ResumenUnidades resumen = new ResumenUnidades(todas.size(), unidadesAnt, totalQuejas, totalAnt,
                top == null ? null : top.siglas(), top == null ? 0 : top.total(),
                todas.isEmpty() ? 0 : Math.round(totalQuejas * 10.0 / todas.size()) / 10.0);

        Map<String, Long> porTipoMap = new LinkedHashMap<>();
        TIPOS.keySet().forEach(k -> porTipoMap.put(k, 0L));
        todas.forEach(f -> porTipoMap.merge(f.tipo(), f.total(), Long::sum));
        List<Conteo> porTipo = new ArrayList<>();
        porTipoMap.forEach((k, v) -> porTipo.add(new Conteo(k, TIPOS.get(k)[1], v)));

        return new TablaUnidades(pagFilas, pag, tam, filtradas.size(), (int) Math.ceil(filtradas.size() / (double) tam),
                resumen, porTipo, todas.stream().limit(10).toList());
    }

    private static FilaUnidad fila(ResultSet rs) throws SQLException {
        long granTotal = rs.getLong("gran_total");
        String tipo = rs.getString("tipo");
        return new FilaUnidad(rs.getInt("posicion"), rs.getString("clave"), rs.getString("nombre"), rs.getString("siglas"),
                tipo, TIPOS.getOrDefault(tipo, TIPOS.get("OTROS"))[0], rs.getLong("total"), rs.getLong("en_tramite"),
                rs.getLong("cerradas"), granTotal == 0 ? 0 : Math.round(rs.getLong("total") * 1000.0 / granTotal) / 10.0,
                ts(rs.getTimestamp("ultima")));
    }

    // ======================================================================================
    // Carga de trabajo
    // ======================================================================================

    /**
     * Estadísticas de cada integrante. Las quejas se ligan por correo (recepcionistas:
     * revisando_por / validado_por) y por nombre o correo (abogados: defensor_asignado).
     */
    private static final String MIEMBROS_SQL = """
            SELECT p.id, p.nombre_completo, p.correo_institucional, p.rol, p.activo, p.ultimo_login,
                   count(q.id) FILTER (WHERE q.estatus = 'EN_VALIDACION' AND q.es_rev)                    AS abiertas,
                   count(q.id) FILTER (WHERE q.es_val AND q.fecha_validacion >= :desde)                    AS atendidas,
                   count(q.id) FILTER (WHERE q.es_val AND q.fecha_turnado >= :desde)                       AS turnadas,
                   count(q.id) FILTER (WHERE q.es_val AND q.estatus = 'RECHAZADA')                         AS observaciones,
                   count(q.id) FILTER (WHERE q.es_val AND q.fecha_turnado IS NOT NULL AND q.etapa <> 'CERRADA') AS tramite_recep,
                   count(q.id) FILTER (WHERE q.es_abog AND q.etapa NOT IN ('CERRADA','CON_QUEJOSO'))       AS tramite_abog,
                   count(q.id) FILTER (WHERE q.es_abog AND q.etapa = 'PRIMER_CONTACTO')                    AS en_pc,
                   count(q.id) FILTER (WHERE q.es_abog AND q.etapa = 'SUBDEFENSORIA')                      AS en_sub,
                   count(q.id) FILTER (WHERE q.es_abog AND q.etapa = 'CERRADA')                            AS cerradas,
                   count(q.id) FILTER (WHERE q.es_abog AND coalesce(q.fecha_turnado, q.fecha_creacion) >= :desde) AS asignadas,
                   greatest(max(q.fecha_validacion) FILTER (WHERE q.es_val),
                            max(q.fecha_inicio_revision) FILTER (WHERE q.es_rev),
                            max(q.fecha_turnado) FILTER (WHERE q.es_abog),
                            p.ultimo_login)                                                                AS ultima
              FROM personal_administrativo p
              LEFT JOIN LATERAL (
                SELECT q.id, q.estatus, q.fecha_validacion, q.fecha_turnado, q.fecha_inicio_revision, q.fecha_creacion,
                       %s AS etapa,
                       lower(coalesce(q.revisando_por,'')) = lower(p.correo_institucional) AS es_rev,
                       lower(coalesce(q.validado_por,'')) = lower(p.correo_institucional) AS es_val,
                       lower(trim(coalesce(q.defensor_asignado,''))) IN (lower(p.nombre_completo), lower(p.correo_institucional)) AS es_abog
                  FROM quejas q
                 WHERE lower(coalesce(q.revisando_por,'')) = lower(p.correo_institucional)
                    OR lower(coalesce(q.validado_por,'')) = lower(p.correo_institucional)
                    OR lower(trim(coalesce(q.defensor_asignado,''))) IN (lower(p.nombre_completo), lower(p.correo_institucional))
              ) q ON true
             WHERE p.rol IN (:roles)
             GROUP BY p.id, p.nombre_completo, p.correo_institucional, p.rol, p.activo, p.ultimo_login
            """.formatted(ETAPA_SQL);

    public Carga carga(int dias) {
        int d = normalizarDias(dias);
        LocalDateTime[] p = periodo(d);
        MapSqlParameterSource prm = new MapSqlParameterSource()
                .addValue("desde", Timestamp.valueOf(p[0]))
                .addValue("anterior", Timestamp.valueOf(p[1]))
                .addValue("roles", List.of("RECEPCIONISTA", "SUBDEFENSOR", "DEFENSOR"));

        List<Map<String, Object>> filas = named.queryForList(MIEMBROS_SQL, prm);

        List<MiembroCarga> recep = new ArrayList<>();
        List<MiembroCarga> abog = new ArrayList<>();
        for (Map<String, Object> f : filas) {
            boolean esRecep = "RECEPCIONISTA".equals(f.get("rol"));
            boolean activo = Boolean.TRUE.equals(f.get("activo"));
            if (!activo && n(f, "abiertas") + n(f, "atendidas") + n(f, "tramite_abog") == 0) {
                continue; // cuentas dadas de baja sin nada pendiente
            }
            long carga = esRecep ? n(f, "abiertas") + n(f, "atendidas") : n(f, "tramite_abog");
            MiembroCarga m = new MiembroCarga(((Number) f.get("id")).longValue(), str(f, "nombre_completo"),
                    str(f, "correo_institucional"), str(f, "rol"), activo,
                    esRecep ? n(f, "abiertas") : n(f, "asignadas"),
                    esRecep ? n(f, "tramite_recep") : n(f, "tramite_abog"),
                    esRecep ? n(f, "atendidas") : n(f, "cerradas"),
                    esRecep ? n(f, "turnadas") : 0,
                    esRecep ? n(f, "observaciones") : 0,
                    n(f, "en_pc"), n(f, "en_sub"), n(f, "cerradas"), fecha(f, "ultima"), carga, 0, null);
            (esRecep ? recep : abog).add(m);
        }
        recep = conNivel(recep, p[0]);
        abog = conNivel(abog, p[0]);

        Map<String, Object> k = named.queryForMap("""
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE %1$s NOT IN ('CERRADA')) AS en_tramite,
                       count(*) FILTER (WHERE q.fecha_validacion >= :desde) AS atendidas,
                       avg(EXTRACT(EPOCH FROM (q.fecha_validacion - q.fecha_creacion)) / 86400.0)
                           FILTER (WHERE q.fecha_validacion >= :desde AND q.fecha_validacion >= q.fecha_creacion) AS dias,
                       avg(EXTRACT(EPOCH FROM (q.fecha_validacion - q.fecha_creacion)) / 86400.0)
                           FILTER (WHERE q.fecha_validacion >= :anterior AND q.fecha_validacion < :desde
                                     AND q.fecha_validacion >= q.fecha_creacion) AS dias_ant
                  FROM quejas q
                """.formatted(ETAPA_SQL), prm);
        long recepActivas = recep.stream().filter(MiembroCarga::activo).count();
        long abogActivos = abog.stream().filter(MiembroCarga::activo).count();
        Long personalActivo = jdbc.queryForObject("SELECT count(*) FROM personal_administrativo WHERE activo = true", Long.class);
        CargaKpis kpis = new CargaKpis(personalActivo == null ? 0 : personalActivo, recepActivas, abogActivos,
                n(k, "en_tramite"), n(k, "total"), n(k, "atendidas"), dbl(k, "dias"), d == 0 ? null : dbl(k, "dias_ant"));

        return new Carga(d, kpis, recep, abog, seguimiento.etapas());
    }

    /**
     * Nivel de carga relativo al equipo: Alta si supera 1.5 veces el promedio (y al menos 3),
     * Baja si queda por debajo de la mitad, Sin actividad si no hizo nada en el periodo.
     */
    private static List<MiembroCarga> conNivel(List<MiembroCarga> lista, LocalDateTime desde) {
        long suma = lista.stream().mapToLong(MiembroCarga::carga).sum();
        long activos = lista.stream().filter(MiembroCarga::activo).count();
        double promedio = activos == 0 ? 0 : suma / (double) activos;
        return lista.stream().map(m -> {
            String nivel;
            boolean actividad = m.ultimaActividad() != null && !m.ultimaActividad().isBefore(desde);
            if (m.carga() == 0 && !actividad) {
                nivel = "SIN_ACTIVIDAD";
            } else if (m.carga() >= 3 && m.carga() > promedio * 1.5) {
                nivel = "ALTA";
            } else if (m.carga() < promedio * 0.5) {
                nivel = "BAJA";
            } else {
                nivel = "NORMAL";
            }
            double pct = suma == 0 ? 0 : Math.round(m.carga() * 1000.0 / suma) / 10.0;
            return new MiembroCarga(m.id(), m.nombre(), m.correo(), m.rol(), m.activo(), m.abiertas(), m.enTramite(),
                    m.atendidas(), m.turnadas(), m.observaciones(), m.enPrimerContacto(), m.enSubdefensoria(),
                    m.cerradas(), m.ultimaActividad(), m.carga(), pct, nivel);
        }).sorted((a, b) -> Long.compare(b.carga(), a.carga()) != 0 ? Long.compare(b.carga(), a.carga())
                : a.nombre().compareToIgnoreCase(b.nombre())).toList();
    }

    // ======================================================================================
    // Motivos de rechazo
    // ======================================================================================

    public List<MotivoRechazo> motivos(String texto, String categoria, LocalDate desde, int dias) {
        return motivos(texto, categoria, desde, null, dias);
    }

    public List<MotivoRechazo> motivos(String texto, String categoria, LocalDate desde, LocalDate hasta, int dias) {
        int d = normalizarDias(dias);
        LocalDateTime inicio = desde != null ? desde.atStartOfDay() : periodo(d)[0];
        LocalDateTime fin = hasta != null ? hasta.plusDays(1).atStartOfDay() : LocalDateTime.now().plusYears(1);

        StringBuilder sql = new StringBuilder("SELECT count(*) FILTER (WHERE q.motivo_rechazo IS NOT NULL AND trim(q.motivo_rechazo) <> '') AS total");
        MOTIVOS.keySet().forEach(c -> sql.append(", count(*) FILTER (WHERE ").append(condicionMotivo(c)).append(") AS m_").append(c.toLowerCase()));
        sql.append(" FROM quejas q WHERE coalesce(q.fecha_validacion, q.fecha_creacion) >= :ini"
                + " AND coalesce(q.fecha_validacion, q.fecha_creacion) < :fin");
        Map<String, Object> fila = named.queryForMap(sql.toString(),
                new MapSqlParameterSource().addValue("ini", Timestamp.valueOf(inicio)).addValue("fin", Timestamp.valueOf(fin)));
        long total = n(fila, "total");

        String t = vacio(texto) == null ? null : texto.trim().toLowerCase();
        List<MotivoRechazo> lista = new ArrayList<>();
        MOTIVOS.forEach((clave, m) -> {
            long cantidad = n(fila, "m_" + clave.toLowerCase());
            lista.add(new MotivoRechazo(clave, m[0], m[1], cantidad,
                    total == 0 ? 0 : Math.round(cantidad * 1000.0 / total) / 10.0));
        });
        return lista.stream()
                .filter(x -> t == null || contiene(x.motivo(), t) || contiene(x.categoria(), t))
                .filter(x -> vacio(categoria) == null || x.categoria().equalsIgnoreCase(categoria.trim()))
                .sorted((a, b) -> Long.compare(b.cantidad(), a.cantidad()))
                .toList();
    }

    // ======================================================================================
    // Personal de la Defensoría
    // ======================================================================================

    public List<MiembroPersonal> personal() {
        MapSqlParameterSource prm = new MapSqlParameterSource()
                .addValue("desde", Timestamp.valueOf(INICIO))
                .addValue("roles", List.of("ADMIN_SISTEMAS", "RECEPCIONISTA", "ANALISTA_PRIMER_CONTACTO", "SUBDEFENSOR", "DEFENSOR"));
        Map<Long, Map<String, Object>> stats = new LinkedHashMap<>();
        named.queryForList(MIEMBROS_SQL, prm).forEach(f -> stats.put(((Number) f.get("id")).longValue(), f));

        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT p.*, d.nombre AS unidad_nombre, d.abreviatura AS unidad_siglas
                  FROM personal_administrativo p
                  LEFT JOIN dependencias d ON d.clave = p.unidad_clave
                 ORDER BY p.activo DESC, CASE p.rol WHEN 'DEFENSOR' THEN 0 WHEN 'RECEPCIONISTA' THEN 1
                          WHEN 'SUBDEFENSOR' THEN 2 WHEN 'ANALISTA_PRIMER_CONTACTO' THEN 3 ELSE 4 END,
                          p.nombre_completo
                """);
        return filas.stream().map(f -> miembro(f, stats.get(((Number) f.get("id")).longValue()))).toList();
    }

    public DetallePersonal detallePersonal(Long id) {
        MiembroPersonal persona = personal().stream().filter(m -> m.id().equals(id)).findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontró a esa persona."));
        String correo = persona.correo() == null ? "" : persona.correo().toLowerCase();
        String nombre = persona.nombre() == null ? "" : persona.nombre().toLowerCase();

        List<Actividad> actividad = new ArrayList<>(jdbc.query("""
                SELECT fecha, accion_realizada FROM bitacora_acciones WHERE lower(usuario) = ? ORDER BY fecha DESC LIMIT 25
                """, (rs, i) -> new Actividad(ts(rs.getTimestamp(1)), rs.getString(2), "Panel")
                , correo));
        // Movimientos sobre quejas (Recepción)
        actividad.addAll(jdbc.query("""
                SELECT q.fecha_validacion, q.numero_folio, q.estatus, q.fecha_turnado
                  FROM quejas q WHERE lower(q.validado_por) = ? AND q.fecha_validacion IS NOT NULL
                 ORDER BY q.fecha_validacion DESC LIMIT 25
                """, (rs, i) -> new Actividad(ts(rs.getTimestamp(1)),
                        (rs.getTimestamp(4) != null ? "Turnó la queja " : "Regresó con observaciones la queja ") + rs.getString(2),
                        "Recepción"), correo));
        actividad.addAll(jdbc.query("""
                SELECT q.fecha_inicio_revision, q.numero_folio FROM quejas q
                 WHERE lower(q.revisando_por) = ? AND q.estatus = 'EN_VALIDACION' AND q.fecha_inicio_revision IS NOT NULL
                """, (rs, i) -> new Actividad(ts(rs.getTimestamp(1)), "Está validando la queja " + rs.getString(2), "Recepción"),
                correo));
        if (persona.ultimoAcceso() != null) {
            actividad.add(new Actividad(persona.ultimoAcceso(), "Último inicio de sesión", "Acceso"));
        }
        actividad.sort((a, b) -> b.fecha() == null ? -1 : a.fecha() == null ? 1 : b.fecha().compareTo(a.fecha()));

        List<QuejaAsignada> quejas = jdbc.query("""
                SELECT q.numero_folio, q.fecha_creacion,
                       trim(concat_ws(' ', q.nombre_quejoso, q.apellido1_quejoso, q.apellido2_quejoso)) AS quejoso,
                       coalesce(d.abreviatura, d.nombre, q.unidad_academica_clave) AS unidad, %s AS etapa,
                       coalesce(q.estatus,'RECIBIDA') AS estatus,
                       CASE WHEN lower(trim(coalesce(q.defensor_asignado,''))) IN (?, ?) THEN 'Abogado responsable'
                            WHEN lower(coalesce(q.revisando_por,'')) = ? AND q.estatus = 'EN_VALIDACION' THEN 'La está validando'
                            WHEN q.fecha_turnado IS NOT NULL THEN 'La turnó'
                            ELSE 'La regresó con observaciones' END AS relacion
                  FROM quejas q
                  LEFT JOIN dependencias d ON d.clave = q.unidad_academica_clave
                 WHERE lower(trim(coalesce(q.defensor_asignado,''))) IN (?, ?)
                    OR lower(coalesce(q.validado_por,'')) = ?
                    OR (lower(coalesce(q.revisando_por,'')) = ? AND q.estatus = 'EN_VALIDACION')
                 ORDER BY q.fecha_creacion DESC LIMIT 50
                """.formatted(ETAPA_SQL),
                (rs, i) -> new QuejaAsignada(rs.getString(1), ts(rs.getTimestamp(2)), rs.getString(3), rs.getString(4),
                        rs.getString(5), etiquetaEstatus(rs.getString(6)), rs.getString(7)),
                nombre, correo, correo, nombre, correo, correo, correo);

        return new DetallePersonal(persona, actividad.stream().limit(30).toList(), quejas);
    }

    public MiembroPersonal guardarContacto(Long id, ContactoRequest datos) {
        String unidad = datos == null ? null : vacio(datos.unidadClave());
        String telefono = datos == null ? null : vacio(datos.telefono());
        if (telefono != null && !telefono.matches("[0-9 +()\\-.extEXT]{7,40}")) {
            throw new IllegalArgumentException("El teléfono solo puede llevar números, espacios, guiones, paréntesis y \"ext.\".");
        }
        if (unidad != null && Boolean.FALSE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM dependencias WHERE clave = ?)", Boolean.class, unidad.trim()))) {
            throw new IllegalArgumentException("La dependencia " + unidad + " no existe en el catálogo.");
        }
        int n = jdbc.update("UPDATE personal_administrativo SET unidad_clave = ?, telefono = ? WHERE id = ?",
                unidad == null ? null : unidad.trim(), telefono == null ? null : telefono.trim(), id);
        if (n == 0) {
            throw new RuntimeException("No se encontró a esa persona.");
        }
        return personal().stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow();
    }

    private static MiembroPersonal miembro(Map<String, Object> f, Map<String, Object> s) {
        String rol = str(f, "rol");
        boolean recep = "RECEPCIONISTA".equals(rol);
        boolean abog = "SUBDEFENSOR".equals(rol) || "DEFENSOR".equals(rol);
        long abiertas = 0;
        long tramite = 0;
        long atendidas = 0;
        long turnadas = 0;
        long observaciones = 0;
        if (s != null) {
            if (recep) {
                abiertas = n(s, "abiertas");
                tramite = n(s, "tramite_recep");
                atendidas = n(s, "atendidas");
                turnadas = n(s, "turnadas");
                observaciones = n(s, "observaciones");
            } else if (abog) {
                // Para abogados: asignadas, en trámite, concluidas y en Subdefensoría
                abiertas = n(s, "asignadas");
                tramite = n(s, "tramite_abog");
                atendidas = n(s, "cerradas");
                turnadas = n(s, "en_sub");
            }
        }
        String unidadNombre = str(f, "unidad_siglas") != null ? str(f, "unidad_siglas") : str(f, "unidad_nombre");
        Object temporal = f.get("cuenta_temporal");
        return new MiembroPersonal(((Number) f.get("id")).longValue(), str(f, "nombre_completo"), str(f, "numero_empleado"),
                str(f, "correo_institucional"), rol, Boolean.TRUE.equals(f.get("activo")), Boolean.TRUE.equals(temporal),
                str(f, "unidad_clave"), unidadNombre, str(f, "telefono"), fecha(f, "fecha_creacion"),
                fecha(f, "ultimo_login"), abiertas, tramite, atendidas, turnadas, observaciones);
    }

    // ======================================================================================
    // utilidades
    // ======================================================================================

    static int normalizarDias(int dias) {
        return dias <= 0 ? 0 : Math.min(dias, 3650);
    }

    /** [inicio del periodo, inicio del periodo anterior]. dias = 0 → todo el historial. */
    static LocalDateTime[] periodo(int dias) {
        if (dias <= 0) {
            return new LocalDateTime[] {INICIO, INICIO};
        }
        LocalDateTime ahora = LocalDateTime.now();
        return new LocalDateTime[] {ahora.minusDays(dias), ahora.minusDays(2L * dias)};
    }

    private static boolean contiene(String valor, String t) {
        return valor != null && valor.toLowerCase().contains(t);
    }

    private static String vacio(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static long n(Map<String, Object> fila, String col) {
        Object v = fila.get(col);
        return v == null ? 0 : ((Number) v).longValue();
    }

    private static Double dbl(Map<String, Object> fila, String col) {
        Object v = fila.get(col);
        return v == null ? null : Math.round(((Number) v).doubleValue() * 10.0) / 10.0;
    }

    private static String str(Map<String, Object> fila, String col) {
        Object v = fila.get(col);
        return v == null ? null : v.toString();
    }

    private static LocalDateTime fecha(Map<String, Object> fila, String col) {
        Object v = fila.get(col);
        return v instanceof Timestamp t ? t.toLocalDateTime() : null;
    }

    private static LocalDateTime ts(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
