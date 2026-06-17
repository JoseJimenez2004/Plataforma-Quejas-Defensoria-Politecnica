package ipn.escom.defensoria.recepcion.service;

import ipn.escom.defensoria.recepcion.model.Queja;
import ipn.escom.defensoria.recepcion.repository.QuejaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecepcionService {

    private final QuejaRepository quejaRepository;

    public RecepcionService(QuejaRepository quejaRepository) {
        this.quejaRepository = quejaRepository;
    }

    @Transactional
    public Queja registrarQueja(Queja queja) {
        return quejaRepository.save(queja);
    }
}