package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.service.AnalistaAutenticadoService;
import org.springframework.security.core.Authentication;

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
    private final AnalistaAutenticadoService analistaAutenticadoService;

    public BandejaAnalisisController(BandejaAnalisisService bandejaAnalisisService,
                                     AnalistaAutenticadoService analistaAutenticadoService) {
        this.bandejaAnalisisService = bandejaAnalisisService;
        this.analistaAutenticadoService = analistaAutenticadoService;
    }

    @GetMapping
    public List<BandejaAnalisisDTO> obtenerBandeja(
            Authentication authentication
    ) {
        Long analistaId = analistaAutenticadoService.obtenerAnalista(authentication).getId();
        return bandejaAnalisisService.obtenerBandeja(analistaId);
    }

    @GetMapping("/folio/{folio}")
    public BandejaAnalisisDTO buscarPorFolio(
            @PathVariable String folio,
            Authentication authentication
    ) {
        Long analistaId = analistaAutenticadoService.obtenerAnalista(authentication).getId();
        return bandejaAnalisisService.buscarPorFolio(folio, analistaId);
    }

    @PostMapping("/filtrar")
    public List<BandejaAnalisisDTO> filtrar(
            @Valid @RequestBody FiltroExpedienteDTO filtro,
            Authentication authentication
    ) {
        Long analistaId = analistaAutenticadoService.obtenerAnalista(authentication).getId();
        return bandejaAnalisisService.filtrar(filtro, analistaId);
    }

    @GetMapping("/prioridad/{prioridad}")
    public List<BandejaAnalisisDTO> obtenerPorPrioridad(
            @PathVariable String prioridad,
            Authentication authentication
    ) {
        Long analistaId = analistaAutenticadoService.obtenerAnalista(authentication).getId();
        return bandejaAnalisisService.obtenerPorPrioridad(prioridad, analistaId);
    }

    @GetMapping("/estatus/{estatus}")
    public List<BandejaAnalisisDTO> obtenerPorEstatus(
            @PathVariable String estatus,
            Authentication authentication
    ) {
        Long analistaId = analistaAutenticadoService.obtenerAnalista(authentication).getId();
        return bandejaAnalisisService.obtenerPorEstatus(estatus, analistaId);
    }
}