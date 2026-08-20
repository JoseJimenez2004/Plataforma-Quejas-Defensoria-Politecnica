package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.ExpedienteEntranteDTO;
import ipn.escom.defensoria.subdefensoria.dto.ExpedienteInvestigacionDTO;
import ipn.escom.defensoria.subdefensoria.entity.EstatusExpediente;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class IngestaExpedienteService {

    private final ExpedienteInvestigacionRepository expedienteRepository;

    public IngestaExpedienteService(
            ExpedienteInvestigacionRepository expedienteRepository
    ) {
        this.expedienteRepository = expedienteRepository;
    }

    public ExpedienteInvestigacionDTO recibirExpediente(
            ExpedienteEntranteDTO entrante
    ) {

        /*
         * Si Primer Contacto vuelve a enviar el mismo expediente,
         * no creamos otro folio de Subdefensoría.
         */
        ExpedienteInvestigacion existente =
                expedienteRepository
                        .findByFolioOrigen(
                                entrante.getFolioOrigen()
                        )
                        .orElse(null);

        if (existente != null) {
            return convertirADTO(existente);
        }

        ExpedienteInvestigacion expediente =
                ExpedienteInvestigacion.builder()

                        /*
                         * Folio NUEVO y propio de Subdefensoría.
                         */
                        .folio(generarFolio())

                        /*
                         * Folio recibido desde Primer Contacto.
                         */
                        .folioOrigen(
                                entrante.getFolioOrigen()
                        )

                        .asunto(
                                entrante.getAsunto()
                        )

                        .descripcionHechos(
                                entrante.getDescripcionHechos()
                        )

                        .fechaAdmision(
                                entrante.getFechaAdmision()
                        )

                        .abogadoAsesorId(
                                entrante.getAbogadoAsesorId()
                        )

                        .abogadoAsesorNombre(
                                entrante.getAbogadoAsesorNombre()
                        )

                        .quejosoNombre(
                                entrante.getQuejoso() != null
                                        ? entrante
                                        .getQuejoso()
                                        .getNombreCompleto()
                                        : null
                        )

                        .unidadAcademica(
                                entrante.getQuejoso() != null
                                        ? entrante
                                        .getQuejoso()
                                        .getUnidadAcademica()
                                        : null
                        )

                        .observacionesAnalista(
                                entrante.getObservacionesAnalista()
                        )

                        .estatus(
                                EstatusExpediente.RECIBIDO
                        )

                        .fechaCreacion(
                                LocalDateTime.now()
                        )

                        .build();

        ExpedienteInvestigacion guardado =
                expedienteRepository.save(expediente);

        return convertirADTO(guardado);
    }

    private String generarFolio() {

        return "SD-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }

    private ExpedienteInvestigacionDTO convertirADTO(
            ExpedienteInvestigacion e
    ) {

        return ExpedienteInvestigacionDTO.builder()
                .id(e.getId())
                .folio(e.getFolio())
                .folioOrigen(e.getFolioOrigen())
                .quejosoNombre(e.getQuejosoNombre())
                .unidadAcademica(e.getUnidadAcademica())
                .asunto(e.getAsunto())
                .descripcionHechos(e.getDescripcionHechos())
                .fechaAdmision(
                        e.getFechaAdmision() != null
                                ? e.getFechaAdmision().toString()
                                : null
                )
                .abogadoAsesorId(
                        e.getAbogadoAsesorId()
                )
                .abogadoAsesorNombre(
                        e.getAbogadoAsesorNombre()
                )
                .estatus(e.getEstatus())
                .observacionesAnalista(
                        e.getObservacionesAnalista()
                )
                .build();
    }
}