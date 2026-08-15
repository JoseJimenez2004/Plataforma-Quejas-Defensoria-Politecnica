package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CompetenciaDTO;
import ipn.escom.defensoria.primercontacto.dto.DictamenDTO;
import ipn.escom.defensoria.primercontacto.dto.ImprocedenciaDTO;
import ipn.escom.defensoria.primercontacto.dto.QuejosoResumenRequest;
import ipn.escom.defensoria.primercontacto.dto.ExpedienteAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.ExpedienteEntranteRequest;
import ipn.escom.defensoria.primercontacto.entity.DictamenPrimerContacto;
import ipn.escom.defensoria.primercontacto.repository.DictamenPrimerContactoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DictamenPrimerContactoService {

    private final DictamenPrimerContactoRepository dictamenPrimerContactoRepository;
    private final PlataformaCentralClientService plataformaCentralClientService;
    private final SubdefensoriaClientService subdefensoriaClientService;

    public DictamenPrimerContactoService(
            DictamenPrimerContactoRepository dictamenPrimerContactoRepository,
            PlataformaCentralClientService plataformaCentralClientService,
            SubdefensoriaClientService subdefensoriaClientService
    ) {
        this.dictamenPrimerContactoRepository = dictamenPrimerContactoRepository;
        this.plataformaCentralClientService = plataformaCentralClientService;
        this.subdefensoriaClientService = subdefensoriaClientService;
    }


    public DictamenDTO registrarCompetencia(CompetenciaDTO dto, String token) {

        if (dictamenPrimerContactoRepository.existsByQuejaId(dto.getQuejaId())) {
            throw new RuntimeException("La queja ya cuenta con un dictamen registrado");
        }

        DictamenPrimerContacto dictamen = DictamenPrimerContacto.builder()
                .quejaId(dto.getQuejaId())
                .folio(dto.getFolio())
                .analistaId(dto.getAnalistaId())
                .analistaNombre(dto.getAnalistaNombre())
                .resultado("COMPETENTE")
                .justificacion(dto.getJustificacion())
                .areaTurno(dto.getAreaTurno())
                .responsableTurno(dto.getResponsableTurno())
                .fechaDictamen(LocalDateTime.now())
                .observaciones(dto.getObservaciones())
                .build();

        DictamenPrimerContacto guardado = dictamenPrimerContactoRepository.save(dictamen);

        plataformaCentralClientService.actualizarEstatusQueja(
                dto.getQuejaId(),
                "COMPETENTE",
                token
        );

        ExpedienteAnalisisDTO expediente = plataformaCentralClientService.obtenerExpediente(dto.getQuejaId(), token);

        QuejosoResumenRequest quejosoRequest = expediente.getQuejoso() != null
                ? QuejosoResumenRequest.builder()
                .nombreCompleto(expediente.getQuejoso().getNombreCompleto())
                .correo(expediente.getQuejoso().getCorreo())
                .unidadAcademica(expediente.getQuejoso().getUnidadAcademica())
                .build()
                : null;

        subdefensoriaClientService.enviarExpediente(
                ExpedienteEntranteRequest.builder()
                        .quejaId(dto.getQuejaId())
                        .folio(dto.getFolio())
                        .asunto(expediente.getTema())
                        .descripcionHechos(expediente.getDescripcionHechos())
                        .fechaAdmision(java.time.LocalDate.now())
                        .abogadoAsesorNombre(dto.getResponsableTurno())
                        .quejoso(quejosoRequest)
                        .observacionesAnalista(dto.getObservaciones())
                        .build()
        );

        return convertirADTO(guardado);
    }

    public DictamenDTO registrarImprocedencia(ImprocedenciaDTO dto, String token) {

        if (dictamenPrimerContactoRepository.existsByQuejaId(dto.getQuejaId())) {
            throw new RuntimeException("La queja ya cuenta con un dictamen registrado");
        }

        DictamenPrimerContacto dictamen = DictamenPrimerContacto.builder()
                .quejaId(dto.getQuejaId())
                .folio(dto.getFolio())
                .analistaId(dto.getAnalistaId())
                .analistaNombre(dto.getAnalistaNombre())
                .resultado("IMPROCEDENTE")
                .justificacion(dto.getJustificacion())
                .areaTurno(null)
                .responsableTurno(null)
                .fechaDictamen(LocalDateTime.now())
                .build();

        DictamenPrimerContacto guardado = dictamenPrimerContactoRepository.save(dictamen);

        plataformaCentralClientService.actualizarEstatusQueja(
                dto.getQuejaId(),
                "IMPROCEDENTE",
                token
        );

        return convertirADTO(guardado);
    }

    public DictamenDTO obtenerPorQueja(Long quejaId) {
        return dictamenPrimerContactoRepository.findByQuejaId(quejaId)
                .map(this::convertirADTO)
                .orElseThrow(() -> new RuntimeException("Dictamen no encontrado"));
    }

    public DictamenDTO obtenerPorFolio(String folio) {
        return dictamenPrimerContactoRepository.findByFolio(folio)
                .map(this::convertirADTO)
                .orElseThrow(() -> new RuntimeException("Dictamen no encontrado"));
    }

    private DictamenDTO convertirADTO(DictamenPrimerContacto dictamen) {

        return DictamenDTO.builder()
                .id(dictamen.getId())
                .quejaId(dictamen.getQuejaId())
                .folio(dictamen.getFolio())
                .analistaId(dictamen.getAnalistaId())
                .analistaNombre(dictamen.getAnalistaNombre())
                .resultado(dictamen.getResultado())
                .justificacion(dictamen.getJustificacion())
                .areaTurno(dictamen.getAreaTurno())
                .responsableTurno(dictamen.getResponsableTurno())
                .fechaDictamen(
                        dictamen.getFechaDictamen() != null
                                ? dictamen.getFechaDictamen().toString()
                                : null
                )
                .observaciones(dictamen.getObservaciones())
                .build();
    }



}