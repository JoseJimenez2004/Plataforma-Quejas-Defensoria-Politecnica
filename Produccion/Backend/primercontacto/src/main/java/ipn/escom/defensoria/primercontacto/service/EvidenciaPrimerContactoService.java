package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.entity.QuejaEvidenciaArchivo;
import ipn.escom.defensoria.primercontacto.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.primercontacto.repository.EvidenciaPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.QuejaEvidenciaArchivoRepository;
import org.springframework.stereotype.Service;

/**
 * Contenido real de las evidencias para que el analista las abra al dictaminar
 * (CU-PC-06/07). Ver QuejaEvidenciaArchivo.
 */
@Service
public class EvidenciaPrimerContactoService {

    private final EvidenciaPrimerContactoRepository evidenciaRepository;
    private final QuejaEvidenciaArchivoRepository archivoRepository;

    public EvidenciaPrimerContactoService(
            EvidenciaPrimerContactoRepository evidenciaRepository,
            QuejaEvidenciaArchivoRepository archivoRepository
    ) {
        this.evidenciaRepository = evidenciaRepository;
        this.archivoRepository = archivoRepository;
    }

    /**
     * @param evidenciaOrigenId el id que muestra el expediente (EvidenciaDTO.id), que es el
     *                          de queja_evidencias.
     */
    public QuejaEvidenciaArchivo obtener(Long evidenciaOrigenId) {

        /*
         * Solo evidencias de quejas que llegaron a Primer Contacto: el
         * analista no debe poder descargar cualquier archivo por id.
         */
        if (!evidenciaRepository.existsByEvidenciaOrigenId(evidenciaOrigenId)) {
            throw new RecursoNoEncontradoException(
                    "La evidencia no pertenece a ningún expediente de Primer Contacto."
            );
        }

        QuejaEvidenciaArchivo archivo = archivoRepository.findById(evidenciaOrigenId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El archivo de la evidencia ya no existe."
                ));

        if (archivo.getContenido() == null) {
            throw new RecursoNoEncontradoException("La evidencia no tiene contenido.");
        }

        return archivo;
    }
}
