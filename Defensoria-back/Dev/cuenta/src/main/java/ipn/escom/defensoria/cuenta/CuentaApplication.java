package ipn.escom.defensoria.cuenta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class })
public class CuentaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CuentaApplication.class, args);
		
		System.out.println("====================================================");
		System.out.println("Microservicio de CUENTA levantado con éxito");
		System.out.println("Endpoint: http://localhost:8080/api/usuarios");
		System.out.println("H2 Console: http://localhost:8080/h2-console");
		System.out.println("====================================================");
	}

}