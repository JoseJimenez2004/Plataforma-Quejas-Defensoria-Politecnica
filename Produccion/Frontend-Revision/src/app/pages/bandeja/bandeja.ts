import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import {
  BandejaResumen,
  CargaRecepcionista,
  FiltroBandeja,
  QuejaResumenBandeja,
} from '../../core/models/revision.models';

/** Cada cuánto se vuelve a pedir la bandeja mientras la pantalla está abierta: así cada
 * recepcionista ve casi en vivo qué quejas toman sus compañeras. */
const INTERVALO_REFRESCO_MS = 20000;

/** Quejas por página (la cola muestra 10 y se navega con el paginado). */
const TAMANIO_PAGINA = 10;

/** A partir de cuántos días sin que nadie la tome, la queja más antigua se marca en ámbar. */
const DIAS_ALERTA_ESPERA = 3;

interface OpcionFiltro {
  valor: FiltroBandeja;
  etiqueta: string;
  ayuda: string;
}

@Component({
  selector: 'app-bandeja',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './bandeja.html',
  styleUrl: './bandeja.scss',
})
export class Bandeja implements OnInit, OnDestroy {
  readonly opcionesFiltro: OpcionFiltro[] = [
    { valor: 'todas', etiqueta: 'Toda la cola', ayuda: 'Por atender y en atención' },
    { valor: 'disponibles', etiqueta: 'Disponibles', ayuda: 'Nadie las ha tomado' },
    { valor: 'mias', etiqueta: 'Mis quejas', ayuda: 'Las que tienes abiertas' },
    { valor: 'corregidas', etiqueta: 'Corregidas', ayuda: 'Reenviadas tras observaciones' },
    { valor: 'en_atencion', etiqueta: 'En atención', ayuda: 'Abiertas por el equipo' },
  ];

  resumen: BandejaResumen | null = null;
  cargando = true;
  filtro: FiltroBandeja = 'todas';
  busqueda = '';
  pagina = 0;
  soltando = '';
  /** "Jueves, 15 de octubre de 2026" (igual que el panel de Primer Contacto). */
  readonly fechaHoy = (() => {
    const texto = new Intl.DateTimeFormat('es-MX', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(new Date());
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  })();

  private intervaloId: ReturnType<typeof setInterval> | null = null;
  private busquedaId: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private revisionService: RevisionService,
    private toast: ToastService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargar();
    this.intervaloId = setInterval(() => this.cargar(true), INTERVALO_REFRESCO_MS);
  }

  ngOnDestroy(): void {
    if (this.intervaloId !== null) {
      clearInterval(this.intervaloId);
    }
    if (this.busquedaId !== null) {
      clearTimeout(this.busquedaId);
    }
  }

  // ---------------- filtros, búsqueda y paginado ----------------

  cambiarFiltro(filtro: FiltroBandeja): void {
    if (this.filtro === filtro) {
      return;
    }
    this.filtro = filtro;
    this.pagina = 0;
    this.cargar();
  }

  /** Espera a que la recepcionista deje de escribir para no pedir una página por tecla. */
  buscar(): void {
    if (this.busquedaId !== null) {
      clearTimeout(this.busquedaId);
    }
    this.busquedaId = setTimeout(() => {
      this.pagina = 0;
      this.cargar();
    }, 350);
  }

  limpiarBusqueda(): void {
    this.busqueda = '';
    this.pagina = 0;
    this.cargar();
  }

  irAPagina(pagina: number): void {
    if (!this.resumen || pagina < 0 || pagina >= this.resumen.totalPaginas || pagina === this.pagina) {
      return;
    }
    this.pagina = pagina;
    this.cargar();
  }

  /** Números de página a mostrar: primera, última y las cercanas a la actual, con "…" en medio. */
  get paginasVisibles(): (number | null)[] {
    const total = this.resumen?.totalPaginas ?? 0;
    const paginas: (number | null)[] = [];
    for (let i = 0; i < total; i++) {
      const cerca = Math.abs(i - this.pagina) <= 1;
      if (i === 0 || i === total - 1 || cerca) {
        paginas.push(i);
      } else if (paginas[paginas.length - 1] !== null) {
        paginas.push(null);
      }
    }
    return paginas;
  }

  get rangoMostrado(): string {
    if (!this.resumen || this.resumen.totalElementos === 0) {
      return '';
    }
    const desde = this.resumen.pagina * this.resumen.tamanio + 1;
    const hasta = desde + this.resumen.lista.length - 1;
    return `Mostrando ${desde}–${hasta} de ${this.resumen.totalElementos}`;
  }

  /** Cuántas quejas hay en cada filtro (para los contadores de las pestañas). */
  conteoFiltro(filtro: FiltroBandeja): number | null {
    const i = this.resumen?.indicadores;
    if (!i) {
      return null;
    }
    switch (filtro) {
      case 'todas':
        return i.porAtender + i.enAtencion;
      case 'disponibles':
        return i.porAtender;
      case 'mias':
        return i.enAtencionMias;
      case 'corregidas':
        return i.corregidas;
      case 'en_atencion':
        return i.enAtencion;
    }
  }

  // ---------------- acciones sobre una fila ----------------

  bloqueadaPorOtro(item: QuejaResumenBandeja): boolean {
    return item.estatus === 'EN_VALIDACION' && !item.esMia;
  }

  atender(item: QuejaResumenBandeja): void {
    this.router.navigate(['/validacion', item.numeroFolio]);
  }

  soltar(item: QuejaResumenBandeja): void {
    this.soltando = item.numeroFolio;
    this.revisionService.liberar(item.numeroFolio).subscribe({
      next: () => {
        this.soltando = '';
        this.toast.exito(`La queja ${item.numeroFolio} regresó a la cola.`);
        this.cargar(true);
      },
      error: (err) => {
        this.soltando = '';
        this.toast.error(err?.error?.mensaje ?? 'No se pudo soltar la queja.');
        this.cdr.detectChanges();
      },
    });
  }

  // ---------------- textos de apoyo ----------------

  /** "hace 5 min", "hace 3 h", "hace 2 días". */
  hace(fecha?: string | null): string {
    if (!fecha) {
      return '';
    }
    const minutos = Math.max(0, Math.floor((Date.now() - new Date(fecha).getTime()) / 60000));
    if (minutos < 60) {
      return minutos <= 1 ? 'hace un momento' : `hace ${minutos} min`;
    }
    const horas = Math.floor(minutos / 60);
    if (horas < 24) {
      return `hace ${horas} h`;
    }
    const dias = Math.floor(horas / 24);
    return dias === 1 ? 'hace 1 día' : `hace ${dias} días`;
  }

  /** Cuánto lleva esperando la queja más antigua, en el formato más claro ("2 días", "5 h"). */
  get esperaMasLarga(): string {
    const fecha = this.resumen?.indicadores.fechaMasAntigua;
    if (!fecha) {
      return '—';
    }
    const horas = Math.max(0, (Date.now() - new Date(fecha).getTime()) / 3600000);
    if (horas < 1) {
      return `${Math.max(1, Math.round(horas * 60))} min`;
    }
    if (horas < 24) {
      return `${Math.floor(horas)} h`;
    }
    const dias = Math.floor(horas / 24);
    return dias === 1 ? '1 día' : `${dias} días`;
  }

  get esperaEnAlerta(): boolean {
    const fecha = this.resumen?.indicadores.fechaMasAntigua;
    return !!fecha && Date.now() - new Date(fecha).getTime() >= DIAS_ALERTA_ESPERA * 86400000;
  }

  readonly diasAlertaEspera = DIAS_ALERTA_ESPERA;

  get tiempoPromedio(): string {
    const horas = this.resumen?.indicadores.horasPromedioAtencion;
    if (horas === null || horas === undefined) {
      return '—';
    }
    if (horas < 1) {
      return `${Math.max(1, Math.round(horas * 60))} min`;
    }
    if (horas < 48) {
      return `${Math.round(horas)} h`;
    }
    return `${(horas / 24).toFixed(1)} días`;
  }

  /** Ancho de la barra de "atendidas hoy" relativo a quien más lleva. */
  anchoBarra(persona: CargaRecepcionista): number {
    const maximo = Math.max(1, ...(this.resumen?.equipo ?? []).map((p) => p.atendidasHoy));
    return Math.round((persona.atendidasHoy / maximo) * 100);
  }

  // ---------------- carga ----------------

  private cargar(silencioso = false): void {
    if (!silencioso) {
      this.cargando = true;
    }
    this.revisionService.bandeja(this.filtro, this.busqueda, this.pagina, TAMANIO_PAGINA).subscribe({
      next: (resumen) => {
        // Si la página quedó vacía (p. ej. otra recepcionista atendió las últimas), se regresa
        // a la última que sí tiene quejas.
        if (resumen.lista.length === 0 && resumen.totalPaginas > 0 && this.pagina >= resumen.totalPaginas) {
          this.pagina = resumen.totalPaginas - 1;
          this.cargar(silencioso);
          return;
        }
        this.resumen = resumen;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        if (!silencioso) {
          this.toast.error('No se pudo cargar la bandeja de entrada.');
        }
        this.cdr.detectChanges();
      },
    });
  }
}
