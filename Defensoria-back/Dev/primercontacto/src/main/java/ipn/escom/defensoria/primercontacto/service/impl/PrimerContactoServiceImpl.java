package ipn.escom.defensoria.primercontacto.service.impl;

import ipn.escom.defensoria.primercontacto.entity.Expediente;
import ipn.escom.defensoria.primercontacto.repository.ExpedienteRepository;
import ipn.escom.defensoria.primercontacto.service.PrimerContactoService;
import ipn.escom.defensoria.primercontacto.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrimerContactoServiceImpl implements PrimerContactoService {

    private final ExpedienteRepository repository;

    @Override
    public List<Expediente> obtenerBandejaAnalisis() {
        return repository.findAll();
    }

    @Override
    public Expediente obtenerDetalleExpediente(String folio) {
        return repository.findByFolio(folio)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el expediente con folio: " + folio));
    }

    @Override
    @Transactional
    public Expediente dictaminarCompetencia(String folio, boolean esCompetente, String motivo, String cargoUsuario) {
        // MATRIZ RBAC: Validar que el usuario sea Abogado o Subdefensor
        validarPermisosAbogacia(cargoUsuario);

        Expediente expediente = obtenerDetalleExpediente(folio);
        
        if (esCompetente) {
            expediente.setEstatus("ADMITIDO");
        } else {
            expediente.setEstatus("REMITIDO");
            expediente.setMotivoRemision(motivo);
        }
        
        return repository.save(expediente);
    }

    @Override
    @Transactional
    public void formalizarYNotificar(String folio, String contenidoAcuerdo, String cargoUsuario) {
        // Solo el SUBDEFENSOR o el ABOGADO pueden formalizar el cierre
        validarPermisosAbogacia(cargoUsuario);

        Expediente expediente = obtenerDetalleExpediente(folio);
        
        // Aquí iría la lógica real de envío de correo
        System.out.println("LOG: Formalización realizada por: " + cargoUsuario);
        System.out.println("LOG: Enviando notificación a: " + expediente.getCorreoQuejoso());
    }

    /**
     * Método privado para validar la matriz de acceso RBAC.
     * En un entorno real, esto se manejaría con Spring Security,
     * pero para tu lógica de negocio actual, esto asegura que solo
     * personal jurídico actúe.
     */
    private void validarPermisosAbogacia(String cargo) {
        if (cargo == null || (!cargo.equals("SUBDEFENSOR") && !cargo.equals("ABOGADO_DICTAMINADOR"))) {
            throw new RuntimeException("ACCESO DENEGADO: Solo el personal jurídico (Abogados/Subdefensores) puede realizar esta acción.");
        }
    }
}