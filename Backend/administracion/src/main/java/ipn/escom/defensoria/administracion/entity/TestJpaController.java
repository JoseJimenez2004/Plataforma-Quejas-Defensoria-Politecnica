package ipn.escom.defensoria.administracion.entity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class TestJpaController {

    
    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/test-db")
    public ResponseEntity<String> testJpaConnection() {
        try {
            Object result = entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return ResponseEntity.ok("Conexión exitosa a Neon DB vía JPA. Resultado: " + result.toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error en la capa JPA: " + e.getMessage());
        }
    }
}