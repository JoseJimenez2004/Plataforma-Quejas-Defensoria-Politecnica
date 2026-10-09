package ipn.escom.defensoria.denunciado_service.dto;

import java.time.LocalDateTime;

/** Metadatos de un archivo, sin el contenido. */
public record ArchivoResumen(Long id, String tipo, String nombreArchivo, String tipoMime,
                             long tamanioBytes, LocalDateTime fechaSubida) {
}
