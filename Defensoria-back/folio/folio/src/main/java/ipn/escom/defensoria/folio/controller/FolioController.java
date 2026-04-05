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
    
        // En FolioController.java
    @PostMapping("/generar")
    public ResponseEntity<String> generar(@RequestBody FolioDTO formulario) {
        return ResponseEntity.ok(service.generadorFolio(formulario));
    }
}
