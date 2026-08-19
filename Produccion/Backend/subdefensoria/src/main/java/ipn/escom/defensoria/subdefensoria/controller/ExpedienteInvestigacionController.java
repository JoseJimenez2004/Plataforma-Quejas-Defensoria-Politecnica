package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.dto.ExpedienteInvestigacionDTO;
import ipn.escom.defensoria.subdefensoria.dto.ExpedienteResumenDTO;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.service.ExpedienteResumenService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Consulta de detalle de un expediente (para la pantalla de detalle del front). */
@RestController
@RequestMapping("/api/subdefensoria/expedientes")
public class ExpedienteInvestigacionController {

    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final ExpedienteResumenService expedienteResumenService;

    public ExpedienteInvestigacionController(
            ExpedienteInvestigacionRepository expedienteRepository,
            ExpedienteResumenService expedienteResumenService
    ) {
        this.expedienteRepository = expedienteRepository;
        this.expedienteResumenService = expedienteResumenService;
    }

    /** Bandeja unificada: todos los expedientes, cualquier estatus, con su progreso. */
    @GetMapping
    public List<ExpedienteResumenDTO> listarTodos() {
        return expedienteResumenService.listarTodos();
    }

    @GetMapping("/folio/{folio}")
    public ExpedienteInvestigacionDTO obtenerPorFolio(@PathVariable String folio) {
        ExpedienteInvestigacion e = expedienteRepository.findByFolio(folio)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el expediente con folio " + folio));

        return ExpedienteInvestigacionDTO.builder()
                .id(e.getId())
                .folio(e.getFolio())
                .folioOrigen(e.getFolioOrigen())
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
                .build();
    }
}
