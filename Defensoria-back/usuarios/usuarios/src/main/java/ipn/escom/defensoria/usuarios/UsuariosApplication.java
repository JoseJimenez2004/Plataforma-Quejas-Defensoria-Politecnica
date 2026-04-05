package ipn.escom.defensoria.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

// ESTO EXCLUYE LA SEGURIDAD POR COMPLETO
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class }) 
public class UsuariosApplication {
    public static void main(String[] args) {
        SpringApplication.run(UsuariosApplication.class, args);
    }
}