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

    private final OficioInformacionRepository oficioRepository;
    private final RecordatorioUrgenciaRepository recordatorioRepository;
    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final int diasRecordatorio;

    public RecordatorioService(
            OficioInformacionRepository oficioRepository,
            RecordatorioUrgenciaRepository recordatorioRepository,
            ExpedienteInvestigacionRepository expedienteRepository,
            @Value("${plazos.recordatorio-dias}") int diasRecordatorio
    ) {
        this.oficioRepository = oficioRepository;
        this.recordatorioRepository = recordatorioRepository;
        this.expedienteRepository = expedienteRepository;
        this.diasRecordatorio = diasRecordatorio;
    }

    public RecordatorioDTO generarRecordatorio(GenerarRecordatorioDTO dto) {

        OficioInformacion oficio = oficioRepository.findById(dto.getOficioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el oficio " + dto.getOficioId()));

        ExpedienteInvestigacion expediente = expedienteRepository.findById(oficio.getExpedienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el expediente " + oficio.getExpedienteId()));

        if (EstatusExpediente.CONCLUIDO.equals(expediente.getEstatus())) {
            throw new OperacionInvalidaException("El expediente " + expediente.getFolio() + " ya está concluido.");
        }

        LocalDate hoy = LocalDate.now();
        long diasRetraso = oficio.getEstatus().equals(EstatusOficio.RESPONDIDO)
                ? 0
                : DiasHabilesCalculator.diasHabilesTranscurridos(oficio.getFechaLimite(), hoy);

        RecordatorioUrgencia recordatorio = RecordatorioUrgencia.builder()
                .oficioId(oficio.getId())
                .mensaje(dto.getMensaje())
                .medidasOfrecidas(dto.getMedidasOfrecidas())
                .diasRetraso((int) diasRetraso)
                .fechaEnvio(LocalDateTime.now())
                .build();
        recordatorioRepository.save(recordatorio);

        // TS-06: si el oficio de gestion con el director ya tenia respuesta
        // pero se decidio "no concluyo", se reabre para un nuevo ciclo.
        boolean seReabrio = EstatusOficio.RESPONDIDO.equals(oficio.getEstatus());

        oficio.setTipoPlazo("SUBSECUENTE");
        oficio.setFechaLimite(DiasHabilesCalculator.sumarDiasHabiles(hoy, diasRecordatorio));
        oficio.setEstatus(EstatusOficio.EN_ESPERA);
        OficioInformacion actualizado = oficioRepository.save(oficio);

        if (seReabrio && EstatusExpediente.LISTO_A_DICTAMINAR.equals(expediente.getEstatus())) {
            expediente.setEstatus(EstatusExpediente.EN_GESTION_DIRECTOR);
            expediente.setFechaActualizacion(LocalDateTime.now());
            expedienteRepository.save(expediente);
        }

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
}
