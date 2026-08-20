package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CrearRemisionDTO;
import ipn.escom.defensoria.primercontacto.dto.RemisionDTO;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.RemisionExterna;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.RemisionExternaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RemisionExternaService {

    private final RemisionExternaRepository remisionExternaRepository;
    private final ExpedientePrimerContactoRepository expedienteRepository;

    public RemisionExternaService(
            RemisionExternaRepository remisionExternaRepository,
            ExpedientePrimerContactoRepository expedienteRepository
    ) {
        this.remisionExternaRepository =
                remisionExternaRepository;

        this.expedienteRepository =
                expedienteRepository;
    }

    @Transactional
    public RemisionDTO crearRemision(
            CrearRemisionDTO dto
    ) {

        /*
         * Localizamos el expediente mediante el
         * folio propio de Primer Contacto.
         */
        ExpedientePrimerContacto expediente =
                expedienteRepository
                        .findByFolio(dto.getFolio())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No existe un expediente de Primer Contacto con folio "
                                                + dto.getFolio()
                                )
                        );

        /*
         * Un expediente solamente puede tener
         * una remisión externa.
         */
        if (remisionExternaRepository
                .existsByExpedienteId(expediente.getId())) {

            throw new RuntimeException(
                    "El expediente ya cuenta con una remisión registrada"
            );
        }

        RemisionExterna remision =
                RemisionExterna.builder()
                        .expedienteId(
                                expediente.getId()
                        )
                        .folio(
                                expediente.getFolio()
                        )
                        .analistaId(
                                dto.getAnalistaId()
                        )
                        .analistaNombre(
                                dto.getAnalistaNombre()
                        )
                        .autoridadRemision(
                                dto.getAutoridadRemision()
                        )
                        .justificacionLegal(
                                dto.getJustificacionLegal()
                        )
                        .sugerenciaQuejoso(
                                dto.getSugerenciaQuejoso()
                        )
                        .adjuntarExpediente(
                                dto.getAdjuntarExpediente()
                        )
                        .fechaRemision(
                                LocalDateTime.now()
                        )
                        .build();

        RemisionExterna guardada =
                remisionExternaRepository.save(remision);

        /*
         * Este estado pertenece a Primer Contacto,
         * no a la tabla quejas.
         */
        expediente.setEstatus("REMITIDA");
        expediente.setFechaActualizacion(
                LocalDateTime.now()
        );

        expedienteRepository.save(expediente);

        return convertirADTO(guardada);
    }

    public RemisionDTO obtenerPorExpediente(
            Long expedienteId
    ) {

        return remisionExternaRepository
                .findByExpedienteId(expedienteId)
                .map(this::convertirADTO)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Remisión no encontrada"
                        )
                );
    }

    public RemisionDTO obtenerPorFolio(
            String folio
    ) {

        return remisionExternaRepository
                .findByFolio(folio)
                .map(this::convertirADTO)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Remisión no encontrada"
                        )
                );
    }

    @Transactional
    public RemisionDTO enviarRemision(
            String folio
    ) {

        RemisionExterna remision =
                remisionExternaRepository
                        .findByFolio(folio)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Remisión no encontrada"
                                )
                        );

        ExpedientePrimerContacto expediente =
                expedienteRepository
                        .findByFolio(folio)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Expediente de Primer Contacto no encontrado"
                                )
                        );

        /*
         * El expediente continúa identificándose
         * mediante su folio PC-...
         */
        expediente.setEstatus(
                "REMISION_ENVIADA"
        );

        expediente.setFechaActualizacion(
                LocalDateTime.now()
        );

        expedienteRepository.save(expediente);

        return convertirADTO(remision);
    }

    private RemisionDTO convertirADTO(
            RemisionExterna remision
    ) {

        return RemisionDTO.builder()
                .id(remision.getId())
                .expedienteId(
                        remision.getExpedienteId()
                )
                .folio(
                        remision.getFolio()
                )
                .analistaId(
                        remision.getAnalistaId()
                )
                .analistaNombre(
                        remision.getAnalistaNombre()
                )
                .autoridadRemision(
                        remision.getAutoridadRemision()
                )
                .justificacionLegal(
                        remision.getJustificacionLegal()
                )
                .sugerenciaQuejoso(
                        remision.getSugerenciaQuejoso()
                )
                .adjuntarExpediente(
                        remision.getAdjuntarExpediente()
                )
                .fechaRemision(
                        remision.getFechaRemision() != null
                                ? remision
                                .getFechaRemision()
                                .toString()
                                : null
                )
                .build();
    }
}