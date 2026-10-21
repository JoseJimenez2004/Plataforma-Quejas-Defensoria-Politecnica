import { ExpedienteResumen } from '../models/expediente-resumen';

/*
 * Reglas de presentación del rediseño (2026-10-21): en qué grupo cae cada expediente,
 * qué le toca hacer al subdefensor y cómo se pinta el plazo del oficio. Las comparten
 * Inicio, Mis expedientes y Plazos y oficios, para que digan lo mismo.
 */

export type GrupoExpediente = 'tramite' | 'atender' | 'ua' | 'conciliacion' | 'concluidos';

export const GRUPOS: { id: GrupoExpediente; etiqueta: string }[] = [
  { id: 'tramite', etiqueta: 'En trámite' },
  { id: 'atender', etiqueta: 'Por atender' },
  { id: 'ua', etiqueta: 'Esperando a la unidad' },
  { id: 'conciliacion', etiqueta: 'Pendiente de acuerdo de conciliación' },
  { id: 'concluidos', etiqueta: 'Concluidos' }
];

/** crit = vencido, warn = toca actuar, info = nuevo, espera = depende de otros. */
export type Nivel = 'crit' | 'warn' | 'info' | 'espera' | 'fin';

export type Accion = 'recordatorio' | 'respuesta' | 'oficio' | 'abrir' | 'acuerdos';

export interface SiguientePaso {
  nivel: Nivel;
  boton: string;
  accion: Accion;
  descripcion: string;
}

/** Días que le quedan al oficio vigente (negativo si ya venció). */
export function diasRestantes(e: ExpedienteResumen): number | null {
  if (e.diasLimite == null || e.diasTranscurridos == null) return null;
  return e.diasLimite - e.diasTranscurridos;
}

export function oficioVencido(e: ExpedienteResumen): boolean {
  return e.estatusOficioVigente === 'VENCIDO';
}

/** Oficio en espera que vence en 3 días o menos. */
export function porVencer(e: ExpedienteResumen): boolean {
  const resta = diasRestantes(e);
  return e.estatusOficioVigente === 'EN_ESPERA' && resta !== null && resta <= 3;
}

export function siguientePaso(e: ExpedienteResumen): SiguientePaso {
  const oficio = e.numeroOficioVigente ?? 'el oficio';

  if (oficioVencido(e)) {
    return {
      nivel: 'crit', boton: 'Enviar recordatorio', accion: 'recordatorio',
      descripcion: `${oficio} vencido (${e.diasTranscurridos} de ${e.diasLimite} días) · sin respuesta de ${e.destinatarioNombreVigente ?? 'la unidad'}`
    };
  }
  if (e.estatus === 'RECIBIDO') {
    return { nivel: 'info', boton: 'Abrir expediente', accion: 'abrir', descripcion: 'Llegó de Primer Contacto · aún sin abrir' };
  }
  if (e.siguienteFase) {
    return {
      nivel: 'warn', boton: 'Solicitar oficio', accion: 'oficio',
      descripcion: e.siguienteFase === 'GESTION_DIRECTOR'
        ? 'Toca el oficio de gestión con el director'
        : 'Toca solicitar información a la unidad académica'
    };
  }
  if (e.estatus === 'EN_ESPERA_OFICIO' && e.oficioIdVigente) {
    const resta = diasRestantes(e);
    return {
      nivel: porVencer(e) ? 'warn' : 'espera', boton: 'Registrar respuesta', accion: 'respuesta',
      descripcion: `Esperando respuesta de ${e.destinatarioNombreVigente ?? 'la unidad'}`
        + (resta !== null ? ` · ${resta <= 0 ? 'vence hoy' : 'quedan ' + resta + ' días'}` : '')
    };
  }
  if (e.estatus === 'ELABORO_ACUERDO') {
    return { nivel: 'warn', boton: 'Redactar acuerdo', accion: 'acuerdos', descripcion: 'La unidad respondió · falta el acuerdo de conciliación' };
  }
  if (e.estatus === 'PENDIENTE_CONCILIACION') {
    return { nivel: 'espera', boton: 'Ver acuerdo', accion: 'acuerdos', descripcion: 'Acuerdo enviado · esperando que el quejoso lo acepte o rechace' };
  }
  if (e.estatus === 'PENDIENTE_CONCLUSION') {
    return { nivel: 'warn', boton: 'Redactar conclusión', accion: 'acuerdos', descripcion: 'El quejoso aceptó el acuerdo de conciliación' };
  }
  if (e.estatus === 'CONCLUIDO') {
    return { nivel: 'fin', boton: 'Ver expediente', accion: 'abrir', descripcion: 'Expediente concluido' };
  }
  return { nivel: 'info', boton: 'Abrir expediente', accion: 'abrir', descripcion: 'En investigación' };
}

export function enGrupo(e: ExpedienteResumen, grupo: GrupoExpediente): boolean {
  switch (grupo) {
    case 'tramite': return e.estatus !== 'CONCLUIDO';
    case 'atender': return ['crit', 'warn', 'info'].includes(siguientePaso(e).nivel) && e.estatus !== 'CONCLUIDO';
    case 'ua': return e.estatus === 'EN_ESPERA_OFICIO';
    case 'conciliacion': return e.estatus === 'PENDIENTE_CONCILIACION';
    case 'concluidos': return e.estatus === 'CONCLUIDO';
  }
}

/** Clase del chip de etapa (definidas en styles.css). */
export function claseEtapa(e: ExpedienteResumen): string {
  if (oficioVencido(e)) return 'chip-venc';
  return 'chip-' + e.estatus.toLowerCase().replace(/_/g, '-');
}

/** Orden de urgencia: vencidos, luego lo que toca hacer, luego nuevos, al final lo que espera. */
export const ORDEN_NIVEL: Record<Nivel, number> = { crit: 0, warn: 1, info: 2, espera: 3, fin: 4 };

export function porcentajePlazo(e: ExpedienteResumen): number {
  if (!e.diasLimite || e.diasTranscurridos == null) return 0;
  return Math.min(100, Math.round((e.diasTranscurridos / e.diasLimite) * 100));
}

export function colorPlazo(e: ExpedienteResumen): string {
  if (oficioVencido(e)) return 'var(--peligro)';
  return porcentajePlazo(e) >= 70 ? '#e0912b' : 'var(--exito)';
}

export function formatearFecha(valor: string | null | undefined, conHora = false): string {
  if (!valor) return '—';
  const fecha = new Date(valor.length === 10 ? valor + 'T00:00:00' : valor);
  if (isNaN(fecha.getTime())) return valor;
  return fecha.toLocaleDateString('es-MX', { day: '2-digit', month: '2-digit', year: 'numeric' })
    + (conHora ? ' ' + fecha.toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' }) : '');
}
