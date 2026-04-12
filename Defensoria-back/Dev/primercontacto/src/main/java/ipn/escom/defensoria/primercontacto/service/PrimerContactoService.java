package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.entity.Expediente;
import java.util.List;

public interface PrimerContactoService {
    
    List<Expediente> obtenerBandejaAnalisis();
    
    Expediente obtenerDetalleExpediente(String folio);
    
    // El cargo es vital para la matriz de acceso
    Expediente dictaminarCompetencia(String folio, boolean esCompetente, String motivo, String cargoUsuario);
    
    void formalizarYNotificar(String folio, String contenidoAcuerdo, String cargoUsuario);
}