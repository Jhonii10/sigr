import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert, Box, Button, Card, CardActions, CardContent, Chip, Dialog, DialogActions, DialogContent,
  DialogTitle, Divider, Grid, IconButton, MenuItem, Stack, TextField, Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import api, { mensajeError } from '../services/api';
import { suscribir } from '../services/socket';
import { ESTADOS_PEDIDO, formatoPesos, puedeCancelar } from '../utils/formato';

const COLUMNAS = ['RECIBIDO', 'EN_PREPARACION', 'LISTO', 'ENTREGADO'];

function NuevoPedido({ abierto, onCerrar, onCreado }) {
  const [mesas, setMesas] = useState([]);
  const [platos, setPlatos] = useState([]);
  const [mesaId, setMesaId] = useState('');
  const [items, setItems] = useState([]);
  const [seleccion, setSeleccion] = useState({ platoId: '', cantidad: 1 });
  const [nota, setNota] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!abierto) return;
    Promise.all([api.get('/mesas'), api.get('/menu/platos')])
      .then(([m, p]) => { setMesas(m.data); setPlatos(p.data); })
      .catch((err) => setError(mensajeError(err)));
  }, [abierto]);

  const total = items.reduce((s, i) => s + i.precio * i.cantidad, 0);

  const agregar = () => {
    const plato = platos.find((p) => p.id === seleccion.platoId);
    if (!plato || seleccion.cantidad < 1) return;
    setItems((prev) => {
      const existe = prev.find((i) => i.platoId === plato.id);
      if (existe) return prev.map((i) => (i.platoId === plato.id ? { ...i, cantidad: i.cantidad + Number(seleccion.cantidad) } : i));
      return [...prev, { platoId: plato.id, nombre: plato.nombre, precio: Number(plato.precio), cantidad: Number(seleccion.cantidad) }];
    });
    setSeleccion({ platoId: '', cantidad: 1 });
  };

  const crear = async () => {
    setError('');
    try {
      await api.post('/pedidos', {
        mesaId, nota: nota || null,
        items: items.map(({ platoId, cantidad }) => ({ platoId, cantidad })),
      });
      setItems([]); setMesaId(''); setNota('');
      onCreado();
    } catch (err) {
      setError(mensajeError(err));
    }
  };

  return (
    <Dialog open={abierto} onClose={onCerrar} fullWidth maxWidth="sm">
      <DialogTitle>Nuevo pedido</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField select label="Mesa" value={mesaId} onChange={(e) => setMesaId(e.target.value)}>
            {mesas.map((m) => (
              <MenuItem key={m.id} value={m.id}>
                Mesa {m.numero} · {m.capacidad} personas {m.estado === 'OCUPADA' ? '(ocupada)' : ''}
              </MenuItem>
            ))}
          </TextField>
          <Stack direction="row" spacing={1}>
            <TextField select label="Plato" sx={{ flex: 1 }} value={seleccion.platoId}
              onChange={(e) => setSeleccion({ ...seleccion, platoId: e.target.value })}>
              {platos.map((p) => <MenuItem key={p.id} value={p.id}>{p.nombre} · {formatoPesos(p.precio)}</MenuItem>)}
            </TextField>
            <TextField label="Cant." type="number" sx={{ width: 90 }} value={seleccion.cantidad}
              inputProps={{ min: 1, max: 50 }}
              onChange={(e) => setSeleccion({ ...seleccion, cantidad: e.target.value })} />
            <IconButton color="secondary" aria-label="Agregar plato" onClick={agregar}><AddIcon /></IconButton>
          </Stack>
          {items.map((i) => (
            <Stack key={i.platoId} direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="body2">{i.cantidad} × {i.nombre}</Typography>
              <Stack direction="row" alignItems="center">
                <Typography variant="body2">{formatoPesos(i.precio * i.cantidad)}</Typography>
                <IconButton size="small" aria-label="Quitar" onClick={() => setItems(items.filter((x) => x.platoId !== i.platoId))}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Stack>
            </Stack>
          ))}
          <Divider />
          <Typography fontWeight={700} textAlign="right">Total: {formatoPesos(total)}</Typography>
          <TextField label="Nota (opcional)" value={nota} onChange={(e) => setNota(e.target.value)} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCerrar}>Cancelar</Button>
        <Button variant="contained" disabled={!mesaId || items.length === 0} onClick={crear}>Crear pedido</Button>
      </DialogActions>
    </Dialog>
  );
}

export default function Pedidos() {
  const [pedidos, setPedidos] = useState([]);
  const [error, setError] = useState('');
  const [nuevo, setNuevo] = useState(false);

  const cargar = useCallback(() => {
    api.get('/pedidos').then((r) => setPedidos(r.data)).catch((err) => setError(mensajeError(err)));
  }, []);

  useEffect(() => {
    cargar();
    return suscribir('/topic/pedidos', (p) => {
      setPedidos((prev) => {
        const resto = prev.filter((x) => x.id !== p.id);
        return COLUMNAS.includes(p.estado) ? [...resto, p].sort((a, b) => a.id - b.id) : resto;
      });
    });
  }, [cargar]);

  const columnas = useMemo(() => COLUMNAS.map((estado) => ({
    estado, pedidos: pedidos.filter((p) => p.estado === estado),
  })), [pedidos]);

  const cambiar = async (id, estado) => {
    try {
      const { data } = await api.patch(`/pedidos/${id}/estado`, { estado });
      setPedidos((prev) => prev.map((p) => (p.id === id ? data : p)).filter((p) => COLUMNAS.includes(p.estado)));
    } catch (err) {
      setError(mensajeError(err));
    }
  };

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>Pedidos en tiempo real</Typography>
        <Button variant="contained" color="secondary" startIcon={<AddIcon />} onClick={() => setNuevo(true)}>
          Nuevo pedido
        </Button>
      </Stack>
      {error && <Alert severity="error" onClose={() => setError('')} sx={{ mb: 2 }}>{error}</Alert>}
      <Grid container spacing={2}>
        {columnas.map((col) => (
          <Grid item xs={12} sm={6} md={3} key={col.estado}>
            <Typography fontWeight={700} sx={{ mb: 1 }}>
              {ESTADOS_PEDIDO[col.estado].etiqueta} ({col.pedidos.length})
            </Typography>
            <Stack spacing={1.5}>
              {col.pedidos.map((p) => {
                const siguiente = ESTADOS_PEDIDO[p.estado].siguiente;
                return (
                  <Card key={p.id}>
                    <CardContent sx={{ pb: 1 }}>
                      <Stack direction="row" justifyContent="space-between">
                        <Typography fontWeight={700}>Mesa {p.mesa}</Typography>
                        <Chip size="small" label={`#${p.id}`} />
                      </Stack>
                      <Typography variant="caption" color="text.secondary">
                        {p.cliente ? `Cliente: ${p.cliente}` : p.mesero}
                        {p.cliente && p.mesero ? ` · ${p.mesero}` : ''} · {new Date(p.creadoEn).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })}
                      </Typography>
                      {p.detalles.map((d) => (
                        <Typography key={d.platoId} variant="body2">{d.cantidad} × {d.plato}</Typography>
                      ))}
                      {p.nota && <Typography variant="body2" color="secondary">Nota: {p.nota}</Typography>}
                      <Typography fontWeight={600} sx={{ mt: 1 }}>{formatoPesos(p.total)}</Typography>
                    </CardContent>
                    <CardActions>
                      {siguiente && (
                        <Button size="small" variant="contained" onClick={() => cambiar(p.id, siguiente)}>
                          {ESTADOS_PEDIDO[siguiente].etiqueta}
                        </Button>
                      )}
                      {puedeCancelar(p.estado) && (
                        <Button size="small" color="error" onClick={() => cambiar(p.id, 'CANCELADO')}>Cancelar</Button>
                      )}
                    </CardActions>
                  </Card>
                );
              })}
            </Stack>
          </Grid>
        ))}
      </Grid>
      <NuevoPedido abierto={nuevo} onCerrar={() => setNuevo(false)} onCreado={() => { setNuevo(false); cargar(); }} />
    </Box>
  );
}
