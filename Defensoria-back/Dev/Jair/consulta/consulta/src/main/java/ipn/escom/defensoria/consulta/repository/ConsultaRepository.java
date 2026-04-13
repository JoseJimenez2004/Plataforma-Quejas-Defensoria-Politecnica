package ipn.escom.defensoria.consulta.repository;

import ipn.escom.defensoria.consulta.entity.ConsultaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultaRepository extends JpaRepository<ConsultaEntity, String>{
}