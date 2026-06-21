package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CitaDTO;
import ipn.escom.defensoria.primercontacto.dto.CrearCitaDTO;
import ipn.escom.defensoria.primercontacto.service.CitaPrimerContactoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/primer-contacto/citas")
public class CitaPrimerContactoController {

    private final CitaPrimerContactoService citaPrimerContactoService;

    public CitaPrimerContactoController(CitaPrimerContactoService citaPrimerContactoService) {
        this.citaPrimerContactoService = citaPrimerContactoService;
    }

    @PostMapping
    public CitaDTO crearCita(
            @Valid @RequestBody CrearCitaDTO dto
    ) {
        return citaPrimerContactoService.crearCita(dto);
    }

    @GetMapping("/queja/{quejaId}")
    public List<CitaDTO> listarPorQueja(
            @PathVariable Long quejaId
    ) {
        return citaPrimerContactoService.listarPorQueja(quejaId);
    }

    @GetMapping("/folio/{folio}")
    public List<CitaDTO> listarPorFolio(
            @PathVariable String folio
    ) {
        return citaPrimerContactoService.listarPorFolio(folio);
    }

    @GetMapping("/agenda")
    public List<CitaDTO> obtenerAgendaDia(
            @RequestParam String fecha
    ) {
        return citaPrimerContactoService.obtenerAgendaDia(fecha);
    }

    @GetMapping("/analista/{analistaId}")
    public List<CitaDTO> obtenerAgendaAnalista(
            @PathVariable Long analistaId
    ) {
        return citaPrimerContactoService.obtenerAgendaAnalista(analistaId);
    }

    @PutMapping("/{id}/confirmar")
    public CitaDTO confirmarCita(
            @PathVariable Long id
    ) {
        return citaPrimerContactoService.confirmarCita(id);
    }

    @PutMapping("/{id}/cancelar")
    public CitaDTO cancelarCita(
            @PathVariable Long id
    ) {
        return citaPrimerContactoService.cancelarCita(id);
    }
}