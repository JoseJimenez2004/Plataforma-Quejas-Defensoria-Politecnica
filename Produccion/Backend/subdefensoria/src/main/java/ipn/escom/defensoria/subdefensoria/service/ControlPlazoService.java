package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.ControlPlazoDTO;
import ipn.escom.defensoria.subdefensoria.entity.EstatusExpediente;
import ipn.escom.defensoria.subdefensoria.entity.EstatusOficio;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.entity.OficioInformacion;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.OficioInformacionRepository;
import ipn.escom.defensoria.subdefensoria.util.DiasHabilesCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Pantalla P14 "El Semaforo": por cada expediente EN_INVESTIGACION o
 * EN_GESTION_DIRECTOR muestra el oficio vigente (el mas reciente sin
 * responder) de cualquiera de las dos fases y sus dias transcurridos
 * contra el plazo (10 dias la primera vez, 5 en recordatorios).
 */
@Service
public class ControlPlazoService {

    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final OficioInformacionRepository oficioRepository;
    private final int diasPrimeraSolicitud;
    private final int diasRecordatorio;

    public ControlPlazoService(
            ExpedienteInvestigacionRepository expedienteRepository,
            OficioInformacionRepository oficioRepository,
            @Value("${plazos.primera-solicitud-dias}") int diasPrimeraSolicitud,
            @Value("${plazos.recordatorio-dias}") int diasRecordatorio
    ) {
        this.expedienteRepository = expedienteRepository;
        this.oficioRepository = oficioRepository;
        this.diasPrimeraSolicitud = diasPrimeraSolicitud;
        this.diasRecordatorio = diasRecordatorio;
    }

    public List<ControlPlazoDTO> obtenerSemaforo() {
        List<ExpedienteInvestigacion> activos = expedienteRepository.findByEstatusInOrderByFechaAdmisionAsc(
                List.of(EstatusExpediente.EN_INVESTIGACION, EstatusExpediente.EN_GESTION_DIRECTOR));

        return activos.stream()
                .map(this::filaDelSemaforo)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private ControlPlazoDTO filaDelSemaforo(ExpedienteInvestigacion expediente) {
        Optional<OficioInformacion> vigente = oficioRepository
                .findFirstByExpedienteIdAndEstatusNotOrderByFechaEnvioDesc(
                        expediente.getId(), EstatusOficio.RESPONDIDO);

        if (vigente.isEmpty()) {
            return null;
        }

        OficioInformacion oficio = madurarSiVencido(vigente.get());

        int diasLimite = "PRIMERA".equals(oficio.getTipoPlazo()) ? diasPrimeraSolicitud : diasRecordatorio;
        long diasTranscurridos = DiasHabilesCalculator.diasHabilesTranscurridos(
                oficio.getFechaEnvio(), LocalDate.now());

        return ControlPlazoDTO.builder()
                .expedienteId(expediente.getId())
                .folio(expediente.getFolio())
                .unidadAcademica(expediente.getUnidadAcademica())
                .oficioId(oficio.getId())
                .numeroOficio(oficio.getNumeroOficio())
                .fase(oficio.getFase())
                .estatusOficio(oficio.getEstatus())
                .diasTranscurridos(diasTranscurridos)
                .diasLimite(diasLimite)
                .build();
    }

    private OficioInformacion madurarSiVencido(OficioInformacion oficio) {
        if (EstatusOficio.EN_ESPERA.equals(oficio.getEstatus())
                && oficio.getFechaLimite().isBefore(LocalDate.now())) {
            oficio.setEstatus(EstatusOficio.VENCIDO);
            return oficioRepository.save(oficio);
        }
        return oficio;
    }
}
