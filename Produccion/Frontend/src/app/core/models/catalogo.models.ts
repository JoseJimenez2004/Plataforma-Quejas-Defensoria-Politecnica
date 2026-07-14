/** Un renglón del catálogo de dependencias del IPN (secretarías, direcciones y las
 * unidades académicas: CECyT, ESIME, ESCOM, etc.). Viene de catalogo-service —
 * ver docs/CAMBIOS.md para el detalle de cómo se armó. */
export interface Dependencia {
  id: number;
  clave: string;
  clavePadre?: string | null;
  nombre: string;
  abreviatura?: string | null;
  tipo: string;
  categoria?: string | null;
  nivel: number;
  paginaManual?: number | null;
  activo: boolean;
  notas?: string | null;
}
