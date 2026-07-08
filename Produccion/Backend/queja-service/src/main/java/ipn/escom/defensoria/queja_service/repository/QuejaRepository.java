package ipn.escom.defensoria.queja_service.repository;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ipn.escom.defensoria.queja_service.entity.Queja;

@Repository
public interface QuejaRepository extends JpaRepository<Queja, Long> {
    Optional<Queja> findByNumeroFolioAndCorreoInstitucional(String numeroFolio, String correoInstitucional);
}