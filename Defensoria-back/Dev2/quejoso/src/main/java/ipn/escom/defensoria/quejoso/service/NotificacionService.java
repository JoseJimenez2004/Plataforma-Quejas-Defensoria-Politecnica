package ipn.escom.defensoria.quejoso.service;

import ipn.escom.defensoria.quejoso.dto.NotificacionDTO;
import ipn.escom.defensoria.quejoso.entity.NotificacionEntity;
import ipn.escom.defensoria.quejoso.repository.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    // Este método sería llamado por un Listener de RabbitMQ o Kafka
    // cuando el micro de defensores envíe una notificación
    public void procesarNotificacionExterna(NotificacionEntity nueva) {
        notificacionRepository.save(nueva);
    }

    public List<NotificacionDTO> obtenerPorUsuario(Long usuarioId) {
        // Solo traemos las que no han expirado
        return notificacionRepository.findByUsuarioIdAndFechaExpiracionAfterOrderByFechaCreacionDesc(usuarioId, LocalDateTime.now())
                .stream()
                .map(n -> NotificacionDTO.builder()
                        .id(n.getId())
                        .titulo(n.getTitulo())
                        .mensaje(n.getMensaje())
                        .leida(n.isLeida())
                        .tipo(n.getTipo())
                        .folioRelacionado(n.getFolioQueja())
                        .build())
                .collect(Collectors.toList());
    }

    public void marcarComoLeida(Long id) {
        NotificacionEntity n = notificacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada con ID: " + id));

        n.setLeida(true);
        notificacionRepository.save(n);
    }
}