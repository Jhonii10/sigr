const cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

export const formatoPesos = (valor) => cop.format(Number(valor ?? 0));

export const ESTADOS_PEDIDO = {
  RECIBIDO: { etiqueta: 'Recibido', color: 'default', siguiente: 'EN_PREPARACION' },
  EN_PREPARACION: { etiqueta: 'En preparación', color: 'warning', siguiente: 'LISTO' },
  LISTO: { etiqueta: 'Listo', color: 'info', siguiente: 'ENTREGADO' },
  ENTREGADO: { etiqueta: 'Entregado', color: 'success', siguiente: null },
  PAGADO: { etiqueta: 'Pagado', color: 'success', siguiente: null },
  CANCELADO: { etiqueta: 'Cancelado', color: 'error', siguiente: null },
};

export const puedeCancelar = (estado) => estado === 'RECIBIDO' || estado === 'EN_PREPARACION';

/** Fecha local en formato yyyy-mm-dd (evita el desfase de toISOString con UTC). */
export function hoyISO(dias = 0) {
  const d = new Date();
  d.setDate(d.getDate() + dias);
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}
