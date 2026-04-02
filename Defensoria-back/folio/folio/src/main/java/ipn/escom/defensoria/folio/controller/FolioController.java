package ipn.escom.defensoria.folio.controller;

import ipn.escom.defensoria.folio.dto.FolioDTO;
import ipn.escom.defensoria.folio.service.FolioServiece;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/folio")
@CrossOrigin(origins = "*")

public class FolioController {
    @Autowired
    private FolioServiece service;
    
    @PostMapping("/generar")
    public ResponseEntity<FolioDTO> generarFolio(){
        String folioCreado = service.generadorFolio();
        return ResponseEntity.ok(new FolioDTO(folioCreado, "Queja enviada con éxito"));
    }
    
    
    
}
