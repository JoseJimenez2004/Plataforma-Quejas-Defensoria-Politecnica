package ipn.escom.defensoria.chatbot_service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ipn.escom.defensoria.chatbot_service.entity.PreguntaChatbot;

@Repository
public interface PreguntaChatbotRepository extends JpaRepository<PreguntaChatbot, Long> {

    List<PreguntaChatbot> findByActivoTrueOrderByOrdenAsc();

    List<PreguntaChatbot> findByActivoTrueAndCategoriaOrderByOrdenAsc(String categoria);

    /** Lista TODAS (activas e inactivas) -- usado por el CRUD de administración. */
    List<PreguntaChatbot> findAllByOrderByOrdenAsc();
}
