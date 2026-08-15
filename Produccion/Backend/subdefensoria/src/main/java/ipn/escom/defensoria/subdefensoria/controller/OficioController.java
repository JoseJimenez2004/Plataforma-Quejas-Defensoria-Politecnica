package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.dto.CrearOficioDTO;
import ipn.escom.defensoria.subdefensoria.dto.OficioDTO;
import ipn.escom.defensoria.subdefensoria.service.OficioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subdefensoria/oficios")
public class OficioController {

    private final OficioService oficioService;

    public OficioController(OficioService oficioService) {
        this.oficioService = oficioService;
    }

    /** Redacta y envia el oficio vigente (TS-01 o TS-04, segun el estatus del expediente). */
    @PostMapping
    public OficioDTO crearOficio(@Valid @RequestBody CrearOficioDTO dto) {
        return oficioService.crearOficio(dto);
    }

    @GetMapping("/folio/{folio}")
    public List<OficioDTO> historialPorFolio(@PathVariable String folio) {
        return oficioService.historialPorFolio(folio);
    }

    @GetMapping("/{oficioId}")
    public OficioDTO obtenerPorId(@PathVariable Long oficioId) {
        return oficioService.obtenerPorId(oficioId);
    }
}
