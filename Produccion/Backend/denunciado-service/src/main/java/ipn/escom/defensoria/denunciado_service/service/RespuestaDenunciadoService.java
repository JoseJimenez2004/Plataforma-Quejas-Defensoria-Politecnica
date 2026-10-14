package ipn.escom.defensoria.denunciado_service.service;

import static ipn.escom.defensoria.denunciado_service.validacion.ReglasDenunciado.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ipn.escom.defensoria.denunciado_service.dto.ArchivoResumen;
import ipn.escom.defensoria.denunciado_service.dto.RegistroRespuestaRequest;
import ipn.escom.defensoria.denunciado_service.dto.RespuestaDetalleModel;
import ipn.escom.defensoria.denunciado_service.dto.RespuestaRegistradaModel;
import ipn.escom.defensoria.denunciado_service.entity.RespuestaDenunciado;
import ipn.escom.defensoria.denunciado_service.entity.RespuestaDenunciadoArchivo;
import ipn.escom.defensoria.denunciado_service.repository.RespuestaDenunciadoArchivoRepository;
import ipn.escom.defensoria.denunciado_service.repository.RespuestaDenunciadoRepository;
import ipn.escom.defensoria.denunciado_service.validacion.DetectorTipoArchivo;
import ipn.escom.defensoria.denunciado_service.validacion.Textos;
import ipn.escom.defensoria.denunciado_service.validacion.TipoArchivoDetectado;
import ipn.escom.defensoria.denunciado_service.validacion.ValidacionException;

@Service
public class RespuestaDenunciadoService {

    private static final Logger log = LoggerFactory.getLogger(RespuestaDenunciadoService.class);
    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
    private static final String ESTATUS_RECIBIDA = "RECIBIDA";

    private final RespuestaDenunciadoRepository respuestaRepository;
    private final RespuestaDenunciadoArchivoRepository archivoRepository;

    public RespuestaDenunciadoService(RespuestaDenunciadoRepository respuestaRepository,
                                      RespuestaDenunciadoArchivoRepository archivoRepository) {
        this.respuestaRepository = respuestaRepository;
        this.archivoRepository = archivoRepository;
    }

    // ------------------------------------------------------------------ registro

    @Transactional
    public RespuestaRegistradaModel registrar(RegistroRespuestaRequest d) {
        String folioQueja = mayusculas(Textos.normalizar(d.getFolioQueja()));
        String nombre = Textos.normalizar(d.getNombre());
        String apellido1 = Textos.normalizar(d.getApellido1());
        String apellido2 = Textos.normalizar(d.getApellido2());
        String unidad = Textos.normalizar(d.getUnidadProcedenciaClave());
        String tipo = mayusculas(Textos.normalizar(d.getTipoIdentificacion()));
        String numero = Textos.normalizar(d.getNumeroIdentificacion());
        String descripcion = d.getDescripcionHechos() == null ? null : d.getDescripcionHechos().trim();

        if (folioQueja == null || !folioQueja.matches(REGEX_FOLIO_QUEJA)) {
            throw new ValidacionException("El folio de la queja no es válido (formato FOL-XXXXXXXX).");
        }
        validarNombre(nombre, "el nombre", true);
        validarNombre(apellido1, "el primer apellido", true);
        validarNombre(apellido2, "el segundo apellido", false);

        if (unidad == null) {
            throw new ValidacionException("Selecciona tu unidad de procedencia.");
        }
        if (unidad.length() > CLAVE_UNIDAD_LONGITUD_MAXIMA) {
            throw new ValidacionException("La unidad de procedencia no es válida.");
        }
        if (!TIPO_ALUMNO.equals(tipo) && !TIPO_EMPLEADO.equals(tipo)) {
            throw new ValidacionException("Indica si eres alumno o empleado.");
        }
        if (numero == null || !numero.matches(REGEX_NUMERO_IDENTIFICACION)) {
            throw new ValidacionException(TIPO_ALUMNO.equals(tipo)
                    ? "El número de boleta solo admite dígitos (máximo 10)."
                    : "El número de empleado solo admite dígitos (máximo 10).");
        }
        if (descripcion == null || descripcion.length() < DESCRIPCION_LONGITUD_MINIMA) {
            throw new ValidacionException("Describe los hechos con al menos " + DESCRIPCION_LONGITUD_MINIMA + " caracteres.");
        }
        if (descripcion.length() > DESCRIPCION_LONGITUD_MAXIMA) {
            throw new ValidacionException("La descripción no puede exceder " + DESCRIPCION_LONGITUD_MAXIMA + " caracteres.");
        }
        if (!Boolean.TRUE.equals(d.getAvisoPrivacidadAceptado())) {
            throw new ValidacionException("Debes leer y aceptar el aviso de privacidad.");
        }

        List<MultipartFile> credencial = sinVacios(d.getCredencial());
        List<MultipartFile> evidencias = sinVacios(d.getEvidencias());
        List<TipoArchivoDetectado> tiposCredencial = validarCredencial(credencial);
        List<TipoArchivoDetectado> tiposEvidencia = validarEvidencias(evidencias);

        LocalDateTime ahora = LocalDateTime.now(ZONA);
        RespuestaDenunciado r = new RespuestaDenunciado();
        r.setFolioRespuesta(generarFolio());
        r.setFolioQueja(folioQueja);
        r.setNombre(nombre);
        r.setApellido1(apellido1);
        r.setApellido2(apellido2);
        r.setUnidadProcedenciaClave(unidad);
        r.setTipoIdentificacion(tipo);
        r.setNumeroIdentificacion(numero);
        r.setDescripcionHechos(descripcion);
        r.setAvisoPrivacidadAceptado(true);
        r.setAvisoPrivacidadVersion(Textos.esVacio(d.getAvisoPrivacidadVersion())
                ? AVISO_PRIVACIDAD_VERSION : d.getAvisoPrivacidadVersion().trim());
        r.setAvisoPrivacidadFecha(ahora);
        r.setEstatus(ESTATUS_RECIBIDA);
        r.setFechaRegistro(ahora);

        for (int i = 0; i < credencial.size(); i++) {
            r.agregarArchivo(aArchivo(credencial.get(i), tiposCredencial.get(i),
                    RespuestaDenunciadoArchivo.TIPO_IDENTIFICACION, ahora));
        }
        for (int i = 0; i < evidencias.size(); i++) {
            r.agregarArchivo(aArchivo(evidencias.get(i), tiposEvidencia.get(i),
                    RespuestaDenunciadoArchivo.TIPO_EVIDENCIA, ahora));
        }

        RespuestaDenunciado guardada = respuestaRepository.save(r);
        log.info("Respuesta {} registrada para la queja {} ({} credencial, {} evidencias)",
                guardada.getFolioRespuesta(), folioQueja, credencial.size(), evidencias.size());

        return new RespuestaRegistradaModel(guardada.getFolioRespuesta(), guardada.getFolioQueja(),
                nombreCompleto(guardada), guardada.getFechaRegistro(), credencial.size(), evidencias.size());
    }

    // ------------------------------------------------------------------ consulta (personal)

    @Transactional(readOnly = true)
    public List<RespuestaDetalleModel> listarPorFolioQueja(String folioQueja) {
        String folio = mayusculas(Textos.normalizar(folioQueja));
        if (folio == null) {
            throw new ValidacionException("Indica el folio de la queja.");
        }
        return respuestaRepository.findByFolioQuejaOrderByFechaRegistroDesc(folio).stream()
                .map(this::aDetalle)
                .toList();
    }

    @Transactional(readOnly = true)
    public RespuestaDenunciadoArchivo obtenerArchivo(Long id) {
        return archivoRepository.findById(id)
                .orElseThrow(() -> new ValidacionException("No se encontró el archivo."));
    }

    // ------------------------------------------------------------------ validaciones

    private void validarNombre(String valor, String etiqueta, boolean obligatorio) {
        if (valor == null) {
            if (obligatorio) {
                throw new ValidacionException("Escribe " + etiqueta + ".");
            }
            return;
        }
        if (valor.length() < NOMBRE_LONGITUD_MINIMA || valor.length() > NOMBRE_LONGITUD_MAXIMA) {
            throw new ValidacionException(capitalizar(etiqueta) + " debe tener entre "
                    + NOMBRE_LONGITUD_MINIMA + " y " + NOMBRE_LONGITUD_MAXIMA + " caracteres.");
        }
        if (!valor.matches(REGEX_NOMBRE)) {
            throw new ValidacionException(capitalizar(etiqueta)
                    + " solo admite letras (se aceptan acentos, ñ, guion y apóstrofe).");
        }
    }

    private List<TipoArchivoDetectado> validarCredencial(List<MultipartFile> archivos) {
        if (archivos.size() < CREDENCIAL_CANTIDAD_MINIMA) {
            throw new ValidacionException("Adjunta la imagen de tu credencial con la que te identificas.");
        }
        if (archivos.size() > CREDENCIAL_CANTIDAD_MAXIMA) {
            throw new ValidacionException("Adjunta máximo " + CREDENCIAL_CANTIDAD_MAXIMA
                    + " imágenes de tu credencial (frente y reverso).");
        }
        List<TipoArchivoDetectado> tipos = new ArrayList<>();
        for (MultipartFile a : archivos) {
            if (a.getSize() > CREDENCIAL_TAMANIO_MAXIMO) {
                throw new ValidacionException("La imagen \"" + a.getOriginalFilename() + "\" pesa más de 3 MB.");
            }
            TipoArchivoDetectado tipo = DetectorTipoArchivo.detectar(a);
            if (!tipo.esImagen()) {
                throw new ValidacionException("La credencial debe ser una imagen JPG o PNG (\""
                        + a.getOriginalFilename() + "\" no lo es).");
            }
            tipos.add(tipo);
        }
        return tipos;
    }

    private List<TipoArchivoDetectado> validarEvidencias(List<MultipartFile> archivos) {
        if (archivos.size() > EVIDENCIA_CANTIDAD_MAXIMA) {
            throw new ValidacionException("Puedes adjuntar máximo " + EVIDENCIA_CANTIDAD_MAXIMA + " evidencias.");
        }
        long total = 0;
        List<TipoArchivoDetectado> tipos = new ArrayList<>();
        for (MultipartFile a : archivos) {
            if (a.getSize() > EVIDENCIA_TAMANIO_MAXIMO) {
                throw new ValidacionException("La evidencia \"" + a.getOriginalFilename() + "\" pesa más de 30 MB.");
            }
            total += a.getSize();
            TipoArchivoDetectado tipo = DetectorTipoArchivo.detectar(a);
            if (tipo == TipoArchivoDetectado.DESCONOCIDO) {
                throw new ValidacionException("\"" + a.getOriginalFilename()
                        + "\" no es un tipo de archivo permitido (PDF, JPG, PNG, MP4 o MP3).");
            }
            tipos.add(tipo);
        }
        if (total > EVIDENCIA_TAMANIO_TOTAL_MAXIMO) {
            throw new ValidacionException("Las evidencias suman más de 95 MB. Quita alguna o redúcela.");
        }
        return tipos;
    }

    // ------------------------------------------------------------------ utilidades

    private RespuestaDenunciadoArchivo aArchivo(MultipartFile a, TipoArchivoDetectado tipo, String clase,
                                                LocalDateTime fecha) {
        RespuestaDenunciadoArchivo archivo = new RespuestaDenunciadoArchivo();
        archivo.setTipo(clase);
        archivo.setNombreArchivo(nombreSeguro(a.getOriginalFilename()));
        archivo.setTipoMime(tipo.mime());
        archivo.setTamanioBytes(a.getSize());
        archivo.setFechaSubida(fecha);
        try {
            archivo.setContenido(a.getBytes());
        } catch (IOException e) {
            throw new ValidacionException("No se pudo leer el archivo \"" + a.getOriginalFilename() + "\".");
        }
        return archivo;
    }

    private RespuestaDetalleModel aDetalle(RespuestaDenunciado r) {
        List<ArchivoResumen> archivos = r.getArchivos().stream()
                .map(a -> new ArchivoResumen(a.getId(), a.getTipo(), a.getNombreArchivo(), a.getTipoMime(),
                        a.getTamanioBytes(), a.getFechaSubida()))
                .toList();
        return new RespuestaDetalleModel(r.getId(), r.getFolioRespuesta(), r.getFolioQueja(), r.getNombre(),
                r.getApellido1(), r.getApellido2(), r.getUnidadProcedenciaClave(), r.getTipoIdentificacion(),
                r.getNumeroIdentificacion(), r.getDescripcionHechos(), r.getEstatus(), r.getFechaRegistro(),
                r.getAvisoPrivacidadVersion(), archivos);
    }

    private String generarFolio() {
        String folio;
        do {
            folio = "RD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        } while (respuestaRepository.existsByFolioRespuesta(folio));
        return folio;
    }

    private static List<MultipartFile> sinVacios(List<MultipartFile> archivos) {
        return archivos == null ? List.of() : archivos.stream().filter(a -> a != null && !a.isEmpty()).toList();
    }

    /** Quita rutas y caracteres de control del nombre que manda el navegador. */
    private static String nombreSeguro(String original) {
        String nombre = original == null ? "archivo" : original.replaceAll("^.*[\\\\/]", "");
        nombre = nombre.replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (nombre.isEmpty()) {
            nombre = "archivo";
        }
        return nombre.length() > 200 ? nombre.substring(nombre.length() - 200) : nombre;
    }

    private static String nombreCompleto(RespuestaDenunciado r) {
        return (r.getNombre() + " " + r.getApellido1() + (r.getApellido2() != null ? " " + r.getApellido2() : "")).trim();
    }

    private static String mayusculas(String valor) {
        return valor == null ? null : valor.toUpperCase(Locale.ROOT);
    }

    private static String capitalizar(String texto) {
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
