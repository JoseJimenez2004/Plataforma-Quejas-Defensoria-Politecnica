package ipn.escom.defensoria.auth.service.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "quejas-service", url = "${quejas.service.url:http://localhost:8084}")
public interface QuejasClient {

    @PostMapping("/api/quejoso/quejas/validar-folio")
    boolean validarFolioYCorreo(@RequestBody Map<String, String> datos);
}