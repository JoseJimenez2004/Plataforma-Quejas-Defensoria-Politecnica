package ipn.escom.defensoria.recepcionista;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal del microservicio Recepcionista.
 *
 * Módulo: Gestión de Quejas - Rol Recepcionista
 * Puerto: 8083
 * Base de datos: PostgreSQL (defensoria)
 *
 * Este microservicio cubre las funciones de la recepcionista de la
 * Defensoría de Derechos Politécnicos (DDP) del IPN:
 *
 *   - Dashboard de Recepción: bandeja de quejas pendientes (estatus RECIBIDA)
 *   - Validación de Requisitos: revisar detalle y documentación de una queja
 *   - Aceptar queja → pasa a EN_REVISION para ser atendida por un abogado
 *   - Rechazar queja → registra motivo y cierra como ATENDIDA
 *   - Notificar rechazo → registra notificación al quejoso (futura integración email)
 *   - Búsqueda de Antecedentes → consulta quejas previas de un quejoso por nombre
 *   - Canalización → registra la opción de canalización elegida
 *
 * Arquitectura: Spring Boot 4.0.5 / Java 21 / PostgreSQL / Maven
 * Patrón: Modelo 3 capas (Controller → Service → Repository)
 *
 * @SpringBootApplication activa el escaneo automático de componentes
 * (@RestController, @Service, @Repository) en el paquete base
 * y todos sus subpaquetes.
 */
@SpringBootApplication
public class RecepcionistaApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecepcionistaApplication.class, args);
	}
}
