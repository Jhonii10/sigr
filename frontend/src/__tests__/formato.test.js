import { ESTADOS_PEDIDO, formatoPesos, hoyISO, puedeCancelar } from '../utils/formato';

describe('utilidades de formato', () => {
  it('formatea pesos colombianos sin decimales', () => {
    expect(formatoPesos(28000).replace(/\s/g, ' ')).toMatch(/28\.000/);
  });

  it('define el flujo de estados del pedido', () => {
    expect(ESTADOS_PEDIDO.RECIBIDO.siguiente).toBe('EN_PREPARACION');
    expect(ESTADOS_PEDIDO.LISTO.siguiente).toBe('ENTREGADO');
    expect(ESTADOS_PEDIDO.ENTREGADO.siguiente).toBeNull();
  });

  it('solo permite cancelar antes de que el pedido esté listo', () => {
    expect(puedeCancelar('RECIBIDO')).toBe(true);
    expect(puedeCancelar('EN_PREPARACION')).toBe(true);
    expect(puedeCancelar('LISTO')).toBe(false);
  });

  it('genera fechas locales en formato ISO', () => {
    expect(hoyISO()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});
