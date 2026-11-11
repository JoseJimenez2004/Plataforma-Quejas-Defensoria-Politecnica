package ipn.escom.defensoria.primercontacto.dto;

/** Instancia a la que se puede remitir, con el correo al que saldrá el oficio. */
public record InstanciaRemisionDTO(String clave, String sigla, String nombre, String correo) {
}
