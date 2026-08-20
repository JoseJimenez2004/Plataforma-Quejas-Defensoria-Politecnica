package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.ExpedienteTurnadoRequest;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.service.IngresoPrimerContactoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/primer-contacto/ingesta")
public class IngestaRevisionController {

    private final IngresoPrimerContactoService ingresoService;

    public IngestaRevisionController(
            IngresoPrimerContactoService ingresoService
    ) {
        this.ingresoService = ingresoService;
    }

    /**
     * Recibe un expediente que fue turnado desde el área de Revisión.
     *
     * La comunicación entre áreas se hace utilizando el folio
     * administrativo de origen, NO el id interno de la tabla quejas.
     *
     * Primer Contacto genera su propio id y su propio folio.
     */
    @PostMapping("/expedientes")
    public ResponseEntity<ExpedientePrimerContacto> recibirExpediente(
            @Valid @RequestBody ExpedienteTurnadoRequest request
    ) {

        ExpedientePrimerContacto expediente =
                ingresoService.recibir(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(expediente);
    }
}