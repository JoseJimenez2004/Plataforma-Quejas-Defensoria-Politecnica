package ipn.escom.defensoria.revision_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.revision_service.entity.AcuerdoConciliacion;
import ipn.escom.defensoria.revision_service.entity.Queja;
import ipn.escom.defensoria.revision_service.model.CrearConciliacionRequest;
import ipn.escom.defensoria.revision_service.repository.AcuerdoConciliacionRepository;
import ipn.escom.defensoria.revision_service.repository.QuejaRepository;

/** Lado del personal (recepcionista/subdefensor/defensor) para emitir acuerdos de
 * conciliación. El quejoso solo los lee y responde desde queja-service. */
@Service
public class ConciliacionRevisionService {

    @Autowired
    private AcuerdoConciliacionRepository acuerdoConciliacionRepository;

    @Autowired
    private QuejaRepository quejaRepository;

    @Autowired
    private NotificacionQuejaService notificacionService;

    public AcuerdoConciliacion crear(CrearConciliacionRequest datos, String correoStaff) {
        Queja queja = quejaRepository.findByNumeroFolio(datos.getNumeroFolio())
                .orElseThrow(() -> new RuntimeException("No existe una queja con el folio " + datos.getNumeroFolio()));

        AcuerdoConciliacion acuerdo = new AcuerdoConciliacion();
        acuerdo.setNumeroFolio(datos.getNumeroFolio());
        acuerdo.setCorreoInstitucional(queja.getCorreoInstitucional());
        acuerdo.setAsunto(datos.getAsunto());
        acuerdo.setTerminos(datos.getTerminos());
        acuerdo.setCreadoPor(correoStaff);

        AcuerdoConciliacion guardado = acuerdoConciliacionRepository.save(acuerdo);

        notificacionService.registrarConciliacion(queja.getCorreoInstitucional(), datos.getNumeroFolio(), datos.getAsunto());

        return guardado;
    }

    public List<AcuerdoConciliacion> listar(String folio) {
        if (folio != null && !folio.isBlank()) {
            return acuerdoConciliacionRepository.findByNumeroFolioOrderByFechaEmisionDesc(folio);
        }
        return acuerdoConciliacionRepository.findAllByOrderByFechaEmisionDesc();
    }
}
