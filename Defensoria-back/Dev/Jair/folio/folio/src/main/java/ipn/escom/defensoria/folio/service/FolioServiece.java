package ipn.escom.defensoria.folio.service;

import ipn.escom.defensoria.folio.dto.FolioDTO; 
import ipn.escom.defensoria.folio.entity.FolioEntity;
import ipn.escom.defensoria.folio.entity.EvidenciaEntity; 
import ipn.escom.defensoria.folio.repository.FolioRepository;
import ipn.escom.defensoria.folio.repository.EvidenciaRepository; 
import ipn.escom.defensoria.folio.service.PdfService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate; 
import org.springframework.web.multipart.MultipartFile; 
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap; 
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FolioServiece { 
  
    @Autowired
    private FolioRepository repository;

    @Autowired
    private EvidenciaRepository evidenciaRepository;
    
    @Autowired
    private RestTemplate restTemplate;

    
    
    public String generadorFolio(FolioDTO formulario, List<MultipartFile> archivos) {
        int yearActual = LocalDate.now().getYear();
        String prefijoBusqueda = "DDP" + yearActual;
        
        long totalPorYear = repository.countByCodigoFolioStartingWith(prefijoBusqueda);
        long siguienteFolio = totalPorYear + 1;
        String nuevoFolio = String.format("%s-%04d", prefijoBusqueda, siguienteFolio);

        FolioEntity entidad = new FolioEntity();
        entidad.setCodigoFolio(nuevoFolio);
        entidad.setFechaCreacion(LocalDateTime.now());
        
        // Datos de la queja
        entidad.setAsunto(formulario.getAsunto());
        entidad.setFechaHechos(formulario.getFechaHechos());
        entidad.setDescripcion(formulario.getDescripcion());
        entidad.setUnidad(formulario.getUnidad());
        
        // Datos del quejoso
        entidad.setNombre(formulario.getNombre());
        entidad.setPrimerApellido(formulario.getPrimerApellido());
        entidad.setSegundoApellido(formulario.getSegundoApellido());
        entidad.setCorreo(formulario.getCorreo());
        entidad.setFechaNacimiento(formulario.getFechaNacimiento());
        entidad.setBoleta(formulario.getBoleta());
        
        // Datos del tutor
        entidad.setNombreTutor(formulario.getNombreTutor());
        entidad.setPrimerApellidoTutor(formulario.getPrimerApellidoTutor());
        entidad.setSegundoApellidoTutor(formulario.getSegundoApellidoTutor());
        entidad.setParentesco(formulario.getParentesco());
        entidad.setCorreoTutor(formulario.getCorreoTutor());
        entidad.setTelefonoTutor(formulario.getTelefonoTutor());
        
        // Guardamos el Folio primero para generar su ID
        FolioEntity folioGuardado = repository.save(entidad);
        
        // Variable para acumular los nombres de archivos para el Microservicio de Consulta
        String nombresParaConsulta = ""; 

        if (archivos != null && !archivos.isEmpty()) {
            String folder = "uploads/";
            try {
                Files.createDirectories(Paths.get(folder));
                List<String> listaNombres = new ArrayList<>(); // Lista temporal

                for (MultipartFile archivo : archivos) {
                    if (!archivo.isEmpty()) {
                        String nombreArchivo = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
                        Path ruta = Paths.get(folder + nombreArchivo);
                        Files.write(ruta, archivo.getBytes());

                        EvidenciaEntity evidencia = new EvidenciaEntity();
                        evidencia.setNombreArchivo(archivo.getOriginalFilename());
                        evidencia.setTipoArchivo(archivo.getContentType());
                        evidencia.setCodigoFolio(nuevoFolio);
                        evidencia.setRutaAlmacenamiento(ruta.toString());
                        evidencia.setFolio(folioGuardado);

                        evidenciaRepository.save(evidencia);

                        listaNombres.add(archivo.getOriginalFilename());
                    }
                }
                // Convertimos la lista a un String separado por comas: "foto.jpg,doc.pdf"
                nombresParaConsulta = String.join(",", listaNombres);

            } catch (IOException e) {
                System.err.println("Error al guardar archivos de evidencia: " + e.getMessage());
            }
        }
        
        try {
            Map<String, Object> datosQueja = new HashMap<>();
            datosQueja.put("folio", nuevoFolio);
            datosQueja.put("asunto", formulario.getAsunto()); 
            datosQueja.put("fechaInicio", LocalDate.now().toString());
            datosQueja.put("status", "RECIBIDA");
            
            datosQueja.put("evidencias", nombresParaConsulta);
            
            restTemplate.postForObject("http://localhost:8081/api/consulta/crear", datosQueja, String.class);
        } catch (Exception e) {
            System.err.println("Error al sincronizar con Microservicio de Consulta: " + e.getMessage());
        }
        
        // Lógica de creación de cuenta 
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
        try {
            restTemplate.postForObject("http://localhost:8082/api/usuarios/vincular?correo=" + formulario.getCorreo() + "&folio=" + nuevoFolio, null, String.class);
        } catch (Exception e) {
            System.err.println("Error crítico al vincular: " + e.getMessage());
        }
        
        return nuevoFolio;
    }
    
    public FolioEntity buscarPorCodigo(String codigo) {
        return repository.findByCodigoFolio(codigo)
                .orElseThrow(() -> new RuntimeException("Folio no encontrado: " + codigo));
    }
    
    public String guardarEvidenciasExtra(String codigoFolio, List<MultipartFile> archivos) {
    // Buscamos el folio existente
    FolioEntity folio = repository.findByCodigoFolio(codigoFolio)
            .orElseThrow(() -> new RuntimeException("Folio no encontrado"));

    List<String> nuevosNombres = new ArrayList<>();
    String folder = "uploads/";

    try {
        for (MultipartFile archivo : archivos) {
            if (!archivo.isEmpty()) {
                // Nombre único
                String nombreUnico = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
                Path ruta = Paths.get(folder + nombreUnico);
                Files.write(ruta, archivo.getBytes());

                // Guardar en BD de Folios
                EvidenciaEntity evidencia = new EvidenciaEntity();
                evidencia.setNombreArchivo(archivo.getOriginalFilename());
                evidencia.setCodigoFolio(codigoFolio);
                evidencia.setTipoArchivo(archivo.getContentType());
                evidencia.setRutaAlmacenamiento(ruta.toString());
                evidencia.setFolio(folio);
                evidenciaRepository.save(evidencia);

                nuevosNombres.add(archivo.getOriginalFilename());
            }
        }

        // Sincronizamos con el Microservicio de Consulta (8081)
        if (!nuevosNombres.isEmpty()) {
            String nombresCSV = String.join(",", nuevosNombres);
            restTemplate.postForObject(
                "http://localhost:8081/api/consulta/actualizar-evidencias?folio=" + codigoFolio, 
                nombresCSV, 
                String.class
            );
        }

    } catch (IOException e) {
        return "Error al procesar archivos: " + e.getMessage();
    }
    return "Evidencias agregadas correctamente al folio " + codigoFolio;
}
}