package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CrearNotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.NotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.entity.NotaAnalisis;
import ipn.escom.defensoria.primercontacto.repository.NotaAnalisisRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotaAnalisisService {

    private final NotaAnalisisRepository notaAnalisisRepository;

    public NotaAnalisisService(NotaAnalisisRepository notaAnalisisRepository) {
        this.notaAnalisisRepository = notaAnalisisRepository;
    }

    public NotaAnalisisDTO crearNota(CrearNotaAnalisisDTO dto) {

        NotaAnalisis nota = NotaAnalisis.builder()
                .quejaId(dto.getQuejaId())
                .folio(dto.getFolio())
                .analistaId(dto.getAnalistaId())
                .analistaNombre(dto.getAnalistaNombre())
                .contenido(dto.getContenido())
                .fechaCreacion(LocalDateTime.now())
                .build();

        NotaAnalisis guardada = notaAnalisisRepository.save(nota);

        return convertirADTO(guardada);
    }

    public List<NotaAnalisisDTO> listarPorQueja(Long quejaId) {
        return notaAnalisisRepository
                .findByQuejaIdOrderByFechaCreacionDesc(quejaId)
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
                .quejaId(nota.getQuejaId())
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
