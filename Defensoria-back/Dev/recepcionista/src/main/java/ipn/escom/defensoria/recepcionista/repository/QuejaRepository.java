package ipn.escom.defensoria.recepcionista.repository;

import ipn.escom.defensoria.recepcionista.entity.Queja;
import ipn.escom.defensoria.recepcionista.entity.EstadoQueja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuejaRepository extends JpaRepository<Queja, Long> {

    // Buscar por estado para el panel y la bandeja
    List<Queja> findByEstado(EstadoQueja estado);

    // CU33.1: Buscar antecedencia por correo
    List<Queja> findByCorreoPromovente(String correoPromovente);

    // CU33.2: Buscar antecedencia por boleta
    List<Queja> findByBoleta(String boleta);

    // Búsqueda opcional por folio para el CU de seguimiento
    Optional<Queja> findByFolio(String folio);
}