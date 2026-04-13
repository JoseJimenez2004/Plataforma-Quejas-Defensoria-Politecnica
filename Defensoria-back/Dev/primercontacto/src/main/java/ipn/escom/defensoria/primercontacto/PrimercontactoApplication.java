package ipn.escom.defensoria.primercontacto;

import ipn.escom.defensoria.primercontacto.entity.Usuario;
import ipn.escom.defensoria.primercontacto.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PrimercontactoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrimercontactoApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                // --- NIVEL 3: SUBDEFENSORES (Acceso Total) ---
                repository.save(new Usuario(null, "bryan.subdefensor@ipn.mx", "admin123", "Mtro. Bryan Jimenez", "SUBDEFENSOR", "NIVEL_3"));
                repository.save(new Usuario(null, "omar.subdefensor@ipn.mx", "admin123", "Mtra. Omar Velazquez", "SUBDEFENSOR", "NIVEL_3"));

                // --- NIVEL 2: ABOGADOS DICTAMINADORES (Análisis y Dictamen) ---
                repository.save(new Usuario(null, "antonio.abogado@ipn.mx", "abogado123", "Lic. Antonio Bicho", "ABOGADO_DICTAMINADOR", "NIVEL_2"));
                repository.save(new Usuario(null, "fernanda.abogada@ipn.mx", "abogado123", "Lic. Fernanda Ramos", "ABOGADO_DICTAMINADOR", "NIVEL_2"));
                repository.save(new Usuario(null, "carlos.abogado@ipn.mx", "abogado123", "Lic. Carlos Ruiz", "ABOGADO_DICTAMINADOR", "NIVEL_2"));
                repository.save(new Usuario(null, "lucia.abogada@ipn.mx", "abogado123", "Lic. Lucia Mendez", "ABOGADO_DICTAMINADOR", "NIVEL_2"));

                // --- NIVEL 1: AUXILIARES / PRIMER CONTACTO (Citas y Recepción) ---
                repository.save(new Usuario(null, "auxiliar1@ipn.mx", "aux123", "Juanito Perez", "AUXILIAR", "NIVEL_1"));
                repository.save(new Usuario(null, "auxiliar2@ipn.mx", "aux123", "Maria Sodi", "AUXILIAR", "NIVEL_1"));
                repository.save(new Usuario(null, "recepcion.celex@ipn.mx", "recep123", "Admin CELEX", "AUXILIAR", "NIVEL_1"));
                repository.save(new Usuario(null, "ventanilla.escom@ipn.mx", "escom123", "Control Escolar ESCOM", "AUXILIAR", "NIVEL_1"));

                System.out.println("---------------------------------------------------------");
                System.out.println("LOG: 10 Usuarios (Abogados y Staff) cargados con éxito.");
                System.out.println("---------------------------------------------------------");
            }
        };
    }
}