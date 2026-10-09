package ipn.escom.defensoria.queja_service.validacion;

/**
 * Tipo real de un archivo, deducido de sus primeros bytes (firma o "magic bytes"), no de su
 * extensión ni del Content-Type que manda el navegador. Ambos los controla el cliente y se
 * falsifican cambiando el nombre del archivo, así que no sirven como control de seguridad en
 * un endpoint público sin autenticación.
 */
public enum TipoArchivoDetectado {

    JPEG("image/jpeg", true),
    PNG("image/png", true),
    PDF("application/pdf", false),
    MP4("video/mp4", false),
    MP3("audio/mpeg", false),
    DESCONOCIDO(null, false);

    private final String mime;
    private final boolean imagen;

    TipoArchivoDetectado(String mime, boolean imagen) {
        this.mime = mime;
        this.imagen = imagen;
    }

    public String mime() {
        return mime;
    }

    public boolean esImagen() {
        return imagen;
    }
}
