package ipn.escom.defensoria.admin_service.model;

/** Resumen del dashboard del panel de la Defensora (CU-ADM-05). Sin Lombok a propósito. */
public class DashboardResumenModel {

    private final long totalPersonalActivo;
    private final long totalDependencias;
    private final String ultimoRespaldo;
    private final long totalPlantillasActivas;
    /** null si no se pudo consultar (p. ej. la tabla quejas todavía no existe). */
    private final IndicadoresQuejasModel quejas;

    public DashboardResumenModel(long totalPersonalActivo, long totalDependencias, String ultimoRespaldo,
                                 long totalPlantillasActivas, IndicadoresQuejasModel quejas) {
        this.totalPersonalActivo = totalPersonalActivo;
        this.totalDependencias = totalDependencias;
        this.ultimoRespaldo = ultimoRespaldo;
        this.totalPlantillasActivas = totalPlantillasActivas;
        this.quejas = quejas;
    }

    public long getTotalPersonalActivo() { return totalPersonalActivo; }
    public long getTotalDependencias() { return totalDependencias; }
    public String getUltimoRespaldo() { return ultimoRespaldo; }
    public long getTotalPlantillasActivas() { return totalPlantillasActivas; }
    public IndicadoresQuejasModel getQuejas() { return quejas; }
}
