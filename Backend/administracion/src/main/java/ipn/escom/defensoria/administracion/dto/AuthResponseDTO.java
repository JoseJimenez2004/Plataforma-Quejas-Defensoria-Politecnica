package ipn.escom.defensoria.administracion.dto;

import java.util.List;

public record AuthResponseDTO(
    String token,
    String nombreCompleto,
    List<String> roles
) {}