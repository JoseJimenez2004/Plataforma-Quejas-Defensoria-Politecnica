package ipn.escom.defensoria.quejoso.entity;

import lombok.Getter;

@Getter
public enum EstatusQuejaEntity {
    RECIBIDA("Recibidas"),
    EN_ANALISIS("En análisis"),
    COMPETENTE("Competente"),
    IMPROCEDENTE("Improcedente"),
    ASIGNADO_A_ABOGADO("Asignado a abogado"),
    EN_INVESTIGACION("En investigación"),
    PROCESO_CONCLUSION("Proceso de conclusión"),
    FINALIZADA("Finalizada"),
    REMISION("Remisión"),
    CANCELADA("Cancelada");

    private final String descripcion;

    // El nombre del constructor debe ser igual al del Enum
    EstatusQuejaEntity(String descripcion) {
        this.descripcion = descripcion;
    }
}