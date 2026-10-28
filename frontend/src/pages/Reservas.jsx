import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, Chip, Grid, MenuItem, Paper, Stack, Table, TableBody, TableCell, TableHead,
  TableRow, TextField, Typography,
} from '@mui/material';
import api, { mensajeError } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { hoyISO } from '../utils/formato';

function TablaReservas({ reservas, onCancelar, mostrarCliente }) {
  if (reservas.length === 0) {
    return <Typography color="text.secondary" sx={{ p: 2 }}>No hay reservas.</Typography>;
  }
  return (
    <Box sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            {mostrarCliente && <TableCell>Cliente</TableCell>}
            <TableCell>Fecha</TableCell><TableCell>Hora</TableCell><TableCell>Mesa</TableCell>
            <TableCell>Personas</TableCell><TableCell>Estado</TableCell><TableCell />
          </TableRow>
        </TableHead>
        <TableBody>
          {reservas.map((r) => (
            <TableRow key={r.id}>
              {mostrarCliente && <TableCell>{r.cliente}</TableCell>}
              <TableCell>{r.fecha}</TableCell>
              <TableCell>{r.hora}</TableCell>
              <TableCell>{r.mesa}</TableCell>
              <TableCell>{r.personas}</TableCell>
              <TableCell>
                <Chip size="small" label={r.estado} color={r.estado === 'CONFIRMADA' ? 'success' : 'default'} />
              </TableCell>
              <TableCell align="right">
                {r.estado === 'CONFIRMADA' && (
                  <Button size="small" color="error" onClick={() => onCancelar(r.id)}>Cancelar</Button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}

export default function Reservas() {
  const { tieneRol } = useAuth();
  const esPersonal = tieneRol('MESERO', 'ADMIN');
  const [form, setForm] = useState({ fecha: hoyISO(1), hora: '19:00', personas: 2, mesaId: '' });
  const [mesas, setMesas] = useState([]);
  const [mias, setMias] = useState([]);
  const [fechaDia, setFechaDia] = useState(hoyISO());
  const [delDia, setDelDia] = useState([]);
  const [mensaje, setMensaje] = useState(null);

  const cargarMias = useCallback(() => {
    api.get('/reservas/mias').then((r) => setMias(r.data)).catch(() => {});
  }, []);

  const cargarDelDia = useCallback(() => {
    if (!esPersonal) return;
    api.get('/reservas', { params: { fecha: fechaDia } })
      .then((r) => setDelDia(r.data))
      .catch((err) => setMensaje({ tipo: 'error', texto: mensajeError(err) }));
  }, [esPersonal, fechaDia]);

  useEffect(() => { cargarMias(); }, [cargarMias]);
  useEffect(() => { cargarDelDia(); }, [cargarDelDia]);

  const consultar = async () => {
    setMensaje(null);
    try {
      const { data } = await api.get('/reservas/disponibilidad', {
        params: { fecha: form.fecha, hora: form.hora, personas: form.personas },
      });
      setMesas(data);
      setForm((f) => ({ ...f, mesaId: data[0]?.id ?? '' }));
      if (data.length === 0) setMensaje({ tipo: 'warning', texto: 'No hay mesas disponibles para esa franja.' });
    } catch (err) {
      setMesas([]);
      setMensaje({ tipo: 'error', texto: mensajeError(err) });
    }
  };

  const reservar = async () => {
    try {
      const { data } = await api.post('/reservas', { ...form, personas: Number(form.personas), mesaId: form.mesaId || null });
      setMensaje({ tipo: 'success', texto: `Reserva confirmada: mesa ${data.mesa}, ${data.fecha} a las ${data.hora}.` });
      setMesas([]);
      cargarMias();
      cargarDelDia();
    } catch (err) {
      setMensaje({ tipo: 'error', texto: mensajeError(err) });
    }
  };

  const cancelar = async (id) => {
    try {
      await api.patch(`/reservas/${id}/cancelar`);
      cargarMias();
      cargarDelDia();
    } catch (err) {
      setMensaje({ tipo: 'error', texto: mensajeError(err) });
    }
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} sx={{ mb: 3 }}>Reservas</Typography>
      {mensaje && <Alert severity={mensaje.tipo} onClose={() => setMensaje(null)} sx={{ mb: 2 }}>{mensaje.texto}</Alert>}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>Nueva reserva</Typography>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={3}>
            <TextField fullWidth type="date" label="Fecha" InputLabelProps={{ shrink: true }}
              inputProps={{ min: hoyISO() }} value={form.fecha}
              onChange={(e) => { setForm({ ...form, fecha: e.target.value }); setMesas([]); }} />
          </Grid>
          <Grid item xs={6} sm={2}>
            <TextField fullWidth type="time" label="Hora" InputLabelProps={{ shrink: true }}
              inputProps={{ min: '11:00', max: '21:00', step: 900 }} value={form.hora}
              onChange={(e) => { setForm({ ...form, hora: e.target.value }); setMesas([]); }} />
          </Grid>
          <Grid item xs={6} sm={2}>
            <TextField fullWidth type="number" label="Personas" inputProps={{ min: 1, max: 20 }} value={form.personas}
              onChange={(e) => { setForm({ ...form, personas: e.target.value }); setMesas([]); }} />
          </Grid>
          <Grid item xs={12} sm={2}>
            <Button fullWidth variant="outlined" onClick={consultar}>Disponibilidad</Button>
          </Grid>
          {mesas.length > 0 && (
            <>
              <Grid item xs={12} sm={2}>
                <TextField fullWidth select label="Mesa" value={form.mesaId}
                  onChange={(e) => setForm({ ...form, mesaId: e.target.value })}>
                  {mesas.map((m) => <MenuItem key={m.id} value={m.id}>Mesa {m.numero} ({m.capacidad} p.)</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={1}>
                <Button fullWidth variant="contained" color="secondary" onClick={reservar}>Reservar</Button>
              </Grid>
            </>
          )}
        </Grid>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
          Horario de reservas: 11:00 a 21:00. Cada reserva bloquea la mesa durante 2 horas.
        </Typography>
      </Paper>

      <Paper sx={{ p: 2, mb: 3 }}>
        <Typography variant="h6" sx={{ mb: 1 }}>Mis reservas</Typography>
        <TablaReservas reservas={mias} onCancelar={cancelar} />
      </Paper>

      {esPersonal && (
        <Paper sx={{ p: 2 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
            <Typography variant="h6">Reservas del día</Typography>
            <TextField size="small" type="date" value={fechaDia} onChange={(e) => setFechaDia(e.target.value)} />
          </Stack>
          <TablaReservas reservas={delDia} onCancelar={cancelar} mostrarCliente />
        </Paper>
      )}
    </Box>
  );
}
