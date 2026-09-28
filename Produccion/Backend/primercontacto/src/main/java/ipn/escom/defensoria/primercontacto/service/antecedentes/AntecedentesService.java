package ipn.escom.defensoria.primercontacto.service.antecedentes;

import ipn.escom.defensoria.primercontacto.dto.AntecedenteDTO;
import ipn.escom.defensoria.primercontacto.dto.BusquedaAntecedentesDTO;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.QuejaReferencia;
import ipn.escom.defensoria.primercontacto.repository.QuejaReferenciaRepository;
import ipn.escom.defensoria.primercontacto.service.TransicionExpedienteService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Búsqueda de antecedentes de una queja: otras quejas del sistema que podrían estar
 * relacionadas con la que se analiza (mismo quejoso, mismos hechos, misma escuela...).
 *
 * Arma las candidatas y delega el "parecido" en MotorAntecedentes, que es donde se
 * conectará el modelo. Hoy las candidatas son todas las quejas de defensoria_db salvo la
 * propia; si el volumen crece, el modelo debería traer su propio índice.
 */
@Service
public class AntecedentesService {

    private final TransicionExpedienteService transicionService;
    private final QuejaReferenciaRepository quejaRepository;
    private final MotorAntecedentes motor;

    public AntecedentesService(
            TransicionExpedienteService transicionService,
            QuejaReferenciaRepository quejaRepository,
            MotorAntecedentes motor
    ) {
        this.transicionService = transicionService;
        this.quejaRepository = quejaRepository;
        this.motor = motor;
    }

    public BusquedaAntecedentesDTO buscar(String folio) {

        ExpedientePrimerContacto expediente = transicionService.obtenerPorFolio(folio);

        QuejaReferencia queja = quejaRepository
                .findByNumeroFolio(expediente.getFolioOrigen())
                .orElse(null);

        List<QuejaReferencia> candidatas = quejaRepository.findAll().stream()
                .filter(q -> !q.getNumeroFolio().equals(expediente.getFolioOrigen()))
                .toList();

        List<AntecedenteDTO> resultados = motor.buscar(expediente, queja, candidatas);

        return BusquedaAntecedentesDTO.builder()
                .folio(expediente.getFolio())
                .folioQueja(expediente.getFolioOrigen())
                .motor(motor.nombre())
                .descripcionMotor(motor.descripcion())
                .generadoEn(LocalDateTime.now().toString())
                .quejasAnalizadas(candidatas.size())
                .resultados(resultados)
                .build();
    }
}
