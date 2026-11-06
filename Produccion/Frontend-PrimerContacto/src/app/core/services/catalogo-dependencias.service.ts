import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, shareReplay } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Dependencia del catálogo del IPN (lo administra la Defensora en su panel). */
export interface Dependencia {
  clave: string;
  nombre: string;
  abreviatura: string | null;
  tipo: string | null;
  correoContacto: string | null;
  nombreTitular: string | null;
}

/** Quita acentos, mayúsculas y espacios de más para comparar nombres de unidad. */
export function normalizar(texto: string | null | undefined): string {
  return (texto ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/\s+/g, ' ').trim();
}

/**
 * Catálogo de dependencias (GET /api/catalogos/dependencias, público). Se pide una vez
 * y se guarda en memoria mientras dure la sesión.
 */
@Injectable({ providedIn: 'root' })
export class CatalogoDependenciasService {
  private lista$?: Observable<Dependencia[]>;

  constructor(private http: HttpClient) {}

  listar(): Observable<Dependencia[]> {
    if (!this.lista$) {
      this.lista$ = this.http.get<Dependencia[]>(`${environment.catalogosUrl}/dependencias`).pipe(
        catchError(() => {
          this.lista$ = undefined; // que el siguiente intento vuelva a pedirla
          return of([] as Dependencia[]);
        }),
        shareReplay(1)
      );
    }
    return this.lista$;
  }

  /** La dependencia que corresponde a la unidad del expediente (abreviatura, clave o nombre). */
  buscar(lista: Dependencia[], unidad: string | null | undefined): Dependencia | undefined {
    const u = normalizar(unidad);
    if (!u) return undefined;
    return lista.find(d => normalizar(d.abreviatura) === u)
      ?? lista.find(d => normalizar(d.clave) === u)
      ?? lista.find(d => normalizar(d.nombre) === u)
      ?? lista.find(d => !!d.abreviatura && u.split(/[^a-z0-9]+/).includes(normalizar(d.abreviatura)));
  }
}
