package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CitaDTO;
import ipn.escom.defensoria.primercontacto.dto.CrearCitaDTO;
import ipn.escom.defensoria.primercontacto.entity.CitaPrimerContacto;
import ipn.escom.defensoria.primercontacto.repository.CitaPrimerContactoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class CitaPrimerContactoService {

    private final CitaPrimerContactoRepository citaPrimerContactoRepository;

    public CitaPrimerContactoService(CitaPrimerContactoRepository citaPrimerContactoRepository) {
        this.citaPrimerContactoRepository = citaPrimerContactoRepository;
    }

    public CitaDTO crearCita(CrearCitaDTO dto) {

        CitaPrimerContacto cita = CitaPrimerContacto.builder()
                .quejaId(dto.getQuejaId())
                .folio(dto.getFolio())
                .quejosoId(dto.getQuejosoId())
                .quejosoNombre(dto.getQuejosoNombre())
                .analistaId(dto.getAnalistaId())
                .analistaNombre(dto.getAnalistaNombre())
                .fechaCita(LocalDate.parse(dto.getFechaCita()))
                .horaCita(LocalTime.parse(dto.getHoraCita()))
                .tipoCita(dto.getTipoCita())
                .motivo(dto.getMotivo())
                .estatus("PROGRAMADA")
                .fechaCreacion(LocalDateTime.now())
                .build();

        CitaPrimerContacto guardada = citaPrimerContactoRepository.save(cita);

        return convertirADTO(guardada);
    }

    public List<CitaDTO> listarPorQueja(Long quejaId) {
        return citaPrimerContactoRepository
                .findByQuejaIdOrderByFechaCitaDescHoraCitaDesc(quejaId)
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
                .quejaId(cita.getQuejaId())
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