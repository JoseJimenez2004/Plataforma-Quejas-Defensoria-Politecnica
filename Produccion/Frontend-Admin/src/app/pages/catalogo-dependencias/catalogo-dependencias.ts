import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { concat, Observable, of } from 'rxjs';
import { catchError, map, toArray } from 'rxjs/operators';

import {
  CatalogoAdminService,
  DependenciaRequest,
} from '../../core/services/catalogo-admin.service';
import { ToastService } from '../../core/services/toast.service';
import { Dependencia } from '../../core/models/admin.models';
import { TITULARES_IPN, TitularIpn, FECHA_DIRECTORIO_IPN, titularDe } from '../../core/data/titulares-ipn';

/** Sección del directorio completo (public/datos/directorio-ipn.json). */
interface SeccionDirectorio {
  clave: string | null;
  area: string;
  seccion: string;
  nivel: string;
  rama: string | null;
  direccion: string;
  conmutador: string;
  web: string;
  personas: { grado: string; nombre: string; cargo: string; correo: string; extension: string }[];
}

interface Directorio {
  fuente: string;
  consultado: string;
  secciones: SeccionDirectorio[];
}

/** Diferencia entre el catálogo y el directorio del IPN para una dependencia. */
interface Cambio {
  clave: string;
  nombre: string;
  tipo: 'nueva' | 'titular' | 'correo' | 'ambos';
  antes: string;
  despues: string;
  titular: TitularIpn;
  dep?: Dependencia;
}

/** Unidades que existen en el IPN y no estaban en el catálogo (mismas que el script SQL). */
const NUEVAS: { clave: string; nombre: string; abreviatura: string; tipo: string; categoria: string }[] = [
  { clave: 'CECYT20', nombre: 'CECyT No. 20 "Natalia Serdán Alatriste"', abreviatura: 'CECyT 20', tipo: 'Unidad Académica', categoria: 'Unidades Académicas de Nivel Medio Superior Interdisciplinarias' },
  { clave: 'ESIQIE', nombre: 'Escuela Superior de Ingeniería Química e Industrias Extractivas', abreviatura: 'ESIQIE', tipo: 'Unidad Académica', categoria: 'Unidades Académicas de Nivel Superior, Rama de Ingeniería y Ciencias Físico Matemáticas' },
  { clave: 'ENBA', nombre: 'Escuela Nacional de Biblioteconomía y Archivonomía', abreviatura: 'ENBA', tipo: 'Unidad Académica', categoria: 'Unidades Académicas de Nivel Superior, Rama de Ciencias Sociales y Administrativas' },
  { clave: 'UPIEM', nombre: 'Unidad Profesional Interdisciplinaria de Energía y Movilidad', abreviatura: 'UPIEM', tipo: 'Unidad Académica', categoria: 'Unidades Académicas de Nivel Superior Interdisciplinarias' },
  { clave: 'UPIIH', nombre: 'Unidad Profesional Interdisciplinaria de Ingeniería, Campus Hidalgo', abreviatura: 'UPIIH', tipo: 'Unidad Académica', categoria: 'Unidades Académicas de Nivel Superior Interdisciplinarias' },
  { clave: 'UPIIP', nombre: 'Unidad Profesional Interdisciplinaria de Ingeniería, Campus Palenque', abreviatura: 'UPIIP', tipo: 'Unidad Académica', categoria: 'Unidades Académicas de Nivel Superior Interdisciplinarias' },
  { clave: 'UPIIT', nombre: 'Unidad Profesional Interdisciplinaria en Ingeniería, Campus Tlaxcala', abreviatura: 'UPIIT', tipo: 'Unidad Académica', categoria: 'Unidades Académicas de Nivel Superior Interdisciplinarias' },
  { clave: 'UPIIAP', nombre: 'Unidad Profesional Interdisciplinaria de Ingeniería, Campus "Alejo Peralta" Puebla', abreviatura: 'UPIIAP', tipo: 'Unidad Académica', categoria: 'Unidades Académicas de Nivel Superior Interdisciplinarias' },
  { clave: 'CIITA-JUA', nombre: 'Centro de Innovación e Integración de Tecnologías Avanzadas, Unidad Ciudad Juárez', abreviatura: 'CIITA Cd. Juárez', tipo: 'Centro', categoria: 'Centros de Innovación e Integración de Tecnologías Avanzadas' },
  { clave: 'CIITA-PAP', nombre: 'Centro de Innovación e Integración de Tecnologías Avanzadas, Unidad Papantla', abreviatura: 'CIITA Papantla', tipo: 'Centro', categoria: 'Centros de Innovación e Integración de Tecnologías Avanzadas' },
  { clave: 'CIITA-PUE', nombre: 'Centro de Innovación e Integración de Tecnologías Avanzadas, Unidad Puebla', abreviatura: 'CIITA Puebla', tipo: 'Centro', categoria: 'Centros de Innovación e Integración de Tecnologías Avanzadas' },
];

@Component({
  selector: 'app-catalogo-dependencias',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalogo-dependencias.html',
  styleUrl: './catalogo-dependencias.scss',
})
export class CatalogoDependencias implements OnInit {
  readonly fechaDirectorio = FECHA_DIRECTORIO_IPN;

  pestana: 'catalogo' | 'directorio' = 'catalogo';

  lista: Dependencia[] = [];
  cargando = true;
  importando = false;
  busqueda = '';
  soloSinTitular = false;

  mostrarModal = false;
  editandoClave: string | null = null;
  formulario: DependenciaRequest = this.formularioVacio();

  // ---- directorio IPN
  directorio: Directorio | null = null;
  cargandoDirectorio = false;
  busquedaDir = '';
  areaDir = '';
  areasDir: string[] = [];

  // ---- sincronización
  mostrarSincronizar = false;
  cambios: Cambio[] = [];
  sincronizando = false;

  constructor(
    private catalogoService: CatalogoAdminService,
    private http: HttpClient,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargarLista();
  }

  // ====================================================================== catálogo

  get listaFiltrada(): Dependencia[] {
    const termino = this.busqueda.trim().toLowerCase();
    return this.lista.filter((d) => {
      if (this.soloSinTitular && d.nombreTitular) {
        return false;
      }
      if (!termino) {
        return true;
      }
      return d.nombre.toLowerCase().includes(termino) ||
        (d.abreviatura ?? '').toLowerCase().includes(termino) ||
        d.clave.toLowerCase().includes(termino) ||
        (d.correoContacto ?? '').toLowerCase().includes(termino) ||
        (d.nombreTitular ?? '').toLowerCase().includes(termino);
    });
  }

  get conTitular(): number {
    return this.lista.filter((d) => d.nombreTitular).length;
  }

  /** El titular del catálogo no coincide con el del directorio del IPN. */
  desactualizada(d: Dependencia): TitularIpn | null {
    const t = titularDe(d.clave);
    if (!t) {
      return null;
    }
    const titularDistinto = !!t.titular && t.titular !== (d.nombreTitular ?? '');
    const correoDistinto = !!t.correo && t.correo !== (d.correoContacto ?? '');
    return titularDistinto || correoDistinto ? t : null;
  }

  private cargarLista(): void {
    this.cargando = true;
    this.catalogoService.listarTodas().subscribe({
      next: (lista) => {
        this.lista = lista;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('No se pudo cargar el catálogo de dependencias.');
        this.cdr.detectChanges();
      },
    });
  }

  abrirCrear(): void {
    this.editandoClave = null;
    this.formulario = this.formularioVacio();
    this.mostrarModal = true;
  }

  abrirEditar(dep: Dependencia): void {
    this.editandoClave = dep.clave;
    this.formulario = this.requestDe(dep);
    this.mostrarModal = true;
  }

  /** Abre el formulario de edición con el titular y correo del directorio ya puestos. */
  usarDirectorio(dep: Dependencia, t: TitularIpn): void {
    this.abrirEditar(dep);
    if (t.titular) {
      this.formulario.nombreTitular = t.titular;
    }
    if (t.correo) {
      this.formulario.correoContacto = t.correo;
    }
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.editandoClave = null;
  }

  guardar(): void {
    if (!this.formulario.clave || !this.formulario.nombre || !this.formulario.tipo) {
      this.toast.advertencia('Completa clave, nombre y tipo.');
      return;
    }

    const accion = this.editandoClave
      ? this.catalogoService.editar(this.editandoClave, this.formulario)
      : this.catalogoService.crear(this.formulario);

    accion.subscribe({
      next: () => {
        this.mostrarModal = false;
        this.toast.exito(this.editandoClave ? 'Dependencia actualizada.' : 'Dependencia creada.');
        this.cargarLista();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.toast.error(err?.error?.mensaje ?? 'No se pudo guardar la dependencia.');
        this.cdr.detectChanges();
      },
    });
  }

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo) return;

    this.importando = true;
    this.catalogoService.importarExcel(archivo).subscribe({
      next: (resumen) => {
        this.importando = false;
        this.toast.exito(
          `Importación completa: ${resumen.filasCreadas} creadas, ${resumen.filasActualizadas} actualizadas.`,
        );
        if (resumen.errores.length) {
          this.toast.advertencia(`${resumen.errores.length} filas con errores, revisa el formato del archivo.`);
        }
        this.cargarLista();
        this.cdr.detectChanges();
        input.value = '';
      },
      error: (err) => {
        this.importando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo importar el archivo.');
        this.cdr.detectChanges();
        input.value = '';
      },
    });
  }

  // ====================================================================== sincronización

  /** Compara el catálogo con el directorio del IPN y muestra la lista de cambios a aplicar. */
  abrirSincronizar(): void {
    const porClave = new Map(this.lista.map((d) => [d.clave, d]));
    const cambios: Cambio[] = [];
    for (const n of NUEVAS) {
      const t = titularDe(n.clave);
      if (!porClave.has(n.clave) && t) {
        cambios.push({ clave: n.clave, nombre: n.abreviatura, tipo: 'nueva', antes: 'No existe en el catálogo', despues: t.titular || n.nombre, titular: t });
      }
    }
    for (const t of TITULARES_IPN) {
      const d = porClave.get(t.clave);
      if (!d) {
        continue;
      }
      const tit = !!t.titular && t.titular !== (d.nombreTitular ?? '');
      const cor = !!t.correo && t.correo !== (d.correoContacto ?? '');
      if (tit || cor) {
        cambios.push({
          clave: d.clave,
          nombre: d.abreviatura || d.nombre,
          tipo: tit && cor ? 'ambos' : tit ? 'titular' : 'correo',
          antes: [tit ? d.nombreTitular || '(sin titular)' : '', cor ? d.correoContacto || '(sin correo)' : ''].filter(Boolean).join(' · '),
          despues: [tit ? t.titular : '', cor ? t.correo : ''].filter(Boolean).join(' · '),
          titular: t,
          dep: d,
        });
      }
    }
    this.cambios = cambios;
    this.mostrarSincronizar = true;
  }

  cerrarSincronizar(): void {
    if (!this.sincronizando) {
      this.mostrarSincronizar = false;
    }
  }

  /** El catálogo original usa tipos en texto libre ("Unidad Académica"); se muestran tal cual. */
  get tipoFueraDeLista(): boolean {
    return !!this.formulario.tipo && !['UNIDAD_ACADEMICA', 'AREA_CENTRAL', 'SECRETARIA', 'DIRECCION'].includes(this.formulario.tipo);
  }

  aplicarSincronizacion(): void {
    if (!this.cambios.length) {
      this.mostrarSincronizar = false;
      return;
    }
    this.sincronizando = true;
    const tareas: Observable<boolean>[] = this.cambios.map((c) => {
      let peticion: Observable<unknown>;
      if (c.tipo === 'nueva') {
        const n = NUEVAS.find((x) => x.clave === c.clave)!;
        peticion = this.catalogoService.crear({
          clave: n.clave, nombre: n.nombre, abreviatura: n.abreviatura, tipo: n.tipo, categoria: n.categoria, nivel: 1,
          nombreTitular: c.titular.titular || undefined, correoContacto: c.titular.correo || undefined,
        });
      } else {
        const req = this.requestDe(c.dep!);
        if (c.titular.titular) req.nombreTitular = c.titular.titular;
        if (c.titular.correo) req.correoContacto = c.titular.correo;
        peticion = this.catalogoService.editar(c.clave, req);
      }
      return peticion.pipe(map(() => true), catchError(() => of(false)));
    });
    // Una petición a la vez para no saturar al catálogo.
    concat(...tareas).pipe(toArray()).subscribe((res) => {
      const ok = res.filter(Boolean).length;
      this.sincronizando = false;
      this.mostrarSincronizar = false;
      if (ok === res.length) {
        this.toast.exito(`Catálogo sincronizado con el directorio del IPN: ${ok} cambios aplicados.`);
      } else {
        this.toast.advertencia(`Se aplicaron ${ok} de ${res.length} cambios; revisa los restantes.`);
      }
      this.cargarLista();
      this.cdr.detectChanges();
    });
  }

  // ====================================================================== directorio IPN

  cambiarPestana(p: 'catalogo' | 'directorio'): void {
    this.pestana = p;
    if (p === 'directorio' && !this.directorio && !this.cargandoDirectorio) {
      this.cargandoDirectorio = true;
      this.http.get<Directorio>('datos/directorio-ipn.json').subscribe({
        next: (d) => {
          this.directorio = d;
          this.areasDir = [...new Set(d.secciones.map((s) => s.area))];
          this.cargandoDirectorio = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.cargandoDirectorio = false;
          this.toast.error('No se pudo cargar el directorio del IPN.');
          this.cdr.detectChanges();
        },
      });
    }
  }

  get seccionesFiltradas(): SeccionDirectorio[] {
    if (!this.directorio) {
      return [];
    }
    const t = this.normalizar(this.busquedaDir.trim());
    return this.directorio.secciones
      .filter((s) => !this.areaDir || s.area === this.areaDir)
      .map((s) => {
        if (!t) {
          return s;
        }
        const enSeccion = this.normalizar(`${s.seccion} ${s.clave ?? ''}`).includes(t);
        if (enSeccion) {
          return s;
        }
        const personas = s.personas.filter((p) => this.normalizar(`${p.nombre} ${p.cargo} ${p.correo}`).includes(t));
        return personas.length ? { ...s, personas } : null;
      })
      .filter((s): s is SeccionDirectorio => !!s)
      .slice(0, 60);
  }

  get totalPersonas(): number {
    return this.directorio?.secciones.reduce((n, s) => n + s.personas.length, 0) ?? 0;
  }

  copiar(texto: string): void {
    navigator.clipboard?.writeText(texto).then(
      () => this.toast.exito(`Copiado: ${texto}`),
      () => this.toast.advertencia('No se pudo copiar al portapapeles.'),
    );
  }

  private normalizar(s: string): string {
    return s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  }

  // ====================================================================== apoyo

  private requestDe(dep: Dependencia): DependenciaRequest {
    return {
      clave: dep.clave,
      clavePadre: dep.clavePadre,
      nombre: dep.nombre,
      abreviatura: dep.abreviatura,
      tipo: dep.tipo,
      categoria: dep.categoria,
      nivel: dep.nivel,
      correoContacto: dep.correoContacto,
      nombreTitular: dep.nombreTitular,
    };
  }

  private formularioVacio(): DependenciaRequest {
    return { clave: '', nombre: '', tipo: 'UNIDAD_ACADEMICA', nivel: 1 };
  }
}
