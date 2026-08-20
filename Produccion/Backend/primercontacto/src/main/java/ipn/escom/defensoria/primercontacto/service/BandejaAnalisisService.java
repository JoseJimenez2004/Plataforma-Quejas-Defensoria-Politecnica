package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.BandejaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.FiltroExpedienteDTO;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.repository.CitaPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BandejaAnalisisService {

    private final ExpedientePrimerContactoRepository expedienteRepository;
    private final CitaPrimerContactoRepository citaRepository;

    public BandejaAnalisisService(
            ExpedientePrimerContactoRepository expedienteRepository,
            CitaPrimerContactoRepository citaRepository
    ) {
        this.expedienteRepository = expedienteRepository;
        this.citaRepository = citaRepository;
    }

    public List<BandejaAnalisisDTO> obtenerBandeja(
            String token
    ) {

        return expedienteRepository.findAll()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    public BandejaAnalisisDTO buscarPorFolio(
            String folio,
            String token
    ) {

        return expedienteRepository
                .findByFolio(folio)
                .map(this::convertirADTO)
                .orElse(null);
    }

    public List<BandejaAnalisisDTO> filtrar(
            FiltroExpedienteDTO filtro,
            String token
    ) {

        return expedienteRepository.findAll()
                .stream()

                .filter(e ->
                        filtro.getFolio() == null
                                || filtro.getFolio().isBlank()
                                || contieneIgnoreCase(
                                e.getFolio(),
                                filtro.getFolio()
                        )
                                || contieneIgnoreCase(
                                e.getFolioOrigen(),
                                filtro.getFolio()
                        )
                )

                .filter(e ->
                        filtro.getPrioridad() == null
                                || filtro.getPrioridad().isBlank()
                                || igualesIgnoreCase(
                                e.getPrioridad(),
                                filtro.getPrioridad()
                        )
                )

                .filter(e ->
                        filtro.getEstatus() == null
                                || filtro.getEstatus().isBlank()
                                || igualesIgnoreCase(
                                e.getEstatus(),
                                filtro.getEstatus()
                        )
                )

                .filter(e ->
                        filtro.getUnidadAcademica() == null
                                || filtro.getUnidadAcademica().isBlank()
                                || igualesIgnoreCase(
                                e.getUnidadAcademica(),
                                filtro.getUnidadAcademica()
                        )
                )

                .map(this::convertirADTO)
                .toList();
    }

    public List<BandejaAnalisisDTO> obtenerPorPrioridad(
            String prioridad,
            String token
    ) {

        return expedienteRepository.findAll()
                .stream()
                .filter(e ->
                        igualesIgnoreCase(
                                e.getPrioridad(),
                                prioridad
                        )
                )
                .map(this::convertirADTO)
                .toList();
    }

    public List<BandejaAnalisisDTO> obtenerPorEstatus(
            String estatus,
            String token
    ) {

        return expedienteRepository.findAll()
                .stream()
                .filter(e ->
                        igualesIgnoreCase(
                                e.getEstatus(),
                                estatus
                        )
                )
                .map(this::convertirADTO)
                .toList();
    }

    private BandejaAnalisisDTO convertirADTO(
            ExpedientePrimerContacto expediente
    ) {

        /*
         * Conservamos temporalmente el comportamiento anterior:
         * si tiene una cita activa y todavía está en análisis,
         * la bandeja muestra CON_CITA.
         *
         * No sobrescribimos estados finales como IMPROCEDENTE,
         * TURNADO_SUBDEFENSORIA o REMISION_ENVIADA.
         */
        boolean tieneCita =
                citaRepository.existsByFolioAndEstatusNot(
                        expediente.getFolio(),
                        "CANCELADA"
                );

        String estatusVisual =
                tieneCita
                        && "PENDIENTE_ANALISIS"
                        .equalsIgnoreCase(expediente.getEstatus())
                        ? "CON_CITA"
                        : expediente.getEstatus();

        return BandejaAnalisisDTO.builder()
                .expedienteId(expediente.getId())
                .folio(expediente.getFolio())
                .folioOrigen(expediente.getFolioOrigen())
                .nombreQuejoso(
                        expediente.getQuejosoNombre()
                )
                .unidadAcademica(
                        expediente.getUnidadAcademica()
                )
                .tema(expediente.getTema())
                .prioridad(expediente.getPrioridad())
                .estatus(estatusVisual)
                .fechaRecepcion(
                        expediente.getFechaRecepcionOrigen()
                )
                .build();
    }

    private boolean igualesIgnoreCase(
            String valor,
            String filtro
    ) {
        return valor != null
                && filtro != null
                && valor.equalsIgnoreCase(filtro);
    }

    private boolean contieneIgnoreCase(
            String valor,
            String filtro
    ) {
        return valor != null
                && filtro != null
                && valor.toLowerCase()
                .contains(filtro.toLowerCase());
    }
}