package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CrearNotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.NotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.service.NotaAnalisisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/primer-contacto/notas")
public class NotaAnalisisController {

    private final NotaAnalisisService notaAnalisisService;

    public NotaAnalisisController(NotaAnalisisService notaAnalisisService) {
        this.notaAnalisisService = notaAnalisisService;
    }

    @PostMapping
    public NotaAnalisisDTO crearNota(
            @Valid @RequestBody CrearNotaAnalisisDTO dto
    ) {
        return notaAnalisisService.crearNota(dto);
    }

    @GetMapping("/queja/{quejaId}")
    public List<NotaAnalisisDTO> listarPorQueja(
            @PathVariable Long quejaId
    ) {
        return notaAnalisisService.listarPorQueja(quejaId);
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