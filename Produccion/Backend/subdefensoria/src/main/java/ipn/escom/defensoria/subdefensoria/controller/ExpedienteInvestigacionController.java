package ipn.escom.defensoria.subdefensoria.controller;

import org.springframework.security.core.Authentication;
import ipn.escom.defensoria.subdefensoria.service.AbogadoSesionService;
import ipn.escom.defensoria.subdefensoria.entity.EstatusExpediente;
import ipn.escom.defensoria.subdefensoria.service.TransicionExpedienteService;
import ipn.escom.defensoria.subdefensoria.dto.ExpedienteInvestigacionDTO;
import ipn.escom.defensoria.subdefensoria.dto.ExpedienteResumenDTO;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.service.ExpedienteResumenService;
import ipn.escom.defensoria.subdefensoria.util.Prioridad;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Consulta de detalle de un expediente (para la pantalla de detalle del front). */
@RestController
@RequestMapping("/api/subdefensoria/expedientes")
public class ExpedienteInvestigacionController {

    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final ExpedienteResumenService expedienteResumenService;
    private final TransicionExpedienteService transicionService;
    private final AbogadoSesionService abogadoSesion;

    public ExpedienteInvestigacionController(
            ExpedienteInvestigacionRepository expedienteRepository,
            ExpedienteResumenService expedienteResumenService,
            TransicionExpedienteService transicionService,
            AbogadoSesionService abogadoSesion
    ) {
        this.abogadoSesion = abogadoSesion;
        this.expedienteRepository = expedienteRepository;
        this.expedienteResumenService = expedienteResumenService;
        this.transicionService = transicionService;
    }

    /** Bandeja unificada: todos los expedientes, cualquier estatus, con su progreso. */
    @GetMapping
    public List<ExpedienteResumenDTO> listarTodos(Authentication authentication) {
        // 2026-11: el abogado solo ve los suyos; el Defensor titular ve todos.
        return expedienteResumenService.listarTodos(abogadoSesion.visibles(authentication));
    }

    @GetMapping("/folio/{folio}")
    public ExpedienteInvestigacionDTO obtenerPorFolio(@PathVariable String folio, Authentication authentication) {
        ExpedienteInvestigacion e = buscar(folio);
        abogadoSesion.exigirAcceso(e, authentication);
        return convertir(e);
    }

    /*
     * CU-SD-05 / fila 14 del diagrama: el subdefensor abre el expediente para trabajarlo,
     * RECIBIDO -> EN_INVESTIGACION (en otro estado solo lo devuelve). Es explícito y no un
     * efecto del GET, igual que iniciar-analisis de Primer Contacto: otras pantallas también
     * consultan el expediente y eso no es "abrirlo".
     */
    @PostMapping("/folio/{folio}/iniciar-investigacion")
    public ExpedienteInvestigacionDTO iniciarInvestigacion(@PathVariable String folio, Authentication authentication) {
        ExpedienteInvestigacion e = buscar(folio);
        abogadoSesion.exigirAcceso(e, authentication);
        if (EstatusExpediente.RECIBIDO.equals(e.getEstatus())) {
            e = transicionService.cambiar(e, EstatusExpediente.EN_INVESTIGACION);
        }
        return convertir(e);
    }

    /**
     * SD-6: el subdefensor cambia la prioridad (BAJA|MEDIA|ALTA|URGENTE). El cambio no regresa
     * a Primer Contacto. Cualquier otro valor responde 400.
     */
    @PatchMapping("/{id}/prioridad")
    public ResponseEntity<?> cambiarPrioridad(@PathVariable Long id, @RequestBody Map<String, String> cuerpo,
                                              Authentication authentication) {
        abogadoSesion.exigirExpediente(id, authentication);
        String valor = cuerpo == null ? null : cuerpo.get("prioridad");
        if (!Prioridad.esValida(valor)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prioridad inválida. Usa BAJA, MEDIA, ALTA o URGENTE."));
        }
        ExpedienteInvestigacion e = expedienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el expediente " + id));
        e.setPrioridad(Prioridad.normalizar(valor));
        e.setFechaActualizacion(LocalDateTime.now());
        return ResponseEntity.ok(convertir(expedienteRepository.save(e)));
    }

    private ExpedienteInvestigacion buscar(String folio) {
        return expedienteRepository.findByFolio(folio)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el expediente con folio " + folio));
    }

    private ExpedienteInvestigacionDTO convertir(ExpedienteInvestigacion e) {

        return ExpedienteInvestigacionDTO.builder()
                .id(e.getId())
                .folio(e.getFolio())
                .folioOrigen(e.getFolioOrigen())
                .folioQueja(e.getFolioQueja())
                .quejosoNombre(e.getQuejosoNombre())
                .unidadAcademica(e.getUnidadAcademica())
                .asunto(e.getAsunto())
                .descripcionHechos(e.getDescripcionHechos())
                .fechaAdmision(
                        e.getFechaAdmision() != null
                                ? e.getFechaAdmision().toString()
                                : null
                )
                .abogadoAsesorId(e.getAbogadoAsesorId())
                .abogadoAsesorNombre(e.getAbogadoAsesorNombre())
                .estatus(e.getEstatus())
                .observacionesAnalista(
                        e.getObservacionesAnalista()
                )
                .prioridad(e.getPrioridad())
                .build();
    }
}
