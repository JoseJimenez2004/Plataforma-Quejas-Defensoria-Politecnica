package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.AcuerdoConciliacionDTO;
import ipn.escom.defensoria.primercontacto.dto.CrearConciliacionDTO;
import ipn.escom.defensoria.primercontacto.service.AnalistaAutenticadoService;
import ipn.escom.defensoria.primercontacto.service.ConciliacionPrimerContactoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * CU-PC-10: proponer y listar acuerdos de conciliación.
 */
@RestController
@RequestMapping("/api/primer-contacto/conciliaciones")
public class ConciliacionPrimerContactoController {

    private final ConciliacionPrimerContactoService conciliacionService;
    private final AnalistaAutenticadoService analistaAutenticadoService;

    public ConciliacionPrimerContactoController(
            ConciliacionPrimerContactoService conciliacionService,
            AnalistaAutenticadoService analistaAutenticadoService
    ) {
        this.conciliacionService = conciliacionService;
        this.analistaAutenticadoService = analistaAutenticadoService;
    }

    @PostMapping
    public ResponseEntity<AcuerdoConciliacionDTO> proponer(
            @Valid @RequestBody CrearConciliacionDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                conciliacionService.proponer(
                        dto,
                        analistaAutenticadoService.obtenerAnalista(authentication)
                )
        );
    }

    /*
     * Acuerdos del expediente (folio PC-...), del más reciente al más antiguo.
     */
    @GetMapping("/expediente/{folio}")
    public List<AcuerdoConciliacionDTO> listar(
            @PathVariable String folio
    ) {
        return conciliacionService.listar(folio);
    }
}
