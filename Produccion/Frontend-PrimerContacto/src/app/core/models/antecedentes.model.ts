/** De dónde sale un antecedente: quejas del sistema o casos históricos. */
export type OrigenAntecedente = 'SISTEMA' | 'HISTORICO';

/** Cómo se encontró: búsqueda manual, el modelo o el motor provisional por reglas. */
export type FuenteAntecedente = 'MANUAL' | 'MODELO' | 'REGLAS_PROVISIONAL';

/** Una queja previa que podría ser antecedente de la que se analiza. */
export interface Antecedente {
  /** Folio de la queja previa (FOL-..., o el del caso histórico). */
  folioQueja: string;
  /** Si también pasó por Primer Contacto, su folio PC-... para abrirla. */
  folioPrimerContacto?: string;

  origen: OrigenAntecedente;

  fecha?: string;
  asunto?: string;
  extracto?: string;
  /** Narrativa completa, para el resumen. */
  descripcion?: string;
  unidadAcademica?: string;
  nombreQuejoso?: string;
  nombreDenunciado?: string;
  estatus?: string;
  /** Solo históricos: cómo terminó el caso. */
  resultado?: string;

  /** 0-100 según el motor de búsqueda (en la búsqueda manual solo sirve para ordenar). */
  similitud: number;
  /** Por qué salió: "Mismo quejoso", "Denunciado: ...", "Narrativa similar (modelo)"... */
  coincidencias: string[];
  mismoQuejoso: boolean;
}

export interface BusquedaAntecedentes {
  folio: string;
  folioQueja: string;

  /** MODELO, REGLAS_PROVISIONAL (si el modelo no está disponible) o MANUAL. */
  motor: FuenteAntecedente;
  descripcionMotor: string;

  generadoEn: string;
  quejasAnalizadas: number;

  resultados: Antecedente[];

  /** Avisos para el analista: el modelo no respondió, el histórico no está disponible... */
  avisos?: string[];
}

/** Antecedente elegido como final para el expediente. */
export interface AntecedenteGuardado {
  id?: number;
  origen: OrigenAntecedente;
  folioQueja: string;
  fuente: FuenteAntecedente;
  similitud?: number | null;

  asunto?: string;
  fecha?: string;
  nombreQuejoso?: string;
  nombreDenunciado?: string;
  unidadAcademica?: string;
  estatus?: string;
  folioPrimerContacto?: string;
  extracto?: string;

  analistaNombre?: string;
  fechaRegistro?: string;
}

/** Clave única de un antecedente, la misma en la pestaña manual y en la del modelo. */
export function claveAntecedente(a: { origen: string; folioQueja: string }): string {
  return `${a.origen}:${a.folioQueja}`;
}
