package ipn.escom.defensoria.admin_service.model;

import java.time.LocalDateTime;
import java.util.List;

import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Conteo;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Etapa;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Mes;

/**
 * Modelos del Panel de la Defensora con el diseño del 2026-10-18: indicadores por periodo,
 * casos que requieren atención, quejas por unidad, carga de trabajo del personal, motivos de
 * rechazo y personal con sus estadísticas. Todo de solo lectura salvo {@link ContactoRequest}.
 */
public final class PanelModels {

    private PanelModels() {
    }

    // ---------------------------------------------------------------- panel general

    public record PanelKpis(
            long total,
            long recibidasPeriodo,
            long recibidasAnterior,
            long enRecepcion,
            long sinTomarMas3Dias,
            long conQuejoso,
            long rechazadasAlgunaVez,
            long enPrimerContacto,
            long enSubdefensoria,
            long cerradas) {
    }

    public record Panel(
            int dias,
            PanelKpis kpis,
            List<Etapa> etapas,
            List<Mes> porMes,
            List<FilaUnidad> unidades,
            PaginaCasos atencion,
            List<MiembroCarga> recepcion,
            List<MotivoRechazo> motivos) {
    }

    // ---------------------------------------------------------------- casos que requieren atención

    /** Regla con la que se detecta un caso: se muestra al usuario para que sepa por qué aparece. */
    public record Regla(String clave, String etiqueta, String descripcion, int umbralDias) {
    }

    public record Caso(
            String folio,
            LocalDateTime fecha,
            String quejoso,
            String motivo,
            String motivoEtiqueta,
            long dias,
            String severidad,
            String etapa,
            String estatus,
            String estatusEtiqueta,
            String unidadClave,
            String unidadNombre,
            String responsable) {
    }

    public record PaginaCasos(List<Caso> filas, int pagina, int tamanio, long totalElementos, int totalPaginas,
                              List<Regla> reglas) {
    }

    // ---------------------------------------------------------------- unidades

    public record FilaUnidad(
            int posicion,
            String clave,
            String nombre,
            String siglas,
            String tipo,
            String tipoEtiqueta,
            long total,
            long enTramite,
            long cerradas,
            double porcentaje,
            LocalDateTime ultima) {
    }

    public record ResumenUnidades(
            long unidadesConQueja,
            long unidadesAnterior,
            long totalQuejas,
            long totalAnterior,
            String unidadTop,
            long unidadTopTotal,
            double promedio) {
    }

    public record TablaUnidades(
            List<FilaUnidad> filas,
            int pagina,
            int tamanio,
            long totalElementos,
            int totalPaginas,
            ResumenUnidades resumen,
            List<Conteo> porTipo,
            List<FilaUnidad> top) {
    }

    // ---------------------------------------------------------------- carga de trabajo

    public record MiembroCarga(
            Long id,
            String nombre,
            String correo,
            String rol,
            boolean activo,
            long abiertas,
            long enTramite,
            long atendidas,
            long turnadas,
            long observaciones,
            long enPrimerContacto,
            long enSubdefensoria,
            long cerradas,
            LocalDateTime ultimaActividad,
            long carga,
            double porcentaje,
            String nivel) {
    }

    public record CargaKpis(
            long personalActivo,
            long recepcionistas,
            long abogados,
            long enTramite,
            long totalQuejas,
            long atendidas,
            Double diasPromedio,
            Double diasPromedioAnterior) {
    }

    public record Carga(int dias, CargaKpis kpis, List<MiembroCarga> recepcionistas, List<MiembroCarga> abogados,
                        List<Etapa> etapas) {
    }

    // ---------------------------------------------------------------- motivos de rechazo

    public record MotivoRechazo(String clave, String motivo, String categoria, long cantidad, double porcentaje) {
    }

    // ---------------------------------------------------------------- personal

    public record MiembroPersonal(
            Long id,
            String nombre,
            String numeroEmpleado,
            String correo,
            String rol,
            boolean activo,
            boolean cuentaTemporal,
            String unidadClave,
            String unidadNombre,
            String telefono,
            LocalDateTime fechaRegistro,
            LocalDateTime ultimoAcceso,
            long abiertas,
            long enTramite,
            long atendidas,
            long turnadas,
            long observaciones) {
    }

    public record Actividad(LocalDateTime fecha, String titulo, String detalle) {
    }

    public record QuejaAsignada(String folio, LocalDateTime fecha, String quejoso, String unidad, String etapa,
                                String estatusEtiqueta, String relacion) {
    }

    public record DetallePersonal(MiembroPersonal persona, List<Actividad> actividad, List<QuejaAsignada> quejas) {
    }

    /** Datos de contacto que no están en la entidad de personal: área de adscripción y teléfono. */
    public record ContactoRequest(String unidadClave, String telefono) {
    }
}
