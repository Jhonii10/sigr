import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, Grid, MenuItem, Paper, Stack, Table, TableBody, TableCell, TableHead,
  TableRow, TextField, Typography,
} from '@mui/material';
import api, { mensajeError } from '../services/api';
import { formatoPesos, hoyISO } from '../utils/formato';

function Indicador({ titulo, valor }) {
  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="caption" color="text.secondary">{titulo}</Typography>
      <Typography variant="h6" fontWeight={700}>{valor}</Typography>
    </Paper>
  );
}

export default function Caja() {
  const [caja, setCaja] = useState(null);
  const [pendientes, setPendientes] = useState([]);
  const [apertura, setApertura] = useState('100000');
  const [pago, setPago] = useState({ pedidoId: '', medioPago: 'EFECTIVO' });
  const [fechaReporte, setFechaReporte] = useState(hoyISO());
  const [mensaje, setMensaje] = useState(null);

  const cargar = useCallback(async () => {
    try {
      const [c, p] = await Promise.all([api.get('/caja/actual'), api.get('/pedidos')]);
      setCaja(c.status === 204 ? null : c.data);
      setPendientes(p.data.filter((x) => x.estado === 'ENTREGADO'));
    } catch (err) {
      setMensaje({ tipo: 'error', texto: mensajeError(err) });
    }
  }, []);

  useEffect(() => { cargar(); }, [cargar]);

  const ejecutar = async (accion, exito) => {
    try {
      await accion();
      setMensaje({ tipo: 'success', texto: exito });
      cargar();
    } catch (err) {
      setMensaje({ tipo: 'error', texto: mensajeError(err) });
    }
  };

  const abrir = () => ejecutar(() => api.post('/caja/abrir', { montoApertura: Number(apertura) }), 'Caja abierta.');
  const pagar = () => ejecutar(async () => {
    await api.post('/caja/pagos', pago);
    setPago({ pedidoId: '', medioPago: 'EFECTIVO' });
  }, 'Pago registrado.');
  const cerrar = () => {
    if (!window.confirm('¿Cerrar la caja? No se podrán registrar más pagos en ella.')) return;
    ejecutar(async () => {
      const { data } = await api.post('/caja/cerrar');
      setMensaje({ tipo: 'success', texto: `Caja cerrada. Efectivo esperado: ${formatoPesos(data.montoCierre)}` });
    }, 'Caja cerrada.');
  };

  const descargarReporte = async () => {
    try {
      const { data } = await api.get('/caja/reporte', { params: { fecha: fechaReporte }, responseType: 'blob' });
      const url = URL.createObjectURL(data);
      const a = document.createElement('a');
      a.href = url;
      a.download = `reporte-ventas-${fechaReporte}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setMensaje({ tipo: 'error', texto: mensajeError(err) });
    }
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} sx={{ mb: 3 }}>Caja y reportes</Typography>
      {mensaje && <Alert severity={mensaje.tipo} onClose={() => setMensaje(null)} sx={{ mb: 2 }}>{mensaje.texto}</Alert>}

      {!caja ? (
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>No hay una caja abierta</Typography>
          <Stack direction="row" spacing={2}>
            <TextField label="Monto de apertura (COP)" type="number" value={apertura}
              onChange={(e) => setApertura(e.target.value)} />
            <Button variant="contained" color="secondary" onClick={abrir}>Abrir caja</Button>
          </Stack>
        </Paper>
      ) : (
        <>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={6} md={3}><Indicador titulo="Apertura" valor={formatoPesos(caja.montoApertura)} /></Grid>
            <Grid item xs={6} md={3}><Indicador titulo="Ventas" valor={formatoPesos(caja.totalVentas)} /></Grid>
            <Grid item xs={6} md={3}><Indicador titulo="Efectivo esperado" valor={formatoPesos(caja.efectivoEsperado)} /></Grid>
            <Grid item xs={6} md={3}><Indicador titulo="Pagos" valor={caja.pagos.length} /></Grid>
          </Grid>

          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>Registrar pago</Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField select label="Pedido entregado" sx={{ minWidth: 260 }} value={pago.pedidoId}
                onChange={(e) => setPago({ ...pago, pedidoId: e.target.value })}>
                {pendientes.length === 0 && <MenuItem disabled value="">No hay pedidos por cobrar</MenuItem>}
                {pendientes.map((p) => (
                  <MenuItem key={p.id} value={p.id}>#{p.id} · Mesa {p.mesa} · {formatoPesos(p.total)}</MenuItem>
                ))}
              </TextField>
              <TextField select label="Medio de pago" sx={{ minWidth: 180 }} value={pago.medioPago}
                onChange={(e) => setPago({ ...pago, medioPago: e.target.value })}>
                <MenuItem value="EFECTIVO">Efectivo</MenuItem>
                <MenuItem value="TARJETA">Tarjeta</MenuItem>
                <MenuItem value="TRANSFERENCIA">Transferencia</MenuItem>
              </TextField>
              <Button variant="contained" disabled={!pago.pedidoId} onClick={pagar}>Cobrar</Button>
              <Box sx={{ flexGrow: 1 }} />
              <Button variant="outlined" color="error" onClick={cerrar}>Cerrar caja</Button>
            </Stack>
          </Paper>

          <Paper sx={{ p: 2, mb: 3, overflowX: 'auto' }}>
            <Typography variant="h6" sx={{ mb: 1 }}>Pagos de la caja actual</Typography>
            <Table size="small">
              <TableHead>
                <TableRow><TableCell>Hora</TableCell><TableCell>Pedido</TableCell><TableCell>Mesa</TableCell>
                  <TableCell>Medio</TableCell><TableCell align="right">Valor</TableCell></TableRow>
              </TableHead>
              <TableBody>
                {caja.pagos.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell>{new Date(p.creadoEn).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })}</TableCell>
                    <TableCell>#{p.pedidoId}</TableCell><TableCell>{p.mesa}</TableCell>
                    <TableCell>{p.medioPago}</TableCell><TableCell align="right">{formatoPesos(p.valor)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        </>
      )}

      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>Reporte de ventas diario</Typography>
        <Stack direction="row" spacing={2}>
          <TextField type="date" label="Fecha" InputLabelProps={{ shrink: true }} value={fechaReporte}
            onChange={(e) => setFechaReporte(e.target.value)} />
          <Button variant="contained" onClick={descargarReporte}>Descargar PDF</Button>
        </Stack>
      </Paper>
    </Box>
  );
}
