import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import {
  BandejaResumen,
  FiltroBandeja,
  QuejaResumenBandeja,
} from '../../core/models/revision.models';

/** Cada cuánto se vuelve a pedir la bandeja mientras la pantalla está abierta: así cada
 * recepcionista ve casi en vivo qué quejas toman sus compañeras. */
const INTERVALO_REFRESCO_MS = 20000;

/** Opciones de "quejas por página" que puede elegir la recepcionista. */
const OPCIONES_TAMANIO = [5, 10, 15, 20, 30, 50];

/** Tamaño por defecto si la recepcionista nunca ha elegido uno. */
const TAMANIO_POR_DEFECTO = 10;

/** Dónde se recuerda la elección (solo en este navegador). */
const CLAVE_TAMANIO = 'revision.bandeja.tamanioPagina';

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
  readonly opcionesTamanio = OPCIONES_TAMANIO;
  tamanioPagina = Bandeja.leerTamanioGuardado();
  soltando = '';
  buscandoSiguiente = false;
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

  /** La recepcionista elige cuántas quejas ver por página; se recuerda en su navegador. */
  cambiarTamanio(tamanio: number | string): void {
    const valor = Number(tamanio);
    if (!OPCIONES_TAMANIO.includes(valor) || valor === this.tamanioPagina) {
      return;
    }
    // Conserva aproximadamente la posición: la primera queja visible sigue en pantalla.
    const primera = this.pagina * this.tamanioPagina;
    this.tamanioPagina = valor;
    this.pagina = Math.floor(primera / valor);
    try {
      localStorage.setItem(CLAVE_TAMANIO, String(valor));
    } catch {
      /* navegador sin almacenamiento: solo dura esta sesión */
    }
    this.cargar();
  }

  private static leerTamanioGuardado(): number {
    try {
      const guardado = Number(localStorage.getItem(CLAVE_TAMANIO));
      return OPCIONES_TAMANIO.includes(guardado) ? guardado : TAMANIO_POR_DEFECTO;
    } catch {
      return TAMANIO_POR_DEFECTO;
    }
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

  abrirFolio(folio: string): void {
    this.router.navigate(['/validacion', folio]);
  }

  get hayDisponibles(): boolean {
    return (this.resumen?.indicadores.porAtender ?? 0) > 0;
  }

  /**
   * Abre la queja disponible que más tiempo lleva esperando (la primera en llegar se atiende
   * primero). La cola viene ordenada de la más reciente a la más antigua, así que la más
   * antigua es el último elemento de la última página del filtro "disponibles".
   */
  atenderSiguiente(): void {
    if (this.buscandoSiguiente) {
      return;
    }
    this.buscandoSiguiente = true;
    const fallo = () => {
      this.buscandoSiguiente = false;
      this.toast.error('No se pudo obtener la siguiente queja.');
      this.cdr.detectChanges();
    };
    this.revisionService.bandeja('disponibles', '', 0, 1).subscribe({
      next: (primera) => {
        const total = primera.totalElementos;
        if (total === 0) {
          this.buscandoSiguiente = false;
          this.toast.exito('No hay quejas disponibles. ¡Cola al día!');
          this.cdr.detectChanges();
          return;
        }
        // Con tamaño 1, la página (total - 1) contiene exactamente la queja más antigua.
        this.revisionService.bandeja('disponibles', '', total - 1, 1).subscribe({
          next: (ultima) => {
            this.buscandoSiguiente = false;
            const item = ultima.lista[ultima.lista.length - 1];
            if (item) {
              this.atender(item);
            } else {
              fallo();
            }
          },
          error: fallo,
        });
      },
      error: fallo,
    });
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

  /** Semáforo de antigüedad para la franja de color de cada fila:
   * verde < 1 día, ámbar < DIAS_ALERTA_ESPERA días, rojo a partir de ahí. */
  nivelEspera(fecha?: string | null): 'baja' | 'media' | 'alta' {
    if (!fecha) {
      return 'baja';
    }
    const dias = (Date.now() - new Date(fecha).getTime()) / 86400000;
    if (dias >= DIAS_ALERTA_ESPERA) {
      return 'alta';
    }
    return dias >= 1 ? 'media' : 'baja';
  }

  // ---------------- carga ----------------

  private cargar(silencioso = false): void {
    if (!silencioso) {
      this.cargando = true;
    }
    this.revisionService.bandeja(this.filtro, this.busqueda, this.pagina, this.tamanioPagina).subscribe({
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
