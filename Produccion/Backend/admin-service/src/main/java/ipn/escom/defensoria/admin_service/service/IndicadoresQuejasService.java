package ipn.escom.defensoria.admin_service.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.admin_service.model.IndicadoresQuejasModel;
import ipn.escom.defensoria.admin_service.model.IndicadoresQuejasModel.ConteoModel;

/**
 * Cuenta quejas directamente en defensoria_db. Solo lectura: admin-service no mapea la tabla
 * quejas como entidad (es de queja-service), por eso se usa SQL y no JPA.
 */
@Service
public class IndicadoresQuejasService {

    private static final Logger log = LoggerFactory.getLogger(IndicadoresQuejasService.class);

    private final JdbcTemplate jdbc;

    public IndicadoresQuejasService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public IndicadoresQuejasModel obtener() {
        try {
            Map<String, Long> porEstatus = new HashMap<>();
            jdbc.query("SELECT COALESCE(estatus, 'RECIBIDA') AS estatus, COUNT(*) AS total FROM quejas GROUP BY 1",
                    rs -> { porEstatus.merge(rs.getString("estatus"), rs.getLong("total"), Long::sum); });
            long total = porEstatus.values().stream().mapToLong(Long::longValue).sum();

            long ultimos30 = contar("SELECT COUNT(*) FROM quejas WHERE fecha_creacion >= NOW() - INTERVAL '30 days'");
            long turnadasMes = contar("SELECT COUNT(*) FROM quejas WHERE estatus = 'TURNADA' "
                    + "AND fecha_turnado >= date_trunc('month', NOW())");
            long respuestas = contarSiExiste("respuestas_denunciado", "SELECT COUNT(*) FROM respuestas_denunciado");

            List<ConteoModel> unidades = jdbc.query(
                    "SELECT q.unidad_academica_clave AS clave, "
                    + "COALESCE(d.abreviatura, d.nombre, q.unidad_academica_clave) AS etiqueta, COUNT(*) AS total "
                    + "FROM quejas q LEFT JOIN dependencias d ON d.clave = q.unidad_academica_clave "
                    + "WHERE q.unidad_academica_clave IS NOT NULL "
                    + "GROUP BY 1, 2 ORDER BY 3 DESC, 2 LIMIT 5",
                    (rs, i) -> new ConteoModel(rs.getString("clave"), rs.getString("etiqueta"), rs.getLong("total")));

            List<ConteoModel> porMes = jdbc.query(
                    "SELECT to_char(m.mes, 'YYYY-MM') AS clave, to_char(m.mes, 'YYYY-MM') AS etiqueta, "
                    + "COUNT(q.id) AS total "
                    + "FROM generate_series(date_trunc('month', NOW()) - INTERVAL '5 months', "
                    + "date_trunc('month', NOW()), INTERVAL '1 month') AS m(mes) "
                    + "LEFT JOIN quejas q ON date_trunc('month', q.fecha_creacion) = m.mes "
                    + "GROUP BY m.mes ORDER BY m.mes",
                    (rs, i) -> new ConteoModel(rs.getString("clave"), rs.getString("etiqueta"), rs.getLong("total")));

            return new IndicadoresQuejasModel(total,
                    porEstatus.getOrDefault("RECIBIDA", 0L),
                    porEstatus.getOrDefault("EN_VALIDACION", 0L),
                    porEstatus.getOrDefault("CORREGIDA", 0L),
                    porEstatus.getOrDefault("TURNADA", 0L),
                    porEstatus.getOrDefault("RECHAZADA", 0L),
                    porEstatus.getOrDefault("CANCELADA", 0L),
                    ultimos30, turnadasMes, respuestas, unidades, porMes);
        } catch (Exception ex) {
            // El dashboard no debe caerse si la tabla quejas aún no existe (queja-service no ha arrancado).
            log.warn("No se pudieron calcular los indicadores de quejas: {}", ex.getMessage());
            return null;
        }
    }

    private long contar(String sql) {
        Long valor = jdbc.queryForObject(sql, Long.class);
        return valor == null ? 0 : valor;
    }

    private long contarSiExiste(String tabla, String sql) {
        Boolean existe = jdbc.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class, "public." + tabla);
        return Boolean.TRUE.equals(existe) ? contar(sql) : 0;
    }
}
