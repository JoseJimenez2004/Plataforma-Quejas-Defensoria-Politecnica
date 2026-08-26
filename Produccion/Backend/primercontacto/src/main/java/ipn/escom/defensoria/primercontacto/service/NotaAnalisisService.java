package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CrearNotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.NotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.entity.NotaAnalisis;
import ipn.escom.defensoria.primercontacto.repository.NotaAnalisisRepository;
import org.springframework.stereotype.Service;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotaAnalisisService {

    private final NotaAnalisisRepository notaAnalisisRepository;
    private final ExpedientePrimerContactoRepository expedienteRepository;

    public NotaAnalisisService(
            NotaAnalisisRepository notaAnalisisRepository,
            ExpedientePrimerContactoRepository expedienteRepository
    ) {
        this.notaAnalisisRepository = notaAnalisisRepository;
        this.expedienteRepository = expedienteRepository;
    }

    public NotaAnalisisDTO crearNota(
            CrearNotaAnalisisDTO dto,
            PersonalAdministrativo analista
    ) {
        /*
         * El frontend manda el folio de Primer Contacto.
         * A partir del folio obtenemos el expediente interno.
         */
        ExpedientePrimerContacto expediente =
                expedienteRepository.findByFolio(dto.getFolio())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No existe un expediente de Primer Contacto con folio "
                                                + dto.getFolio()
                                )
                        );

        NotaAnalisis nota = NotaAnalisis.builder()
                .expedienteId(expediente.getId())
                .folio(expediente.getFolio())
                .analistaId(analista.getId())
                .analistaNombre(analista.getNombreCompleto())
                .contenido(dto.getContenido())
                .fechaCreacion(LocalDateTime.now())
                .build();

        NotaAnalisis guardada =
                notaAnalisisRepository.save(nota);

        return convertirADTO(guardada);
    }

    public List<NotaAnalisisDTO> listarPorExpediente(
            Long expedienteId
    ) {
        return notaAnalisisRepository
                .findByExpedienteIdOrderByFechaCreacionDesc(
                        expedienteId
                )
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    public List<NotaAnalisisDTO> listarPorFolio(String folio) {
        return notaAnalisisRepository
                .findByFolioOrderByFechaCreacionDesc(folio)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    public NotaAnalisisDTO actualizarNota(Long id, CrearNotaAnalisisDTO dto) {

        NotaAnalisis nota = notaAnalisisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nota de análisis no encontrada"));

        nota.setContenido(dto.getContenido());
        nota.setFechaActualizacion(LocalDateTime.now());

        NotaAnalisis actualizada = notaAnalisisRepository.save(nota);

        return convertirADTO(actualizada);
    }

    public void eliminarNota(Long id) {

        if (!notaAnalisisRepository.existsById(id)) {
            throw new RuntimeException("Nota de análisis no encontrada");
        }

        notaAnalisisRepository.deleteById(id);
    }

    private NotaAnalisisDTO convertirADTO(NotaAnalisis nota) {

        return NotaAnalisisDTO.builder()
                .id(nota.getId())
                .expedienteId(nota.getExpedienteId())
                .folio(nota.getFolio())
                .analistaId(nota.getAnalistaId())
                .analistaNombre(nota.getAnalistaNombre())
                .contenido(nota.getContenido())
                .fechaCreacion(
                        nota.getFechaCreacion() != null
                                ? nota.getFechaCreacion().toString()
                                : null
                )
                .fechaActualizacion(
                        nota.getFechaActualizacion() != null
                                ? nota.getFechaActualizacion().toString()
                                : null
                )
                .build();
    }
}
