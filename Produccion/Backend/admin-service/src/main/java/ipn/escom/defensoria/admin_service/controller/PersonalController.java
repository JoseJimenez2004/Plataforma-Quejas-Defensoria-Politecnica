package ipn.escom.defensoria.admin_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.admin_service.entity.PersonalAdministrativo;
import ipn.escom.defensoria.admin_service.model.PersonalCreadoResponseModel;
import ipn.escom.defensoria.admin_service.model.PersonalRequest;
import ipn.escom.defensoria.admin_service.model.PersonalResumenModel;
import ipn.escom.defensoria.admin_service.model.ResetPasswordResponseModel;
import ipn.escom.defensoria.admin_service.service.BitacoraService;
import ipn.escom.defensoria.admin_service.service.PersonalAdministrativoService;
import jakarta.servlet.http.HttpServletRequest;

/** Todo este controller es exclusivo de ADMIN_SISTEMAS -- "Usuarios y Roles" del mockup. */
@RestController
@RequestMapping("/api/admin/personal")
@PreAuthorize("hasRole('ADMIN_SISTEMAS')")
@Tag(name = "Personal Administrativo", description = "Gestión de usuarios y roles del personal")
public class PersonalController {

    private final PersonalAdministrativoService personalService;
    private final BitacoraService bitacoraService;

    public PersonalController(PersonalAdministrativoService personalService, BitacoraService bitacoraService) {
        this.personalService = personalService;
        this.bitacoraService = bitacoraService;
    }

    @GetMapping
    @Operation(summary = "Lista todo el personal administrativo")
    public ResponseEntity<List<PersonalResumenModel>> listar() {
        return ResponseEntity.ok(personalService.listar());
    }

    @PostMapping
    @Operation(summary = "Crea una nueva cuenta de personal administrativo con contraseña temporal")
    public ResponseEntity<PersonalCreadoResponseModel> crear(@RequestBody PersonalRequest datos,
            HttpServletRequest request) {
        PersonalCreadoResponseModel creado = personalService.crear(datos);
        bitacoraService.registrar(usuarioActual(), "Creación de usuario: " + creado.getCorreoInstitucional(), request);
        return ResponseEntity.ok(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita nombre, correo, rol; o restablece contraseña / desactiva la cuenta")
    public ResponseEntity<PersonalAdministrativo> editar(@PathVariable Long id, @RequestBody PersonalRequest datos,
            HttpServletRequest request) {
        PersonalAdministrativo actualizado = personalService.editar(id, datos);
        bitacoraService.registrar(usuarioActual(), "Edición de usuario: " + actualizado.getCorreoInstitucional(), request);
        actualizado.setPassword(null);
        return ResponseEntity.ok(actualizado);
    }

    @PostMapping("/{id}/resetear-password")
    @Operation(summary = "Genera una nueva contraseña temporal para esa cuenta")
    public ResponseEntity<ResetPasswordResponseModel> resetearPassword(@PathVariable Long id,
            HttpServletRequest request) {
        String nueva = personalService.resetearPassword(id);
        bitacoraService.registrar(usuarioActual(), "Restablecimiento de contraseña (id " + id + ")", request);
        return ResponseEntity.ok(new ResetPasswordResponseModel(nueva));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Da de baja (desactiva) una cuenta de personal administrativo")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id, HttpServletRequest request) {
        personalService.darDeBaja(id);
        bitacoraService.registrar(usuarioActual(), "Baja de usuario (id " + id + ")", request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivar")
    @Operation(summary = "Reactiva una cuenta previamente dada de baja")
    public ResponseEntity<Void> reactivar(@PathVariable Long id, HttpServletRequest request) {
        personalService.reactivar(id);
        bitacoraService.registrar(usuarioActual(), "Reactivación de usuario (id " + id + ")", request);
        return ResponseEntity.noContent().build();
    }

    private String usuarioActual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
