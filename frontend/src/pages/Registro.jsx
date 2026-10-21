import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Link, Paper, Stack, TextField, Typography } from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { mensajeError } from '../services/api';

export default function Registro() {
  const { registrarse } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ nombre: '', email: '', password: '' });
  const [error, setError] = useState('');

  const enviar = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await registrarse(form);
      navigate('/reservas', { replace: true });
    } catch (err) {
      setError(mensajeError(err));
    }
  };

  const campo = (k) => ({ value: form[k], onChange: (e) => setForm({ ...form, [k]: e.target.value }) });

  return (
    <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 2 }}>
      <Paper sx={{ p: 4, width: '100%', maxWidth: 420 }}>
        <Typography variant="h5" fontWeight={700} sx={{ mb: 3 }}>Crear cuenta de cliente</Typography>
        <Stack component="form" spacing={2} onSubmit={enviar}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField label="Nombre completo" required {...campo('nombre')} />
          <TextField label="Email" type="email" required {...campo('email')} />
          <TextField label="Contraseña" type="password" required helperText="Mínimo 8 caracteres"
            inputProps={{ minLength: 8 }} {...campo('password')} />
          <Button type="submit" variant="contained" size="large">Registrarme</Button>
          <Typography variant="body2" textAlign="center">
            ¿Ya tienes cuenta? <Link component={RouterLink} to="/login">Ingresa</Link>
          </Typography>
        </Stack>
      </Paper>
    </Box>
  );
}
