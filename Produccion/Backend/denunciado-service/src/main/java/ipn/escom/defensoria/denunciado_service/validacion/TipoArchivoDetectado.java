package ipn.escom.defensoria.denunciado_service.validacion;

/** Tipo real del archivo según su firma binaria, no según extensión ni Content-Type. */
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
