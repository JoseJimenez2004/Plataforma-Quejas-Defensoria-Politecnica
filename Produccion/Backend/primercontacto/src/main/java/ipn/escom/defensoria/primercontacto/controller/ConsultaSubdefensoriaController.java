package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CitaDTO;
import ipn.escom.defensoria.primercontacto.dto.DictamenDTO;
import ipn.escom.defensoria.primercontacto.dto.ExpedienteAnalisisDTO;
import ipn.escom.defensoria.primercontacto.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.primercontacto.service.CitaPrimerContactoService;
import ipn.escom.defensoria.primercontacto.service.DictamenPrimerContactoService;
import ipn.escom.defensoria.primercontacto.service.ExpedienteAnalisisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lo que trabajó Primer Contacto, de solo lectura, para la pestaña "Primer Contacto" del
 * expediente de Subdefensoría (2026-10-21). Lo pueden consultar SUBDEFENSOR y DEFENSOR
 * (ver SecurityConfig), además del analista.
 *
 * No cambia el estado del expediente: usa los mismos GET que la pantalla de Primer Contacto,
 * nunca iniciar-analisis. Los PDF de los oficios y las evidencias se abren con
 * GET /notas/{id}/pdf y GET /evidencias/{id}, que también quedan abiertos a esos roles.
 */
@RestController
@RequestMapping("/api/primer-contacto/consulta")
public class ConsultaSubdefensoriaController {

    private final ExpedienteAnalisisService expedienteService;
    private final DictamenPrimerContactoService dictamenService;
    private final CitaPrimerContactoService citaService;

    public ConsultaSubdefensoriaController(
            ExpedienteAnalisisService expedienteService,
            DictamenPrimerContactoService dictamenService,
            CitaPrimerContactoService citaService
    ) {
        this.expedienteService = expedienteService;
        this.dictamenService = dictamenService;
        this.citaService = citaService;
    }

    /** Expediente (datos, denunciado, evidencias, oficios), dictamen y citas. */
    public record ExpedientePrimerContactoConsulta(
            ExpedienteAnalisisDTO expediente,
            DictamenDTO dictamen,
            List<CitaDTO> citas
    ) {}

    /** {folio} es el folio PC-... (folioOrigen del expediente de Subdefensoría). */
    @GetMapping("/expedientes/{folio}")
    public ExpedientePrimerContactoConsulta consultar(@PathVariable String folio) {

        ExpedienteAnalisisDTO expediente = expedienteService.obtenerPorFolio(folio);

        DictamenDTO dictamen;
        try {
            dictamen = dictamenService.obtenerPorFolio(folio);
        } catch (RecursoNoEncontradoException sinDictamen) {
            dictamen = null;
        }

        return new ExpedientePrimerContactoConsulta(
                expediente,
                dictamen,
                citaService.listarPorFolio(folio)
        );
    }
}
