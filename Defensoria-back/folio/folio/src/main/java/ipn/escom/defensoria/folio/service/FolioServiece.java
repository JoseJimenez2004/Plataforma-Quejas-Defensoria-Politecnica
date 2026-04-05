package ipn.escom.defensoria.folio.service;

import ipn.escom.defensoria.folio.dto.FolioDTO; 
import ipn.escom.defensoria.folio.entity.FolioEntity;
import ipn.escom.defensoria.folio.repository.FolioRepository;
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

    // AHORA RECIBE EL DTO CON LOS DATOS DEL FORMULARIO
    public String generadorFolio(FolioDTO formulario) {
        int yearActual = LocalDate.now().getYear();
        String prefijoBusqueda = "DDP" + yearActual;
        
        long totalPorYear = repository.countByCodigoFolioStartingWith(prefijoBusqueda);
        long siguienteFolio = totalPorYear + 1;
        
        String nuevoFolio = String.format("%s-%04d", prefijoBusqueda, siguienteFolio);
        
        // 1. MAPEAMOS LOS DATOS DEL FORMULARIO A LA ENTIDAD
        FolioEntity entidad = new FolioEntity();
        entidad.setCodigoFolio(nuevoFolio);
        entidad.setFechaCreacion(LocalDateTime.now());
        
        // Datos de la queja
        entidad.setAsunto(formulario.getAsunto());
        entidad.setFechaHechos(formulario.getFechaHechos()); // <--- FALTABA
        entidad.setDescripcion(formulario.getDescripcion());
        entidad.setUnidad(formulario.getUnidad());
        
        // Datos del quejoso
        entidad.setNombre(formulario.getNombre());
        entidad.setPrimerApellido(formulario.getPrimerApellido());
        entidad.setSegundoApellido(formulario.getSegundoApellido());
        entidad.setCorreo(formulario.getCorreo());
        entidad.setFechaNacimiento(formulario.getFechaNacimiento()); // <--- FALTABA
        entidad.setBoleta(formulario.getBoleta()); // <--- FALTABA
        
        // Datos del tutor (Modal)
        entidad.setNombreTutor(formulario.getNombreTutor());
        entidad.setPrimerApellidoTutor(formulario.getPrimerApellidoTutor()); // <--- FALTABA
        entidad.setSegundoApellidoTutor(formulario.getSegundoApellidoTutor()); // <--- FALTABA
        entidad.setParentesco(formulario.getParentesco());
        entidad.setCorreoTutor(formulario.getCorreoTutor()); // <--- FALTABA
        entidad.setTelefonoTutor(formulario.getTelefonoTutor()); // <--- FALTABA
        
        // Guardamos todo en la BD de Folios
        repository.save(entidad);
        
        // Guardamos todo en la BD de Folios
        repository.save(entidad);
        
        // 2. SINCRONIZAMOS CON EL MICROSERVICIO DE CONSULTA
        try {
            Map<String, Object> datosQueja = new HashMap<>();
            datosQueja.put("folio", nuevoFolio);
            // Ahora enviamos el ASUNTO REAL que escribió el usuario
            datosQueja.put("asunto", formulario.getAsunto()); 
            datosQueja.put("fechaInicio", LocalDate.now().toString());
            datosQueja.put("status", "RECIBIDA");

            restTemplate.postForObject("http://localhost:8081/api/consulta/crear", datosQueja, String.class);
        } catch (Exception e) {
            System.err.println("Error al sincronizar con Microservicio de Consulta: " + e.getMessage());
        }
        
        if (formulario.isCrearCuenta()) {
            try {
                Map<String, Object> datosUsuario = new HashMap<>();
                datosUsuario.put("correo", formulario.getCorreo());
                datosUsuario.put("password", formulario.getPassword());
                datosUsuario.put("nombre", formulario.getNombre());
                datosUsuario.put("primerApellido", formulario.getPrimerApellido());
                datosUsuario.put("segundoApellido", formulario.getSegundoApellido());
                datosUsuario.put("boleta", formulario.getBoleta());
                datosUsuario.put("folioVinculado", nuevoFolio);

                // Llamamos al microservicio de Usuarios (8082)
                restTemplate.postForObject("http://localhost:8082/api/usuarios/registro", datosUsuario, String.class);
                System.out.println("Cuenta de usuario creada exitosamente");
            } catch (Exception e) {
                System.err.println("Error al crear cuenta: " + e.getMessage());
            }
        }
        
        return nuevoFolio;
    }
}