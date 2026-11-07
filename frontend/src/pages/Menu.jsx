import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert, Badge, Box, Button, Card, CardActions, CardContent, Chip, Dialog, DialogActions,
  DialogContent, DialogTitle, Divider, Fab, FormControlLabel, Grid, IconButton, MenuItem, Stack,
  Switch, TextField, Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import EditIcon from '@mui/icons-material/Edit';
import PhotoCameraIcon from '@mui/icons-material/PhotoCamera';
import RestaurantMenuIcon from '@mui/icons-material/RestaurantMenu';
import ShoppingBagIcon from '@mui/icons-material/ShoppingBag';
import api, { mensajeError } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { formatoPesos } from '../utils/formato';

const PLATO_VACIO = { categoriaId: '', nombre: '', descripcion: '', precio: '', disponible: true, imagenUrl: '' };
const TIPOS_IMAGEN = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_IMAGEN = 5 * 1024 * 1024;
const FONDOS = ['#FDEBD8', '#E3ECF7', '#E4F3E8', '#F7E3EC', '#EEE8F7', '#F5F0DC'];

/** Foto del plato o, si no tiene o no carga, un recuadro con el ícono del menú. */
function FotoPlato({ url, nombre, indice, alto = 170 }) {
  const [fallo, setFallo] = useState(false);
  useEffect(() => setFallo(false), [url]);
  if (url && !fallo) {
    return (
      <Box component="img" src={url} alt={nombre} loading="lazy" onError={() => setFallo(true)}
        sx={{ width: '100%', height: alto, objectFit: 'cover', display: 'block' }} />
    );
  }
  return (
    <Box sx={{ height: alto, display: 'grid', placeItems: 'center', bgcolor: FONDOS[indice % FONDOS.length] }}
      role="img" aria-label={`${nombre} sin foto`}>
      <RestaurantMenuIcon sx={{ fontSize: 48, color: 'rgba(30,58,95,0.35)' }} />
    </Box>
  );
}

function DialogoPlato({ plato, categorias, onCerrar, onGuardado }) {
  const [datos, setDatos] = useState(plato);
  const [archivo, setArchivo] = useState(null);
  const [vista, setVista] = useState(plato.imagenUrl || '');
  const [quitarFoto, setQuitarFoto] = useState(false);
  const [error, setError] = useState('');
  const [guardando, setGuardando] = useState(false);

  useEffect(() => () => { if (archivo) URL.revokeObjectURL(vista); }, [archivo, vista]);

  const elegirArchivo = (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (!f) return;
    if (!TIPOS_IMAGEN.includes(f.type)) { setError('Use una imagen JPG, PNG o WEBP.'); return; }
    if (f.size > MAX_IMAGEN) { setError('La imagen no puede superar 5 MB.'); return; }
    setError('');
    setArchivo(f);
    setQuitarFoto(false);
    setVista(URL.createObjectURL(f));
  };

  const quitar = () => {
    setArchivo(null);
    setVista('');
    setQuitarFoto(Boolean(plato.imagenUrl));
  };

  const guardar = async () => {
    setError('');
    setGuardando(true);
    try {
      const cuerpo = { ...datos, precio: Number(datos.precio) };
      const { data } = datos.id
        ? await api.put(`/menu/platos/${datos.id}`, cuerpo)
        : await api.post('/menu/platos', cuerpo);
      if (archivo) {
        const form = new FormData();
        form.append('archivo', archivo);
        await api.post(`/menu/platos/${data.id}/imagen`, form);
      } else if (quitarFoto) {
        await api.delete(`/menu/platos/${data.id}/imagen`);
      }
      onGuardado();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <Dialog open onClose={onCerrar} fullWidth maxWidth="sm">
      <DialogTitle>{datos.id ? 'Editar plato' : 'Nuevo plato'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <Box sx={{ borderRadius: 2, overflow: 'hidden', border: '1px solid #E5E7EB' }}>
            <FotoPlato url={vista} nombre={datos.nombre || 'Plato'} indice={0} alto={200} />
          </Box>
          <Stack direction="row" spacing={1}>
            <Button component="label" variant="outlined" startIcon={<PhotoCameraIcon />}>
              {vista ? 'Cambiar foto' : 'Subir foto'}
              <input hidden type="file" accept={TIPOS_IMAGEN.join(',')} onChange={elegirArchivo} />
            </Button>
            {vista && <Button color="error" onClick={quitar}>Quitar foto</Button>}
          </Stack>
          <Typography variant="caption" color="text.secondary">JPG, PNG o WEBP, máximo 5 MB.</Typography>
          <TextField select label="Categoría" value={datos.categoriaId}
            onChange={(e) => setDatos({ ...datos, categoriaId: e.target.value })}>
            {categorias.map((c) => <MenuItem key={c.id} value={c.id}>{c.nombre}</MenuItem>)}
          </TextField>
          <TextField label="Nombre" required value={datos.nombre}
            onChange={(e) => setDatos({ ...datos, nombre: e.target.value })} />
          <TextField label="Descripción" multiline minRows={2} value={datos.descripcion ?? ''}
            onChange={(e) => setDatos({ ...datos, descripcion: e.target.value })} />
          <TextField label="Precio (COP)" type="number" required value={datos.precio}
            onChange={(e) => setDatos({ ...datos, precio: e.target.value })} />
          <FormControlLabel label="Disponible" control={
            <Switch checked={datos.disponible}
              onChange={(e) => setDatos({ ...datos, disponible: e.target.checked })} />} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCerrar}>Cancelar</Button>
        <Button variant="contained" onClick={guardar} disabled={guardando}>
          {guardando ? 'Guardando…' : 'Guardar'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function DialogoCarrito({ abierto, items, onCambiar, onCerrar, onEnviado }) {
  const [mesas, setMesas] = useState([]);
  const [mesaId, setMesaId] = useState('');
  const [nota, setNota] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (!abierto) return;
    api.get('/mesas').then((r) => setMesas(r.data)).catch((err) => setError(mensajeError(err)));
  }, [abierto]);

  const total = items.reduce((s, i) => s + i.precio * i.cantidad, 0);

  const enviar = async () => {
    setError('');
    setEnviando(true);
    try {
      await api.post('/pedidos', {
        mesaId, nota: nota || null,
        items: items.map(({ platoId, cantidad }) => ({ platoId, cantidad })),
      });
      setMesaId(''); setNota('');
      onEnviado();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Dialog open={abierto} onClose={onCerrar} fullWidth maxWidth="sm">
      <DialogTitle>Tu pedido</DialogTitle>
      <DialogContent>
        <Stack spacing={1.5} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          {items.length === 0 && <Typography color="text.secondary">Aún no has agregado platos.</Typography>}
          {items.map((i) => (
            <Stack key={i.platoId} direction="row" alignItems="center" spacing={1}>
              <Typography variant="body2" sx={{ flex: 1 }}>{i.nombre}</Typography>
              <IconButton size="small" aria-label={`Quitar uno de ${i.nombre}`} onClick={() => onCambiar(i.platoId, -1)}>
                <RemoveIcon fontSize="small" />
              </IconButton>
              <Typography variant="body2" sx={{ minWidth: 20, textAlign: 'center' }}>{i.cantidad}</Typography>
              <IconButton size="small" aria-label={`Agregar uno de ${i.nombre}`} onClick={() => onCambiar(i.platoId, 1)}>
                <AddIcon fontSize="small" />
              </IconButton>
              <Typography variant="body2" sx={{ minWidth: 90, textAlign: 'right' }}>
                {formatoPesos(i.precio * i.cantidad)}
              </Typography>
            </Stack>
          ))}
          <Divider />
          <Typography fontWeight={700} textAlign="right">Total: {formatoPesos(total)}</Typography>
          <TextField select label="¿En qué mesa estás?" value={mesaId} onChange={(e) => setMesaId(e.target.value)}>
            {mesas.map((m) => (
              <MenuItem key={m.id} value={m.id}>
                Mesa {m.numero} · {m.capacidad} personas {m.estado === 'OCUPADA' ? '(ocupada)' : ''}
              </MenuItem>
            ))}
          </TextField>
          <TextField label="Nota para la cocina (opcional)" value={nota} inputProps={{ maxLength: 255 }}
            onChange={(e) => setNota(e.target.value)} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCerrar}>Seguir viendo el menú</Button>
        <Button variant="contained" color="secondary" onClick={enviar}
          disabled={!mesaId || items.length === 0 || enviando}>
          {enviando ? 'Enviando…' : 'Enviar pedido'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default function Menu() {
  const { usuario, tieneRol } = useAuth();
  const navigate = useNavigate();
  const esAdmin = tieneRol('ADMIN');
  const esCliente = tieneRol('CLIENTE');
  const [categorias, setCategorias] = useState([]);
  const [platos, setPlatos] = useState([]);
  const [error, setError] = useState('');
  const [dialogo, setDialogo] = useState(null);
  const [nuevaCategoria, setNuevaCategoria] = useState('');
  const [carrito, setCarrito] = useState([]);
  const [verCarrito, setVerCarrito] = useState(false);

  const cargar = useCallback(async () => {
    try {
      const [c, p] = await Promise.all([
        api.get('/menu/categorias'),
        api.get('/menu/platos', { params: { todos: esAdmin } }),
      ]);
      setCategorias(c.data);
      setPlatos(p.data);
    } catch (err) {
      setError(mensajeError(err));
    }
  }, [esAdmin]);

  useEffect(() => { cargar(); }, [cargar]);

  const porCategoria = useMemo(() => categorias.map((c) => ({
    ...c, platos: platos.filter((p) => p.categoriaId === c.id),
  })), [categorias, platos]);

  const enCarrito = carrito.reduce((s, i) => s + i.cantidad, 0);

  const agregar = (p) => setCarrito((prev) => {
    const existe = prev.find((i) => i.platoId === p.id);
    if (existe) return prev.map((i) => (i.platoId === p.id ? { ...i, cantidad: Math.min(i.cantidad + 1, 50) } : i));
    return [...prev, { platoId: p.id, nombre: p.nombre, precio: Number(p.precio), cantidad: 1 }];
  });

  const cambiarCantidad = (platoId, delta) => setCarrito((prev) => prev
    .map((i) => (i.platoId === platoId ? { ...i, cantidad: Math.min(i.cantidad + delta, 50) } : i))
    .filter((i) => i.cantidad > 0));

  const crearCategoria = async () => {
    if (!nuevaCategoria.trim()) return;
    try {
      await api.post('/menu/categorias', { nombre: nuevaCategoria });
      setNuevaCategoria('');
      cargar();
    } catch (err) {
      setError(mensajeError(err));
    }
  };

  return (
    <Box sx={{ pb: esCliente ? 8 : 0 }}>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} sx={{ mb: 3 }} spacing={2}>
        <Typography variant="h4" fontWeight={700}>Menú</Typography>
        {esAdmin && (
          <Stack direction="row" spacing={1}>
            <TextField size="small" label="Nueva categoría" value={nuevaCategoria}
              onChange={(e) => setNuevaCategoria(e.target.value)} />
            <Button variant="outlined" onClick={crearCategoria}>Agregar</Button>
            <Button variant="contained" color="secondary" startIcon={<AddIcon />}
              onClick={() => setDialogo({ ...PLATO_VACIO, categoriaId: categorias[0]?.id ?? '' })}>
              Plato
            </Button>
          </Stack>
        )}
      </Stack>
      {!usuario && (
        <Alert severity="info" sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={() => navigate('/login', { state: { desde: '/menu' } })}>Ingresar</Button>}>
          Ingresa con tu cuenta para hacer tu pedido desde la mesa.
        </Alert>
      )}
      {error && <Alert severity="error" onClose={() => setError('')} sx={{ mb: 2 }}>{error}</Alert>}

      {porCategoria.map((c, ci) => (
        <Box key={c.id} sx={{ mb: 4 }}>
          <Typography variant="h6" color="secondary" fontWeight={700}>{c.nombre}</Typography>
          {c.descripcion && <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>{c.descripcion}</Typography>}
          <Grid container spacing={2}>
            {c.platos.map((p) => (
              <Grid item xs={12} sm={6} md={4} key={p.id}>
                <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', opacity: p.disponible ? 1 : 0.55 }}>
                  <FotoPlato url={p.imagenUrl} nombre={p.nombre} indice={ci} />
                  <CardContent sx={{ flexGrow: 1 }}>
                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                      <Typography fontWeight={600}>{p.nombre}</Typography>
                      {esAdmin && (
                        <IconButton size="small" aria-label={`Editar ${p.nombre}`} onClick={() => setDialogo({ ...p })}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                      )}
                    </Stack>
                    <Typography variant="body2" color="text.secondary" sx={{ minHeight: 40 }}>{p.descripcion}</Typography>
                  </CardContent>
                  <CardActions sx={{ px: 2, pb: 2, justifyContent: 'space-between' }}>
                    <Typography fontWeight={700}>{formatoPesos(p.precio)}</Typography>
                    {!p.disponible && <Chip size="small" label="No disponible" />}
                    {esCliente && p.disponible && (
                      <Button size="small" variant="contained" color="secondary" startIcon={<AddIcon />} onClick={() => agregar(p)}>
                        Agregar
                      </Button>
                    )}
                  </CardActions>
                </Card>
              </Grid>
            ))}
            {c.platos.length === 0 && (
              <Grid item xs={12}><Typography variant="body2" color="text.secondary">Sin platos en esta categoría.</Typography></Grid>
            )}
          </Grid>
        </Box>
      ))}

      {esCliente && (
        <Fab variant="extended" color="secondary" onClick={() => setVerCarrito(true)}
          sx={{ position: 'fixed', right: 24, bottom: 24 }}>
          <Badge badgeContent={enCarrito} color="primary" sx={{ mr: 1.5 }}><ShoppingBagIcon /></Badge>
          Ver pedido
        </Fab>
      )}

      <DialogoCarrito abierto={verCarrito} items={carrito} onCambiar={cambiarCantidad}
        onCerrar={() => setVerCarrito(false)}
        onEnviado={() => { setCarrito([]); setVerCarrito(false); navigate('/mis-pedidos', { state: { enviado: true } }); }} />

      {dialogo && (
        <DialogoPlato plato={dialogo} categorias={categorias} onCerrar={() => setDialogo(null)}
          onGuardado={() => { setDialogo(null); cargar(); }} />
      )}
    </Box>
  );
}
