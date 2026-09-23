/**
 * Reglas de validación del formulario de quejas (CU-Q01).
 *
 * ESPEJO del backend: Backend/queja-service/.../validacion/ReglasQueja.java.
 * Si un valor cambia aquí, hay que cambiarlo también allá — y al revés. El servidor es la
 * fuente de verdad; esto solo existe para que el usuario reciba el error al instante en vez
 * de después de enviar el formulario.
 */

// ---- Nombres y apellidos ---------------------------------------------------------------
/**
 * Letras (con acentos y ñ), permitiendo espacio interno, apóstrofe y guion entre palabras.
 * A propósito NO es "solo A-Z": eso rechazaría nombres reales como "María José",
 * "D'Angelo" o "Pérez-Gómez".
 */
export const PATRON_NOMBRE = "[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+([ '\\-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*";
export const REGEX_NOMBRE = new RegExp(`^${PATRON_NOMBRE}$`);
export const NOMBRE_LONGITUD_MINIMA = 2;
export const NOMBRE_LONGITUD_MAXIMA = 50;

// ---- Boleta / número de empleado --------------------------------------------------------
export const PATRON_NUMERO_IDENTIFICACION = '[0-9]{1,10}';
export const REGEX_NUMERO_IDENTIFICACION = /^[0-9]{1,10}$/;
export const NUMERO_IDENTIFICACION_LONGITUD_MAXIMA = 10;

// ---- Fechas ------------------------------------------------------------------------------
export const FECHA_NACIMIENTO_MINIMA = '1920-01-01';

/**
 * 31 de diciembre del año pasado: durante el año en curso nadie puede declarar haber nacido
 * ese mismo año. Se calcula en tiempo de ejecución para que la regla no caduque en enero.
 */
export function fechaNacimientoMaxima(): string {
  return `${new Date().getFullYear() - 1}-12-31`;
}

export function hoyIso(): string {
  return new Date().toISOString().split('T')[0];
}

/**
 * Comprueba que una cadena sea una fecha REAL en formato yyyy-mm-dd.
 *
 * No basta con comparar cadenas contra los límites: "2004-21-19" es mayor que "1920-01-01" y
 * menor que "2025-12-31", así que pasaba las dos comparaciones y llegaba hasta el backend,
 * donde reventaba al convertirla a LocalDate. Aquí se reconstruye la fecha y se verifica que
 * el año, mes y día que devuelve sean los mismos que se escribieron — eso descarta tanto los
 * meses inexistentes como los "31 de febrero", que JavaScript convertiría en 3 de marzo.
 */
export function esFechaValida(iso: string | null | undefined): boolean {
  if (!iso || !/^\d{4}-\d{2}-\d{2}$/.test(iso)) {
    return false;
  }
  const [anio, mes, dia] = iso.split('-').map(Number);
  const fecha = new Date(anio, mes - 1, dia);
  return (
    fecha.getFullYear() === anio && fecha.getMonth() === mes - 1 && fecha.getDate() === dia
  );
}

// ---- Descripción --------------------------------------------------------------------------
export const DESCRIPCION_LONGITUD_MINIMA = 20;
export const DESCRIPCION_LONGITUD_MAXIMA = 4000;

// ---- Identificación oficial ---------------------------------------------------------------
/** Solo imágenes: se quitó PDF por decisión del usuario (validación CU-Q01). */
export const IDENTIFICACION_TIPOS_MIME = ['image/jpeg', 'image/png'];
export const IDENTIFICACION_EXTENSIONES = ['.jpg', '.jpeg', '.png'];
export const IDENTIFICACION_TAMANIO_MAXIMO = 3 * 1024 * 1024;
export const IDENTIFICACION_CANTIDAD_MAXIMA = 2;
/** Prefijo con el que viaja la credencial dentro de la lista de archivos (ver backend). */
export const PREFIJO_IDENTIFICACION = 'IDENTIFICACION_';

// ---- Evidencias ----------------------------------------------------------------------------
export const EVIDENCIA_EXTENSIONES = ['.pdf', '.jpg', '.jpeg', '.png', '.mp4', '.mp3'];
export const EVIDENCIA_TAMANIO_MAXIMO = 30 * 1024 * 1024;
/** Por debajo del max-request-size (100MB) del backend, con margen para el resto del form. */
export const EVIDENCIA_TAMANIO_TOTAL_MAXIMO = 95 * 1024 * 1024;

// ---- Aviso de privacidad --------------------------------------------------------------------
export const AVISO_PRIVACIDAD_VERSION = '1.0';

// ============================================================================================
// Correo — validación en DOS NIVELES (misma lógica que ValidadorCorreo.java)
// ============================================================================================

const CORREO_FORMATO_GENERAL =
  /^[A-Za-z0-9._%+-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\.[A-Za-z]{2,}$/;
const CORREO_LOCAL_GOOGLE = /^[A-Za-z0-9.]{6,30}$/;
const DOMINIOS_GOOGLE = ['gmail.com', 'googlemail.com'];
const CORREO_LONGITUD_MAXIMA = 254;

/**
 * Devuelve el mensaje de error, o null si el correo es válido.
 *
 * Nivel 1 — formato general, para cualquier dominio.
 * Nivel 2 — restricciones de Google, SOLO si el dominio es gmail.com / googlemail.com.
 *
 * Esas restricciones (nada de & = _ ' - + , < > ni espacios) son de Gmail al crear una
 * cuenta, no del correo electrónico en general: aplicarlas a todos los dominios rechazaría
 * direcciones válidas y en uso como juan-perez@outlook.com o maria_lopez@yahoo.com.mx.
 */
export function validarCorreo(valor: string | null | undefined): string | null {
  const correo = (valor ?? '').trim().toLowerCase();

  if (!correo) {
    return 'Escribe tu correo electrónico.';
  }
  if (correo.length > CORREO_LONGITUD_MAXIMA) {
    return 'El correo electrónico es demasiado largo.';
  }
  if (!CORREO_FORMATO_GENERAL.test(correo)) {
    return 'El correo no tiene un formato válido. Ejemplo: nombre@dominio.com';
  }

  const arroba = correo.lastIndexOf('@');
  const parteLocal = correo.slice(0, arroba);
  const dominio = correo.slice(arroba + 1);

  if (parteLocal.startsWith('.') || parteLocal.endsWith('.')) {
    return 'El correo no puede empezar ni terminar con un punto antes de la arroba.';
  }
  if (parteLocal.includes('..')) {
    return 'El correo no puede tener dos puntos seguidos.';
  }
  if (DOMINIOS_GOOGLE.includes(dominio) && !CORREO_LOCAL_GOOGLE.test(parteLocal)) {
    return 'Una dirección de Gmail solo admite letras, números y puntos antes de la arroba, con un mínimo de 6 y un máximo de 30 caracteres.';
  }

  return null;
}

/** Mensaje de error de un nombre/apellido, o null si es válido. */
export function validarNombre(
  valor: string | null | undefined,
  etiqueta: string,
  obligatorio: boolean,
): string | null {
  const texto = (valor ?? '').trim().replace(/\s+/g, ' ');

  if (!texto) {
    return obligatorio ? `Escribe ${etiqueta}.` : null;
  }
  if (texto.length < NOMBRE_LONGITUD_MINIMA) {
    return `${capitalizar(etiqueta)} debe tener al menos ${NOMBRE_LONGITUD_MINIMA} letras.`;
  }
  if (texto.length > NOMBRE_LONGITUD_MAXIMA) {
    return `${capitalizar(etiqueta)} no puede exceder ${NOMBRE_LONGITUD_MAXIMA} caracteres.`;
  }
  if (!REGEX_NOMBRE.test(texto)) {
    return `${capitalizar(etiqueta)} solo admite letras (se aceptan acentos, ñ, guion y apóstrofe), sin números ni otros símbolos.`;
  }
  return null;
}

function capitalizar(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

/** "1024" -> "1.0 KB" */
export function formatearTamanio(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
