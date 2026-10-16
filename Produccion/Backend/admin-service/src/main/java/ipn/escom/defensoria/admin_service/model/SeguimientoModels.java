package ipn.escom.defensoria.admin_service.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Modelos del seguimiento de quejas del Panel de la Defensora (2026-10-16). Todo es de solo
 * lectura y se arma con SQL sobre las tablas de quejas, personal y dependencias.
 */
public final class SeguimientoModels {

    private SeguimientoModels() {
    }

    /** Etapa del proceso en que va una queja (Recepción, con el quejoso, Primer Contacto…). */
    public record Conteo(String clave, String etiqueta, long total) {
    }

    public record Etapa(String clave, String etiqueta, long total, List<Conteo> estatus) {
    }

    public record Kpis(
            long total,
            long ultimos30,
            long enRecepcion,
            long sinTomarMas3Dias,
            long conQuejoso,
            long rechazadasAlgunaVez,
            long enPrimerContacto,
            long enSubdefensoria,
            long cerradas,
            long atendidasRecepcion,
            Double horasPromedioRecepcion,
            long respuestasDenunciado) {
    }

    public record Mes(String clave, String etiqueta, long recibidas, long atendidas) {
    }

    public record UnidadConteo(String clave, String nombre, String abreviatura, String tipo,
                               long total, long enTramite, long cerradas, LocalDateTime ultimaQueja) {
    }

    public record Recepcionista(String nombre, String correo, long abiertasAhora, long atendidas,
                                long turnadas, long conObservaciones, long atendidasHoy) {
    }

    public record Abogado(String nombre, String rol, long enTramite, long total, long cerradas) {
    }

    public record RechazoReciente(String folio, LocalDateTime fecha, String quejoso, String recepcionista,
                                  String motivo, String estatusActual) {
    }

    public record Personal(long total, List<Conteo> porRol, long dependencias) {
    }

    public record Tablero(
            Kpis kpis,
            List<Etapa> etapas,
            List<Mes> porMes,
            List<UnidadConteo> unidades,
            List<Recepcionista> recepcion,
            List<Abogado> abogados,
            List<Conteo> motivosRechazo,
            List<RechazoReciente> rechazosRecientes,
            Personal personal) {
    }

    public record FilaQueja(
            String folio,
            LocalDateTime fecha,
            String quejoso,
            String unidadClave,
            String unidadNombre,
            String etapa,
            String estatus,
            String estatusEtiqueta,
            String recepcionista,
            boolean atendiendoAhora,
            String abogado,
            String folioPrimerContacto,
            String origen) {
    }

    public record PaginaQuejas(List<FilaQueja> filas, int pagina, int tamanio, long totalElementos, int totalPaginas) {
    }

    public record Persona(String correo, String nombre) {
    }

    public record Filtros(List<Persona> recepcionistas, List<String> abogados, List<Conteo> etapas) {
    }

    public record Evidencia(Long id, String nombre, String tipoMime, String tipo, Long tamanio, LocalDateTime fecha) {
    }

    public record Evento(LocalDateTime fecha, String titulo, String detalle) {
    }

    public record DetalleQueja(
            String folio,
            String etapa,
            String etapaEtiqueta,
            String estatus,
            String estatusEtiqueta,
            String origen,
            LocalDateTime fechaCreacion,
            // quejoso
            String quejoso,
            String correo,
            String telefono,
            LocalDate fechaNacimiento,
            String tipoIdentificacion,
            String numeroIdentificacion,
            // hechos
            String unidadClave,
            String unidadNombre,
            LocalDate fechaHechos,
            String denunciado,
            String descripcion,
            // seguimiento
            String recepcionista,
            boolean atendiendoAhora,
            String abogado,
            String folioPrimerContacto,
            String comentariosRecepcion,
            String motivoRechazo,
            // ventanilla
            String tipoDocumentoFisico,
            String numeroOficio,
            LocalDate fechaRecepcionFisica,
            String ubicacionFisica,
            List<Evidencia> evidencias,
            long respuestasDenunciado,
            List<Evento> linea) {
    }
}
