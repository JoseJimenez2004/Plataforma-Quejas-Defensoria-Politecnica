/** Una queja previa que podría ser antecedente de la que se analiza. */
export interface Antecedente {
  /** Folio de la queja previa (FOL-...). */
  folioQueja: string;
  /** Si también pasó por Primer Contacto, su folio PC-... para abrirla. */
  folioPrimerContacto?: string;

  fecha?: string;
  asunto?: string;
  extracto?: string;
  unidadAcademica?: string;
  nombreQuejoso?: string;
  estatus?: string;

  /** 0-100 según el motor de búsqueda. */
  similitud: number;
  /** Por qué salió: "Mismo quejoso", "Misma unidad académica", "Hechos similares: ...". */
  coincidencias: string[];
  mismoQuejoso: boolean;
}

export interface BusquedaAntecedentes {
  folio: string;
  folioQueja: string;

  /** REGLAS_PROVISIONAL hoy; el modelo cuando esté listo. */
  motor: string;
  descripcionMotor: string;

  generadoEn: string;
  quejasAnalizadas: number;

  resultados: Antecedente[];
}
