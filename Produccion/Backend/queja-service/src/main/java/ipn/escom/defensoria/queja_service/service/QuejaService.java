package ipn.escom.defensoria.queja_service.service;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ipn.escom.defensoria.queja_service.entity.Queja;
import ipn.escom.defensoria.queja_service.repository.QuejaRepository;

@Service
public class QuejaService {

    @Autowired
    private QuejaRepository quejaRepository;

    @Value("${storage.location}")
    private String storageLocation;

    public boolean validarFolioYCorreo(String folio, String correo) {
        return quejaRepository.findByNumeroFolioAndCorreoInstitucional(folio, correo).isPresent();
    }

    public Queja registrarQueja(String motivo, String descripcion, String correo, MultipartFile archivo) {
        Queja queja = new Queja();
        queja.setNumeroFolio("FOL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        queja.setCorreoInstitucional(correo);
        queja.setMotivo(motivo);
        queja.setDescripcion(descripcion);

        if (archivo != null && !archivo.isEmpty()) {
            queja.setRutaEvidencia(guardarArchivo(archivo, queja.getNumeroFolio()));
        }

        return quejaRepository.save(queja);
    }

    private String guardarArchivo(MultipartFile archivo, String folio) {
        try {
            Path root = Paths.get(storageLocation);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
            String nombreArchivo = folio + "_" + archivo.getOriginalFilename();
            Path destino = root.resolve(nombreArchivo);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return destino.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la evidencia física: " + e.getMessage());
        }
    }
}