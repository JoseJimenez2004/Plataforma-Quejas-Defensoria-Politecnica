package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.BandejaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.FiltroExpedienteDTO;
import ipn.escom.defensoria.primercontacto.service.BandejaAnalisisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/primer-contacto/bandeja")
public class BandejaAnalisisController {

    private final BandejaAnalisisService bandejaAnalisisService;

    public BandejaAnalisisController(BandejaAnalisisService bandejaAnalisisService) {
        this.bandejaAnalisisService = bandejaAnalisisService;
    }

    @GetMapping
    public List<BandejaAnalisisDTO> obtenerBandeja(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        return bandejaAnalisisService.obtenerBandeja(token);
    }

    @GetMapping("/folio/{folio}")
    public BandejaAnalisisDTO buscarPorFolio(
            @PathVariable String folio,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        return bandejaAnalisisService.buscarPorFolio(folio, token);
    }

    @PostMapping("/filtrar")
    public List<BandejaAnalisisDTO> filtrar(
            @Valid @RequestBody FiltroExpedienteDTO filtro,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        return bandejaAnalisisService.filtrar(filtro, token);
    }

    @GetMapping("/prioridad/{prioridad}")
    public List<BandejaAnalisisDTO> obtenerPorPrioridad(
            @PathVariable String prioridad,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        return bandejaAnalisisService.obtenerPorPrioridad(prioridad, token);
    }

    @GetMapping("/estatus/{estatus}")
    public List<BandejaAnalisisDTO> obtenerPorEstatus(
            @PathVariable String estatus,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        return bandejaAnalisisService.obtenerPorEstatus(estatus, token);
    }
}