package ipn.escom.defensoria.recepcionista.service;

import ipn.escom.defensoria.recepcionista.entity.Queja;
import ipn.escom.defensoria.recepcionista.entity.EstadoQueja;
import ipn.escom.defensoria.recepcionista.repository.QuejaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GestionService {

    private final QuejaRepository quejaRepository;

    // CU25 y CU28: Consultar panel y abrir historial
    public List<Queja> obtenerTodas() {
        return quejaRepository.findAll();
    }

    public List<Queja> obtenerPendientes() {
        return quejaRepository.findByEstado(EstadoQueja.PENDIENTE);
    }

    // CU27: Abrir queja específica
    public Queja obtenerPorId(Long id) {
        return quejaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada con ID: " + id));
    }

    // CU33, CU33.1, CU33.2: Buscar antecedencia
    public List<Queja> buscarAntecedentes(String correo, String boleta) {
        if (correo != null && !correo.isEmpty()) {
            return quejaRepository.findByCorreoPromovente(correo);
        }
        if (boleta != null && !boleta.isEmpty()) {
            return quejaRepository.findByBoleta(boleta);
        }
        return List.of();
    }

    // CU30 y CU29: Validar y Generar Folio
    @Transactional
    public Queja validarQueja(Long id) {
        Queja queja = obtenerPorId(id);
        long conteo = quejaRepository.count() + 1;
        queja.setFolio(String.format("DEF-%d-%04d", LocalDate.now().getYear(), conteo));
        queja.setEstado(EstadoQueja.VALIDADA);
        return quejaRepository.save(queja);
    }

    // CU31 y CU32: Rechazar y Enviar notificación
    @Transactional
    public Queja rechazarQueja(Long id, String motivo) {
        Queja queja = obtenerPorId(id);
        queja.setEstado(EstadoQueja.RECHAZADA);
        queja.setDescripcion(queja.getDescripcion() + "\n[MOTIVO RECHAZO]: " + motivo);
        System.out.println("Notificación enviada a " + queja.getCorreoPromovente() + ": Rechazo por " + motivo);
        return quejaRepository.save(queja);
    }

    // CU34, CU35, CU46, CU47: Turnar, Canalizar y Notificar
    @Transactional
    public Queja turnarAAbogado(Long id, String nombreAbogado) {
        Queja queja = obtenerPorId(id);
        queja.setAbogadoAsignado(nombreAbogado);
        queja.setEstado(EstadoQueja.EN_REVISION); // CU46
        
        System.out.println("Notificación enviada a " + queja.getCorreoPromovente() + ": Turnado a " + nombreAbogado);
        return quejaRepository.save(queja);
    }
}