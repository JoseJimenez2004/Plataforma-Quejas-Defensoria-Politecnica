package ipn.escom.defensoria.admin_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.admin_service.entity.BitacoraAccion;
import ipn.escom.defensoria.admin_service.repository.BitacoraAccionRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class BitacoraService {

    @Autowired
    private BitacoraAccionRepository repository;

    public void registrar(String usuario, String accion, HttpServletRequest request) {
        String ip = (request != null) ? request.getRemoteAddr() : "desconocida";
        repository.save(new BitacoraAccion(usuario, accion, ip));
    }

    public List<BitacoraAccion> listarRecientes() {
        return repository.findTop50ByOrderByFechaDesc();
    }
}
