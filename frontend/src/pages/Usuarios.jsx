import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, Chip, Grid, MenuItem, Paper, Switch, Table, TableBody, TableCell, TableHead,
  TableRow, TextField, Typography,
} from '@mui/material';
import api, { mensajeError } from '../services/api';

const VACIO = { nombre: '', email: '', password: '', rol: 'MESERO' };

export default function Usuarios() {
  const [usuarios, setUsuarios] = useState([]);
  const [form, setForm] = useState(VACIO);
  const [mensaje, setMensaje] = useState(null);

  const cargar = useCallback(() => {
    api.get('/usuarios').then((r) => setUsuarios(r.data))
      .catch((err) => setMensaje({ tipo: 'error', texto: mensajeError(err) }));
  }, []);

  useEffect(() => { cargar(); }, [cargar]);

  const crear = async (e) => {
    e.preventDefault();
    try {
      await api.post('/usuarios', form);
      setForm(VACIO);
      setMensaje({ tipo: 'success', texto: 'Usuario creado.' });
      cargar();
    } catch (err) {
      setMensaje({ tipo: 'error', texto: mensajeError(err) });
    }
  };

  const alternar = async (u) => {
    try {
      await api.patch(`/usuarios/${u.id}/activo`, { activo: !u.activo });
      cargar();
    } catch (err) {
      setMensaje({ tipo: 'error', texto: mensajeError(err) });
    }
  };

  const campo = (k) => ({ value: form[k], onChange: (e) => setForm({ ...form, [k]: e.target.value }) });

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} sx={{ mb: 3 }}>Usuarios</Typography>
      {mensaje && <Alert severity={mensaje.tipo} onClose={() => setMensaje(null)} sx={{ mb: 2 }}>{mensaje.texto}</Alert>}
      <Paper component="form" onSubmit={crear} sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>Crear usuario</Typography>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={3}><TextField fullWidth required label="Nombre" {...campo('nombre')} /></Grid>
          <Grid item xs={12} sm={3}><TextField fullWidth required type="email" label="Email" {...campo('email')} /></Grid>
          <Grid item xs={12} sm={2}>
            <TextField fullWidth required type="password" label="Contraseña" inputProps={{ minLength: 8 }} {...campo('password')} />
          </Grid>
          <Grid item xs={12} sm={2}>
            <TextField fullWidth select label="Rol" {...campo('rol')}>
              <MenuItem value="MESERO">Mesero</MenuItem>
              <MenuItem value="ADMIN">Administrador</MenuItem>
              <MenuItem value="CLIENTE">Cliente</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={12} sm={2}><Button fullWidth type="submit" variant="contained" sx={{ height: '100%' }}>Crear</Button></Grid>
        </Grid>
      </Paper>
      <Paper sx={{ p: 2, overflowX: 'auto' }}>
        <Table size="small">
          <TableHead>
            <TableRow><TableCell>Nombre</TableCell><TableCell>Email</TableCell><TableCell>Rol</TableCell><TableCell>Activo</TableCell></TableRow>
          </TableHead>
          <TableBody>
            {usuarios.map((u) => (
              <TableRow key={u.id}>
                <TableCell>{u.nombre}</TableCell>
                <TableCell>{u.email}</TableCell>
                <TableCell><Chip size="small" label={u.rol} /></TableCell>
                <TableCell><Switch checked={u.activo} onChange={() => alternar(u)} /></TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
}
