package ipn.escom.defensoria.quejoso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuejosoApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuejosoApplication.class, args);
		System.out.println("----------------------------------------------");
		System.out.println("  Microservicio Quejoso - DDP - Iniciado      ");
		System.out.println("----------------------------------------------");
	}
}