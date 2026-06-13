package ipn.escom.defensoria.administracion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AdministracionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdministracionApplication.class, args);
        System.out.println("----------------------------------------------");
        System.out.println("  Microservicio Administración - DDP - Iniciado");
        System.out.println("----------------------------------------------");
    }
}