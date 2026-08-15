package ipn.escom.defensoria.notificaciones_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.notificaciones_service.entity.Notificacion;
import ipn.escom.defensoria.notificaciones_service.model.RegistrarNotificacionRequest;
import ipn.escom.defensoria.notificaciones_service.repository.NotificacionRepository;

@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    public Notificacion registrar(RegistrarNotificacionRequest datos) {
        if (esVacio(datos.getCorreoDestino()) || esVacio(datos.getTitulo())) {
            throw new RuntimeException("Faltan datos obligatorios de la notificación.");
        }
        Notificacion notificacion = new Notificacion();
        notificacion.setCorreoDestino(datos.getCorreoDestino());
        notificacion.setTipo(esVacio(datos.getTipo()) ? "GENERAL" : datos.getTipo());
        notificacion.setTitulo(datos.getTitulo());
        notificacion.setMensaje(datos.getMensaje());
        notificacion.setEnlace(datos.getEnlace());
        return notificacionRepository.save(notificacion);
    }

    public List<Notificacion> listarMias(String correo) {
        return notificacionRepository.findByCorreoDestinoOrderByFechaCreacionDesc(correo);
    }

    public long contarNoLeidas(String correo) {
        return notificacionRepository.countByCorreoDestinoAndLeidaFalse(correo);
    }

    public Notificacion marcarLeida(Long id, String correo) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la notificación indicada."));
        if (!notificacion.getCorreoDestino().equalsIgnoreCase(correo)) {
            throw new RuntimeException("Esa notificación no corresponde a tu cuenta.");
        }
        notificacion.setLeida(true);
        return notificacionRepository.save(notificacion);
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
