import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert, Box, Button, Card, CardContent, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControlLabel, Grid, IconButton, MenuItem, Stack, Switch, TextField, Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import api, { mensajeError } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { formatoPesos } from '../utils/formato';

const PLATO_VACIO = { categoriaId: '', nombre: '', descripcion: '', precio: '', disponible: true, imagenUrl: '' };

export default function Menu() {
  const { tieneRol } = useAuth();
  const esAdmin = tieneRol('ADMIN');
  const [categorias, setCategorias] = useState([]);
  const [platos, setPlatos] = useState([]);
  const [error, setError] = useState('');
  const [dialogo, setDialogo] = useState(null);
  const [nuevaCategoria, setNuevaCategoria] = useState('');

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

  const guardarPlato = async () => {
    try {
      const datos = { ...dialogo, precio: Number(dialogo.precio) };
      if (dialogo.id) await api.put(`/menu/platos/${dialogo.id}`, datos);
      else await api.post('/menu/platos', datos);
      setDialogo(null);
      cargar();
    } catch (err) {
      setError(mensajeError(err));
    }
  };

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
    <Box>
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
      {error && <Alert severity="error" onClose={() => setError('')} sx={{ mb: 2 }}>{error}</Alert>}

      {porCategoria.map((c) => (
        <Box key={c.id} sx={{ mb: 4 }}>
          <Typography variant="h6" color="secondary" fontWeight={700}>{c.nombre}</Typography>
          {c.descripcion && <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>{c.descripcion}</Typography>}
          <Grid container spacing={2}>
            {c.platos.map((p) => (
              <Grid item xs={12} sm={6} md={4} key={p.id}>
                <Card sx={{ height: '100%', opacity: p.disponible ? 1 : 0.55 }}>
                  <CardContent>
                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                      <Typography fontWeight={600}>{p.nombre}</Typography>
                      {esAdmin && (
                        <IconButton size="small" aria-label="Editar" onClick={() => setDialogo({ ...p })}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                      )}
                    </Stack>
                    <Typography variant="body2" color="text.secondary" sx={{ minHeight: 40 }}>{p.descripcion}</Typography>
                    <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mt: 1 }}>
                      <Typography fontWeight={700}>{formatoPesos(p.precio)}</Typography>
                      {!p.disponible && <Chip size="small" label="No disponible" />}
                    </Stack>
                  </CardContent>
                </Card>
              </Grid>
            ))}
            {c.platos.length === 0 && (
              <Grid item xs={12}><Typography variant="body2" color="text.secondary">Sin platos en esta categoría.</Typography></Grid>
            )}
          </Grid>
        </Box>
      ))}

      <Dialog open={Boolean(dialogo)} onClose={() => setDialogo(null)} fullWidth maxWidth="sm">
        <DialogTitle>{dialogo?.id ? 'Editar plato' : 'Nuevo plato'}</DialogTitle>
        {dialogo && (
          <DialogContent>
            <Stack spacing={2} sx={{ mt: 1 }}>
              <TextField select label="Categoría" value={dialogo.categoriaId}
                onChange={(e) => setDialogo({ ...dialogo, categoriaId: e.target.value })}>
                {categorias.map((c) => <MenuItem key={c.id} value={c.id}>{c.nombre}</MenuItem>)}
              </TextField>
              <TextField label="Nombre" required value={dialogo.nombre}
                onChange={(e) => setDialogo({ ...dialogo, nombre: e.target.value })} />
              <TextField label="Descripción" multiline minRows={2} value={dialogo.descripcion ?? ''}
                onChange={(e) => setDialogo({ ...dialogo, descripcion: e.target.value })} />
              <TextField label="Precio (COP)" type="number" required value={dialogo.precio}
                onChange={(e) => setDialogo({ ...dialogo, precio: e.target.value })} />
              <TextField label="URL de imagen" value={dialogo.imagenUrl ?? ''}
                onChange={(e) => setDialogo({ ...dialogo, imagenUrl: e.target.value })} />
              <FormControlLabel label="Disponible" control={
                <Switch checked={dialogo.disponible}
                  onChange={(e) => setDialogo({ ...dialogo, disponible: e.target.checked })} />} />
            </Stack>
          </DialogContent>
        )}
        <DialogActions>
          <Button onClick={() => setDialogo(null)}>Cancelar</Button>
          <Button variant="contained" onClick={guardarPlato}>Guardar</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
