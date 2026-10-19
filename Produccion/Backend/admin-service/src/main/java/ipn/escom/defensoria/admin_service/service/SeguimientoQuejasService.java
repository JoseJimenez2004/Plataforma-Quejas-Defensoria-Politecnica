package ipn.escom.defensoria.admin_service.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Conteo;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.DetalleQueja;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Etapa;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Evento;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Evidencia;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.FilaQueja;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Filtros;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Mes;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.PaginaQuejas;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Persona;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.UnidadConteo;

/**
 * Seguimiento de quejas para el Panel de la Defensora (2026-10-16): tablero con cruces de
 * información (etapas, meses, unidades, Recepción, abogados, motivos de rechazo), explorador
 * de quejas con filtros y detalle de solo lectura. Todo con SQL de solo lectura: admin-service no
 * mapea la tabla quejas como entidad (es de queja-service).
 */
@Service
public class SeguimientoQuejasService {

    /** Etapa del proceso según quejas.estatus. */
    static final String ETAPA_SQL = """
            CASE
              WHEN q.estatus IS NULL OR q.estatus IN ('RECIBIDA','EN_VALIDACION','CORREGIDA') THEN 'RECEPCION'
              WHEN q.estatus = 'RECHAZADA' THEN 'CON_QUEJOSO'
              WHEN q.estatus IN ('TURNADA','EN_ANALISIS','PROCEDENTE') THEN 'PRIMER_CONTACTO'
              WHEN q.estatus IN ('RECIBIDA_SUBDEFENSORIA','EN_INVESTIGACION','EN_ESPERA_OFICIO','ELABORO_ACUERDO',
                                 'PENDIENTE_CONCILIACION','PENDIENTE_CONCLUSION') THEN 'SUBDEFENSORIA'
              WHEN q.estatus IN ('CONCLUIDA','IMPROCEDENTE','REMITIDA','CANCELADA') THEN 'CERRADA'
              ELSE 'OTRA'
            END""";

    static final Map<String, String> ETAPAS = new LinkedHashMap<>();
    static final Map<String, String> ESTATUS = new LinkedHashMap<>();

    static {
        ETAPAS.put("RECEPCION", "Recepción");
        ETAPAS.put("CON_QUEJOSO", "Con el quejoso (observaciones)");
        ETAPAS.put("PRIMER_CONTACTO", "Primer Contacto");
        ETAPAS.put("SUBDEFENSORIA", "Subdefensoría");
        ETAPAS.put("CERRADA", "Cerradas");

        ESTATUS.put("RECIBIDA", "Nueva, sin tomar");
        ESTATUS.put("EN_VALIDACION", "En validación");
        ESTATUS.put("CORREGIDA", "Corregida por el quejoso");
        ESTATUS.put("RECHAZADA", "Con observaciones");
        ESTATUS.put("TURNADA", "Turnada");
        ESTATUS.put("EN_ANALISIS", "En análisis");
        ESTATUS.put("PROCEDENTE", "Procedente");
        ESTATUS.put("RECIBIDA_SUBDEFENSORIA", "Recibida en Subdefensoría");
        ESTATUS.put("EN_INVESTIGACION", "En investigación");
        ESTATUS.put("EN_ESPERA_OFICIO", "En espera de oficio");
        ESTATUS.put("ELABORO_ACUERDO", "Acuerdo elaborado");
        ESTATUS.put("PENDIENTE_CONCILIACION", "Pendiente de conciliación");
        ESTATUS.put("PENDIENTE_CONCLUSION", "Pendiente de conclusión");
        ESTATUS.put("CONCLUIDA", "Concluida");
        ESTATUS.put("IMPROCEDENTE", "Improcedente");
        ESTATUS.put("REMITIDA", "Remitida");
        ESTATUS.put("CANCELADA", "Cancelada por el quejoso");
    }

    private static final String[] MESES = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public SeguimientoQuejasService(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
        this.jdbc = jdbc;
        this.named = named;
    }

    // ======================================================================================
    // Etapas y meses (los usa el Panel general y la Carga de trabajo)
    // ======================================================================================

    /** Quejas por etapa con su desglose por estatus (siempre las 5 etapas, aunque estén en 0). */
    public List<Etapa> etapas() {
        Map<String, List<Conteo>> desglose = new LinkedHashMap<>();
        ETAPAS.keySet().forEach(e -> desglose.put(e, new ArrayList<>()));
        jdbc.query("SELECT %s AS etapa, coalesce(q.estatus,'RECIBIDA') AS estatus, count(*) AS total FROM quejas q GROUP BY 1, 2 ORDER BY 3 DESC"
                        .formatted(ETAPA_SQL),
                rs -> {
                    desglose.computeIfAbsent(rs.getString("etapa"), x -> new ArrayList<>())
                            .add(new Conteo(rs.getString("estatus"), etiquetaEstatus(rs.getString("estatus")), rs.getLong("total")));
                });
        List<Etapa> etapas = new ArrayList<>();
        desglose.forEach((clave, lista) -> {
            long total = lista.stream().mapToLong(Conteo::total).sum();
            if (total > 0 || ETAPAS.containsKey(clave)) {
                etapas.add(new Etapa(clave, ETAPAS.getOrDefault(clave, "Otras"), total, lista));
            }
        });
        return etapas;
    }

    /** Últimos {@code meses} meses (incluido el actual): recibidas y atendidas por Recepción. */
    public List<Mes> porMes(int meses) {
        return jdbc.query("""
                SELECT to_char(m.mes, 'YYYY-MM') AS clave, extract(month FROM m.mes)::int AS mes_num,
                       extract(year FROM m.mes)::int AS anio,
                       (SELECT count(*) FROM quejas q WHERE date_trunc('month', q.fecha_creacion) = m.mes) AS recibidas,
                       (SELECT count(*) FROM quejas q WHERE date_trunc('month', q.fecha_validacion) = m.mes) AS atendidas
                  FROM generate_series(date_trunc('month', now()) - (? * interval '1 month'), date_trunc('month', now()),
                                       interval '1 month') AS m(mes)
                 ORDER BY m.mes
                """,
                (rs, i) -> new Mes(rs.getString("clave"),
                        MESES[rs.getInt("mes_num") - 1] + " " + String.valueOf(rs.getInt("anio")).substring(2),
                        rs.getLong("recibidas"), rs.getLong("atendidas")),
                Math.max(0, meses - 1));
    }

    // ======================================================================================
    // Unidades
    // ======================================================================================

    /** Dependencias y unidades académicas con quejas, de la que más tiene a la que menos. */
    public List<UnidadConteo> unidades(String texto, int limite) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(" WHERE q.unidad_academica_clave IS NOT NULL ");
        if (texto != null && !texto.isBlank()) {
            where.append(" AND (lower(q.unidad_academica_clave) LIKE :t OR lower(coalesce(d.nombre,'')) LIKE :t"
                    + " OR lower(coalesce(d.abreviatura,'')) LIKE :t) ");
            p.addValue("t", "%" + texto.trim().toLowerCase() + "%");
        }
        p.addValue("limite", limite <= 0 ? 500 : limite);
        return named.query("""
                SELECT q.unidad_academica_clave AS clave, d.nombre, d.abreviatura, d.tipo,
                       count(*) AS total,
                       count(*) FILTER (WHERE %s NOT IN ('CERRADA')) AS en_tramite,
                       count(*) FILTER (WHERE %s = 'CERRADA') AS cerradas,
                       max(q.fecha_creacion) AS ultima
                  FROM quejas q
                  LEFT JOIN dependencias d ON d.clave = q.unidad_academica_clave
                """.formatted(ETAPA_SQL, ETAPA_SQL) + where + """
                 GROUP BY q.unidad_academica_clave, d.nombre, d.abreviatura, d.tipo
                 ORDER BY total DESC, clave
                 LIMIT :limite
                """,
                p,
                (rs, i) -> new UnidadConteo(rs.getString("clave"), rs.getString("nombre"), rs.getString("abreviatura"),
                        rs.getString("tipo"), rs.getLong("total"), rs.getLong("en_tramite"), rs.getLong("cerradas"),
                        ts(rs.getTimestamp("ultima"))));
    }

    // ======================================================================================
    // Explorador de quejas
    // ======================================================================================

    private static final String NOMBRE_QUEJOSO_SQL =
            "trim(concat_ws(' ', q.nombre_quejoso, q.apellido1_quejoso, q.apellido2_quejoso))";

    /** Estado resumido de la queja para la Defensora (columna "Estado" del explorador). */
    static final String ESTADO_SQL = """
            CASE
              WHEN q.estatus IS NULL OR q.estatus IN ('RECIBIDA','CORREGIDA') THEN 'SIN_ATENCION'
              WHEN q.estatus = 'RECHAZADA' THEN 'CON_QUEJOSO'
              WHEN q.estatus IN ('CONCLUIDA','IMPROCEDENTE','REMITIDA','CANCELADA') THEN 'CERRADA'
              ELSE 'EN_TRAMITE'
            END""";

    static final Map<String, String> ESTADOS = new LinkedHashMap<>();

    static {
        ESTADOS.put("SIN_ATENCION", "Sin atención");
        ESTADOS.put("EN_TRAMITE", "En trámite");
        ESTADOS.put("CON_QUEJOSO", "Con el quejoso");
        ESTADOS.put("CERRADA", "Cerrada");
    }

    /** Ordenes permitidos (la clave viene del front; nunca se concatena texto del usuario). */
    private static final Map<String, String> ORDENES = Map.of(
            "reciente", "q.fecha_creacion DESC, q.id DESC",
            "antigua", "q.fecha_creacion ASC, q.id ASC",
            "folio", "q.numero_folio ASC",
            "unidad", "unidad_nombre ASC NULLS LAST, q.fecha_creacion DESC",
            "quejoso", "quejoso ASC, q.fecha_creacion DESC");

    /**
     * Explorador de quejas. {@code desde}/{@code hasta} son meses (AAAA-MM, inclusivos);
     * {@code mes} se conserva para los enlaces de la gráfica mensual. {@code tamanio} llega
     * hasta 5000 para que "Exportar" pueda bajar todo el resultado de una vez.
     */
    public PaginaQuejas quejas(Map<String, String> f, int pagina, int tamanio) {
        String etapa = f.get("etapa");
        String estatus = f.get("estatus");
        String unidad = f.get("unidad");
        String abogado = f.get("abogado");
        String recepcionista = f.get("recepcionista");
        String mes = f.get("mes");
        String texto = f.get("q");
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        if (vacio(etapa) != null) {
            where.append(" AND ").append(ETAPA_SQL).append(" = :etapa ");
            p.addValue("etapa", etapa.trim().toUpperCase());
        }
        if (vacio(estatus) != null) {
            where.append(" AND coalesce(q.estatus,'RECIBIDA') = :estatus ");
            p.addValue("estatus", estatus.trim().toUpperCase());
        }
        if (vacio(f.get("estado")) != null) {
            where.append(" AND ").append(ESTADO_SQL).append(" = :estado ");
            p.addValue("estado", f.get("estado").trim().toUpperCase());
        }
        if (vacio(unidad) != null) {
            where.append(" AND q.unidad_academica_clave = :unidad ");
            p.addValue("unidad", unidad.trim());
        }
        if (vacio(abogado) != null) {
            if ("SIN_ASIGNAR".equalsIgnoreCase(abogado.trim())) {
                where.append(" AND (q.defensor_asignado IS NULL OR trim(q.defensor_asignado) = '') ");
            } else {
                where.append(" AND lower(trim(q.defensor_asignado)) = lower(:abogado) ");
                p.addValue("abogado", abogado.trim());
            }
        }
        if (vacio(recepcionista) != null) {
            if ("NADIE".equalsIgnoreCase(recepcionista.trim())) {
                where.append(" AND q.revisando_por IS NULL AND q.validado_por IS NULL ");
            } else {
                where.append(" AND (lower(q.revisando_por) = lower(:recep) OR lower(q.validado_por) = lower(:recep)) ");
                p.addValue("recep", recepcionista.trim());
            }
        }
        if (vacio(mes) != null) {
            where.append(" AND to_char(q.fecha_creacion, 'YYYY-MM') = :mes ");
            p.addValue("mes", mes.trim());
        }
        if (vacio(f.get("desde")) != null) {
            where.append(" AND to_char(q.fecha_creacion, 'YYYY-MM') >= :desde ");
            p.addValue("desde", f.get("desde").trim().substring(0, Math.min(7, f.get("desde").trim().length())));
        }
        if (vacio(f.get("hasta")) != null) {
            where.append(" AND to_char(q.fecha_creacion, 'YYYY-MM') <= :hasta ");
            p.addValue("hasta", f.get("hasta").trim().substring(0, Math.min(7, f.get("hasta").trim().length())));
        }
        if (vacio(f.get("motivo")) != null) {
            String condicion = PanelDefensoraService.condicionMotivo(f.get("motivo").trim().toUpperCase());
            if (condicion != null) {
                where.append(" AND (").append(condicion).append(") ");
            }
        }
        if (vacio(texto) != null) {
            where.append(" AND (lower(q.numero_folio) LIKE :t OR lower(").append(NOMBRE_QUEJOSO_SQL)
                    .append(") LIKE :t OR lower(coalesce(q.defensor_asignado,'')) LIKE :t"
                            + " OR lower(trim(concat_ws(' ', q.nombre_denunciado, q.apellido1_denunciado))) LIKE :t"
                            + " OR lower(coalesce(q.descripcion,'')) LIKE :t) ");
            p.addValue("t", "%" + texto.trim().toLowerCase().replace("%", "") + "%");
        }

        // Prioridad: la asigna Primer Contacto en su expediente (si esa tabla existe en la base).
        boolean conPrioridad = existeTabla("expedientes_primer_contacto");
        String prioridadSql = conPrioridad ? "upper(epc.prioridad)" : "CAST(NULL AS varchar)";
        if (vacio(f.get("prioridad")) != null) {
            if (conPrioridad) {
                where.append(" AND upper(coalesce(epc.prioridad,'SIN')) = :prioridad ");
                p.addValue("prioridad", f.get("prioridad").trim().toUpperCase());
            } else if (!"SIN".equalsIgnoreCase(f.get("prioridad").trim())) {
                where.append(" AND 1=0 ");
            }
        }

        int tam = Math.max(1, Math.min(tamanio <= 0 ? 20 : tamanio, 5000));
        int pag = Math.max(0, pagina);
        String from = """
                  FROM quejas q
                  LEFT JOIN dependencias d ON d.clave = q.unidad_academica_clave
                  LEFT JOIN personal_administrativo pr ON lower(pr.correo_institucional) = lower(q.revisando_por)
                  LEFT JOIN personal_administrativo pv ON lower(pv.correo_institucional) = lower(q.validado_por)
                """ + (conPrioridad
                ? " LEFT JOIN expedientes_primer_contacto epc ON epc.folio_origen = q.numero_folio "
                : "");
        Long total = named.queryForObject("SELECT count(*) " + from + where, p, Long.class);
        long totalElementos = total == null ? 0 : total;

        String orden = ORDENES.getOrDefault(f.getOrDefault("orden", "reciente"), ORDENES.get("reciente"));
        p.addValue("limite", tam).addValue("desplazamiento", (long) pag * tam);
        List<FilaQueja> filas = named.query("""
                SELECT q.numero_folio, q.fecha_creacion, %s AS quejoso, q.unidad_academica_clave,
                       coalesce(d.abreviatura, d.nombre, q.unidad_academica_clave) AS unidad_nombre, %s AS etapa,
                       coalesce(q.estatus,'RECIBIDA') AS estatus,
                       CASE WHEN q.estatus = 'EN_VALIDACION'
                            THEN coalesce(pr.nombre_completo, q.revisando_por_nombre, q.revisando_por)
                            ELSE coalesce(pv.nombre_completo, q.validado_por) END AS recepcionista,
                       q.defensor_asignado, q.folio_primer_contacto, q.origen_registro,
                       %s AS estado, %s AS prioridad,
                       floor(extract(epoch FROM (now() - q.fecha_creacion)) / 86400)::bigint AS dias
                """.formatted(NOMBRE_QUEJOSO_SQL, ETAPA_SQL, ESTADO_SQL, prioridadSql) + from + where
                        + " ORDER BY " + orden + " LIMIT :limite OFFSET :desplazamiento",
                p,
                (rs, i) -> new FilaQueja(rs.getString(1), ts(rs.getTimestamp(2)), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), etiquetaEstatus(rs.getString(7)),
                        rs.getString(8), "EN_VALIDACION".equals(rs.getString(7)), rs.getString(9), rs.getString(10),
                        "MANUAL".equals(rs.getString(11)) ? "Ventanilla" : "Web",
                        rs.getString("estado"), ESTADOS.getOrDefault(rs.getString("estado"), rs.getString("estado")),
                        rs.getString("prioridad"), rs.getLong("dias")));

        int totalPaginas = (int) Math.ceil(totalElementos / (double) tam);
        return new PaginaQuejas(filas, pag, tam, totalElementos, totalPaginas);
    }

    public Filtros filtros() {
        List<Persona> recep = jdbc.query("""
                SELECT correo_institucional, nombre_completo FROM personal_administrativo
                 WHERE rol = 'RECEPCIONISTA' ORDER BY nombre_completo
                """, (rs, i) -> new Persona(rs.getString(1), rs.getString(2)));
        List<String> abogados = jdbc.queryForList("""
                SELECT DISTINCT nombre FROM (
                  SELECT nombre_completo AS nombre FROM personal_administrativo WHERE rol IN ('SUBDEFENSOR','DEFENSOR')
                  UNION SELECT defensor_asignado FROM quejas WHERE defensor_asignado IS NOT NULL AND defensor_asignado <> ''
                ) x ORDER BY nombre
                """, String.class);
        List<Conteo> etapas = new ArrayList<>();
        ETAPAS.forEach((k, v) -> etapas.add(new Conteo(k, v, 0)));
        List<Conteo> estados = new ArrayList<>();
        ESTADOS.forEach((k, v) -> estados.add(new Conteo(k, v, 0)));
        return new Filtros(recep, abogados, etapas, estados);
    }

    // ======================================================================================
    // Detalle de solo lectura
    // ======================================================================================

    public DetalleQueja detalle(String folio) {
        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT q.*, %s AS etapa, coalesce(d.nombre, q.unidad_academica_clave) AS unidad_nombre,
                       pr.nombre_completo AS nombre_revisando, pv.nombre_completo AS nombre_valido
                  FROM quejas q
                  LEFT JOIN dependencias d ON d.clave = q.unidad_academica_clave
                  LEFT JOIN personal_administrativo pr ON lower(pr.correo_institucional) = lower(q.revisando_por)
                  LEFT JOIN personal_administrativo pv ON lower(pv.correo_institucional) = lower(q.validado_por)
                 WHERE q.numero_folio = ?
                """.formatted(ETAPA_SQL), folio);
        if (filas.isEmpty()) {
            throw new RuntimeException("No se encontró la queja " + folio + ".");
        }
        Map<String, Object> q = filas.get(0);
        Long id = ((Number) q.get("id")).longValue();
        String estatus = str(q, "estatus") == null ? "RECIBIDA" : str(q, "estatus");
        boolean atendiendo = "EN_VALIDACION".equals(estatus);
        String recepcionista = atendiendo
                ? primero(str(q, "nombre_revisando"), str(q, "revisando_por_nombre"), str(q, "revisando_por"))
                : primero(str(q, "nombre_valido"), str(q, "validado_por"));

        List<Evidencia> evidencias = jdbc.query("""
                SELECT id, nombre_archivo, tipo_mime, tipo, tamanio_bytes, fecha_subida
                  FROM queja_evidencias WHERE queja_id = ? ORDER BY id
                """,
                (rs, i) -> new Evidencia(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getObject(5) == null ? null : rs.getLong(5), ts(rs.getTimestamp(6))), id);

        long respuestas = existeTabla("respuestas_denunciado")
                ? contar("SELECT count(*) FROM respuestas_denunciado WHERE folio_queja = '" + folio.replace("'", "''") + "'")
                : 0;

        String nombreValido = primero(str(q, "nombre_valido"), str(q, "validado_por"));
        List<Evento> linea = new ArrayList<>();
        linea.add(new Evento(fecha(q, "fecha_creacion"), "Queja recibida",
                "MANUAL".equals(str(q, "origen_registro")) ? "Capturada en ventanilla" : "Registrada en el portal"));
        if (atendiendo && fecha(q, "fecha_inicio_revision") != null) {
            linea.add(new Evento(fecha(q, "fecha_inicio_revision"), "En validación", "La atiende " + recepcionista));
        }
        if (str(q, "motivo_rechazo") != null && !str(q, "motivo_rechazo").isBlank()) {
            linea.add(new Evento("RECHAZADA".equals(estatus) ? fecha(q, "fecha_validacion") : null,
                    "Regresada con observaciones", "RECHAZADA".equals(estatus) ? "Por " + nombreValido : "Antes de ser corregida"));
        }
        if (fecha(q, "fecha_turnado") != null) {
            linea.add(new Evento(fecha(q, "fecha_turnado"), "Turnada a Primer Contacto",
                    "Validó " + nombreValido + (str(q, "defensor_asignado") != null ? " · Abogado: " + str(q, "defensor_asignado") : "")));
        }
        if (!"TURNADA".equals(estatus) && "PRIMER_CONTACTO,SUBDEFENSORIA,CERRADA".contains(str(q, "etapa"))) {
            linea.add(new Evento(null, "Estatus actual: " + etiquetaEstatus(estatus), ETAPAS.get(str(q, "etapa"))));
        }

        return new DetalleQueja(
                folio, str(q, "etapa"), ETAPAS.getOrDefault(str(q, "etapa"), "Otra"), estatus, etiquetaEstatus(estatus),
                "MANUAL".equals(str(q, "origen_registro")) ? "Ventanilla" : "Web", fecha(q, "fecha_creacion"),
                primero(String.join(" ", noNulos(str(q, "nombre_quejoso"), str(q, "apellido1_quejoso"), str(q, "apellido2_quejoso"))).trim(),
                        str(q, "correo_institucional")),
                str(q, "correo_institucional"), str(q, "telefono_contacto"), dia(q, "fecha_nacimiento_quejoso"),
                str(q, "tipo_identificacion_quejoso"), str(q, "numero_identificacion_quejoso"),
                str(q, "unidad_academica_clave"), str(q, "unidad_nombre"), dia(q, "fecha_hechos"),
                String.join(" ", noNulos(str(q, "nombre_denunciado"), str(q, "apellido1_denunciado"), str(q, "apellido2_denunciado"))).trim(),
                str(q, "descripcion"),
                recepcionista, atendiendo, str(q, "defensor_asignado"), str(q, "folio_primer_contacto"),
                str(q, "comentarios_recepcion"), str(q, "motivo_rechazo"),
                str(q, "tipo_documento_fisico"), str(q, "numero_oficio"), dia(q, "fecha_recepcion_fisica"),
                str(q, "ubicacion_fisica_expediente"), evidencias, respuestas, linea);
    }

    /** Contenido de una evidencia, solo si pertenece a la queja indicada. */
    public Map<String, Object> evidencia(String folio, Long id) {
        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT e.nombre_archivo, e.tipo_mime, e.contenido
                  FROM queja_evidencias e JOIN quejas q ON q.id = e.queja_id
                 WHERE q.numero_folio = ? AND e.id = ?
                """, folio, id);
        if (filas.isEmpty()) {
            throw new RuntimeException("No se encontró el archivo.");
        }
        return filas.get(0);
    }

    // ======================================================================================
    // utilidades
    // ======================================================================================

    static String etiquetaEstatus(String estatus) {
        return estatus == null ? "Nueva, sin tomar" : ESTATUS.getOrDefault(estatus, estatus);
    }

    private long contar(String sql) {
        Long v = jdbc.queryForObject(sql, Long.class);
        return v == null ? 0 : v;
    }

    private boolean existeTabla(String tabla) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class, "public." + tabla));
    }

    private static long n(Map<String, Object> fila, String col) {
        Object v = fila.get(col);
        return v == null ? 0 : ((Number) v).longValue();
    }

    private static String str(Map<String, Object> fila, String col) {
        Object v = fila.get(col);
        return v == null ? null : v.toString();
    }

    private static LocalDateTime fecha(Map<String, Object> fila, String col) {
        Object v = fila.get(col);
        return v instanceof Timestamp t ? t.toLocalDateTime() : null;
    }

    private static LocalDate dia(Map<String, Object> fila, String col) {
        Object v = fila.get(col);
        if (v instanceof Date d) {
            return d.toLocalDate();
        }
        return v instanceof Timestamp t ? t.toLocalDateTime().toLocalDate() : null;
    }

    private static LocalDateTime ts(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }

    private static String vacio(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String primero(String... valores) {
        for (String v : valores) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String[] noNulos(String... valores) {
        return java.util.Arrays.stream(valores).filter(v -> v != null && !v.isBlank()).toArray(String[]::new);
    }
}
