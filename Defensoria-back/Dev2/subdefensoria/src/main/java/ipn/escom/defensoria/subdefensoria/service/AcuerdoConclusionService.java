package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.AcuerdoConclusionDTO;
import ipn.escom.defensoria.subdefensoria.dto.CrearAcuerdoConclusionDTO;
import ipn.escom.defensoria.subdefensoria.entity.AcuerdoConclusion;
import ipn.escom.defensoria.subdefensoria.entity.EstatusExpediente;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.exception.OperacionInvalidaException;
import ipn.escom.defensoria.subdefensoria.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.subdefensoria.repository.AcuerdoConclusionRepository;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * TS-07/TS-08: redacta el acuerdo y notificacion al quejoso. Si
 * concluir=true, cierra el expediente dentro de Subdefensoria y lo
 * envia para archivo al area secretarial (CONCLUIDO) - no hay
 * escalacion a Defensoria/Titular en este BPMN. Si concluir=false,
 * solo guarda el borrador (util si TS-05 aun no decide "si concluyo").
 */
@Service
public class AcuerdoConclusionService {

    private final AcuerdoConclusionRepository acuerdoRepository;
    private final ExpedienteInvestigacionRepository expedienteRepository;

    public AcuerdoConclusionService(
            AcuerdoConclusionRepository acuerdoRepository,
            ExpedienteInvestigacionRepository expedienteRepository
    ) {
        this.acuerdoRepository = acuerdoRepository;
        this.expedienteRepository = expedienteRepository;
    }

    public AcuerdoConclusionDTO guardarOConcluir(CrearAcuerdoConclusionDTO dto) {

        ExpedienteInvestigacion expediente = expedienteRepository.findById(dto.getExpedienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el expediente " + dto.getExpedienteId()));

        if (!EstatusExpediente.LISTO_A_DICTAMINAR.equals(expediente.getEstatus())) {
            throw new OperacionInvalidaException(
                    "El expediente " + expediente.getFolio()
                            + " no está listo para dictaminar (estatus actual: " + expediente.getEstatus() + ").");
        }

        AcuerdoConclusion acuerdo = acuerdoRepository.findByExpedienteId(expediente.getId())
                .orElseGet(() -> AcuerdoConclusion.builder()
                        .expedienteId(expediente.getId())
                        .folio(expediente.getFolio())
                        .concluido(false)
                        .fechaCreacion(LocalDateTime.now())
                        .build());

        if (Boolean.TRUE.equals(acuerdo.getConcluido())) {
            throw new OperacionInvalidaException(
                    "El expediente " + expediente.getFolio() + " ya fue concluido.");
        }

        acuerdo.setTextoAcuerdo(dto.getTextoAcuerdo());

        if (Boolean.TRUE.equals(dto.getConcluir())) {
            acuerdo.setConcluido(true);
            acuerdo.setFechaEnvioSecretarial(LocalDateTime.now());
            expediente.setEstatus(EstatusExpediente.CONCLUIDO);
            expediente.setFechaActualizacion(LocalDateTime.now());
            expedienteRepository.save(expediente);
        }

        AcuerdoConclusion guardado = acuerdoRepository.save(acuerdo);

        return convertirADTO(guardado, expediente.getEstatus());
    }

    public AcuerdoConclusionDTO obtenerPorExpediente(Long expedienteId) {
        ExpedienteInvestigacion expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el expediente " + expedienteId));

        return acuerdoRepository.findByExpedienteId(expedienteId)
                .map(a -> convertirADTO(a, expediente.getEstatus()))
                .orElse(null);
    }

    private AcuerdoConclusionDTO convertirADTO(AcuerdoConclusion a, String estatusExpediente) {
        return AcuerdoConclusionDTO.builder()
                .id(a.getId())
                .expedienteId(a.getExpedienteId())
                .folio(a.getFolio())
                .textoAcuerdo(a.getTextoAcuerdo())
                .concluido(a.getConcluido())
                .fechaCreacion(a.getFechaCreacion() != null ? a.getFechaCreacion().toString() : null)
                .fechaEnvioSecretarial(a.getFechaEnvioSecretarial() != null ? a.getFechaEnvioSecretarial().toString() : null)
                .estatusExpediente(estatusExpediente)
                .build();
    }
}
