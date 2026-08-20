package ipn.escom.defensoria.revision_service.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrimerContactoIngresoRequest {

    /*
     * Folio de la queja en Recepcion/Revision.
     * Ejemplo: FOL-A1B2C3D4
     *
     * NO se envia quejas.id.
     */
    private String folioOrigen;

    private String tema;

    private String descripcionHechos;

    /*
     * Se envia como texto ISO.
     * Primer Contacto conserva este dato como fechaRecepcionOrigen.
     */
    private String fechaRecepcion;

    /*
     * Revision actualmente no maneja prioridad.
     * Por ahora se envia null.
     */
    private String prioridad;

    private QuejosoRequest quejoso;

    private List<EvidenciaRequest> evidencias;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuejosoRequest {

        /*
         * Se mantiene null porque NO queremos reutilizar
         * quejas.id como identificador de Primer Contacto.
         */
        private Long id;

        private String nombreCompleto;

        private String correo;

        /*
         * Queja actualmente no contiene telefono.
         */
        private String telefono;

        private String unidadAcademica;

        private String tipoUsuario;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenciaRequest {

        /*
         * Este id corresponde a la evidencia original, NO a quejas.id
         * ni al expediente de Primer Contacto.
         *
         * Primer Contacto generara su propio id para su evidencia.
         */
        private Long id;

        private String nombreArchivo;

        private String tipoArchivo;

        /*
         * Actualmente el archivo vive como BYTEA en queja_evidencias.
         * No inventamos una URL que no existe.
         */
        private String urlArchivo;

        private String fechaCarga;
    }
}