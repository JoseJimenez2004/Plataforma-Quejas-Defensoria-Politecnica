package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.ExpedienteAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.NotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.entity.NotaAnalisis;
import ipn.escom.defensoria.primercontacto.repository.NotaAnalisisRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpedienteAnalisisService {

    private final PlataformaCentralClientService plataformaCentralClientService;
    private final NotaAnalisisRepository notaAnalisisRepository;

    public ExpedienteAnalisisService(
            PlataformaCentralClientService plataformaCentralClientService,
            NotaAnalisisRepository notaAnalisisRepository
    ) {
        this.plataformaCentralClientService = plataformaCentralClientService;
        this.notaAnalisisRepository = notaAnalisisRepository;
    }

    public ExpedienteAnalisisDTO obtenerExpediente(
            Long quejaId,
            String token
    ) {

        ExpedienteAnalisisDTO expediente =
                plataformaCentralClientService
                        .obtenerExpediente(quejaId, token);

        List<NotaAnalisisDTO> notas = notaAnalisisRepository
                .findByQuejaIdOrderByFechaCreacionDesc(quejaId)
                .stream()
                .map(this::convertirNotaDTO)
                .toList();

        expediente.setNotas(notas);

        return expediente;
    }

    public ExpedienteAnalisisDTO obtenerPorFolio(
            String folio,
            String token
    ) {

        ExpedienteAnalisisDTO expediente =
                plataformaCentralClientService
                        .obtenerExpedientePorFolio(folio, token);

        List<NotaAnalisisDTO> notas = notaAnalisisRepository
                .findByFolioOrderByFechaCreacionDesc(folio)
                .stream()
                .map(this::convertirNotaDTO)
                .toList();

        expediente.setNotas(notas);

        return expediente;
    }

    private NotaAnalisisDTO convertirNotaDTO(
            NotaAnalisis nota
    ) {

        return NotaAnalisisDTO.builder()
                .id(nota.getId())
                .quejaId(nota.getQuejaId())
                .folio(nota.getFolio())
                .analistaId(nota.getAnalistaId())
                .analistaNombre(nota.getAnalistaNombre())
                .contenido(nota.getContenido())
                .fechaCreacion(
                        nota.getFechaCreacion().toString())
                .fechaActualizacion(
                        nota.getFechaActualizacion() != null
                                ? nota.getFechaActualizacion().toString()
                                : null)
                .build();
    }
}
