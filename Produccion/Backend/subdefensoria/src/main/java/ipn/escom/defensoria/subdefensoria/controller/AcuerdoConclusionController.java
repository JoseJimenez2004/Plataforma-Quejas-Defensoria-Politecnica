package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.dto.AcuerdoConclusionDTO;
import ipn.escom.defensoria.subdefensoria.dto.CrearAcuerdoConclusionDTO;
import ipn.escom.defensoria.subdefensoria.service.AcuerdoConclusionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subdefensoria/acuerdos-conclusion")
public class AcuerdoConclusionController {

    private final AcuerdoConclusionService acuerdoConclusionService;

    public AcuerdoConclusionController(AcuerdoConclusionService acuerdoConclusionService) {
        this.acuerdoConclusionService = acuerdoConclusionService;
    }

    /**
     * TS-07/08: redactar el acuerdo y, si concluir=true, MANDARSELO AL QUEJOSO.
     * Ya no cierra el expediente: lo deja en PENDIENTE_CONCLUSION esperando su respuesta.
     */
    @PostMapping
    public AcuerdoConclusionDTO guardarOConcluir(@Valid @RequestBody CrearAcuerdoConclusionDTO dto) {
        return acuerdoConclusionService.guardarOConcluir(dto);
    }

    /**
     * El quejoso acepta el acuerdo -> el expediente pasa a CONCLUIDO.
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

    @GetMapping("/expediente/{expedienteId}")
    public AcuerdoConclusionDTO obtenerPorExpediente(@PathVariable Long expedienteId) {
        return acuerdoConclusionService.obtenerPorExpediente(expedienteId);
    }
}
