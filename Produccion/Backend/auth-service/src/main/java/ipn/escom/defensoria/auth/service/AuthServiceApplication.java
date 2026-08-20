package ipn.escom.defensoria.auth.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** @EnableFeignClients es obligatorio aquí: QuejasClient y NotificacionesClient (paquete
 * client/) son interfaces @FeignClient -- sin esta anotación Spring nunca las registra como
 * beans y falla al arrancar con "No qualifying bean". */
@SpringBootApplication
@EnableFeignClients
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
