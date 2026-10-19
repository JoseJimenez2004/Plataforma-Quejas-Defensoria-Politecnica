package ipn.escom.defensoria.primercontacto.controller;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Abogados que pueden recibir un expediente en Subdefensoría (selector "Responsable en
 * Subdefensoría" de la pestaña Resolución, diseño 2026-10-18). Se ordenan del que menos
 * quejas en trámite tiene al que más, para repartir la carga.
 *
 * GET /api/primer-contacto/abogados
 */
@RestController
@RequestMapping("/api/primer-contacto/abogados")
public class AbogadosController {

    /** Abogado activo con las quejas que lleva y que todavía no se cierran. */
    public record AbogadoDTO(Long id, String nombre, String correo, String rol, long enTramite) {
    }

    private final JdbcTemplate jdbc;

    public AbogadosController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<AbogadoDTO> listar() {
        return jdbc.query("""
                SELECT p.id, p.nombre_completo, p.correo_institucional, p.rol,
                       count(q.id) FILTER (WHERE coalesce(q.estatus, '') NOT IN
                           ('CONCLUIDA','IMPROCEDENTE','REMITIDA','CANCELADA','RECHAZADA')) AS en_tramite
                  FROM personal_administrativo p
                  LEFT JOIN quejas q
                         ON lower(trim(coalesce(q.defensor_asignado, ''))) IN
                            (lower(p.nombre_completo), lower(p.correo_institucional))
                 WHERE p.rol IN ('SUBDEFENSOR', 'DEFENSOR') AND p.activo = true
                 GROUP BY p.id, p.nombre_completo, p.correo_institucional, p.rol
                 ORDER BY en_tramite, CASE p.rol WHEN 'SUBDEFENSOR' THEN 0 ELSE 1 END, p.nombre_completo
                """,
                (rs, i) -> new AbogadoDTO(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getLong(5)));
    }
}
