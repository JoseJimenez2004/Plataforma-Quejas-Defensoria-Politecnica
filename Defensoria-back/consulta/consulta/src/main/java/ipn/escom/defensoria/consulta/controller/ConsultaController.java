/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ipn.escom.defensoria.consulta.controller;

import ipn.escom.defensoria.consulta.dto.ConsultaDTO;
import ipn.escom.defensoria.consulta.repository.ConsultaRepository;
import ipn.escom.defensoria.consulta.entity.ConsultaEntity;
import ipn.escom.defensoria.consulta.service.ConsultaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consulta")
@CrossOrigin(origins = "http://localhost:4200")
public class ConsultaController {

    @Autowired
    private ConsultaService service;

    @Autowired
    private ConsultaRepository repository;
    
    @GetMapping("/{folio}")
    public ResponseEntity<ConsultaDTO> obtenerEstatus(@PathVariable String folio) {
        return ResponseEntity.ok(service.buscarEstatusPorFolio(folio));
    }
    
    // En el proyecto de CONSULTA (8081)
    @PostMapping("/crear")
    public ResponseEntity<String> crearQuejaDesdeFolio(@RequestBody ConsultaEntity nuevaQueja) {
        repository.save(nuevaQueja); // 'repository' es tu ConsultaRepository
        return ResponseEntity.ok("Queja sincronizada");
    }
}