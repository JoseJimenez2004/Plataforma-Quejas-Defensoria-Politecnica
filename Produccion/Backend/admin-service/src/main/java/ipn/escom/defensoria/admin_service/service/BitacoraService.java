package ipn.escom.defensoria.admin_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ipn.escom.defensoria.admin_service.entity.BitacoraAccion;
import ipn.escom.defensoria.admin_service.repository.BitacoraAccionRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class BitacoraService {

    private static final String IP_DESCONOCIDA = "desconocida";

    private final BitacoraAccionRepository repository;

    public BitacoraService(BitacoraAccionRepository repository) {
        this.repository = repository;
    }

    public void registrar(String usuario, String accion, HttpServletRequest request) {
        String ip = (request != null) ? ipCliente(request) : IP_DESCONOCIDA;
        String texto = accion != null && accion.length() > 250 ? accion.substring(0, 250) : accion;
        repository.save(new BitacoraAccion(usuario, texto, ip));
    }

    /** Detrás de router-nginx getRemoteAddr() siempre es la IP del servidor frontend; la del
     * cliente real llega en X-Real-IP / X-Forwarded-For (los pone nginx). */
    private static String ipCliente(HttpServletRequest request) {
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public List<BitacoraAccion> listarRecientes() {
        return repository.findTop50ByOrderByFechaDesc();
    }
}
