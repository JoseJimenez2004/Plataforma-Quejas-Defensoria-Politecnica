package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.entity.EstatusExpediente;
import ipn.escom.defensoria.subdefensoria.service.TransicionExpedienteService;
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
    private final TransicionExpedienteService transicionService;

    public ExpedienteInvestigacionController(
            ExpedienteInvestigacionRepository expedienteRepository,
            ExpedienteResumenService expedienteResumenService,
            TransicionExpedienteService transicionService
    ) {
        this.expedienteRepository = expedienteRepository;
        this.expedienteResumenService = expedienteResumenService;
        this.transicionService = transicionService;
    }

    /** Bandeja unificada: todos los expedientes, cualquier estatus, con su progreso. */
    @GetMapping
    public List<ExpedienteResumenDTO> listarTodos() {
        return expedienteResumenService.listarTodos();
    }

    @GetMapping("/folio/{folio}")
    public ExpedienteInvestigacionDTO obtenerPorFolio(@PathVariable String folio) {
        return convertir(buscar(folio));
    }

    /*
     * CU-SD-05 / fila 14 del diagrama: el subdefensor abre el expediente para trabajarlo,
     * RECIBIDO -> EN_INVESTIGACION (en otro estado solo lo devuelve). Es explícito y no un
     * efecto del GET, igual que iniciar-analisis de Primer Contacto: otras pantallas también
     * consultan el expediente y eso no es "abrirlo".
     */
    @PostMapping("/folio/{folio}/iniciar-investigacion")
    public ExpedienteInvestigacionDTO iniciarInvestigacion(@PathVariable String folio) {
        ExpedienteInvestigacion e = buscar(folio);
        if (EstatusExpediente.RECIBIDO.equals(e.getEstatus())) {
            e = transicionService.cambiar(e, EstatusExpediente.EN_INVESTIGACION);
        }
        return convertir(e);
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
                .build();
    }
}
