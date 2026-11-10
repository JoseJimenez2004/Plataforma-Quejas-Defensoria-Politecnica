package ipn.escom.defensoria.subdefensoria.controller;

import org.springframework.security.core.Authentication;
import ipn.escom.defensoria.subdefensoria.service.AbogadoSesionService;
import ipn.escom.defensoria.subdefensoria.dto.AcuerdoConclusionDTO;
import ipn.escom.defensoria.subdefensoria.dto.CrearAcuerdoConclusionDTO;
import ipn.escom.defensoria.subdefensoria.dto.RegistrarConclusionDTO;
import ipn.escom.defensoria.subdefensoria.service.AcuerdoConclusionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subdefensoria/acuerdos-conclusion")
public class AcuerdoConclusionController {

    private final AcuerdoConclusionService acuerdoConclusionService;
    private final AbogadoSesionService abogadoSesion;

    public AcuerdoConclusionController(AcuerdoConclusionService acuerdoConclusionService, AbogadoSesionService abogadoSesion) {
        this.acuerdoConclusionService = acuerdoConclusionService;
        this.abogadoSesion = abogadoSesion;
    }

    /**
     * TS-07/08: redactar el acuerdo y, si concluir=true, MANDARSELO AL QUEJOSO.
     * Ya no cierra el expediente: lo deja en PENDIENTE_CONCLUSION esperando su respuesta.
     */
    @PostMapping
    public AcuerdoConclusionDTO guardarOConcluir(@Valid @RequestBody CrearAcuerdoConclusionDTO dto,
                                                 Authentication authentication) {
        abogadoSesion.exigirExpediente(dto.getExpedienteId(), authentication);
        return acuerdoConclusionService.guardarOConcluir(dto);
    }

    /**
     * El quejoso acepta el acuerdo de conciliación -> PENDIENTE_CONCLUSION.
     *
     * Va bajo /interno porque quien la invoca es queja-service en nombre del quejoso, no
     * una persona de Subdefensoria: el quejoso tiene un JWT de quejoso, que este servicio
     * no reconoce. Mismo patron que /api/subdefensoria/ingesta (sin JWT, restringido por
     * IP de origen en el firewall).
     */
    @PostMapping("/interno/{expedienteId}/aceptar")
    public AcuerdoConclusionDTO aceptar(@PathVariable Long expedienteId,
                                        @RequestParam(required = false) String comentario) {
        return acuerdoConclusionService.aceptarPorQuejoso(expedienteId, comentario);
    }

    /** El quejoso rechaza el acuerdo -> el expediente regresa a EN_INVESTIGACION. */
    @PostMapping("/interno/{expedienteId}/rechazar")
    public AcuerdoConclusionDTO rechazar(@PathVariable Long expedienteId,
                                         @RequestParam(required = false) String comentario) {
        return acuerdoConclusionService.rechazarPorQuejoso(expedienteId, comentario);
    }

    /* Acuerdo de conclusión, con el de conciliación ya aceptado por el quejoso. */
    @PostMapping("/{expedienteId}/conclusion")
    public AcuerdoConclusionDTO registrarConclusion(@PathVariable Long expedienteId,
                                                    @Valid @RequestBody RegistrarConclusionDTO dto,
                                                    Authentication authentication) {
        abogadoSesion.exigirExpediente(expedienteId, authentication);
        return acuerdoConclusionService.registrarConclusion(expedienteId, dto.getTextoConclusion());
    }

    @GetMapping("/expediente/{expedienteId}")
    public AcuerdoConclusionDTO obtenerPorExpediente(@PathVariable Long expedienteId) {
        return acuerdoConclusionService.obtenerPorExpediente(expedienteId);
    }
}
