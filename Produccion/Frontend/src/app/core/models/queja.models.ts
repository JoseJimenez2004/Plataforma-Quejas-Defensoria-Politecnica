export interface Queja {
  id: number;
  numeroFolio: string;
  correoInstitucional: string;
  motivo: string;
  descripcion: string;
  rutaEvidencia?: string;
  fechaCreacion: string;
}

export interface ValidarFolioRequest {
  folio: string;
  correo: string;
}

/** Estatus visual de una queja. El backend hoy no calcula/persiste un estatus de
 * trámite (solo guarda la queja) — esto es un valor de UI hasta que exista esa
 * lógica en queja-service. Ver docs/HALLAZGOS.md. */
export type EstatusQueja = 'Recibida' | 'En Revisión' | 'Finalizada';
