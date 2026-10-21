package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CrearNotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.NotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.entity.NotaAnalisis;
import ipn.escom.defensoria.primercontacto.repository.NotaAnalisisRepository;
import org.springframework.stereotype.Service;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.exception.RecursoNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotaAnalisisService {

    private final NotaAnalisisRepository notaAnalisisRepository;
    private final ExpedientePrimerContactoRepository expedienteRepository;
    private final OficioAnalisisPdfService oficioPdfService;

    public NotaAnalisisService(
            NotaAnalisisRepository notaAnalisisRepository,
            ExpedientePrimerContactoRepository expedienteRepository,
            OficioAnalisisPdfService oficioPdfService
    ) {
        this.notaAnalisisRepository = notaAnalisisRepository;
        this.expedienteRepository = expedienteRepository;
        this.oficioPdfService = oficioPdfService;
    }

    /** Contenido y nombre de archivo del oficio en PDF. */
    public record OficioPdf(String nombreArchivo, byte[] contenido) {}

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
                                new RecursoNoEncontradoException(
                                        "No existe un expediente de Primer Contacto con folio "
                                                + dto.getFolio()
                                )
                        );

        NotaAnalisis nota = NotaAnalisis.builder()
                .expedienteId(expediente.getId())
                .folio(expediente.getFolio())
                .analistaId(analista.getId())
                .analistaNombre(analista.getNombreCompleto())
                .asunto(dto.getAsunto().trim())
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
                .map(NotaAnalisisService::convertirADTO)
                .toList();
    }

    public List<NotaAnalisisDTO> listarPorFolio(String folio) {
        return notaAnalisisRepository
                .findByFolioOrderByFechaCreacionDesc(folio)
                .stream()
                .map(NotaAnalisisService::convertirADTO)
                .toList();
    }

    public NotaAnalisisDTO actualizarNota(
            Long id,
            CrearNotaAnalisisDTO dto,
            PersonalAdministrativo analista
    ) {

        NotaAnalisis nota = obtenerPropia(id, analista, "editar");

        nota.setAsunto(dto.getAsunto().trim());
        nota.setContenido(dto.getContenido());
        nota.setFechaActualizacion(LocalDateTime.now());

        NotaAnalisis actualizada = notaAnalisisRepository.save(nota);

        return convertirADTO(actualizada);
    }

    public void eliminarNota(Long id, PersonalAdministrativo analista) {

        NotaAnalisis nota = obtenerPropia(id, analista, "eliminar");

        notaAnalisisRepository.delete(nota);
    }

    /*
     * El oficio en PDF se arma al vuelo con los datos guardados: si el autor lo edita, el
     * PDF siguiente ya sale con el cambio (y con la fecha de edición).
     */
    public OficioPdf generarPdf(Long id) {

        NotaAnalisis nota = notaAnalisisRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Oficio no encontrado"));

        ExpedientePrimerContacto expediente = expedienteRepository.findById(nota.getExpedienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente del oficio no encontrado"));

        String numero = numeroOficio(nota);

        return new OficioPdf(
                "oficio-" + numero.replace('/', '-') + ".pdf",
                oficioPdfService.generar(nota, numero, expediente)
        );
    }

    /*
     * CU-PC-05: solo el autor de una nota puede editarla o borrarla.
     * El autor es el analista del JWT que la creó (analistaId).
     */
    private NotaAnalisis obtenerPropia(
            Long id,
            PersonalAdministrativo analista,
            String accion
    ) {

        NotaAnalisis nota = notaAnalisisRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Nota de análisis no encontrada"));

        if (!nota.getAnalistaId().equals(analista.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo " + nota.getAnalistaNombre() + ", autor(a) de la nota, puede " + accion + "la."
            );
        }

        return nota;
    }

    /** Número del oficio: DDP/PC/OF/año de creación/id (mismo formato que la remisión). */
    public static String numeroOficio(NotaAnalisis nota) {
        int anio = nota.getFechaCreacion() != null
                ? nota.getFechaCreacion().getYear()
                : LocalDateTime.now().getYear();
        return "DDP/PC/OF/" + anio + "/" + nota.getId();
    }

    /** También lo usa ExpedienteAnalisisService al armar el detalle del expediente. */
    public static NotaAnalisisDTO convertirADTO(NotaAnalisis nota) {

        return NotaAnalisisDTO.builder()
                .id(nota.getId())
                .expedienteId(nota.getExpedienteId())
                .folio(nota.getFolio())
                .analistaId(nota.getAnalistaId())
                .analistaNombre(nota.getAnalistaNombre())
                .numeroOficio(numeroOficio(nota))
                .asunto(nota.getAsunto())
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
