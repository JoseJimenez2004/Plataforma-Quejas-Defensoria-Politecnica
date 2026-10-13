export interface ExpedienteInvestigacion {
  id: number;

  folio: string;
  folioOrigen: string;
  /** Folio FOL-... de la queja (el que conoce el quejoso). */
  folioQueja?: string | null;

  quejosoNombre: string;
  unidadAcademica: string;

  asunto: string;
  descripcionHechos: string;

  fechaAdmision: string;

  abogadoAsesorId: number | null;
  abogadoAsesorNombre: string | null;

  estatus: string;
  observacionesAnalista: string | null;
}