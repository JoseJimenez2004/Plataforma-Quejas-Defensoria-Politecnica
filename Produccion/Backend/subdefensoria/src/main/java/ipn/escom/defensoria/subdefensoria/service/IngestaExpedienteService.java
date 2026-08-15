package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.ExpedienteEntranteDTO;
import ipn.escom.defensoria.subdefensoria.dto.ExpedienteInvestigacionDTO;
import ipn.escom.defensoria.subdefensoria.entity.EstatusExpediente;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.exception.OperacionInvalidaException;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Punto de entrada unico para expedientes que llegan desde Primer
 * Contacto (acuerdo de admision, act. 10 del DDP-PO-02). Es el
 * espejo, en sentido inverso, del IngestaSubdefensoriaController que
 * ya existe en el microservicio de Primer Contacto.
 */
@Service
public class IngestaExpedienteService {

    private final ExpedienteInvestigacionRepository expedienteRepository;

    public IngestaExpedienteService(ExpedienteInvestigacionRepository expedienteRepository) {
        this.expedienteRepository = expedienteRepository;
    }

    public ExpedienteInvestigacionDTO recibirExpediente(ExpedienteEntranteDTO entrante) {

        if (expedienteRepository.existsByQuejaId(entrante.getQuejaId())) {
            throw new OperacionInvalidaException(
                    "Ya existe un expediente de investigación para la queja " + entrante.getQuejaId());
        }

        ExpedienteInvestigacion expediente = ExpedienteInvestigacion.builder()
                .quejaId(entrante.getQuejaId())
                .folio(entrante.getFolio())
                .asunto(entrante.getAsunto())
                .descripcionHechos(entrante.getDescripcionHechos())
                .fechaAdmision(entrante.getFechaAdmision())
                .abogadoAsesorId(entrante.getAbogadoAsesorId())
                .abogadoAsesorNombre(entrante.getAbogadoAsesorNombre())
                .quejosoNombre(entrante.getQuejoso() != null ? entrante.getQuejoso().getNombreCompleto() : null)
                .unidadAcademica(entrante.getQuejoso() != null ? entrante.getQuejoso().getUnidadAcademica() : null)
                .observacionesAnalista(entrante.getObservacionesAnalista())
                .estatus(EstatusExpediente.RECIBIDO)
                .fechaCreacion(LocalDateTime.now())
                .build();

        ExpedienteInvestigacion guardado = expedienteRepository.save(expediente);

        return convertirADTO(guardado);
    }

    private ExpedienteInvestigacionDTO convertirADTO(ExpedienteInvestigacion e) {
        return ExpedienteInvestigacionDTO.builder()
                .id(e.getId())
                .quejaId(e.getQuejaId())
                .folio(e.getFolio())
                .quejosoNombre(e.getQuejosoNombre())
                .unidadAcademica(e.getUnidadAcademica())
                .asunto(e.getAsunto())
                .descripcionHechos(e.getDescripcionHechos())
                .fechaAdmision(e.getFechaAdmision() != null ? e.getFechaAdmision().toString() : null)
                .abogadoAsesorId(e.getAbogadoAsesorId())
                .abogadoAsesorNombre(e.getAbogadoAsesorNombre())
                .estatus(e.getEstatus())
                .observacionesAnalista(e.getObservacionesAnalista())
                .build();
    }
}
