package ipn.escom.defensoria.queja_service.validacion;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import ipn.escom.defensoria.queja_service.dto.RegistroQuejaPublicaRequest;

/**
 * Valida y normaliza TODO lo que llega del formulario público (CU-Q01) antes de convertirlo
 * en una queja.
 *
 * Normaliza in-place sobre el request: los campos de texto quedan sin espacios sobrantes y el
 * correo en minúsculas, de modo que lo que se persiste es siempre la versión limpia.
 *
 * Este validador es la fuente de verdad. El frontend replica las mismas reglas para dar
 * retroalimentación inmediata, pero un cliente que se salte el formulario (Postman, un script)
 * choca igual contra estas validaciones.
 */
@Component
public class ValidadorQuejaPublica {

    private static final Pattern NOMBRE = Pattern.compile(ReglasQueja.REGEX_NOMBRE);
    private static final Pattern NUMERO_IDENTIFICACION =
            Pattern.compile(ReglasQueja.REGEX_NUMERO_IDENTIFICACION);

    private static final String TIPO_ALUMNO = "alumno";
    private static final String TIPO_EMPLEADO = "empleado";

    public ValidadorArchivos.ArchivosClasificados validar(RegistroQuejaPublicaRequest datos) {
        normalizarTextos(datos);

        validarAvisoPrivacidad(datos);
        validarDatosQuejoso(datos);
        validarDatosHechos(datos);
        validarDatosDenunciado(datos);
        validarDescripcion(datos);

        return ValidadorArchivos.validarYClasificar(datos.getArchivos());
    }

    // ---------------------------------------------------------------------------------------

    private void normalizarTextos(RegistroQuejaPublicaRequest datos) {
        datos.setNombre(Textos.normalizar(datos.getNombre()));
        datos.setApellido1(Textos.normalizar(datos.getApellido1()));
        datos.setApellido2(Textos.normalizar(datos.getApellido2()));
        datos.setNombreDenunciado(Textos.normalizar(datos.getNombreDenunciado()));
        datos.setApellido1Denunciado(Textos.normalizar(datos.getApellido1Denunciado()));
        datos.setApellido2Denunciado(Textos.normalizar(datos.getApellido2Denunciado()));
        datos.setNumeroIdentificacion(Textos.normalizar(datos.getNumeroIdentificacion()));
        datos.setUnidadAcademicaClave(Textos.normalizar(datos.getUnidadAcademicaClave()));
        if (datos.getDescripcion() != null) {
            datos.setDescripcion(datos.getDescripcion().trim());
        }
    }

    private void validarAvisoPrivacidad(RegistroQuejaPublicaRequest datos) {
        if (!Boolean.TRUE.equals(datos.getAvisoPrivacidadAceptado())) {
            throw new ValidacionException(
                    "Debes leer y aceptar el aviso de privacidad para poder presentar tu queja.");
        }
    }

    private void validarDatosQuejoso(RegistroQuejaPublicaRequest datos) {
        validarNombre(datos.getNombre(), "el nombre", true);
        validarNombre(datos.getApellido1(), "el primer apellido", true);
        validarNombre(datos.getApellido2(), "el segundo apellido", false);

        datos.setCorreo(ValidadorCorreo.validarYNormalizar(datos.getCorreo()));

        ValidadorFechas.validarFechaNacimiento(datos.getFechaNacimiento());

        validarTipoIdentificacion(datos.getTipoIdentificacion());
        validarNumeroIdentificacion(datos.getNumeroIdentificacion(), datos.getTipoIdentificacion());
    }

    private void validarDatosHechos(RegistroQuejaPublicaRequest datos) {
        if (Textos.esVacio(datos.getUnidadAcademicaClave())) {
            throw new ValidacionException("Selecciona el lugar donde sucedieron los hechos.");
        }
        ValidadorFechas.validarFechaHechos(datos.getFechaHechos(), datos.getFechaNacimiento());
    }

    private void validarDatosDenunciado(RegistroQuejaPublicaRequest datos) {
        validarNombre(datos.getNombreDenunciado(), "el nombre del denunciado", false);
        validarNombre(datos.getApellido1Denunciado(), "el primer apellido del denunciado", false);
        validarNombre(datos.getApellido2Denunciado(),
                "el segundo apellido del denunciado", false);
    }

    private void validarDescripcion(RegistroQuejaPublicaRequest datos) {
        String descripcion = datos.getDescripcion();
        if (Textos.esVacio(descripcion)) {
            throw new ValidacionException("Falta la descripción de los hechos.");
        }
        if (descripcion.length() < ReglasQueja.DESCRIPCION_LONGITUD_MINIMA) {
            throw new ValidacionException("Describe los hechos con al menos "
                    + ReglasQueja.DESCRIPCION_LONGITUD_MINIMA + " caracteres.");
        }
        if (descripcion.length() > ReglasQueja.DESCRIPCION_LONGITUD_MAXIMA) {
            throw new ValidacionException("La descripción de los hechos no puede exceder "
                    + ReglasQueja.DESCRIPCION_LONGITUD_MAXIMA + " caracteres.");
        }
    }

    // ---------------------------------------------------------------------------------------

    /**
     * @param etiqueta cómo se llama el campo en el mensaje de error ("el nombre", "el primer
     *                 apellido del denunciado"...), para que el usuario sepa cuál corregir.
     */
    private void validarNombre(String valor, String etiqueta, boolean obligatorio) {
        if (Textos.esVacio(valor)) {
            if (obligatorio) {
                throw new ValidacionException("Falta " + etiqueta + ".");
            }
            return;
        }
        if (valor.length() < ReglasQueja.NOMBRE_LONGITUD_MINIMA) {
            throw new ValidacionException("Revisa " + etiqueta + ": debe tener al menos "
                    + ReglasQueja.NOMBRE_LONGITUD_MINIMA + " letras.");
        }
        if (valor.length() > ReglasQueja.NOMBRE_LONGITUD_MAXIMA) {
            throw new ValidacionException("Revisa " + etiqueta + ": no puede exceder "
                    + ReglasQueja.NOMBRE_LONGITUD_MAXIMA + " caracteres.");
        }
        if (!NOMBRE.matcher(valor).matches()) {
            throw new ValidacionException("Revisa " + etiqueta
                    + ": solo se permiten letras (se aceptan acentos, ñ, guion y apóstrofe). "
                    + "No se admiten números ni otros símbolos.");
        }
    }

    private void validarTipoIdentificacion(String tipo) {
        if (!TIPO_ALUMNO.equalsIgnoreCase(tipo) && !TIPO_EMPLEADO.equalsIgnoreCase(tipo)) {
            throw new ValidacionException("Indica si eres alumno o empleado.");
        }
    }

    private void validarNumeroIdentificacion(String numero, String tipo) {
        String etiqueta = TIPO_EMPLEADO.equalsIgnoreCase(tipo)
                ? "El número de empleado"
                : "El número de boleta";

        if (Textos.esVacio(numero)) {
            throw new ValidacionException("Falta " + etiqueta.toLowerCase() + ".");
        }
        if (!NUMERO_IDENTIFICACION.matcher(numero).matches()) {
            throw new ValidacionException(etiqueta + " solo puede contener números, "
                    + "con un máximo de " + ReglasQueja.NUMERO_IDENTIFICACION_LONGITUD_MAXIMA
                    + " dígitos.");
        }
    }
}
