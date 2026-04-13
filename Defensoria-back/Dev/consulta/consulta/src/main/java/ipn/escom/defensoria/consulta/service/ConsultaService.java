package ipn.escom.defensoria.consulta.service;

import ipn.escom.defensoria.consulta.dto.ConsultaDTO;
import ipn.escom.defensoria.consulta.entity.ConsultaEntity;
import ipn.escom.defensoria.consulta.entity.HitoEntity;
import ipn.escom.defensoria.consulta.repository.ConsultaRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
        
        
        if (consulta.getEvidencias() != null && !consulta.getEvidencias().isEmpty()) {
        dto.setListaEvidencias(Arrays.asList(consulta.getEvidencias().split(",")));
        } else {
            dto.setListaEvidencias(new ArrayList<>());
        }
        
        switch (consulta.getStatus()) {
            case "RECIBIDA" -> dto.setProgreso(25);
            case "EN_REVISION" -> dto.setProgreso(50);
            case "EN_TRAMITE" -> dto.setProgreso(75);
            case "CONCLUIDA" -> dto.setProgreso(100);
            default -> dto.setProgreso(0);
        }
        
        return dto;
     
    }
    public void registrarHito(String folio, String mensaje) {
    ConsultaEntity queja = repository.findById(folio)
        .orElseThrow(() -> new RuntimeException("Queja no encontrada"));

    HitoEntity hito = new HitoEntity();
    hito.setDescripcion(mensaje);
    hito.setFechaHito(LocalDateTime.now());
    hito.setConsulta(queja);

    queja.getHistorial().add(hito);
    repository.save(queja);
}
}
