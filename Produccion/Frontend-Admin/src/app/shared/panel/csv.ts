/**
 * Exporta una tabla a CSV que Excel abre bien en español: separador ";" para no chocar con
 * las comas de los textos, BOM UTF-8 para los acentos y comillas en cada celda.
 */
export function descargarCsv(nombreArchivo: string, encabezados: string[], filas: (string | number | null | undefined)[][]): void {
  const celda = (v: string | number | null | undefined) => `"${String(v ?? '').replace(/"/g, '""').replace(/\r?\n/g, ' ')}"`;
  const contenido = [encabezados, ...filas].map((fila) => fila.map(celda).join(';')).join('\r\n');
  const blob = new Blob(['﻿' + contenido], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  const hoy = new Date().toISOString().slice(0, 10);
  a.download = `${nombreArchivo}-${hoy}.csv`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/** dd/MM/yyyy y dd/MM/yyyy HH:mm sin depender del locale "es" de Angular. */
export function fechaCorta(iso?: string | null): string {
  if (!iso) {
    return '—';
  }
  const d = new Date(iso);
  if (isNaN(d.getTime())) {
    return '—';
  }
  const dd = String(d.getDate()).padStart(2, '0');
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  return `${dd}/${mm}/${d.getFullYear()}`;
}

export function fechaHora(iso?: string | null): string {
  if (!iso) {
    return '—';
  }
  const d = new Date(iso);
  if (isNaN(d.getTime())) {
    return '—';
  }
  return `${fechaCorta(iso)} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}
