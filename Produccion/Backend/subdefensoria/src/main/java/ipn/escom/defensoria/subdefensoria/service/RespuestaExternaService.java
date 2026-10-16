package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.RegistrarRespuestaExternaDTO;
import ipn.escom.defensoria.subdefensoria.dto.RespuestaExternaDTO;
import ipn.escom.defensoria.subdefensoria.entity.*;
import ipn.escom.defensoria.subdefensoria.exception.OperacionInvalidaException;
import ipn.escom.defensoria.subdefensoria.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.OficioInformacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.RespuestaArchivoRepository;
import ipn.escom.defensoria.subdefensoria.repository.RespuestaExternaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CU-SD-08: registro manual de la respuesta del director a un oficio (no se asume ingesta
 * automática de correo). La respuesta lleva al expediente a ELABORO_ACUERDO.
 *
 * Puede llevar el archivo de la respuesta (oficio escaneado o PDF; ver RespuestaArchivo). Se
 * valida ANTES de registrar la respuesta, para que un archivo inválido no deje la respuesta a
 * medias; también se puede adjuntar o reemplazar después.
 */
@Service
public class RespuestaExternaService {

    private final RespuestaExternaRepository respuestaRepository;
    private final RespuestaArchivoRepository archivoRepository;
    private final OficioInformacionRepository oficioRepository;
    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final TransicionExpedienteService transicionService;

    public RespuestaExternaService(
            RespuestaExternaRepository respuestaRepository,
            RespuestaArchivoRepository archivoRepository,
            OficioInformacionRepository oficioRepository,
            ExpedienteInvestigacionRepository expedienteRepository,
            TransicionExpedienteService transicionService
    ) {
        this.respuestaRepository = respuestaRepository;
        this.archivoRepository = archivoRepository;
        this.oficioRepository = oficioRepository;
        this.expedienteRepository = expedienteRepository;
        this.transicionService = transicionService;
    }

    public RespuestaExternaDTO registrarRespuesta(RegistrarRespuestaExternaDTO dto) {
        return registrarRespuesta(dto, null);
    }

    public RespuestaExternaDTO registrarRespuesta(RegistrarRespuestaExternaDTO dto, MultipartFile archivo) {

        // Primero el archivo: si no sirve, no se registra nada.
        ArchivoRespuestaValidador.Validado validado =
                archivo != null && !archivo.isEmpty() ? ArchivoRespuestaValidador.validar(archivo) : null;

        OficioInformacion oficio = oficioRepository.findById(dto.getOficioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el oficio " + dto.getOficioId()));

        if (EstatusOficio.RESPONDIDO.equals(oficio.getEstatus())) {
            throw new OperacionInvalidaException(
                    "El oficio " + oficio.getNumeroOficio() + " ya tiene una respuesta registrada.");
        }

        ExpedienteInvestigacion expediente = expedienteRepository.findById(oficio.getExpedienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el expediente " + oficio.getExpedienteId()));

        RespuestaExterna respuesta = RespuestaExterna.builder()
                .expedienteId(expediente.getId())
                .oficioId(oficio.getId())
                .canalRecepcion(dto.getCanalRecepcion())
                .numeroOficioRespuestaUA(dto.getNumeroOficioRespuestaUA())
                .archivoPdfPath(dto.getArchivoPdfPath())
                .resumen(dto.getResumen())
                .fechaRecepcion(LocalDateTime.now())
                .build();
        RespuestaExterna guardada = respuestaRepository.save(respuesta);

        if (validado != null) {
            guardarArchivo(guardada.getId(), validado);
        }

        oficio.setEstatus(EstatusOficio.RESPONDIDO);
        oficioRepository.save(oficio);

        // El director responde el oficio: se redacta el acuerdo de conciliación.
        transicionService.cambiar(expediente, EstatusExpediente.ELABORO_ACUERDO);

        return convertir(guardada, expediente, validado != null ? validado.nombre() : null);
    }

    /** Adjunta (o reemplaza) el archivo de una respuesta ya registrada. */
    public RespuestaExternaDTO adjuntarArchivo(Long respuestaId, MultipartFile archivo) {
        ArchivoRespuestaValidador.Validado validado = ArchivoRespuestaValidador.validar(archivo);
        RespuestaExterna respuesta = respuestaRepository.findById(respuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la respuesta " + respuestaId));
        guardarArchivo(respuestaId, validado);
        return convertir(respuesta, expediente(respuesta.getExpedienteId()), validado.nombre());
    }

    public List<RespuestaExternaDTO> porOficio(Long oficioId) {
        List<RespuestaExterna> respuestas = respuestaRepository.findByOficioIdOrderByFechaRecepcionDesc(oficioId);
        if (respuestas.isEmpty()) {
            return List.of();
        }
        Map<Long, String> nombres = new HashMap<>();
        archivoRepository.nombresPorRespuesta(respuestas.stream().map(RespuestaExterna::getId).toList())
                .forEach(fila -> nombres.put((Long) fila[0], (String) fila[1]));
        ExpedienteInvestigacion expediente = expediente(respuestas.get(0).getExpedienteId());
        return respuestas.stream().map(r -> convertir(r, expediente, nombres.get(r.getId()))).toList();
    }

    public RespuestaArchivo archivo(Long respuestaId) {
        return archivoRepository.findByRespuestaId(respuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La respuesta no tiene archivo adjunto."));
    }

    private void guardarArchivo(Long respuestaId, ArchivoRespuestaValidador.Validado validado) {
        RespuestaArchivo existente = archivoRepository.findByRespuestaId(respuestaId).orElse(null);
        RespuestaArchivo archivo = existente != null ? existente : new RespuestaArchivo();
        archivo.setRespuestaId(respuestaId);
        archivo.setNombreArchivo(validado.nombre());
        archivo.setTipoContenido(validado.tipo());
        archivo.setTamanoBytes((long) validado.contenido().length);
        archivo.setContenido(validado.contenido());
        archivo.setFechaCarga(LocalDateTime.now());
        archivoRepository.save(archivo);
    }

    private ExpedienteInvestigacion expediente(Long expedienteId) {
        return expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el expediente " + expedienteId));
    }

    private RespuestaExternaDTO convertir(RespuestaExterna r, ExpedienteInvestigacion expediente, String nombreArchivo) {
        return RespuestaExternaDTO.builder()
                .id(r.getId())
                .expedienteId(expediente.getId())
                .oficioId(r.getOficioId())
                .folio(expediente.getFolio())
                .canalRecepcion(r.getCanalRecepcion())
                .numeroOficioRespuestaUA(r.getNumeroOficioRespuestaUA())
                .archivoPdfPath(r.getArchivoPdfPath())
                .resumen(r.getResumen())
                .fechaRecepcion(r.getFechaRecepcion() != null ? r.getFechaRecepcion().toString() : null)
                .estatusExpediente(expediente.getEstatus())
                .tieneArchivo(nombreArchivo != null)
                .archivoNombre(nombreArchivo)
                .build();
    }
}
