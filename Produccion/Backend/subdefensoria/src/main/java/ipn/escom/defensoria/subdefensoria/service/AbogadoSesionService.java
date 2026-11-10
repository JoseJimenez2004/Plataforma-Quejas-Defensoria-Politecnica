package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.OficioInformacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.RespuestaExternaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * 2026-11: a cada abogado (SUBDEFENSOR) solo le llegan SUS expedientes: los que Primer Contacto
 * le turnó en el dictamen (expedientes_investigacion.abogado_asesor_id). El Defensor titular
 * (DEFENSOR) supervisa y ve todos.
 *
 * El JWT trae el correo del usuario; su id se busca en personal_administrativo (misma base).
 */
@Service
public class AbogadoSesionService {

    private final JdbcTemplate jdbc;
    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final OficioInformacionRepository oficioRepository;
    private final RespuestaExternaRepository respuestaRepository;

    public AbogadoSesionService(JdbcTemplate jdbc,
                                ExpedienteInvestigacionRepository expedienteRepository,
                                OficioInformacionRepository oficioRepository,
                                RespuestaExternaRepository respuestaRepository) {
        this.jdbc = jdbc;
        this.expedienteRepository = expedienteRepository;
        this.oficioRepository = oficioRepository;
        this.respuestaRepository = respuestaRepository;
    }

    public boolean esTitular(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DEFENSOR".equals(a.getAuthority()));
    }

    /** Qué expedientes puede ver quien tiene la sesión. */
    public Predicate<ExpedienteInvestigacion> visibles(Authentication auth) {
        if (esTitular(auth)) {
            return e -> true;
        }
        Long id = idDe(auth);
        return e -> id != null && Objects.equals(e.getAbogadoAsesorId(), id);
    }

    /** 403 si el expediente es de otro abogado (alguien que abre el folio por URL). */
    public void exigirAcceso(ExpedienteInvestigacion e, Authentication auth) {
        if (!visibles(auth).test(e)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este expediente está asignado a "
                            + (e.getAbogadoAsesorNombre() != null ? e.getAbogadoAsesorNombre() : "otro abogado") + ".");
        }
    }

    /*
     * Acciones del trámite (oficios, respuestas, recordatorios, acuerdos, prioridad): el
     * expediente debe ser del abogado de la sesión. Si el id no existe no se hace nada aquí:
     * el servicio de la acción responde su propio 404.
     */
    public void exigirExpediente(Long expedienteId, Authentication auth) {
        if (expedienteId != null) {
            expedienteRepository.findById(expedienteId).ifPresent(e -> exigirAcceso(e, auth));
        }
    }

    public void exigirOficio(Long oficioId, Authentication auth) {
        if (oficioId != null) {
            oficioRepository.findById(oficioId).ifPresent(o -> exigirExpediente(o.getExpedienteId(), auth));
        }
    }

    public void exigirRespuesta(Long respuestaId, Authentication auth) {
        if (respuestaId != null) {
            respuestaRepository.findById(respuestaId).ifPresent(r -> exigirExpediente(r.getExpedienteId(), auth));
        }
    }

    private Long idDe(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return jdbc.query("""
                SELECT id FROM personal_administrativo
                 WHERE lower(correo_institucional) = lower(?) AND activo = true
                 ORDER BY id LIMIT 1
                """, (rs, i) -> rs.getLong(1), auth.getName()).stream().findFirst().orElse(null);
    }
}
