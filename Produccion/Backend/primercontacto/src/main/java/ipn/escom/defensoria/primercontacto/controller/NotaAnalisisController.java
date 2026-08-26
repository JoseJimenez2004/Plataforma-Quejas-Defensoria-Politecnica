package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CrearNotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.NotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.service.AnalistaAutenticadoService;
import ipn.escom.defensoria.primercontacto.service.NotaAnalisisService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/primer-contacto/notas")
public class NotaAnalisisController {

    private final NotaAnalisisService notaAnalisisService;
    private final AnalistaAutenticadoService analistaAutenticadoService;

    public NotaAnalisisController(
            NotaAnalisisService notaAnalisisService,
            AnalistaAutenticadoService analistaAutenticadoService
    ) {
        this.notaAnalisisService = notaAnalisisService;
        this.analistaAutenticadoService = analistaAutenticadoService;
    }

    @PostMapping
    public ResponseEntity<NotaAnalisisDTO> crearNota(
            @Valid @RequestBody CrearNotaAnalisisDTO dto,
            Authentication authentication
    ) {

        PersonalAdministrativo analista =
                analistaAutenticadoService.obtenerAnalista(authentication);

        return ResponseEntity.ok(
                notaAnalisisService.crearNota(
                        dto,
                        analista
                )
        );
    }

    @GetMapping("/expediente/{expedienteId}")
    public List<NotaAnalisisDTO> listarPorExpediente(
            @PathVariable Long expedienteId
    ) {
        return notaAnalisisService
                .listarPorExpediente(expedienteId);
    }

    @GetMapping("/folio/{folio}")
    public List<NotaAnalisisDTO> listarPorFolio(
            @PathVariable String folio
    ) {
        return notaAnalisisService.listarPorFolio(folio);
    }

    @PutMapping("/{id}")
    public NotaAnalisisDTO actualizarNota(
            @PathVariable Long id,
            @Valid @RequestBody CrearNotaAnalisisDTO dto
    ) {
        return notaAnalisisService.actualizarNota(id, dto);
    }

    @DeleteMapping("/{id}")
    public void eliminarNota(
            @PathVariable Long id
    ) {
        notaAnalisisService.eliminarNota(id);
    }
}