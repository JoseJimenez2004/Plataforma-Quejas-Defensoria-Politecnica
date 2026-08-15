package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CrearRemisionDTO;
import ipn.escom.defensoria.primercontacto.dto.RemisionDTO;
import ipn.escom.defensoria.primercontacto.entity.RemisionExterna;
import ipn.escom.defensoria.primercontacto.repository.RemisionExternaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RemisionExternaService {

    private final RemisionExternaRepository remisionExternaRepository;
    private final PlataformaCentralClientService plataformaCentralClientService;

    public RemisionExternaService(
            RemisionExternaRepository remisionExternaRepository,
            PlataformaCentralClientService plataformaCentralClientService
    ) {
        this.remisionExternaRepository = remisionExternaRepository;
        this.plataformaCentralClientService = plataformaCentralClientService;
    }

    public RemisionDTO crearRemision(CrearRemisionDTO dto, String token) {

        if (remisionExternaRepository.existsByQuejaId(dto.getQuejaId())) {
            throw new RuntimeException("La queja ya cuenta con una remisión registrada");
        }

        RemisionExterna remision = RemisionExterna.builder()
                .quejaId(dto.getQuejaId())
                .folio(dto.getFolio())
                .analistaId(dto.getAnalistaId())
                .analistaNombre(dto.getAnalistaNombre())
                .autoridadRemision(dto.getAutoridadRemision())
                .justificacionLegal(dto.getJustificacionLegal())
                .sugerenciaQuejoso(dto.getSugerenciaQuejoso())
                .adjuntarExpediente(dto.getAdjuntarExpediente())
                .fechaRemision(LocalDateTime.now())
                .build();

        RemisionExterna guardada = remisionExternaRepository.save(remision);

        plataformaCentralClientService.actualizarEstatusQueja(
                dto.getQuejaId(),
                "REMITIDA",
                token
        );

        return convertirADTO(guardada);
    }

    public RemisionDTO obtenerPorQueja(Long quejaId) {
        return remisionExternaRepository.findByQuejaId(quejaId)
                .map(this::convertirADTO)
                .orElseThrow(() -> new RuntimeException("Remisión no encontrada"));
    }

    public RemisionDTO obtenerPorFolio(String folio) {
        return remisionExternaRepository.findByFolio(folio)
                .map(this::convertirADTO)
                .orElseThrow(() -> new RuntimeException("Remisión no encontrada"));
    }

    public RemisionDTO enviarRemision(Long quejaId, String token) {

        RemisionExterna remision = remisionExternaRepository.findByQuejaId(quejaId)
                .orElseThrow(() -> new RuntimeException("Remisión no encontrada"));

        plataformaCentralClientService.actualizarEstatusQueja(
                quejaId,
                "REMISION_ENVIADA",
                token
        );

        return convertirADTO(remision);
    }

    private RemisionDTO convertirADTO(RemisionExterna remision) {

        return RemisionDTO.builder()
                .id(remision.getId())
                .quejaId(remision.getQuejaId())
                .folio(remision.getFolio())
                .analistaId(remision.getAnalistaId())
                .analistaNombre(remision.getAnalistaNombre())
                .autoridadRemision(remision.getAutoridadRemision())
                .justificacionLegal(remision.getJustificacionLegal())
                .sugerenciaQuejoso(remision.getSugerenciaQuejoso())
                .adjuntarExpediente(remision.getAdjuntarExpediente())
                .fechaRemision(
                        remision.getFechaRemision() != null
                                ? remision.getFechaRemision().toString()
                                : null
                )
                .build();
    }
}