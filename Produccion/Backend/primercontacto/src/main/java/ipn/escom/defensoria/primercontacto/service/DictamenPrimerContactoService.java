package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CompetenciaDTO;
import ipn.escom.defensoria.primercontacto.dto.DictamenDTO;
import ipn.escom.defensoria.primercontacto.dto.ImprocedenciaDTO;
import ipn.escom.defensoria.primercontacto.entity.DictamenPrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.repository.DictamenPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ipn.escom.defensoria.primercontacto.dto.ExpedienteEntranteRequest;
import ipn.escom.defensoria.primercontacto.dto.QuejosoResumenRequest;
import ipn.escom.defensoria.primercontacto.dto.SubdefensoriaIngresoResponse;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import java.time.LocalDate;


import java.time.LocalDateTime;

@Service
public class DictamenPrimerContactoService {

    private final DictamenPrimerContactoRepository dictamenRepository;
    private final ExpedientePrimerContactoRepository expedienteRepository;
    private final SubdefensoriaClientService subdefensoriaClientService;

    public DictamenPrimerContactoService(
            DictamenPrimerContactoRepository dictamenRepository,
            ExpedientePrimerContactoRepository expedienteRepository,
            SubdefensoriaClientService subdefensoriaClientService
    ) {
        this.dictamenRepository = dictamenRepository;
        this.expedienteRepository = expedienteRepository;
        this.subdefensoriaClientService = subdefensoriaClientService;
    }

    @Transactional
    public DictamenDTO registrarCompetencia(
            CompetenciaDTO dto,
            PersonalAdministrativo analista
    ) {

        /*
         * El frontend manda el folio de Primer Contacto.
         *
         * No recibimos quejaId ni expedienteId.
         */
        ExpedientePrimerContacto expediente =
                expedienteRepository.findByFolio(dto.getFolio())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No existe un expediente de Primer Contacto con folio "
                                                + dto.getFolio()
                                )
                        );

        /*
         * Un expediente solamente puede tener
         * un dictamen de Primer Contacto.
         */
        if (dictamenRepository.existsByExpedienteId(
                expediente.getId()
        )) {
            throw new RuntimeException(
                    "El expediente ya cuenta con un dictamen registrado"
            );
        }

        DictamenPrimerContacto dictamen =
                DictamenPrimerContacto.builder()
                        .expedienteId(expediente.getId())
                        .folio(expediente.getFolio())
                        .analistaId(analista.getId())
                        .analistaNombre(analista.getNombreCompleto())
                        .resultado("COMPETENTE")
                        .justificacion(dto.getJustificacion())
                        .areaTurno(dto.getAreaTurno())
                        .responsableTurno(dto.getResponsableTurno())
                        .fechaDictamen(LocalDateTime.now())
                        .observaciones(dto.getObservaciones())
                        .build();

        DictamenPrimerContacto guardado =
                dictamenRepository.save(dictamen);

        /*
         * Construimos únicamente la información que
         * Subdefensoría necesita.
         *
         * La relación entre áreas se realiza mediante
         * el folio PC-..., nunca mediante IDs.
         */
        QuejosoResumenRequest quejoso =
                QuejosoResumenRequest.builder()
                        .nombreCompleto(
                                expediente.getQuejosoNombre()
                        )
                        .correo(
                                expediente.getQuejosoCorreo()
                        )
                        .unidadAcademica(
                                expediente.getUnidadAcademica()
                        )
                        .build();

        ExpedienteEntranteRequest solicitudSubdefensoria =
                ExpedienteEntranteRequest.builder()

                        /*
                         * PC-XXXXXXXX
                         */
                        .folioOrigen(
                                expediente.getFolio()
                        )

                        .asunto(
                                expediente.getTema()
                        )

                        .descripcionHechos(
                                expediente.getDescripcionHechos()
                        )

                        .fechaAdmision(
                                LocalDate.now()
                        )

                        /*
                         * Por ahora no tenemos el ID del abogado
                         * en CompetenciaDTO.
                         */
                        .abogadoAsesorId(null)

                        .abogadoAsesorNombre(
                                dto.getResponsableTurno()
                        )

                        .quejoso(quejoso)

                        .observacionesAnalista(
                                dto.getObservaciones()
                        )

                        .build();

        SubdefensoriaIngresoResponse respuestaSubdefensoria =
                subdefensoriaClientService
                        .enviarExpediente(
                                solicitudSubdefensoria
                        );

        /*
         * Guardamos solamente el folio generado por
         * Subdefensoría.
         *
         * NO guardamos su id interno.
         */
        expediente.setFolioSubdefensoria(
                respuestaSubdefensoria.getFolio()
        );

        expediente.setEstatus("TURNADO_SUBDEFENSORIA");

        expediente.setFechaActualizacion(
                LocalDateTime.now()
        );

        expedienteRepository.save(expediente);

        return convertirADTO(guardado);
    }

    @Transactional
    public DictamenDTO registrarImprocedencia(
            ImprocedenciaDTO dto,
            PersonalAdministrativo analista
    ) {

        ExpedientePrimerContacto expediente =
                expedienteRepository.findByFolio(dto.getFolio())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No existe un expediente de Primer Contacto con folio "
                                                + dto.getFolio()
                                )
                        );

        if (dictamenRepository.existsByExpedienteId(
                expediente.getId()
        )) {
            throw new RuntimeException(
                    "El expediente ya cuenta con un dictamen registrado"
            );
        }

        DictamenPrimerContacto dictamen =
                DictamenPrimerContacto.builder()
                        .expedienteId(expediente.getId())
                        .folio(expediente.getFolio())
                        .analistaId(analista.getId())
                        .analistaNombre(analista.getNombreCompleto())
                        .resultado("IMPROCEDENTE")
                        .justificacion(dto.getJustificacion())
                        .areaTurno(null)
                        .responsableTurno(null)
                        .fechaDictamen(LocalDateTime.now())
                        .observaciones(null)
                        .build();

        DictamenPrimerContacto guardado =
                dictamenRepository.save(dictamen);

        /*
         * Si es improcedente, el expediente termina
         * su flujo en Primer Contacto.
         */
        expediente.setEstatus("IMPROCEDENTE");
        expediente.setFechaActualizacion(
                LocalDateTime.now()
        );

        expedienteRepository.save(expediente);

        return convertirADTO(guardado);
    }

    public DictamenDTO obtenerPorExpediente(
            Long expedienteId
    ) {

        return dictamenRepository
                .findByExpedienteId(expedienteId)
                .map(this::convertirADTO)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Dictamen no encontrado"
                        )
                );
    }

    public DictamenDTO obtenerPorFolio(
            String folio
    ) {

        return dictamenRepository
                .findByFolio(folio)
                .map(this::convertirADTO)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Dictamen no encontrado"
                        )
                );
    }

    private DictamenDTO convertirADTO(
            DictamenPrimerContacto dictamen
    ) {

        return DictamenDTO.builder()
                .id(dictamen.getId())
                .expedienteId(dictamen.getExpedienteId())
                .folio(dictamen.getFolio())
                .analistaId(dictamen.getAnalistaId())
                .analistaNombre(dictamen.getAnalistaNombre())
                .resultado(dictamen.getResultado())
                .justificacion(dictamen.getJustificacion())
                .areaTurno(dictamen.getAreaTurno())
                .responsableTurno(
                        dictamen.getResponsableTurno()
                )
                .fechaDictamen(
                        dictamen.getFechaDictamen() != null
                                ? dictamen
                                .getFechaDictamen()
                                .toString()
                                : null
                )
                .observaciones(
                        dictamen.getObservaciones()
                )
                .build();
    }
}