package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CitaDTO;
import ipn.escom.defensoria.primercontacto.dto.CrearCitaDTO;
import ipn.escom.defensoria.primercontacto.entity.CitaPrimerContacto;
import ipn.escom.defensoria.primercontacto.repository.CitaPrimerContactoRepository;
import org.springframework.stereotype.Service;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class CitaPrimerContactoService {

    private final CitaPrimerContactoRepository citaPrimerContactoRepository;
    private final ExpedientePrimerContactoRepository expedienteRepository;

    public CitaPrimerContactoService(
            CitaPrimerContactoRepository citaPrimerContactoRepository,
            ExpedientePrimerContactoRepository expedienteRepository
    ) {
        this.citaPrimerContactoRepository = citaPrimerContactoRepository;
        this.expedienteRepository = expedienteRepository;
    }

    public CitaDTO crearCita(
            CrearCitaDTO dto,
            PersonalAdministrativo analista
    ) {

        /*
         * El cliente manda el folio propio de Primer Contacto.
         * Con ese folio obtenemos el expediente interno.
         */
        ExpedientePrimerContacto expediente =
                expedienteRepository.findByFolio(dto.getFolio())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No existe un expediente de Primer Contacto con folio "
                                                + dto.getFolio()
                                )
                        );

        boolean yaTieneCita = citaPrimerContactoRepository
                .existsByExpedienteIdAndEstatusNot(
                        expediente.getId(),
                        "CANCELADA"
                );

        if (yaTieneCita) {
            throw new RuntimeException(
                    "El expediente ya tiene una cita programada"
            );
        }

        CitaPrimerContacto cita =
                CitaPrimerContacto.builder()
                        .expedienteId(expediente.getId())
                        .folio(expediente.getFolio())
                        .quejosoId(dto.getQuejosoId())
                        .quejosoNombre(dto.getQuejosoNombre())
                        .analistaId(analista.getId())
                        .analistaNombre(analista.getNombreCompleto())
                        .fechaCita(LocalDate.parse(dto.getFechaCita()))
                        .horaCita(LocalTime.parse(dto.getHoraCita()))
                        .tipoCita(dto.getTipoCita())
                        .motivo(dto.getMotivo())
                        .estatus("PROGRAMADA")
                        .fechaCreacion(LocalDateTime.now())
                        .build();

        CitaPrimerContacto guardada =
                citaPrimerContactoRepository.save(cita);

        return convertirADTO(guardada);
    }

    public List<CitaDTO> listarPorExpediente(Long expedienteId) {
        return citaPrimerContactoRepository
                .findByExpedienteIdOrderByFechaCitaDescHoraCitaDesc(
                        expedienteId
                )
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    public List<CitaDTO> listarPorFolio(String folio) {
        return citaPrimerContactoRepository
                .findByFolioOrderByFechaCitaDescHoraCitaDesc(folio)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    public List<CitaDTO> obtenerAgendaDia(String fecha) {
        return citaPrimerContactoRepository
                .findByFechaCitaOrderByHoraCitaAsc(LocalDate.parse(fecha))
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    public List<CitaDTO> obtenerAgendaAnalista(Long analistaId) {
        return citaPrimerContactoRepository
                .findByAnalistaIdOrderByFechaCitaAscHoraCitaAsc(analistaId)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    public CitaDTO confirmarCita(Long id) {
        CitaPrimerContacto cita = citaPrimerContactoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        cita.setEstatus("CONFIRMADA");

        return convertirADTO(citaPrimerContactoRepository.save(cita));
    }

    public CitaDTO cancelarCita(Long id) {
        CitaPrimerContacto cita = citaPrimerContactoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        cita.setEstatus("CANCELADA");

        return convertirADTO(citaPrimerContactoRepository.save(cita));
    }

    private CitaDTO convertirADTO(CitaPrimerContacto cita) {

        return CitaDTO.builder()
                .id(cita.getId())
                .expedienteId(cita.getExpedienteId())
                .folio(cita.getFolio())
                .quejosoId(cita.getQuejosoId())
                .quejosoNombre(cita.getQuejosoNombre())
                .analistaId(cita.getAnalistaId())
                .analistaNombre(cita.getAnalistaNombre())
                .fechaCita(cita.getFechaCita() != null ? cita.getFechaCita().toString() : null)
                .horaCita(cita.getHoraCita() != null ? cita.getHoraCita().toString() : null)
                .tipoCita(cita.getTipoCita())
                .motivo(cita.getMotivo())
                .estatus(cita.getEstatus())
                .fechaCreacion(cita.getFechaCreacion() != null ? cita.getFechaCreacion().toString() : null)
                .build();
    }
}