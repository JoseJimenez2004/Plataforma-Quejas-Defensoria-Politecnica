package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CompetenciaDTO;
import ipn.escom.defensoria.primercontacto.dto.DictamenDTO;
import ipn.escom.defensoria.primercontacto.dto.ImprocedenciaDTO;
import ipn.escom.defensoria.primercontacto.entity.DictamenPrimerContacto;
import ipn.escom.defensoria.primercontacto.repository.DictamenPrimerContactoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DictamenPrimerContactoService {

    private final DictamenPrimerContactoRepository dictamenPrimerContactoRepository;
    private final PlataformaCentralClientService plataformaCentralClientService;

    public DictamenPrimerContactoService(
            DictamenPrimerContactoRepository dictamenPrimerContactoRepository,
            PlataformaCentralClientService plataformaCentralClientService
    ) {
        this.dictamenPrimerContactoRepository = dictamenPrimerContactoRepository;
        this.plataformaCentralClientService = plataformaCentralClientService;
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
                .build();

        DictamenPrimerContacto guardado = dictamenPrimerContactoRepository.save(dictamen);

        plataformaCentralClientService.actualizarEstatusQueja(
                dto.getQuejaId(),
                "COMPETENTE",
                token
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
                .build();
    }
}