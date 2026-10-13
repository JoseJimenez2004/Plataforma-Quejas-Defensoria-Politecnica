package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.RegistrarRespuestaExternaDTO;
import ipn.escom.defensoria.subdefensoria.dto.RespuestaExternaDTO;
import ipn.escom.defensoria.subdefensoria.entity.*;
import ipn.escom.defensoria.subdefensoria.exception.OperacionInvalidaException;
import ipn.escom.defensoria.subdefensoria.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.OficioInformacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.RespuestaExternaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Registro manual de la respuesta recibida (TS-02 y TS-05 del BPMN;
 * no se asume ingesta automatica de correo). Segun la fase del
 * oficio respondido, el expediente avanza distinto:
 * - SOLICITUD_INFORMACION respondido -> EN_ESPERA_OFICIO (toca redactar TS-04).
 * - GESTION_DIRECTOR respondido -> ELABORO_ACUERDO (toca decidir TS-05).
 */
@Service
public class RespuestaExternaService {

    private final RespuestaExternaRepository respuestaRepository;
    private final OficioInformacionRepository oficioRepository;
    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final TransicionExpedienteService transicionService;

    public RespuestaExternaService(
            RespuestaExternaRepository respuestaRepository,
            OficioInformacionRepository oficioRepository,
            ExpedienteInvestigacionRepository expedienteRepository,
            TransicionExpedienteService transicionService
    ) {
        this.respuestaRepository = respuestaRepository;
        this.oficioRepository = oficioRepository;
        this.expedienteRepository = expedienteRepository;
        this.transicionService = transicionService;
    }

    public RespuestaExternaDTO registrarRespuesta(RegistrarRespuestaExternaDTO dto) {

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

        oficio.setEstatus(EstatusOficio.RESPONDIDO);
        oficioRepository.save(oficio);

        // Fila 16 con dos rondas fijas: la respuesta a la solicitud de información regresa a
        // EN_INVESTIGACION para emitir el oficio de gestión; la del director, a ELABORO_ACUERDO.
        transicionService.cambiar(expediente, FaseOficio.SOLICITUD_INFORMACION.equals(oficio.getFase())
                ? EstatusExpediente.EN_INVESTIGACION
                : EstatusExpediente.ELABORO_ACUERDO);

        return RespuestaExternaDTO.builder()
                .id(guardada.getId())
                .expedienteId(expediente.getId())
                .oficioId(oficio.getId())
                .folio(expediente.getFolio())
                .canalRecepcion(guardada.getCanalRecepcion())
                .numeroOficioRespuestaUA(guardada.getNumeroOficioRespuestaUA())
                .archivoPdfPath(guardada.getArchivoPdfPath())
                .resumen(guardada.getResumen())
                .fechaRecepcion(guardada.getFechaRecepcion().toString())
                .estatusExpediente(expediente.getEstatus())
                .build();
    }
}
