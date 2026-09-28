package ipn.escom.defensoria.primercontacto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: revisión periódica de acuerdos de conciliación aceptados
// (ConciliacionPrimerContactoService.revisarRespuestas).
@SpringBootApplication
@EnableScheduling
public class PrimercontactoApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrimercontactoApplication.class, args);
	}
}
