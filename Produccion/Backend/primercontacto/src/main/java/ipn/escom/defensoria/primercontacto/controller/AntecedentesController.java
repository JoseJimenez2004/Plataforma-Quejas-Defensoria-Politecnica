package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.AntecedenteGuardadoDTO;
import ipn.escom.defensoria.primercontacto.dto.BusquedaAntecedentesDTO;
import ipn.escom.defensoria.primercontacto.dto.GuardarAntecedentesDTO;
import ipn.escom.defensoria.primercontacto.service.AnalistaAutenticadoService;
import ipn.escom.defensoria.primercontacto.service.antecedentes.AntecedentesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/primer-contacto/antecedentes")
public class AntecedentesController {

    private final AntecedentesService antecedentesService;
    private final AnalistaAutenticadoService analistaAutenticadoService;

    public AntecedentesController(
            AntecedentesService antecedentesService,
            AnalistaAutenticadoService analistaAutenticadoService
    ) {
        this.antecedentesService = antecedentesService;
        this.analistaAutenticadoService = analistaAutenticadoService;
    }

    /*
     * Antecedentes del expediente (folio PC-...) según el modelo.
     *
     * GET /api/primer-contacto/antecedentes/PC-A1B2C3D4
     */
    @GetMapping("/{folio}")
    public BusquedaAntecedentesDTO buscar(@PathVariable String folio) {
        return antecedentesService.buscar(folio);
    }

    /*
     * Búsqueda manual por nombre del quejoso y/o del denunciado.
     *
     * GET /api/primer-contacto/antecedentes/PC-A1B2C3D4/manual?quejoso=...&denunciado=...
     */
    @GetMapping("/{folio}/manual")
    public BusquedaAntecedentesDTO buscarManual(
            @PathVariable String folio,
            @RequestParam(required = false) String quejoso,
            @RequestParam(required = false) String denunciado
    ) {
        return antecedentesService.buscarManual(folio, quejoso, denunciado);
    }

    /* Antecedentes finales ya elegidos para el expediente. */
    @GetMapping("/{folio}/guardados")
    public List<AntecedenteGuardadoDTO> listarGuardados(@PathVariable String folio) {
        return antecedentesService.listarGuardados(folio);
    }

    @PostMapping("/{folio}/guardados")
    public List<AntecedenteGuardadoDTO> guardar(
            @PathVariable String folio,
            @Valid @RequestBody GuardarAntecedentesDTO dto,
            Authentication authentication
    ) {
        return antecedentesService.guardar(
                folio,
                dto.getAntecedentes(),
                analistaAutenticadoService.obtenerAnalista(authentication)
        );
    }

    @DeleteMapping("/{folio}/guardados/{id}")
    public ResponseEntity<Void> quitarGuardado(
            @PathVariable String folio,
            @PathVariable Long id,
            Authentication authentication
    ) {
        analistaAutenticadoService.obtenerAnalista(authentication);
        antecedentesService.quitarGuardado(folio, id);
        return ResponseEntity.noContent().build();
    }
}
