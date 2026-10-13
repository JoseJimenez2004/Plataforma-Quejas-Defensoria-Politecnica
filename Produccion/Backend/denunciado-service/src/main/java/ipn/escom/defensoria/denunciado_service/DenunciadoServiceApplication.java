package ipn.escom.defensoria.denunciado_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Respuesta del denunciado: la persona señalada en una queja captura sus datos, su versión
 * de los hechos, su credencial y sus evidencias. Comparte defensoria_db y jwt.secret con el
 * resto de los microservicios.
 */
@SpringBootApplication
public class DenunciadoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DenunciadoServiceApplication.class, args);
    }
}
