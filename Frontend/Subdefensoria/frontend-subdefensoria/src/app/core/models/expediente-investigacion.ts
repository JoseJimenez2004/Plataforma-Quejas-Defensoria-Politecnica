export interface ExpedienteInvestigacion {
  id: number;
  quejaId: number;
  folio: string;
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
