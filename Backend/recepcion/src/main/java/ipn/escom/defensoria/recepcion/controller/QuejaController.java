package ipn.escom.defensoria.recepcion.controller;

import ipn.escom.defensoria.recepcion.model.Queja;
import ipn.escom.defensoria.recepcion.service.RecepcionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recepcion/quejas")
public class QuejaController {

    private final RecepcionService recepcionService;

    public QuejaController(RecepcionService recepcionService) {
        this.recepcionService = recepcionService;
    }

    @PostMapping
    public Queja crear(@RequestBody Queja queja) {
        return recepcionService.registrarQueja(queja);
    }
}