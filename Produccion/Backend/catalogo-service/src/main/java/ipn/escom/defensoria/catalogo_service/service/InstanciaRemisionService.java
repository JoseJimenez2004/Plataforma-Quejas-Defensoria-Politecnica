package ipn.escom.defensoria.catalogo_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ipn.escom.defensoria.catalogo_service.entity.InstanciaRemision;
import ipn.escom.defensoria.catalogo_service.repository.InstanciaRemisionRepository;

/**
 * Instancias de remisión de Primer Contacto. Son fijas (la Defensora solo cambia el correo);
 * al arrancar se crean las que falten, sin tocar el correo de las que ya existen.
 */
@Service
public class InstanciaRemisionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InstanciaRemisionService.class);
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /** Correo inicial de las tres (de momento, el de pruebas del equipo). */
    static final String CORREO_INICIAL = "leishasr2@gmail.com";

    private static final String[][] INICIALES = {
        {"OAG", "OAG", "Oficina del Abogado General"},
        {"OIC", "OIC", "Órgano Interno de Control en el IPN"},
        {"COSECOVI", "COSECOVI / Género",
            "Comité de Seguridad y Contra la Violencia o instancias del Protocolo para la Prevención, "
                + "Detección, Atención y Sanción de la Violencia de Género"}
    };

    private final InstanciaRemisionRepository repository;

    public InstanciaRemisionService(InstanciaRemisionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            for (int i = 0; i < INICIALES.length; i++) {
                String[] fila = INICIALES[i];
                if (repository.existsById(fila[0])) continue;
                InstanciaRemision instancia = new InstanciaRemision();
                instancia.setClave(fila[0]);
                instancia.setSigla(fila[1]);
                instancia.setNombre(fila[2]);
                instancia.setCorreo(CORREO_INICIAL);
                instancia.setOrden(i + 1);
                instancia.setFechaActualizacion(LocalDateTime.now());
                instancia.setActualizadoPor("Carga inicial");
                repository.save(instancia);
                log.info("Instancia de remisión {} creada", fila[0]);
            }
        } catch (Exception ex) {
            // Que una falla aquí no tumbe el catálogo (lo usa el formulario de quejas).
            log.error("No se pudieron crear las instancias de remisión: {}", ex.getMessage());
        }
    }

    public List<InstanciaRemision> listar() {
        return repository.findAllByOrderByOrdenAsc();
    }

    @Transactional
    public InstanciaRemision cambiarCorreo(String clave, String correo, String quien) {
        InstanciaRemision instancia = repository.findById(clave)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la instancia " + clave));
        String limpio = correo == null ? "" : correo.trim();
        if (!CORREO.matcher(limpio).matches() || limpio.length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe un correo válido.");
        }
        instancia.setCorreo(limpio);
        instancia.setFechaActualizacion(LocalDateTime.now());
        instancia.setActualizadoPor(quien);
        return repository.save(instancia);
    }
}
