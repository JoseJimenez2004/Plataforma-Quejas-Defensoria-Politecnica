package ipn.escom.defensoria.queja_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.queja_service.dto.RespuestaConciliacionRequest;
import ipn.escom.defensoria.queja_service.entity.AcuerdoConciliacion;
import ipn.escom.defensoria.queja_service.repository.AcuerdoConciliacionRepository;

@Service
public class ConciliacionService {

    @Autowired
    private AcuerdoConciliacionRepository acuerdoConciliacionRepository;

    /** Todos los acuerdos de conciliación dirigidos al usuario autenticado (a través de
     * cualquiera de sus quejas), más reciente primero. */
    public List<AcuerdoConciliacion> listarMisAcuerdos(String correo) {
        return acuerdoConciliacionRepository.findByCorreoInstitucionalOrderByFechaEmisionDesc(correo);
    }

    /** El quejoso acepta o rechaza un acuerdo que le fue propuesto -- solo si es suyo y sigue
     * pendiente (una vez respondido, la decisión es definitiva). */
    public AcuerdoConciliacion responder(Long id, String correo, RespuestaConciliacionRequest datos) {
        AcuerdoConciliacion acuerdo = acuerdoConciliacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el acuerdo de conciliación indicado."));

        if (!acuerdo.getCorreoInstitucional().equalsIgnoreCase(correo)) {
            throw new RuntimeException("Ese acuerdo no corresponde a tu cuenta.");
        }
        if (!"PENDIENTE".equalsIgnoreCase(acuerdo.getEstado())) {
            throw new RuntimeException("Este acuerdo ya fue respondido anteriormente.");
        }
        if (!"ACEPTADO".equalsIgnoreCase(datos.getEstado()) && !"RECHAZADO".equalsIgnoreCase(datos.getEstado())) {
            throw new RuntimeException("La respuesta debe ser ACEPTADO o RECHAZADO.");
        }

        acuerdo.setEstado(datos.getEstado().toUpperCase());
        acuerdo.setComentarioQuejoso(datos.getComentario());
        acuerdo.setFechaRespuesta(LocalDateTime.now());

        return acuerdoConciliacionRepository.save(acuerdo);
    }
}
