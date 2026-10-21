import { useState } from 'react';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Link, Paper, Stack, TextField, Typography } from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { mensajeError } from '../services/api';

export default function Login() {
  const { iniciarSesion } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();
    setError('');
    setEnviando(true);
    try {
      const u = await iniciarSesion(form.email, form.password);
      const inicio = u.rol === 'CLIENTE' ? '/reservas' : u.rol === 'MESERO' ? '/pedidos' : '/caja';
      navigate(location.state?.desde || inicio, { replace: true });
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', bgcolor: 'background.default', p: 2 }}>
      <Paper sx={{ p: 4, width: '100%', maxWidth: 400 }}>
        <Typography variant="h5" fontWeight={700} gutterBottom>Ingresar a SIGR</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Sistema Integral de Gestión de Restaurante
        </Typography>
        <Stack component="form" spacing={2} onSubmit={enviar}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField label="Email" type="email" required autoFocus value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <TextField label="Contraseña" type="password" required value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })} />
          <Button type="submit" variant="contained" size="large" disabled={enviando}>
            {enviando ? 'Ingresando…' : 'Ingresar'}
          </Button>
          <Typography variant="body2" textAlign="center">
            ¿Eres cliente nuevo? <Link component={RouterLink} to="/registro">Crea tu cuenta</Link>
          </Typography>
          <Typography variant="body2" textAlign="center">
            <Link component={RouterLink} to="/menu">Ver el menú sin ingresar</Link>
          </Typography>
        </Stack>
      </Paper>
    </Box>
  );
}
