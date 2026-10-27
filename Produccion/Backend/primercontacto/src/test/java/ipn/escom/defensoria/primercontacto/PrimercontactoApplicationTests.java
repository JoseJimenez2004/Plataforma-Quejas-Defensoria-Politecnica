package ipn.escom.defensoria.primercontacto;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de INTEGRACIÓN: levanta todo el contexto de Spring y necesita una base de datos
 * real. Por eso está en el grupo "integracion" y no corre con "mvn test". Para correrla con
 * la BD de pruebas encendida: mvn test -Dgrupos.excluidos=none
 */
@Tag("integracion")
@SpringBootTest
class PrimercontactoApplicationTests {

	@Test
	void contextLoads() {
	}

}
