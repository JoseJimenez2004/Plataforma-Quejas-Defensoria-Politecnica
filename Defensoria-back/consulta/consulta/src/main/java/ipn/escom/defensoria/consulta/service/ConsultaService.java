package ipn.escom.defensoria.consulta.service;

import ipn.escom.defensoria.consulta.dto.ConsultaDTO;
import ipn.escom.defensoria.consulta.entity.ConsultaEntity;
import ipn.escom.defensoria.consulta.repository.ConsultaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConsultaService {
    @Autowired
    private ConsultaRepository repository;
    
    public ConsultaDTO buscarEstatusPorFolio(String folio){
        ConsultaEntity consulta = repository.findById(folio).orElseThrow(()->new RuntimeException("Folio no encontrado"));
        
        ConsultaDTO dto = new ConsultaDTO();
        dto.setFolio(consulta.getFolio());
        dto.setAsunto(consulta.getAsunto());
        dto.setFechaInicio(consulta.getFechaInicio());
        dto.setStatus(consulta.getStatus());
        
        switch (consulta.getStatus()) {
            case "RECIBIDA" -> dto.setProgreso(25);
            case "EN_REVISION" -> dto.setProgreso(50);
            case "EN_TRAMITE" -> dto.setProgreso(75);
            case "CONCLUIDA" -> dto.setProgreso(100);
            default -> dto.setProgreso(0);
        }

        return dto;
     
    }
}
