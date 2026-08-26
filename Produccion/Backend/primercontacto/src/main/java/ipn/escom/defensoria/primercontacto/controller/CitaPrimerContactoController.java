package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CitaDTO;
import ipn.escom.defensoria.primercontacto.dto.CrearCitaDTO;
import ipn.escom.defensoria.primercontacto.service.CitaPrimerContactoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.service.AnalistaAutenticadoService;

import java.util.List;

@RestController
@RequestMapping("/api/primer-contacto/citas")
public class CitaPrimerContactoController {

    private final CitaPrimerContactoService citaPrimerContactoService;
    private final AnalistaAutenticadoService analistaAutenticadoService;

    public CitaPrimerContactoController(
            CitaPrimerContactoService citaPrimerContactoService,
            AnalistaAutenticadoService analistaAutenticadoService
    ) {
        this.citaPrimerContactoService = citaPrimerContactoService;
        this.analistaAutenticadoService = analistaAutenticadoService;
    }


    @PostMapping
    public ResponseEntity<CitaDTO> crearCita(
            @Valid @RequestBody CrearCitaDTO dto,
            Authentication authentication
    ) {

        PersonalAdministrativo analista =
                analistaAutenticadoService.obtenerAnalista(authentication);

        return ResponseEntity.ok(
                citaPrimerContactoService.crearCita(
                        dto,
                        analista
                )
        );
    }

    @GetMapping("/expediente/{expedienteId}")
    public List<CitaDTO> listarPorExpediente(
            @PathVariable Long expedienteId
    ) {
        return citaPrimerContactoService
                .listarPorExpediente(expedienteId);
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