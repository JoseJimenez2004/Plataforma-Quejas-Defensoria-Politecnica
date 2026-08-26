package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CompetenciaDTO;
import ipn.escom.defensoria.primercontacto.dto.DictamenDTO;
import ipn.escom.defensoria.primercontacto.dto.ImprocedenciaDTO;
import ipn.escom.defensoria.primercontacto.service.DictamenPrimerContactoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.service.AnalistaAutenticadoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/primer-contacto/dictamenes")
public class DictamenPrimerContactoController {

    private final DictamenPrimerContactoService dictamenPrimerContactoService;
    private final AnalistaAutenticadoService analistaAutenticadoService;

    public DictamenPrimerContactoController(
            DictamenPrimerContactoService dictamenPrimerContactoService, AnalistaAutenticadoService analistaAutenticadoService
    ) {
        this.dictamenPrimerContactoService =
                dictamenPrimerContactoService;
        this.analistaAutenticadoService = analistaAutenticadoService;
    }

    @PostMapping("/competente")
    public ResponseEntity<DictamenDTO> registrarCompetencia(
            @Valid @RequestBody CompetenciaDTO dto,
            Authentication authentication
    ) {

        PersonalAdministrativo analista =
                analistaAutenticadoService.obtenerAnalista(authentication);

        return ResponseEntity.ok(
                dictamenPrimerContactoService.registrarCompetencia(
                        dto,
                        analista
                )
        );
    }

    @PostMapping("/improcedente")
    public ResponseEntity<DictamenDTO> registrarImprocedencia(
            @Valid @RequestBody ImprocedenciaDTO dto,
            Authentication authentication
    ) {

        PersonalAdministrativo analista =
                analistaAutenticadoService.obtenerAnalista(authentication);

        return ResponseEntity.ok(
                dictamenPrimerContactoService.registrarImprocedencia(
                        dto,
                        analista
                )
        );
    }

    @GetMapping("/expediente/{expedienteId}")
    public DictamenDTO obtenerPorExpediente(
            @PathVariable Long expedienteId
    ) {
        return dictamenPrimerContactoService
                .obtenerPorExpediente(expedienteId);
    }

    @GetMapping("/folio/{folio}")
    public DictamenDTO obtenerPorFolio(
            @PathVariable String folio
    ) {
        return dictamenPrimerContactoService
                .obtenerPorFolio(folio);
    }
}