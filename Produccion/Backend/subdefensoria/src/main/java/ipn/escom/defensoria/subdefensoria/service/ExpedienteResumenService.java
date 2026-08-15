package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.ExpedienteResumenDTO;
import ipn.escom.defensoria.subdefensoria.entity.EstatusExpediente;
import ipn.escom.defensoria.subdefensoria.entity.EstatusOficio;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.entity.OficioInformacion;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.OficioInformacionRepository;
import ipn.escom.defensoria.subdefensoria.util.DiasHabilesCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Alimenta la bandeja unificada de expedientes (reemplaza tener
 * bandejas separadas por fase): trae todos los expedientes sin
 * importar su estatus, y cuando aplica agrega el oficio vigente para
 * que el front pueda mostrar el progreso sin una pantalla aparte.
 */
@Service
public class ExpedienteResumenService {

    private static final Set<String> ESTATUS_CON_OFICIO_ACTIVO = Set.of(
            EstatusExpediente.EN_INVESTIGACION, EstatusExpediente.EN_GESTION_DIRECTOR);

    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final OficioInformacionRepository oficioRepository;
    private final int diasPrimeraSolicitud;
    private final int diasRecordatorio;

    public ExpedienteResumenService(
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

    public List<ExpedienteResumenDTO> listarTodos() {
        return expedienteRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaAdmision")).stream()
                .map(this::convertirADTO)
                .toList();
    }

    private ExpedienteResumenDTO convertirADTO(ExpedienteInvestigacion e) {
        ExpedienteResumenDTO.ExpedienteResumenDTOBuilder builder = ExpedienteResumenDTO.builder()
                .expedienteId(e.getId())
                .folio(e.getFolio())
                .quejosoNombre(e.getQuejosoNombre())
                .asunto(e.getAsunto())
                .unidadAcademica(e.getUnidadAcademica())
                .fechaAdmision(e.getFechaAdmision() != null ? e.getFechaAdmision().toString() : null)
                .estatus(e.getEstatus());

        if (ESTATUS_CON_OFICIO_ACTIVO.contains(e.getEstatus())) {
            oficioVigente(e.getId()).ifPresent(oficio -> {
                int diasLimite = "PRIMERA".equals(oficio.getTipoPlazo()) ? diasPrimeraSolicitud : diasRecordatorio;
                long diasTranscurridos = DiasHabilesCalculator.diasHabilesTranscurridos(
                        oficio.getFechaEnvio(), LocalDate.now());

                builder.oficioIdVigente(oficio.getId())
                        .numeroOficioVigente(oficio.getNumeroOficio())
                        .destinatarioNombreVigente(oficio.getDestinatarioNombre())
                        .faseOficioVigente(oficio.getFase())
                        .estatusOficioVigente(oficio.getEstatus())
                        .diasTranscurridos(diasTranscurridos)
                        .diasLimite(diasLimite);
            });
        }

        return builder.build();
    }

    private Optional<OficioInformacion> oficioVigente(Long expedienteId) {
        Optional<OficioInformacion> vigente = oficioRepository
                .findFirstByExpedienteIdAndEstatusNotOrderByFechaEnvioDesc(expedienteId, EstatusOficio.RESPONDIDO);

        return vigente.map(oficio -> {
            if (EstatusOficio.EN_ESPERA.equals(oficio.getEstatus()) && oficio.getFechaLimite().isBefore(LocalDate.now())) {
                oficio.setEstatus(EstatusOficio.VENCIDO);
                return oficioRepository.save(oficio);
            }
            return oficio;
        });
    }
}
