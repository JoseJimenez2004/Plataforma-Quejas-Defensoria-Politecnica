package ipn.escom.defensoria.auth.service.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Registra un aviso persistido en el centro de notificaciones del quejoso (POST /registrar
 * de notificaciones-service, NO manda correo, solo queda guardado para el panel) -- mismo
 * patrón que QuejasClient. */
@FeignClient(name = "notificaciones-service", url = "${notificaciones.service.url:http://localhost:8085}")
public interface NotificacionesClient {

    @PostMapping("/api/notificaciones/registrar")
    void registrar(@RequestBody Map<String, String> datos);
}
