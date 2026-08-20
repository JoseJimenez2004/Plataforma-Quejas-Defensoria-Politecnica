package ipn.escom.defensoria.auth.service.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import ipn.escom.defensoria.auth.service.model.QuejaResumenModel;

@FeignClient(name = "quejas-service", url = "${quejas.service.url:http://localhost:8084}")
public interface QuejasClient {

    @PostMapping("/api/quejoso/quejas/validar-folio")
    boolean validarFolioYCorreo(@RequestBody Map<String, String> datos);

    // Detalle real de la queja (nombre/apellidos/identificación) — se usa al activar una
    // cuenta para no dejar placeholders genéricos en el Usuario creado.
    @GetMapping("/api/quejoso/quejas/folio/{folio}")
    QuejaResumenModel obtenerPorFolio(@PathVariable("folio") String folio, @RequestParam String correo);
}