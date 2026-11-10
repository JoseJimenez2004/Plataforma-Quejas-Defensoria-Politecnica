/**
 * Paginado en el navegador con el mismo aspecto que el Historial de Recepción:
 * "Mostrando 1–10 de 154" y botones ‹ 1 2 3 … 16 ›.
 */
export class Paginado<T> {
  pagina = 0;
  tamanio: number;

  constructor(tamanio = 10) {
    this.tamanio = tamanio;
  }

  totalPaginas(total: number): number {
    return Math.max(1, Math.ceil(total / this.tamanio));
  }

  /** Corrige la página si la lista se hizo más corta (por un filtro). */
  ajustar(total: number): void {
    this.pagina = Math.min(this.pagina, this.totalPaginas(total) - 1);
  }

  cortar(lista: T[]): T[] {
    this.ajustar(lista.length);
    const desde = this.pagina * this.tamanio;
    return lista.slice(desde, desde + this.tamanio);
  }

  /** Números de página (0-based); null = salto "…". */
  paginasVisibles(total: number): (number | null)[] {
    const n = this.totalPaginas(total);
    const paginas: (number | null)[] = [];
    for (let i = 0; i < n; i++) {
      if (i === 0 || i === n - 1 || Math.abs(i - this.pagina) <= 1) {
        paginas.push(i);
      } else if (paginas[paginas.length - 1] !== null) {
        paginas.push(null);
      }
    }
    return paginas;
  }

  rango(total: number): string {
    if (total === 0) return '';
    const desde = this.pagina * this.tamanio + 1;
    const hasta = Math.min(desde + this.tamanio - 1, total);
    return `Mostrando ${desde}–${hasta} de ${total}`;
  }

  irA(p: number, total: number): void {
    this.pagina = Math.max(0, Math.min(p, this.totalPaginas(total) - 1));
  }
}

/** Días naturales desde '16/10/2026' hasta hoy (null si la fecha no es válida). */
export function diasDesde(fechaDdMmAaaa: string | null | undefined): number | null {
  if (!fechaDdMmAaaa) return null;
  const [d, m, a] = fechaDdMmAaaa.split('/').map(Number);
  if (!d || !m || !a) return null;
  const hoy = new Date();
  const inicioHoy = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate()).getTime();
  return Math.max(0, Math.round((inicioHoy - new Date(a, m - 1, d).getTime()) / 86_400_000));
}

/** '16/10/2026' -> 'hoy', 'hace 1 día', 'hace 24 días'. */
export function haceCuanto(fechaDdMmAaaa: string | null | undefined): string {
  const dias = diasDesde(fechaDdMmAaaa);
  if (dias === null) return '';
  if (dias <= 0) return 'hoy';
  return dias === 1 ? 'hace 1 día' : `hace ${dias} días`;
}
