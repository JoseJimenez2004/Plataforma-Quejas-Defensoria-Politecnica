package ipn.escom.defensoria.catalogo_service.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.catalogo_service.entity.Dependencia;
import ipn.escom.defensoria.catalogo_service.repository.DependenciaRepository;

@Service
public class DependenciaService {

    @Autowired
    private DependenciaRepository dependenciaRepository;

    public List<Dependencia> listarActivas() {
        return dependenciaRepository.findByActivoTrueOrderByNombreAsc();
    }

    public List<Dependencia> listarActivasPorTipo(String tipo) {
        return dependenciaRepository.findByActivoTrueAndTipoOrderByNombreAsc(tipo);
    }

    public Optional<Dependencia> buscarPorClave(String clave) {
        return dependenciaRepository.findByClave(clave);
    }
}
