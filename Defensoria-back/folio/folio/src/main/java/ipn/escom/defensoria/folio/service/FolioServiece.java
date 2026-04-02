package ipn.escom.defensoria.folio.service;

import ipn.escom.defensoria.folio.entity.FolioEntity;
import ipn.escom.defensoria.folio.repository.FolioRepository;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate; 
import java.util.HashMap; 
import java.util.Map;

@Service
public class FolioServiece {
    
    @Autowired
    private FolioRepository repository;
    
    @Autowired
    private RestTemplate restTemplate;
    public String generadorFolio(){
        int yearActual = LocalDate.now().getYear();
        String prefijoBusqueda = "DDP" + yearActual;
        
        long totalPorYear = repository.countByCodigoFolioStartingWith(prefijoBusqueda);
        long siguienteFolio = totalPorYear+1;
        
        String nuevoFolio= String.format("%s-%04d", prefijoBusqueda, siguienteFolio);
        FolioEntity entidad = new FolioEntity();
        entidad.setCodigoFolio(nuevoFolio);
        entidad.setFechaCreacion(LocalDateTime.now());
        repository.save(entidad);
        
        try {
            // Preparamos los datos para el microservicio de CONSULTA
            Map<String, Object> datosQueja = new HashMap<>();
            datosQueja.put("folio", nuevoFolio);
            datosQueja.put("asunto", "Queja registrada automáticamente");
            datosQueja.put("fechaInicio", LocalDate.now().toString());
            datosQueja.put("status", "RECIBIDA");

            // Enviamos los datos al puerto 8081
            restTemplate.postForObject("http://localhost:8081/api/consulta/crear", datosQueja, String.class);
        } catch (Exception e) {
            // Si el microservicio de consulta está apagado, imprimimos el error
            System.err.println("Error al sincronizar con Microservicio de Consulta: " + e.getMessage());
        }
        
        return nuevoFolio;
    }
}
