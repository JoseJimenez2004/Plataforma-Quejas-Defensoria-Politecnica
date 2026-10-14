package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.GenerarRecordatorioDTO;
import ipn.escom.defensoria.subdefensoria.dto.RecordatorioDTO;
import ipn.escom.defensoria.subdefensoria.entity.*;
import ipn.escom.defensoria.subdefensoria.exception.OperacionInvalidaException;
import ipn.escom.defensoria.subdefensoria.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.OficioInformacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.RecordatorioUrgenciaRepository;
import ipn.escom.defensoria.subdefensoria.util.DiasHabilesCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TS-03 (recordatorio simple, fase SOLICITUD_INFORMACION) y TS-06
 * (recordatorio + medidas, fase GESTION_DIRECTOR, incluso reabriendo
 * un oficio ya RESPONDIDO cuando en TS-05 se decidio "no concluyo").
 */
@Service
public class RecordatorioService {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioService.class);

    private final OficioInformacionRepository oficioRepository;
    private final RecordatorioUrgenciaRepository recordatorioRepository;
    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final int diasRecordatorio;
    private final DocumentoService documentoService;
    private final CorreoUnidadAcademicaService correoService;

    public RecordatorioService(
            OficioInformacionRepository oficioRepository,
            RecordatorioUrgenciaRepository recordatorioRepository,
            ExpedienteInvestigacionRepository expedienteRepository,
            @Value("${plazos.recordatorio-dias}") int diasRecordatorio,
            DocumentoService documentoService,
            CorreoUnidadAcademicaService correoService
    ) {
        this.oficioRepository = oficioRepository;
        this.recordatorioRepository = recordatorioRepository;
        this.expedienteRepository = expedienteRepository;
        this.diasRecordatorio = diasRecordatorio;
        this.documentoService = documentoService;
        this.correoService = correoService;
    }

    public RecordatorioDTO generarRecordatorio(GenerarRecordatorioDTO dto) {

        OficioInformacion oficio = oficioRepository.findById(dto.getOficioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el oficio " + dto.getOficioId()));

        ExpedienteInvestigacion expediente = expedienteRepository.findById(oficio.getExpedienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el expediente " + oficio.getExpedienteId()));

        // Un recordatorio es para un oficio que sigue sin respuesta (CU-SD-07). Si el quejoso
        // no queda conforme con un acuerdo, el camino es su rechazo (fila 18), no reabrir oficios.
        if (EstatusOficio.RESPONDIDO.equals(oficio.getEstatus())) {
            throw new OperacionInvalidaException(
                    "El oficio " + oficio.getNumeroOficio() + " ya tiene respuesta; no requiere recordatorio.");
        }
        if (!EstatusExpediente.EN_ESPERA_OFICIO.equals(expediente.getEstatus())) {
            throw new OperacionInvalidaException("El expediente " + expediente.getFolio()
                    + " no está esperando la respuesta de un oficio (estatus: " + expediente.getEstatus() + ").");
        }

        LocalDate hoy = LocalDate.now();
        long diasRetraso = DiasHabilesCalculator.diasHabilesTranscurridos(oficio.getFechaLimite(), hoy);

        RecordatorioUrgencia recordatorio = RecordatorioUrgencia.builder()
                .oficioId(oficio.getId())
                .mensaje(dto.getMensaje())
                .medidasOfrecidas(dto.getMedidasOfrecidas())
                .diasRetraso((int) diasRetraso)
                .fechaEnvio(LocalDateTime.now())
                .build();
        recordatorioRepository.save(recordatorio);

        oficio.setTipoPlazo("SUBSECUENTE");
        oficio.setFechaLimite(DiasHabilesCalculator.sumarDiasHabiles(hoy, diasRecordatorio));
        oficio.setEstatus(EstatusOficio.EN_ESPERA);
        OficioInformacion actualizado = oficioRepository.save(oficio);

        enviarPorCorreo(recordatorio, actualizado, expediente);

        return RecordatorioDTO.builder()
                .id(recordatorio.getId())
                .oficioId(actualizado.getId())
                .numeroOficio(actualizado.getNumeroOficio())
                .mensaje(recordatorio.getMensaje())
                .medidasOfrecidas(recordatorio.getMedidasOfrecidas())
                .diasRetraso(recordatorio.getDiasRetraso())
                .fechaEnvio(recordatorio.getFechaEnvio().toString())
                .nuevaFechaLimite(actualizado.getFechaLimite().toString())
                .estatusExpediente(expediente.getEstatus())
                .build();
    }

    /** Correo a la unidad académica con el recordatorio en PDF; si falla, ya quedó guardado. */
    private void enviarPorCorreo(RecordatorioUrgencia recordatorio, OficioInformacion oficio,
                                 ExpedienteInvestigacion expediente) {
        try {
            correoService.enviar(
                    oficio.getDestinatarioCorreo(),
                    "Recordatorio del oficio " + oficio.getNumeroOficio() + " (" + expediente.getFolio() + ")",
                    "Estimado(a) " + oficio.getDestinatarioNombre() + ",\n\nSe adjunta el recordatorio del oficio "
                            + oficio.getNumeroOficio() + ", que sigue sin respuesta. Le agradeceremos que la haga "
                            + "llegar a más tardar el " + oficio.getFechaLimite() + ".",
                    documentoService.recordatorio(recordatorio.getId()));
        } catch (Exception ex) {
            log.error("No se pudo enviar el recordatorio del oficio {} por correo: {}",
                    oficio.getNumeroOficio(), ex.getMessage());
        }
    }

    public java.util.List<RecordatorioDTO> porOficio(Long oficioId) {
        OficioInformacion oficio = oficioRepository.findById(oficioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el oficio " + oficioId));
        return recordatorioRepository.findByOficioIdOrderByFechaEnvioDesc(oficioId).stream()
                .map(r -> RecordatorioDTO.builder()
                        .id(r.getId())
                        .oficioId(oficio.getId())
                        .numeroOficio(oficio.getNumeroOficio())
                        .mensaje(r.getMensaje())
                        .medidasOfrecidas(r.getMedidasOfrecidas())
                        .diasRetraso(r.getDiasRetraso())
                        .fechaEnvio(r.getFechaEnvio() != null ? r.getFechaEnvio().toString() : null)
                        .build())
                .toList();
    }
}
