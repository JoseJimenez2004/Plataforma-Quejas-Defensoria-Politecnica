package ipn.escom.defensoria.revision_service.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.revision_service.model.CargaRecepcionistaModel;
import ipn.escom.defensoria.revision_service.model.IndicadoresBandejaModel;

/**
 * Indicadores de la Bandeja de Entrada y carga de trabajo del equipo de Recepción
 * (2026-10-15). Se calculan con SQL directo (conteos y un promedio) para no traer quejas
 * completas -- con descripción y archivos -- solo para contarlas.
 *
 * "Hoy" se toma con LocalDate.now() del contenedor, igual que el resto del servicio y que las
 * fechas que guarda queja-service, para que los conteos cuadren con las fechas guardadas.
 */
@Service
public class IndicadoresBandejaService {

    private final JdbcTemplate jdbc;

    public IndicadoresBandejaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public IndicadoresBandejaModel indicadores(String correo) {
        Timestamp inicioHoy = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Timestamp hace30Dias = Timestamp.valueOf(LocalDateTime.now().minusDays(30));

        Map<String, Object> c = jdbc.queryForMap("""
                SELECT
                  count(*) FILTER (WHERE estatus IN ('RECIBIDA', 'CORREGIDA'))                 AS por_atender,
                  count(*) FILTER (WHERE fecha_creacion >= ?)                                  AS nuevas_hoy,
                  count(*) FILTER (WHERE estatus = 'CORREGIDA')                                AS corregidas,
                  count(*) FILTER (WHERE estatus = 'EN_VALIDACION')                            AS en_atencion,
                  count(*) FILTER (WHERE estatus = 'EN_VALIDACION'
                                     AND lower(revisando_por) = lower(?))                      AS en_atencion_mias,
                  count(*) FILTER (WHERE fecha_validacion >= ?)                                AS atendidas_hoy,
                  count(*) FILTER (WHERE fecha_validacion >= ? AND lower(validado_por) = lower(?)) AS atendidas_hoy_mias,
                  count(*) FILTER (WHERE fecha_turnado >= ?)                                   AS turnadas_hoy,
                  count(*) FILTER (WHERE estatus = 'RECHAZADA' AND fecha_validacion >= ?)      AS rechazadas_hoy,
                  avg(EXTRACT(EPOCH FROM (fecha_validacion - fecha_creacion)) / 3600.0)
                      FILTER (WHERE fecha_validacion >= ? AND fecha_creacion IS NOT NULL)      AS horas_promedio,
                  count(*) FILTER (WHERE fecha_validacion >= ?)                                AS atendidas_30
                FROM quejas
                """,
                inicioHoy, correo, inicioHoy, inicioHoy, correo, inicioHoy, inicioHoy, hace30Dias, hace30Dias);

        String folioMasAntiguo = null;
        LocalDateTime fechaMasAntigua = null;
        List<Map<String, Object>> antigua = jdbc.queryForList("""
                SELECT numero_folio, fecha_creacion FROM quejas
                 WHERE estatus IN ('RECIBIDA', 'CORREGIDA')
                 ORDER BY fecha_creacion ASC NULLS LAST, id ASC
                 LIMIT 1
                """);
        if (!antigua.isEmpty()) {
            folioMasAntiguo = (String) antigua.get(0).get("numero_folio");
            Object f = antigua.get(0).get("fecha_creacion");
            fechaMasAntigua = f instanceof Timestamp ts ? ts.toLocalDateTime() : null;
        }

        Object horas = c.get("horas_promedio");
        Double horasPromedio = horas == null ? null : ((Number) horas).doubleValue();

        return new IndicadoresBandejaModel(
                num(c, "por_atender"), num(c, "nuevas_hoy"), num(c, "corregidas"),
                num(c, "en_atencion"), num(c, "en_atencion_mias"),
                num(c, "atendidas_hoy"), num(c, "atendidas_hoy_mias"),
                num(c, "turnadas_hoy"), num(c, "rechazadas_hoy"),
                folioMasAntiguo, fechaMasAntigua, horasPromedio, num(c, "atendidas_30"));
    }

    /** Cada recepcionista activa con lo que tiene abierto y lo que validó hoy. */
    public List<CargaRecepcionistaModel> equipo(String correo) {
        Timestamp inicioHoy = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        return jdbc.query("""
                SELECT p.nombre_completo,
                       lower(p.correo_institucional) = lower(?) AS soy_yo,
                       (SELECT count(*) FROM quejas q
                         WHERE q.estatus = 'EN_VALIDACION'
                           AND lower(q.revisando_por) = lower(p.correo_institucional)) AS en_atencion,
                       (SELECT count(*) FROM quejas q
                         WHERE q.fecha_validacion >= ?
                           AND lower(q.validado_por) = lower(p.correo_institucional)) AS atendidas_hoy
                  FROM personal_administrativo p
                 WHERE p.rol = 'RECEPCIONISTA' AND p.activo = true
                 ORDER BY atendidas_hoy DESC, p.nombre_completo ASC
                """,
                (rs, i) -> new CargaRecepcionistaModel(
                        rs.getString("nombre_completo"),
                        rs.getBoolean("soy_yo"),
                        rs.getLong("en_atencion"),
                        rs.getLong("atendidas_hoy")),
                correo, inicioHoy);
    }

    /**
     * Quejas por abogado asignado: [en trámite, total]. La llave es el nombre en minúsculas
     * (quejas.defensor_asignado guarda el NOMBRE del abogado, no su id).
     */
    public Map<String, long[]> cargaPorAbogado(List<String> estatusCerrados) {
        Map<String, long[]> cargas = new java.util.HashMap<>();
        String enLista = String.join(",", java.util.Collections.nCopies(estatusCerrados.size(), "?"));
        jdbc.query("""
                SELECT lower(defensor_asignado) AS abogado,
                       count(*) FILTER (WHERE estatus NOT IN (%s)) AS activas,
                       count(*) AS total
                  FROM quejas
                 WHERE defensor_asignado IS NOT NULL AND defensor_asignado <> ''
                 GROUP BY lower(defensor_asignado)
                """.formatted(enLista),
                rs -> {
                    cargas.put(rs.getString("abogado"), new long[] {rs.getLong("activas"), rs.getLong("total")});
                },
                estatusCerrados.toArray());
        return cargas;
    }

    private static long num(Map<String, Object> fila, String columna) {
        Object v = fila.get(columna);
        return v == null ? 0 : ((Number) v).longValue();
    }
}
