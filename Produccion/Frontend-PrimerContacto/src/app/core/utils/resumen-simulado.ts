/**
 * Resumen SIMULADO de una queja, mientras se conecta el modelo de resumen (resumen-service).
 *
 * Es solo una aproximación en el navegador para mostrar cómo se verá la función:
 *   - resumen: la primera oración y la de más "impacto", en su orden original;
 *   - frases de impacto: las oraciones que mencionan violencia, amenazas, acoso,
 *     discriminación, etc., de la más a la menos cargada.
 * Cuando el modelo esté listo, este archivo se sustituye por la llamada al servicio.
 */

export interface ResumenSimulado {
  resumen: string;
  frasesImpacto: string[];
}

/** Raíces de palabras que suelen marcar los hechos graves de una queja. */
const RAICES_IMPACTO = [
  'golpe', 'golpeó', 'agred', 'amenaz', 'acos', 'hostig', 'humill', 'insult', 'grit',
  'discrimin', 'ofens', 'violen', 'abus', 'intimid', 'burl', 'exhib', 'reprob', 'castig',
  'extors', 'sexual', 'miedo', 'lesion', 'empuj', 'represal', 'toc', 'oblig', 'despid',
  'inútil', 'inutil', 'denigr', 'excluy', 'negó', 'nego'
];

const MAX_RESUMEN = 380;
const MAX_FRASES = 3;

export function generarResumenSimulado(texto?: string | null): ResumenSimulado {
  const limpio = (texto ?? '').replace(/\s+/g, ' ').trim();
  if (!limpio) {
    return { resumen: '', frasesImpacto: [] };
  }

  const oraciones = limpio
    .split(/(?<=[.!?;])\s+/)
    .map(o => o.trim())
    .filter(o => o.length > 12);

  if (oraciones.length === 0) {
    return { resumen: recortar(limpio, MAX_RESUMEN), frasesImpacto: [] };
  }

  const puntuadas = oraciones.map((oracion, indice) => ({
    oracion,
    indice,
    puntos: puntaje(oracion)
  }));

  const conImpacto = puntuadas
    .filter(o => o.puntos > 0)
    .sort((a, b) => b.puntos - a.puntos || a.indice - b.indice);

  // Resumen: primera oración + la de más impacto (si es otra), en orden de lectura.
  const elegidas = [puntuadas[0]];
  if (conImpacto.length > 0 && conImpacto[0].indice !== 0) {
    elegidas.push(conImpacto[0]);
  } else if (puntuadas.length > 1) {
    elegidas.push(puntuadas[1]);
  }
  const resumen = elegidas
    .sort((a, b) => a.indice - b.indice)
    .map(o => o.oracion)
    .join(' ');

  return {
    resumen: recortar(resumen, MAX_RESUMEN),
    frasesImpacto: conImpacto.slice(0, MAX_FRASES).map(o => recortar(o.oracion, 220))
  };
}

function puntaje(oracion: string): number {
  const minusculas = oracion.toLowerCase();
  return RAICES_IMPACTO.reduce((total, raiz) => total + (minusculas.includes(raiz) ? 1 : 0), 0);
}

function recortar(texto: string, max: number): string {
  return texto.length <= max ? texto : texto.slice(0, max - 1).trimEnd() + '…';
}
