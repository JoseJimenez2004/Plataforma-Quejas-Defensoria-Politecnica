package ipn.escom.defensoria.notificaciones_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ipn.escom.defensoria.notificaciones_service.entity.Notificacion;
import ipn.escom.defensoria.notificaciones_service.exception.AccesoNotificacionDenegadoException;
import ipn.escom.defensoria.notificaciones_service.exception.DatosNotificacionInvalidosException;
import ipn.escom.defensoria.notificaciones_service.exception.NotificacionNoEncontradaException;
import ipn.escom.defensoria.notificaciones_service.model.RegistrarNotificacionRequest;
import ipn.escom.defensoria.notificaciones_service.repository.NotificacionRepository;
import lombok.SneakyThrows;

@Service
public class NotificacionService {

    private static final String TIPO_POR_DEFECTO = "GENERAL";

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @SneakyThrows
    public Notificacion registrar(RegistrarNotificacionRequest datos) {
        if (esVacio(datos.getCorreoDestino()) || esVacio(datos.getTitulo())) {
            throw new DatosNotificacionInvalidosException("Faltan datos obligatorios de la notificación.");
        }
        Notificacion notificacion = new Notificacion();
        notificacion.setCorreoDestino(datos.getCorreoDestino());
        notificacion.setTipo(esVacio(datos.getTipo()) ? TIPO_POR_DEFECTO : datos.getTipo());
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

    @SneakyThrows
    public Notificacion marcarLeida(Long id, String correo) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new NotificacionNoEncontradaException("No se encontró la notificación indicada."));
        if (!notificacion.getCorreoDestino().equalsIgnoreCase(correo)) {
            throw new AccesoNotificacionDenegadoException("Esa notificación no corresponde a tu cuenta.");
        }
        notificacion.setLeida(true);
        return notificacionRepository.save(notificacion);
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
