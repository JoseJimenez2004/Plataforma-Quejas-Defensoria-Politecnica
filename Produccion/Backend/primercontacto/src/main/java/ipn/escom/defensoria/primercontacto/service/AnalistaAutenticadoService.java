package ipn.escom.defensoria.primercontacto.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.entity.RolStaff;
import ipn.escom.defensoria.primercontacto.repository.PersonalAdministrativoRepository;

@Service
public class AnalistaAutenticadoService {

    private final PersonalAdministrativoRepository personalRepository;

    public AnalistaAutenticadoService(
            PersonalAdministrativoRepository personalRepository
    ) {
        this.personalRepository = personalRepository;
    }

    public PersonalAdministrativo obtenerAnalista(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado."
            );
        }

        String correo = authentication.getName();

        PersonalAdministrativo personal =
                personalRepository
                        .findByCorreoInstitucional(correo)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "No se encontró el usuario autenticado."
                                )
                        );

        if (!personal.isActivo()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario se encuentra inactivo."
            );
        }

        if (personal.getRol() != RolStaff.ANALISTA_PRIMER_CONTACTO) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario no pertenece al área de Primer Contacto."
            );
        }

        return personal;
    }
}