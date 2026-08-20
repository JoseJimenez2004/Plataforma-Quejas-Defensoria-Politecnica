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
        String ip = (request != null) ? request.getRemoteAddr() : IP_DESCONOCIDA;
        repository.save(new BitacoraAccion(usuario, accion, ip));
    }

    public List<BitacoraAccion> listarRecientes() {
        return repository.findTop50ByOrderByFechaDesc();
    }
}
